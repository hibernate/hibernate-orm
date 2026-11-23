package org.hibernate.dialect.sql.ast.internal;

import java.util.ArrayDeque;
import java.util.List;

import org.hibernate.Locking;
import org.hibernate.dialect.sql.ast.spi.DerivedTableRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.InsertConflictRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.PaginationRenderingRequest;
import org.hibernate.dialect.sql.ast.spi.PaginationRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardPaginationRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardDerivedTableRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardInsertConflictRenderingSupport;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.dialect.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.FullJoinEmulation;
import org.hibernate.sql.ast.spi.query.select.SqlSelection;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.sql.ast.spi.query.delete.DeleteStatement;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.Literal;
import org.hibernate.sql.ast.spi.query.expression.SqlTuple;
import org.hibernate.sql.ast.spi.query.expression.Summarization;
import org.hibernate.sql.ast.spi.query.select.QueryPart;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.ast.spi.query.select.SelectClause;
import org.hibernate.sql.ast.spi.query.select.SortSpecification;
import org.hibernate.sql.ast.spi.query.update.UpdateStatement;
import org.hibernate.sql.exec.spi.JdbcOperation;

/**
 * A SQL AST translator for Sybase.
 *
 * @author Christian Beikov
 */
public class SybaseSqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {

	private final ArrayDeque<FullJoinEmulation> fullJoinEmulations = new ArrayDeque<>();

	public SybaseSqlAstTranslator(SqlAstTranslationRequest<? extends Statement, T> request) {
		super( request );
		this.fullJoinEmulations.push( new FullJoinEmulation( this ) );
	}

	private FullJoinEmulation currentFullJoinEmulationHelper() {
		return fullJoinEmulations.getFirst();
	}

	@Override
	protected InsertConflictRenderingSupport getInsertConflictRenderingSupport() {
		return StandardInsertConflictRenderingSupport.TERMINATED_MERGE;
	}

	@Override
	protected void renderDeleteClause(DeleteStatement statement) {
		appendSql( "delete " );
		renderDmlTargetTableExpression( statement.getTargetTable() );
		visitFromClause( statement.getFromClause() );
	}

	@Override
	protected void renderFromClauseAfterUpdateSet(UpdateStatement statement) {
		visitFromClause( statement.getFromClause() );
	}

	// Sybase does not allow CASE expressions where all result arms contain plain parameters.
	// At least one result arm must provide some type context for inference,
	// so we cast the first result arm if we encounter this condition

	@Override
	protected LockStrategy determineLockingStrategy(
			QuerySpec querySpec,
			Locking.FollowOn followOnStrategy) {
		// No need for follow on locking
		return LockStrategy.CLAUSE;
	}

	@Override
	protected DerivedTableRenderingSupport getDerivedTableRenderingSupport() {
		return StandardDerivedTableRenderingSupport.VALUES_SELECT_UNION;
	}

	@Override
	protected PaginationRenderingSupport getPaginationRenderingSupport() {
		return PaginationRenderingSupport.withValidation( paginationPlanSelection(), this::validatePaginationRequest );
	}

	private PaginationRenderingSupport paginationPlanSelection() {
		return StandardPaginationRenderingSupport.NONE;
	}

	private void validatePaginationRequest(PaginationRenderingRequest request) {
		final QueryPart queryPart = request.queryPart();
		if ( !currentFullJoinEmulationHelper().isFullJoinEmulationQueryPart( queryPart ) ) {
			assertRowsOnlyFetchClauseType( queryPart );
			if ( !queryPart.isRoot() && queryPart.getOffsetClauseExpression() != null ) {
				throw new IllegalArgumentException( "Can't emulate offset clause in subquery" );
			}
		}
	}

	@Override
	protected void renderQuerySpec(QuerySpec querySpec) {
		final var helper = currentFullJoinEmulationHelper();
		final boolean needsNestedHelper =
				helper.hasActiveFullJoinEmulation()
						&& !helper.isFullJoinEmulationQueryPart( querySpec );
		if ( needsNestedHelper ) {
			fullJoinEmulations.push( new FullJoinEmulation( this ) );
		}
		try {
			final var currentHelper = currentFullJoinEmulationHelper();
			if ( !currentHelper.renderFullJoinEmulationBranchIfNeeded( querySpec, super::renderQuerySpec )
					&& !currentHelper.emulateFullJoinWithUnionIfNeeded( querySpec ) ) {
				super.renderQuerySpec( querySpec );
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
	protected void renderOrderBy(List<SortSpecification> sortSpecifications) {
		currentFullJoinEmulationHelper().renderOrderByIfNeeded( getCurrentQueryPart(), sortSpecifications, super::renderOrderBy );
	}

	@Override
	protected void renderComparison(Expression lhs, ComparisonOperator operator, Expression rhs) {
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

}
