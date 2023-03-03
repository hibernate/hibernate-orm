/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.tree.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.tree.expression.CaseSimpleExpression;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.AggregateColumnWriteExpression;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.Summarization;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.UnionTableReference;
import org.hibernate.sql.ast.tree.predicate.BooleanExpressionPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.QueryGroup;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.exec.spi.JdbcOperation;

/**
 * A SQL AST translator for Sybase ASE.
 *
 * @author Christian Beikov
 */
public class SybaseASESqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {

	public SybaseASESqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
		super( sessionFactory, statement );
	}

	@Override
	protected boolean supportsWithClause() {
		return false;
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
		super.renderNamedTableReference( tableReference, lockMode );
		return false;
	}

	@Override
	protected void renderTableGroupJoin(TableGroupJoin tableGroupJoin, List<TableGroupJoin> tableGroupJoinCollector) {
		if ( tableGroupJoin.getJoinType() == SqlAstJoinType.CROSS ) {
			appendSql( ", " );
		}
		else {
			appendSql( WHITESPACE );
			appendSql( tableGroupJoin.getJoinType().getText() );
			appendSql( "join " );
		}

		final Predicate predicate;
		if ( tableGroupJoin.getPredicate() == null ) {
			if ( tableGroupJoin.getJoinType() == SqlAstJoinType.CROSS ) {
				predicate = null;
			}
			else {
				predicate = new BooleanExpressionPredicate( new QueryLiteral<>( true, getBooleanType() ) );
			}
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
	protected void visitSqlSelections(SelectClause selectClause) {
		renderTopClause( (QuerySpec) getQueryPartStack().getCurrent(), true, false );
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
	public void visitOffsetFetchClause(QueryPart queryPart) {
		assertRowsOnlyFetchClauseType( queryPart );
		if ( !queryPart.isRoot() && queryPart.hasOffsetOrFetchClause() ) {
			if ( queryPart.getFetchClauseExpression() != null && queryPart.getOffsetClauseExpression() != null ) {
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
		// I think intersect is only supported in 16.0 SP3
		if ( getDialect().isAnsiNullOn() ) {
			if ( supportsDistinctFromPredicate() ) {
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
						appendSql( "<>" );
						break;
					case NOT_DISTINCT_FROM:
						appendSql( '=' );
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
				if ( supportsDistinctFromPredicate() ) {
					renderComparisonEmulateIntersect( lhs, operator, rhs );
				}
				else {
					renderComparisonEmulateCase( lhs, operator, rhs );
				}
			}
		}
	}

	@Override
	protected boolean supportsIntersect() {
		// At least the version that
		return false;
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
		arithmeticExpression.getLeftHandOperand().accept( this );
		appendSql( arithmeticExpression.getOperator().getOperatorSqlTextString() );
		arithmeticExpression.getRightHandOperand().accept( this );
		appendSql( CLOSE_PARENTHESIS );
	}

	@Override
	public void visitColumnReference(ColumnReference columnReference) {
		final String dmlTargetTableAlias = getDmlTargetTableAlias();
		if ( dmlTargetTableAlias != null && dmlTargetTableAlias.equals( columnReference.getQualifier() ) ) {
			// Sybase needs a table name prefix
			// but not if this is a restricted union table reference subquery
			final QuerySpec currentQuerySpec = (QuerySpec) getQueryPartStack().getCurrent();
			final List<TableGroup> roots;
			if ( currentQuerySpec != null && !currentQuerySpec.isRoot()
					&& (roots = currentQuerySpec.getFromClause().getRoots()).size() == 1
					&& roots.get( 0 ).getPrimaryTableReference() instanceof UnionTableReference ) {
				columnReference.appendReadExpression( this );
			}
			// for now, use the unqualified form
			else if ( columnReference.isColumnExpressionFormula() ) {
				// For formulas, we have to replace the qualifier as the alias was already rendered into the formula
				// This is fine for now as this is only temporary anyway until we render aliases for table references
				appendSql(
						columnReference.getColumnExpression()
								.replaceAll( "(\\b)(" + dmlTargetTableAlias + "\\.)(\\b)", "$1$3" )
				);
			}
			else {
				columnReference.appendReadExpression(
						this,
						getCurrentDmlStatement().getTargetTable().getTableExpression()
				);
			}
		}
		else {
			columnReference.appendReadExpression( this );
		}
	}

	@Override
	public void visitAggregateColumnWriteExpression(AggregateColumnWriteExpression aggregateColumnWriteExpression) {
		final String dmlTargetTableAlias = getDmlTargetTableAlias();
		final ColumnReference columnReference = aggregateColumnWriteExpression.getColumnReference();
		if ( dmlTargetTableAlias != null && dmlTargetTableAlias.equals( columnReference.getQualifier() ) ) {
			// Sybase needs a table name prefix
			// but not if this is a restricted union table reference subquery
			final QuerySpec currentQuerySpec = (QuerySpec) getQueryPartStack().getCurrent();
			final List<TableGroup> roots;
			if ( currentQuerySpec != null && !currentQuerySpec.isRoot()
					&& (roots = currentQuerySpec.getFromClause().getRoots()).size() == 1
					&& roots.get( 0 ).getPrimaryTableReference() instanceof UnionTableReference ) {
				aggregateColumnWriteExpression.appendWriteExpression( this, this );
			}
			else {
				aggregateColumnWriteExpression.appendWriteExpression(
						this,
						this,
						getCurrentDmlStatement().getTargetTable().getTableExpression()
				);
			}
		}
		else {
			aggregateColumnWriteExpression.appendWriteExpression( this, this );
		}
	}

	@Override
	protected boolean needsRowsToSkip() {
		return true;
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

	@Override
	protected String getFromDual() {
		return " from (select 1) dual(c1)";
	}

	private boolean supportsParameterOffsetFetchExpression() {
		return false;
	}
}
