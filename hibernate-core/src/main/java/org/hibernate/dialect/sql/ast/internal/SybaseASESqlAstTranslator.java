/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.internal;

import java.util.ArrayDeque;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.Locking;
import org.hibernate.dialect.sql.ast.spi.DmlTargetColumnQualifierSupport;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.dialect.sql.ast.spi.DerivedTableRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.InsertConflictRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.PaginationRenderingPlan;
import org.hibernate.dialect.sql.ast.spi.PaginationRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.PredicateSupport.Capability;
import org.hibernate.dialect.sql.ast.spi.StandardDerivedTableRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardInsertConflictRenderingSupport;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.sql.ast.spi.translation.SqlAstNodeRenderingMode;
import org.hibernate.dialect.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.FullJoinEmulation;
import org.hibernate.sql.ast.spi.query.select.SqlSelection;
import org.hibernate.sql.ast.spi.query.MutationStatement;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.sql.ast.spi.query.delete.DeleteStatement;
import org.hibernate.sql.ast.spi.query.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.spi.query.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.spi.query.expression.CaseSimpleExpression;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.Literal;
import org.hibernate.sql.ast.spi.query.expression.SqlTuple;
import org.hibernate.sql.ast.spi.query.expression.Summarization;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.query.from.UnionTableReference;
import org.hibernate.sql.ast.spi.query.select.QueryGroup;
import org.hibernate.sql.ast.spi.query.select.QueryPart;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.ast.spi.query.select.SelectClause;
import org.hibernate.sql.ast.spi.query.select.SortSpecification;
import org.hibernate.sql.ast.spi.query.update.UpdateStatement;
import org.hibernate.sql.exec.spi.JdbcOperation;


/**
 * A SQL AST translator for Sybase ASE.
 *
 * @author Christian Beikov
 */
public class SybaseASESqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {

	private final ArrayDeque<FullJoinEmulation> fullJoinEmulations = new ArrayDeque<>();

	public SybaseASESqlAstTranslator(SqlAstTranslationRequest<? extends Statement, T> request) {
		super( request );
		this.fullJoinEmulations.push( new FullJoinEmulation( this ) );
	}

	private FullJoinEmulation currentFullJoinEmulationHelper() {
		return fullJoinEmulations.getFirst();
	}

	@Override
	protected InsertConflictRenderingSupport getInsertConflictRenderingSupport() {
		return StandardInsertConflictRenderingSupport.MERGE;
	}

	@Override
	protected void renderDeleteClause(DeleteStatement statement) {
		appendSql( "delete " );
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
	protected LockStrategy determineLockingStrategy(
			QuerySpec querySpec,
			Locking.FollowOn followOnStrategy) {
		// No need for follow on locking
		return LockStrategy.CLAUSE;
	}

	@Override
	protected PaginationRenderingSupport getPaginationRenderingSupport() {
		return request -> currentFullJoinEmulationHelper().isFullJoinEmulationQueryPart( request.queryPart() )
				? new PaginationRenderingPlan.None()
				: new PaginationRenderingPlan.Top( true, false );
	}

	@Override
	protected void renderFetchPlusOffsetExpression(
			Expression fetchClauseExpression,
			Expression offsetClauseExpression,
			int offset) {
		renderFetchPlusOffsetExpressionAsLiteral( fetchClauseExpression, offsetClauseExpression, offset );
	}

	@Override
	public void visitQuerySpec(QuerySpec querySpec) {
		final var helper = currentFullJoinEmulationHelper();
		final boolean needsNestedHelper =
				helper.hasActiveFullJoinEmulation()
						&& !helper.isFullJoinEmulationQueryPart( querySpec );
		if ( needsNestedHelper ) {
			fullJoinEmulations.push( new FullJoinEmulation( this ) );
		}
		try {
			final var currentHelper = currentFullJoinEmulationHelper();
			if ( !currentHelper.renderFullJoinEmulationBranchIfNeeded( querySpec, super::visitQuerySpec )
					&& !currentHelper.emulateFullJoinWithUnionIfNeeded( querySpec ) ) {
				super.visitQuerySpec( querySpec );
			}
		}
		finally {
			if ( needsNestedHelper ) {
				fullJoinEmulations.pop();
			}
		}
	}

	@Override
	protected void renderSelectClause(SelectClause selectClause) {
		if ( !currentFullJoinEmulationHelper().renderSelectClauseIfNeeded( selectClause ) ) {
			super.renderSelectClause( selectClause );
		}
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
			renderQueryGroupWithoutOrderByAndOffsetFetch( queryGroup );
			appendSql( ") grp_(c0" );
			// Sybase doesn't have implicit names for non-column select expressions, so we need to assign names
			final int itemCount = assignNamesToSelectItems( queryGroup );
			for ( int i = 1; i < itemCount; i++ ) {
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

	private int assignNamesToSelectItems(QueryGroup queryGroup) {
		int itemCount =
				currentFullJoinEmulationHelper()
						.countRenderedSelectItemsIncludingEmulationSelections(
								queryGroup.getFirstQuerySpec() );
		final var sortSpecifications = queryGroup.getSortSpecifications();
		if ( sortSpecifications != null ) {
			for ( var sortSpecification : sortSpecifications ) {
				final int[] sortSelectionIndexes = sortSpecification.getSortSelectionIndexes();
				if ( sortSelectionIndexes != null ) {
					for ( int sortSelectionIndex : sortSelectionIndexes ) {
						if ( sortSelectionIndex >= 0 && sortSelectionIndex + 1 > itemCount ) {
							itemCount = sortSelectionIndex + 1;
						}
					}
				}
			}
		}
		return itemCount;
	}

	@Override
	protected DerivedTableRenderingSupport getDerivedTableRenderingSupport() {
		return StandardDerivedTableRenderingSupport.VALUES_SELECT_UNION;
	}

	@Override
	public void visitOffsetFetchClause(QueryPart queryPart) {
		if ( !currentFullJoinEmulationHelper().isFullJoinEmulationQueryPart( queryPart ) ) {
			assertRowsOnlyFetchClauseType( queryPart );
			if ( !queryPart.isRoot() && queryPart.hasOffsetOrFetchClause() ) {
				if ( queryPart.getFetchClauseExpression() != null && queryPart.getOffsetClauseExpression() != null ) {
					throw new IllegalArgumentException( "Can't emulate offset fetch clause in subquery" );
				}
			}
		}
	}

	@Override
	protected void visitOrderBy(List<SortSpecification> sortSpecifications) {
		currentFullJoinEmulationHelper().renderOrderByIfNeeded( getCurrentQueryPart(), sortSpecifications, super::visitOrderBy );
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
		final boolean ansiNullOn = ((SybaseASEDialect) getDialect()).isAnsiNullOn();
		if ( isLob ) {
			switch ( operator ) {
				case DISTINCT_FROM:
					if ( ansiNullOn ) {
						appendSql( "case when " );
						lhs.accept( this );
						appendSql( " like " );
						rhs.accept( this );
						appendSql( " or " );
						lhs.accept( this );
						appendSql( " is null and " );
						rhs.accept( this );
						appendSql( " is null then 0 else 1 end=1" );
					}
					else {
						lhs.accept( this );
						appendSql( " not like " );
						rhs.accept( this );
						appendSql( " and (" );
						lhs.accept( this );
						appendSql( " is not null or " );
						rhs.accept( this );
						appendSql( " is not null)" );
					}
					return;
				case NOT_DISTINCT_FROM:
					if ( ansiNullOn ) {
						appendSql( "case when " );
						lhs.accept( this );
						appendSql( " like " );
						rhs.accept( this );
						appendSql( " or " );
						lhs.accept( this );
						appendSql( " is null and " );
						rhs.accept( this );
						appendSql( " is null then 0 else 1 end=0" );
					}
					else {
						lhs.accept( this );
						appendSql( " like " );
						rhs.accept( this );
						appendSql( " or " );
						lhs.accept( this );
						appendSql( " is null and " );
						rhs.accept( this );
						appendSql( " is null" );
					}
					return;
				case EQUAL:
					lhs.accept( this );
					appendSql( " like " );
					rhs.accept( this );
					return;
				case NOT_EQUAL:
					lhs.accept( this );
					appendSql( " not like " );
					rhs.accept( this );
					if ( !ansiNullOn ) {
						appendSql( " and " );
						lhs.accept( this );
						appendSql( " is not null and " );
						rhs.accept( this );
						appendSql( " is not null" );
					}
					return;
				default:
					// Fall through
					break;
			}
		}
		// I think intersect is only supported in 16.0 SP3
		if ( ansiNullOn ) {
			if ( getDialect().getPredicateSupport().supports( Capability.DISTINCT_FROM ) ) {
				renderComparisonEmulateIntersect( lhs, operator, rhs );
			}
			else {
				renderComparisonEmulateCase( lhs, operator, rhs );
			}
		}
		else {
			// The ansinull setting only matters if using a parameter or literal and the eq operator according to the docs
			// http://infocenter.sybase.com/help/index.jsp?topic=/com.sybase.infocenter.dc32300.1570/html/sqlug/sqlug89.htm
			boolean lhsAffectedByAnsiNullOff = lhs instanceof Literal || isParameter( lhs );
			boolean rhsAffectedByAnsiNullOff = rhs instanceof Literal || isParameter( rhs );
			if ( lhsAffectedByAnsiNullOff || rhsAffectedByAnsiNullOff ) {
				lhs.accept( this );
				switch ( operator ) {
					case DISTINCT_FROM:
						// Since this is the ansinull=off case, this comparison is enough
						appendSql( "<>" );
						break;
					case NOT_DISTINCT_FROM:
						// Since this is the ansinull=off case, this comparison is enough
						appendSql( '=' );
						break;
					default:
						appendSql( operator.sqlText() );
						break;
				}
				rhs.accept( this );
				if ( operator == ComparisonOperator.EQUAL || operator == ComparisonOperator.NOT_EQUAL ) {
					appendSql( " and " );
					lhs.accept( this );
					appendSql( " is not null and " );
					rhs.accept( this );
					appendSql( " is not null" );
				}
			}
			else {
				if ( getDialect().getPredicateSupport().supports( Capability.DISTINCT_FROM ) ) {
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

	private boolean supportsParameterOffsetFetchExpression() {
		return false;
	}
}
