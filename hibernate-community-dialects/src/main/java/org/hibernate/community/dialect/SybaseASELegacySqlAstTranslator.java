/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.dialect.DmlTargetColumnQualifierSupport;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.MutationStatement;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.tree.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.tree.expression.CaseSimpleExpression;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.Summarization;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.UnionTableReference;
import org.hibernate.sql.ast.tree.from.ValuesTableReference;
import org.hibernate.sql.ast.tree.insert.ConflictClause;
import org.hibernate.sql.ast.tree.insert.InsertSelectStatement;
import org.hibernate.sql.ast.tree.insert.Values;
import org.hibernate.sql.ast.tree.predicate.BooleanExpressionPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.QueryGroup;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.exec.spi.JdbcOperation;

import static org.hibernate.dialect.SybaseASESqlAstTranslator.isLob;

/**
 * A SQL AST translator for Sybase ASE.
 *
 * @author Christian Beikov
 */
public class SybaseASELegacySqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {

	private static final String UNION_ALL = " union all ";

	public SybaseASELegacySqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
		super( sessionFactory, statement );
	}

	@Override
	protected void visitInsertStatementOnly(InsertSelectStatement statement) {
		if ( statement.getConflictClause() == null || statement.getConflictClause().isDoNothing() ) {
			// Render plain insert statement and possibly run into unique constraint violation
			super.visitInsertStatementOnly( statement );
		}
		else {
			visitInsertStatementEmulateMerge( statement );
		}
	}

	@Override
	protected void renderDeleteClause(DeleteStatement statement) {
		appendSql( "delete " );
		final Stack<Clause> clauseStack = getClauseStack();
		try {
			clauseStack.push( Clause.DELETE );
			renderDmlTargetTableExpression( statement.getTargetTable() );
			if ( statement.getFromClause().getRoots().isEmpty() ) {
				appendSql( " from " );
				renderDmlTargetTableExpression( statement.getTargetTable() );
				renderTableReferenceIdentificationVariable( statement.getTargetTable() );
			}
			else {
				visitFromClause( statement.getFromClause() );
			}
		}
		finally {
			clauseStack.pop();
		}
	}

	@Override
	protected void renderUpdateClause(UpdateStatement updateStatement) {
		appendSql( "update " );
		final Stack<Clause> clauseStack = getClauseStack();
		try {
			clauseStack.push( Clause.UPDATE );
			renderDmlTargetTableExpression( updateStatement.getTargetTable() );
		}
		finally {
			clauseStack.pop();
		}
	}

	@Override
	protected void renderFromClauseAfterUpdateSet(UpdateStatement statement) {
		if ( statement.getFromClause().getRoots().isEmpty() ) {
			appendSql( " from " );
			renderDmlTargetTableExpression( statement.getTargetTable() );
			renderTableReferenceIdentificationVariable( statement.getTargetTable() );
		}
		else {
			visitFromClause( statement.getFromClause() );
		}
	}

	@Override
	protected void visitConflictClause(ConflictClause conflictClause) {
		if ( conflictClause != null ) {
			if ( conflictClause.isDoUpdate() && conflictClause.getConstraintName() != null ) {
				throw new IllegalQueryOperationException( "Insert conflict 'do update' clause with constraint name is not supported" );
			}
		}
	}

	// Sybase ASE does not allow CASE expressions where all result arms contain plain parameters.
	// At least one result arm must provide some type context for inference,
	// so we cast the first result arm if we encounter this condition

	@Override
	protected void visitAnsiCaseSearchedExpression(
			CaseSearchedExpression caseSearchedExpression,
			Consumer<Expression> resultRenderer) {
		if ( getParameterRenderingMode() == SqlAstNodeRenderingMode.DEFAULT && areAllResultsParameters( caseSearchedExpression ) ) {
			final List<CaseSearchedExpression.WhenFragment> whenFragments = caseSearchedExpression.getWhenFragments();
			final Expression firstResult = whenFragments.get( 0 ).getResult();
			super.visitAnsiCaseSearchedExpression(
					caseSearchedExpression,
					e -> {
						if ( e == firstResult ) {
							renderCasted( e );
						}
						else {
							resultRenderer.accept( e );
						}
					}
			);
		}
		else {
			super.visitAnsiCaseSearchedExpression( caseSearchedExpression, resultRenderer );
		}
	}

	@Override
	protected void visitAnsiCaseSimpleExpression(
			CaseSimpleExpression caseSimpleExpression,
			Consumer<Expression> resultRenderer) {
		if ( getParameterRenderingMode() == SqlAstNodeRenderingMode.DEFAULT && areAllResultsParameters( caseSimpleExpression ) ) {
			final List<CaseSimpleExpression.WhenFragment> whenFragments = caseSimpleExpression.getWhenFragments();
			final Expression firstResult = whenFragments.get( 0 ).getResult();
			super.visitAnsiCaseSimpleExpression(
					caseSimpleExpression,
					e -> {
						if ( e == firstResult ) {
							renderCasted( e );
						}
						else {
							resultRenderer.accept( e );
						}
					}
			);
		}
		else {
			super.visitAnsiCaseSimpleExpression( caseSimpleExpression, resultRenderer );
		}
	}

	@Override
	protected boolean renderNamedTableReference(NamedTableReference tableReference, LockMode lockMode) {
		final String tableExpression = tableReference.getTableExpression();
		if ( tableReference instanceof UnionTableReference && lockMode != LockMode.NONE && tableExpression.charAt( 0 ) == '(' ) {
			// SQL Server requires to push down the lock hint to the actual table names
			int searchIndex = 0;
			int unionIndex;
			while ( ( unionIndex = tableExpression.indexOf( UNION_ALL, searchIndex ) ) != -1 ) {
				append( tableExpression, searchIndex, unionIndex );
				renderLockHint( lockMode );
				appendSql( UNION_ALL );
				searchIndex = unionIndex + UNION_ALL.length();
			}
			append( tableExpression, searchIndex, tableExpression.length() - 1 );
			renderLockHint( lockMode );
			appendSql( " )" );

			registerAffectedTable( tableReference );
			renderTableReferenceIdentificationVariable( tableReference );
		}
		else {
			super.renderNamedTableReference( tableReference, lockMode );
			renderLockHint( lockMode );
		}
		// Just always return true because SQL Server doesn't support the FOR UPDATE clause
		return true;
	}

	private void renderLockHint(LockMode lockMode) {
		final int effectiveLockTimeout = getEffectiveLockTimeout( lockMode );
		switch ( lockMode ) {
			case PESSIMISTIC_READ:
			case PESSIMISTIC_WRITE:
			case WRITE: {
				switch ( effectiveLockTimeout ) {
					case LockOptions.SKIP_LOCKED:
						appendSql( " holdlock readpast" );
						break;
					default:
						appendSql( " holdlock" );
						break;
				}
				break;
			}
			case UPGRADE_SKIPLOCKED: {
				appendSql( " holdlock readpast" );
				break;
			}
			case UPGRADE_NOWAIT: {
				appendSql( " holdlock" );
				break;
			}
		}
	}

	@Override
	protected void renderTableGroupJoin(TableGroupJoin tableGroupJoin, List<TableGroupJoin> tableGroupJoinCollector) {
		appendSql( WHITESPACE );
		if ( tableGroupJoin.getJoinType() != SqlAstJoinType.CROSS ) {
			// No support for cross joins, so we emulate it with an inner join and always true on condition
			appendSql( tableGroupJoin.getJoinType().getText() );
		}
		appendSql( "join " );

		final Predicate predicate;
		if ( tableGroupJoin.getPredicate() == null ) {
			predicate = new BooleanExpressionPredicate( new QueryLiteral<>( true, getBooleanType() ) );
		}
		else {
			predicate = tableGroupJoin.getPredicate();
		}
		if ( predicate != null && !predicate.isEmpty() ) {
			renderTableGroup( tableGroupJoin.getJoinedGroup(), predicate, tableGroupJoinCollector );
		}
		else {
			renderTableGroup( tableGroupJoin.getJoinedGroup(), null, tableGroupJoinCollector );
		}
	}

	@Override
	protected LockStrategy determineLockingStrategy(
			QuerySpec querySpec,
			ForUpdateClause forUpdateClause,
			Boolean followOnLocking) {
		// No need for follow on locking
		return LockStrategy.CLAUSE;
	}

	@Override
	protected void renderForUpdateClause(QuerySpec querySpec, ForUpdateClause forUpdateClause) {
		// Sybase ASE does not really support the FOR UPDATE clause
	}

	@Override
	protected void visitSqlSelections(SelectClause selectClause) {
		if ( supportsTopClause() ) {
			renderTopClause( (QuerySpec) getQueryPartStack().getCurrent(), true, false );
		}
		super.visitSqlSelections( selectClause );
	}

	@Override
	protected void renderFetchPlusOffsetExpression(
			Expression fetchClauseExpression,
			Expression offsetClauseExpression,
			int offset) {
		renderFetchPlusOffsetExpressionAsLiteral( fetchClauseExpression, offsetClauseExpression, offset );
	}

	@Override
	public void visitQueryGroup(QueryGroup queryGroup) {
		if ( queryGroup.hasSortSpecifications() || queryGroup.hasOffsetOrFetchClause() ) {
			appendSql( "select " );
			renderTopClause(
					queryGroup.getOffsetClauseExpression(),
					queryGroup.getFetchClauseExpression(),
					queryGroup.getFetchClauseType(),
					true,
					false
			);
			appendSql( "* from (" );
			renderQueryGroup( queryGroup, false );
			appendSql( ") grp_(c0" );
			// Sybase doesn't have implicit names for non-column select expressions, so we need to assign names
			final int itemCount = queryGroup.getFirstQuerySpec().getSelectClause().getSqlSelections().size();
			for (int i = 1; i < itemCount; i++) {
				appendSql( ",c" );
				appendSql( i );
			}
			appendSql( ')' );
			visitOrderBy( queryGroup.getSortSpecifications() );
		}
		else {
			super.visitQueryGroup( queryGroup );
		}
	}

	@Override
	protected void visitValuesList(List<Values> valuesList) {
		visitValuesListEmulateSelectUnion( valuesList );
	}

	@Override
	public void visitValuesTableReference(ValuesTableReference tableReference) {
		append( '(' );
		visitValuesListEmulateSelectUnion( tableReference.getValuesList() );
		append( ')' );
		renderDerivedTableReferenceIdentificationVariable( tableReference );
	}

	@Override
	public void visitOffsetFetchClause(QueryPart queryPart) {
		assertRowsOnlyFetchClauseType( queryPart );
		if ( !queryPart.isRoot() && queryPart.hasOffsetOrFetchClause() ) {
			if ( queryPart.getFetchClauseExpression() != null && !supportsTopClause() || queryPart.getOffsetClauseExpression() != null ) {
				throw new IllegalArgumentException( "Can't emulate offset fetch clause in subquery" );
			}
		}
	}

	@Override
	protected void renderFetchExpression(Expression fetchExpression) {
		if ( supportsParameterOffsetFetchExpression() ) {
			super.renderFetchExpression( fetchExpression );
		}
		else {
			renderExpressionAsLiteral( fetchExpression, getJdbcParameterBindings() );
		}
	}

	@Override
	protected void renderOffsetExpression(Expression offsetExpression) {
		if ( supportsParameterOffsetFetchExpression() ) {
			super.renderOffsetExpression( offsetExpression );
		}
		else {
			renderExpressionAsLiteral( offsetExpression, getJdbcParameterBindings() );
		}
	}

	@Override
	protected void renderComparison(Expression lhs, ComparisonOperator operator, Expression rhs) {
		// In Sybase ASE, XMLTYPE is not "comparable", so we have to cast the two parts to varchar for this purpose
		final boolean isLob = isLob( lhs.getExpressionType() );
		if ( isLob ) {
			switch ( operator ) {
				case EQUAL:
					lhs.accept( this );
					appendSql( " like " );
					rhs.accept( this );
					return;
				case NOT_EQUAL:
					lhs.accept( this );
					appendSql( " not like " );
					rhs.accept( this );
					return;
				default:
					// Fall through
					break;
			}
		}
		// I think intersect is only supported in 16.0 SP3
		if ( ( (SybaseASELegacyDialect) getDialect() ).isAnsiNullOn() ) {
			if ( isLob ) {
				switch ( operator ) {
					case DISTINCT_FROM:
						appendSql( "case when " );
						lhs.accept( this );
						appendSql( " like " );
						rhs.accept( this );
						appendSql( " or " );
						lhs.accept( this );
						appendSql( " is null and " );
						rhs.accept( this );
						appendSql( " is null then 0 else 1 end=1" );
						return;
					case NOT_DISTINCT_FROM:
						appendSql( "case when " );
						lhs.accept( this );
						appendSql( " like " );
						rhs.accept( this );
						appendSql( " or " );
						lhs.accept( this );
						appendSql( " is null and " );
						rhs.accept( this );
						appendSql( " is null then 0 else 1 end=0" );
						return;
					default:
						// Fall through
						break;
				}
			}
			if ( getDialect().supportsDistinctFromPredicate() ) {
				renderComparisonEmulateIntersect( lhs, operator, rhs );
			}
			else {
				renderComparisonEmulateCase( lhs, operator, rhs );
			}
		}
		else {
			// The ansinull setting only matters if using a parameter or literal and the eq operator according to the docs
			// http://infocenter.sybase.com/help/index.jsp?topic=/com.sybase.infocenter.dc32300.1570/html/sqlug/sqlug89.htm
			boolean rhsNotNullPredicate =
					lhs instanceof Literal
					|| isParameter( lhs );
			boolean lhsNotNullPredicate =
					rhs instanceof Literal
					|| isParameter( rhs );
			if ( rhsNotNullPredicate || lhsNotNullPredicate ) {
				lhs.accept( this );
				switch ( operator ) {
					case DISTINCT_FROM:
						if ( isLob ) {
							appendSql( " not like " );
						}
						else {
							appendSql( "<>" );
						}
						break;
					case NOT_DISTINCT_FROM:
						if ( isLob ) {
							appendSql( " like " );
						}
						else {
							appendSql( '=' );
						}
						break;
					case LESS_THAN:
					case GREATER_THAN:
					case LESS_THAN_OR_EQUAL:
					case GREATER_THAN_OR_EQUAL:
						// These operators are not affected by ansinull=off
						lhsNotNullPredicate = false;
						rhsNotNullPredicate = false;
					default:
						appendSql( operator.sqlText() );
						break;
				}
				rhs.accept( this );
				if ( lhsNotNullPredicate ) {
					appendSql( " and " );
					lhs.accept( this );
					appendSql( " is not null" );
				}
				if ( rhsNotNullPredicate ) {
					appendSql( " and " );
					rhs.accept( this );
					appendSql( " is not null" );
				}
			}
			else {
				if ( getDialect().supportsDistinctFromPredicate() ) {
					renderComparisonEmulateIntersect( lhs, operator, rhs );
				}
				else {
					renderComparisonEmulateCase( lhs, operator, rhs );
				}
			}
		}
	}

	@Override
	protected void renderSelectTupleComparison(
			List<SqlSelection> lhsExpressions,
			SqlTuple tuple,
			ComparisonOperator operator) {
		emulateSelectTupleComparison( lhsExpressions, tuple.getExpressions(), operator, true );
	}

	@Override
	protected void renderPartitionItem(Expression expression) {
		if ( expression instanceof Literal ) {
			// Note that this depends on the SqmToSqlAstConverter to add a dummy table group
			appendSql( "dummy_.x" );
		}
		else if ( expression instanceof Summarization ) {
			// This could theoretically be emulated by rendering all grouping variations of the query and
			// connect them via union all but that's probably pretty inefficient and would have to happen
			// on the query spec level
			throw new UnsupportedOperationException( "Summarization is not supported by DBMS" );
		}
		else {
			expression.accept( this );
		}
	}

	@Override
	public void visitBinaryArithmeticExpression(BinaryArithmeticExpression arithmeticExpression) {
		appendSql( OPEN_PARENTHESIS );
		visitArithmeticOperand( arithmeticExpression.getLeftHandOperand() );
		appendSql( arithmeticExpression.getOperator().getOperatorSqlTextString() );
		visitArithmeticOperand( arithmeticExpression.getRightHandOperand() );
		appendSql( CLOSE_PARENTHESIS );
	}

	@Override
	protected String determineColumnReferenceQualifier(ColumnReference columnReference) {
		final DmlTargetColumnQualifierSupport qualifierSupport = getDialect().getDmlTargetColumnQualifierSupport();
		final MutationStatement currentDmlStatement;
		final String dmlAlias;
		if ( qualifierSupport == DmlTargetColumnQualifierSupport.TABLE_ALIAS
				|| ( currentDmlStatement = getCurrentDmlStatement() ) == null
				|| ( dmlAlias = currentDmlStatement.getTargetTable().getIdentificationVariable() ) == null
				|| !dmlAlias.equals( columnReference.getQualifier() ) ) {
			return columnReference.getQualifier();
		}
		// Sybase needs a table name prefix
		// but not if this is a restricted union table reference subquery
		final QuerySpec currentQuerySpec = (QuerySpec) getQueryPartStack().getCurrent();
		final List<TableGroup> roots;
		if ( currentQuerySpec != null && !currentQuerySpec.isRoot()
				&& (roots = currentQuerySpec.getFromClause().getRoots()).size() == 1
				&& roots.get( 0 ).getPrimaryTableReference() instanceof UnionTableReference ) {
			return columnReference.getQualifier();
		}
		else if ( columnReference.isColumnExpressionFormula() ) {
			// For formulas, we have to replace the qualifier as the alias was already rendered into the formula
			// This is fine for now as this is only temporary anyway until we render aliases for table references
			return null;
		}
		else {
			return getCurrentDmlStatement().getTargetTable().getTableExpression();
		}
	}

	@Override
	protected boolean needsRowsToSkip() {
		return true;
	}

	@Override
	protected boolean needsMaxRows() {
		return !supportsTopClause();
	}

	private boolean supportsTopClause() {
		return getDialect().getVersion().isSameOrAfter( 12, 5 );
	}

	private boolean supportsParameterOffsetFetchExpression() {
		return false;
	}
}
