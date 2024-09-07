/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect;

import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.common.FetchClauseType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.Summarization;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.UnionTableReference;
import org.hibernate.sql.ast.tree.insert.ConflictClause;
import org.hibernate.sql.ast.tree.insert.InsertSelectStatement;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.QueryGroup;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.ast.tree.select.SortSpecification;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.model.internal.OptionalTableUpdate;
import org.hibernate.type.SqlTypes;

/**
 * A SQL AST translator for SQL Server.
 *
 * @author Christian Beikov
 */
public class SQLServerSqlAstTranslator<T extends JdbcOperation> extends SqlAstTranslatorWithMerge<T> {

	private static final String UNION_ALL = " union all ";

	private Predicate lateralPredicate;

	public SQLServerSqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
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
			// A merge statement must end with a `;` on SQL Server
			appendSql( ';' );
		}
	}

	@Override
	protected void renderDeleteClause(DeleteStatement statement) {
		appendSql( "delete" );
		final Stack<Clause> clauseStack = getClauseStack();
		try {
			clauseStack.push( Clause.DELETE );
			renderTableReferenceIdentificationVariable( statement.getTargetTable() );
			if ( statement.getFromClause().getRoots().isEmpty() ) {
				appendSql( " from " );
				renderDmlTargetTableExpression( statement.getTargetTable() );
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
		appendSql( "update" );
		final Stack<Clause> clauseStack = getClauseStack();
		try {
			clauseStack.push( Clause.UPDATE );
			renderTableReferenceIdentificationVariable( updateStatement.getTargetTable() );
		}
		finally {
			clauseStack.pop();
		}
	}

	@Override
	protected void renderDmlTargetTableExpression(NamedTableReference tableReference) {
		super.renderDmlTargetTableExpression( tableReference );
		if ( getClauseStack().getCurrent() != Clause.INSERT ) {
			renderTableReferenceIdentificationVariable( tableReference );
		}
	}

	@Override
	protected boolean supportsJoinsInDelete() {
		return true;
	}

	@Override
	protected void renderFromClauseAfterUpdateSet(UpdateStatement statement) {
		if ( statement.getFromClause().getRoots().isEmpty() ) {
			appendSql( " from " );
			renderDmlTargetTableExpression( statement.getTargetTable() );
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

	@Override
	protected boolean needsRecursiveKeywordInWithClause() {
		return false;
	}

	@Override
	protected boolean supportsWithClauseInSubquery() {
		return false;
	}

	@Override
	protected void renderTableGroupJoin(TableGroupJoin tableGroupJoin, List<TableGroupJoin> tableGroupJoinCollector) {
		appendSql( WHITESPACE );
		if ( tableGroupJoin.getJoinedGroup().isLateral() ) {
			if ( tableGroupJoin.getJoinType() == SqlAstJoinType.LEFT ) {
				appendSql( "outer apply " );
			}
			else {
				appendSql( "cross apply " );
			}
		}
		else {
			appendSql( tableGroupJoin.getJoinType().getText() );
			appendSql( "join " );
		}

		final Predicate predicate = tableGroupJoin.getPredicate();
		if ( predicate != null && !predicate.isEmpty() ) {
			if ( tableGroupJoin.getJoinedGroup().isLateral() ) {
				// We have to inject the lateral predicate into the sub-query
				final Predicate lateralPredicate = this.lateralPredicate;
				this.lateralPredicate = predicate;
				renderTableGroup( tableGroupJoin.getJoinedGroup(), null, tableGroupJoinCollector );
				this.lateralPredicate = lateralPredicate;
			}
			else {
				renderTableGroup( tableGroupJoin.getJoinedGroup(), predicate, tableGroupJoinCollector );
			}
		}
		else {
			renderTableGroup( tableGroupJoin.getJoinedGroup(), null, tableGroupJoinCollector );
		}
	}

	protected boolean renderPrimaryTableReference(TableGroup tableGroup, LockMode lockMode) {
		if ( shouldInlineCte( tableGroup ) ) {
			inlineCteTableGroup( tableGroup, lockMode );
			return false;
		}
		final TableReference tableReference = tableGroup.getPrimaryTableReference();
		if ( tableReference instanceof NamedTableReference ) {
			return renderNamedTableReference( (NamedTableReference) tableReference, lockMode );
		}
		tableReference.accept( this );
		return false;
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
			case PESSIMISTIC_WRITE:
			case WRITE: {
				switch ( effectiveLockTimeout ) {
					case LockOptions.SKIP_LOCKED:
						appendSql( " with (updlock,rowlock,readpast)" );
						break;
					case LockOptions.NO_WAIT:
						appendSql( " with (updlock,holdlock,rowlock,nowait)" );
						break;
					default:
						appendSql( " with (updlock,holdlock,rowlock)" );
						break;
				}
				break;
			}
			case PESSIMISTIC_READ: {
				switch ( effectiveLockTimeout ) {
					case LockOptions.SKIP_LOCKED:
						appendSql( " with (updlock,rowlock,readpast)" );
						break;
					case LockOptions.NO_WAIT:
						appendSql( " with (holdlock,rowlock,nowait)" );
						break;
					default:
						appendSql( " with (holdlock,rowlock)" );
						break;
				}
				break;
			}
			case UPGRADE_SKIPLOCKED: {
				if ( effectiveLockTimeout == LockOptions.NO_WAIT ) {
					appendSql( " with (updlock,rowlock,readpast,nowait)" );
				}
				else {
					appendSql( " with (updlock,rowlock,readpast)" );
				}
				break;
			}
			case UPGRADE_NOWAIT: {
				appendSql( " with (updlock,holdlock,rowlock,nowait)" );
				break;
			}
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
		// SQL Server does not support the FOR UPDATE clause
	}

	protected OffsetFetchClauseMode getOffsetFetchClauseMode(QueryPart queryPart) {
		final boolean hasLimit;
		final boolean hasOffset;
		if ( queryPart.isRoot() && hasLimit() ) {
			hasLimit = getLimit().getMaxRows() != null;
			hasOffset = getLimit().getFirstRow() != null;
		}
		else {
			hasLimit = queryPart.getFetchClauseExpression() != null;
			hasOffset = queryPart.getOffsetClauseExpression() != null;
		}
		if ( queryPart instanceof QueryGroup ) {
			// We can't use TOP for set operations
			if ( hasOffset || hasLimit ) {
				if ( !isRowsOnlyFetchClauseType( queryPart ) ) {
					return OffsetFetchClauseMode.EMULATED;
				}
				else {
					return OffsetFetchClauseMode.STANDARD;
				}
			}

			return null;
		}
		else {
			if ( !hasOffset ) {
				return hasLimit ? OffsetFetchClauseMode.TOP_ONLY : null;
			}
			else if ( !isRowsOnlyFetchClauseType( queryPart ) ) {
				return OffsetFetchClauseMode.EMULATED;
			}
			else if ( !queryPart.hasSortSpecifications() && ((QuerySpec) queryPart).getSelectClause().isDistinct() ) {
				// order by (select 0) workaround for offset / fetch does not work when query is distinct
				return OffsetFetchClauseMode.EMULATED;
			}
			else {
				return OffsetFetchClauseMode.STANDARD;
			}
		}
	}

	@Override
	protected boolean supportsSimpleQueryGrouping() {
		// SQL Server is quite strict i.e. it requires `select .. union all select * from (select ...)`
		// rather than `select .. union all (select ...)` because parenthesis followed by select
		// is always treated as a subquery, which is not supported in a set operation
		return false;
	}

	protected boolean shouldEmulateFetchClause(QueryPart queryPart) {
		// Check if current query part is already row numbering to avoid infinite recursion
		return getQueryPartForRowNumbering() != queryPart && getOffsetFetchClauseMode( queryPart ) == OffsetFetchClauseMode.EMULATED;
	}

	@Override
	public void visitQueryGroup(QueryGroup queryGroup) {
		final Predicate lateralPredicate = this.lateralPredicate;
		if ( lateralPredicate != null ) {
			this.lateralPredicate = null;
			addAdditionalWherePredicate( lateralPredicate );
		}
		if ( shouldEmulateFetchClause( queryGroup ) ) {
			emulateFetchOffsetWithWindowFunctions( queryGroup, !isRowsOnlyFetchClauseType( queryGroup ) );
		}
		else {
			super.visitQueryGroup( queryGroup );
		}
	}

	@Override
	public void visitQuerySpec(QuerySpec querySpec) {
		if ( shouldEmulateFetchClause( querySpec ) ) {
			emulateFetchOffsetWithWindowFunctions( querySpec, !isRowsOnlyFetchClauseType( querySpec ) );
		}
		else {
			super.visitQuerySpec( querySpec );
		}
	}

	@Override
	public void visitSelectClause(SelectClause selectClause) {
		if ( lateralPredicate != null ) {
			addAdditionalWherePredicate( lateralPredicate );
			lateralPredicate = null;
		}
		super.visitSelectClause( selectClause );
	}

	@Override
	protected void visitSqlSelections(SelectClause selectClause) {
		final QuerySpec querySpec = (QuerySpec) getQueryPartStack().getCurrent();
		final OffsetFetchClauseMode offsetFetchClauseMode = getOffsetFetchClauseMode( querySpec );
		if ( offsetFetchClauseMode == OffsetFetchClauseMode.TOP_ONLY ) {
			renderTopClause( querySpec, true, true );
		}
		else if ( offsetFetchClauseMode == OffsetFetchClauseMode.EMULATED ) {
			renderTopClause( querySpec, isRowsOnlyFetchClauseType( querySpec ), true );
		}
		else if ( getQueryPartStack().depth() > 1 && querySpec.hasSortSpecifications()
				&& getQueryPartStack().peek( 1 ) instanceof QueryGroup ) {
			// If the current query spec has a query group parent, no offset/fetch clause, but an order by clause,
			// then we must render "top 100 percent" as that is needed for the SQL to be valid
			appendSql( "top 100 percent " );
		}
		super.visitSqlSelections( selectClause );
	}

	@Override
	protected void renderOrderBy(boolean addWhitespace, List<SortSpecification> sortSpecifications) {
		if ( sortSpecifications != null && !sortSpecifications.isEmpty() ) {
			super.renderOrderBy( addWhitespace, sortSpecifications );
		}
		else if ( getClauseStack().getCurrent() == Clause.OVER ) {
			if ( addWhitespace ) {
				appendSql( ' ' );
			}
			renderEmptyOrderBy();
		}
	}

	protected void renderEmptyOrderBy() {
		// Always need an order by clause: https://blog.jooq.org/2014/05/13/sql-server-trick-circumvent-missing-order-by-clause/
		appendSql( "order by (select 0)" );
	}

	@Override
	public void visitOffsetFetchClause(QueryPart queryPart) {
		if ( !isRowNumberingCurrentQueryPart() ) {
			final OffsetFetchClauseMode offsetFetchClauseMode = getOffsetFetchClauseMode( queryPart );
			if ( offsetFetchClauseMode == OffsetFetchClauseMode.STANDARD ) {
				if ( !queryPart.hasSortSpecifications() ) {
					appendSql( ' ' );
					renderEmptyOrderBy();
				}
				final Expression offsetExpression;
				final Expression fetchExpression;
				final FetchClauseType fetchClauseType;
				if ( queryPart.isRoot() && hasLimit() ) {
					prepareLimitOffsetParameters();
					offsetExpression = getOffsetParameter();
					fetchExpression = getLimitParameter();
					fetchClauseType = FetchClauseType.ROWS_ONLY;
				}
				else {
					offsetExpression = queryPart.getOffsetClauseExpression();
					fetchExpression = queryPart.getFetchClauseExpression();
					fetchClauseType = queryPart.getFetchClauseType();
				}
				if ( offsetExpression == null ) {
					appendSql( " offset 0 rows" );
				}
				else {
					renderOffset( offsetExpression, true );
				}

				if ( fetchExpression != null ) {
					renderFetch( fetchExpression, null, fetchClauseType );
				}
			}
		}
	}

	@Override
	protected void renderComparison(Expression lhs, ComparisonOperator operator, Expression rhs) {
		final JdbcMappingContainer lhsExpressionType = lhs.getExpressionType();
		if ( lhsExpressionType != null && lhsExpressionType.getJdbcTypeCount() == 1
				&& lhsExpressionType.getSingleJdbcMapping().getJdbcType().getDdlTypeCode() == SqlTypes.SQLXML ) {
			// In SQL Server, XMLTYPE is not "comparable", so we have to cast the two parts to varchar for this purpose
			switch ( operator ) {
				case EQUAL:
				case NOT_DISTINCT_FROM:
				case NOT_EQUAL:
				case DISTINCT_FROM:
					appendSql( "cast(" );
					lhs.accept( this );
					appendSql( " as nvarchar(max))" );
					appendSql( operator.sqlText() );
					appendSql( "cast(" );
					rhs.accept( this );
					appendSql( " as nvarchar(max))" );
					return;
				default:
					// Fall through
					break;
			}
		}
		renderComparisonEmulateIntersect( lhs, operator, rhs );
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
			appendSql( "()" );
		}
		else if ( expression instanceof Summarization ) {
			Summarization summarization = (Summarization) expression;
			renderCommaSeparated( summarization.getGroupings() );
			appendSql( " with " );
			appendSql( summarization.getKind().sqlText() );
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
	protected boolean supportsRowValueConstructorSyntax() {
		return false;
	}

	@Override
	protected boolean supportsRowValueConstructorSyntaxInInList() {
		return false;
	}

	@Override
	protected boolean supportsRowValueConstructorSyntaxInQuantifiedPredicates() {
		return false;
	}

	enum OffsetFetchClauseMode {
		STANDARD,
		TOP_ONLY,
		EMULATED;
	}

	protected void renderMergeStatement(OptionalTableUpdate optionalTableUpdate) {
		super.renderMergeStatement( optionalTableUpdate );
		appendSql( ";" );
	}

	@Override
	protected void renderStringContainsExactlyPredicate(Expression haystack, Expression needle) {
		// SQL Server ignores NUL characters in string on case-insensitive collations, so we force a binary collation.
		// This is needed for the emulation of cycle detection in recursive queries
		appendSql( "charindex(" );
		needle.accept( this );
		appendSql( " collate Latin1_General_100_BIN2," );
		haystack.accept( this );
		append( ")>0" );
	}
}
