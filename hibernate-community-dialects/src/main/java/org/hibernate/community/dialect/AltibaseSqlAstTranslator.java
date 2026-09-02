/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.util.List;

import org.hibernate.Internal;
import org.hibernate.Locking;
import org.hibernate.dialect.sql.ast.spi.DerivedTableKind;
import org.hibernate.dialect.sql.ast.spi.DerivedTableRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.InsertConflictRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.PaginationRenderingPlan;
import org.hibernate.dialect.sql.ast.spi.PaginationRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardDerivedTableRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardInsertConflictRenderingSupport;
import org.hibernate.metamodel.mapping.EntityVersionMapping;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.common.FrameExclusion;
import org.hibernate.query.common.FrameKind;
import org.hibernate.query.common.FetchClauseType;
import org.hibernate.sql.ast.spi.translation.Clause;
import org.hibernate.sql.ast.spi.translation.SqlAstNodeRenderingMode;
import org.hibernate.dialect.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.dialect.lock.spi.LockingClauseStrategy;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.dialect.function.spi.WindowFunctionSupport;
import org.hibernate.sql.ast.spi.SqlAstNode;
import org.hibernate.sql.ast.spi.query.delete.DeleteStatement;
import org.hibernate.sql.ast.spi.query.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.spi.query.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.spi.query.expression.CaseSimpleExpression;
import org.hibernate.sql.ast.spi.query.expression.CastTarget;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.FunctionExpression;
import org.hibernate.sql.ast.spi.query.expression.Literal;
import org.hibernate.sql.ast.spi.query.expression.Over;
import org.hibernate.sql.ast.spi.query.expression.SqlTuple;
import org.hibernate.sql.ast.spi.query.expression.SqlTupleContainer;
import org.hibernate.sql.ast.spi.query.expression.Summarization;
import org.hibernate.sql.ast.spi.query.from.NamedTableReference;
import org.hibernate.sql.ast.spi.query.insert.InsertSelectStatement;
import org.hibernate.sql.ast.spi.query.predicate.Predicate;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.ast.spi.query.update.Assignment;
import org.hibernate.sql.ast.spi.query.update.UpdateStatement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;

/**
 * A SQL AST translator for Altibase.
 *
 * @author Geoffrey Park
 */
public class AltibaseSqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {

	// Tracks SELECT-list expression rendering so CASE result parameters can be rendered Altibase-compatibly.
	private boolean renderingSelectExpression;

	public AltibaseSqlAstTranslator(SqlAstTranslationRequest<? extends Statement, T> request) {
		super( request );
	}

	@Override
	protected PaginationRenderingSupport getPaginationRenderingSupport() {
		return request -> request.hasFetch()
				&& request.fetchClauseType() != FetchClauseType.ROWS_ONLY
				&& getDialect().getWindowFunctionSupport()
						.supports( WindowFunctionSupport.Feature.WINDOW_FUNCTIONS )
						? new PaginationRenderingPlan.Window( true )
						: new PaginationRenderingPlan.CombinedLimit();
	}

	@Override
	protected void renderComparison(Expression lhs, ComparisonOperator operator, Expression rhs) {
		if ( operator == ComparisonOperator.DISTINCT_FROM || operator == ComparisonOperator.NOT_DISTINCT_FROM ) {
			// Emulate null-safe comparisons directly; DISTINCT FROM needs explicit null-mismatch predicates.
			final SqlTuple lhsTuple = SqlTupleContainer.getSqlTuple( lhs );
			final SqlTuple rhsTuple = SqlTupleContainer.getSqlTuple( rhs );
			if ( lhsTuple != null && rhsTuple != null ) {
				renderDistinctFromPredicate( lhsTuple.getExpressions(), operator, rhsTuple.getExpressions() );
			}
			else {
				renderDistinctFromPredicate( List.of( lhs ), operator, List.of( rhs ) );
			}
			return;
		}
		if ( renderCastedScalarSubqueryComparison( lhs, operator, rhs ) ) {
			return;
		}
		lhs.accept( this );
		appendSql( operator.sqlText() );
		rhs.accept( this );
	}

	private boolean renderCastedScalarSubqueryComparison(Expression lhs, ComparisonOperator operator, Expression rhs) {
		if ( operator != ComparisonOperator.EQUAL ) {
			return false;
		}
		if ( rhs instanceof SelectStatement selectStatement && hasSingleJdbcType( lhs ) ) {
			lhs.accept( this );
			appendSql( operator.sqlText() );
			renderCastedScalarSubquery( selectStatement, lhs );
			return true;
		}
		if ( lhs instanceof SelectStatement selectStatement && hasSingleJdbcType( rhs ) ) {
			renderCastedScalarSubquery( selectStatement, rhs );
			appendSql( operator.sqlText() );
			rhs.accept( this );
			return true;
		}
		return false;
	}

	private boolean hasSingleJdbcType(Expression expression) {
		return expression.getExpressionType() != null && expression.getExpressionType().getJdbcTypeCount() == 1;
	}

	private void renderCastedScalarSubquery(SelectStatement selectStatement, Expression targetTypeExpression) {
		appendSql( "cast(" );
		selectStatement.accept( this );
		appendSql( " as " );
		new CastTarget( targetTypeExpression.getExpressionType().getSingleJdbcMapping() ).accept( this );
		appendSql( ')' );
	}

	@Override
	protected void emulateTupleComparison(
			List<? extends SqlAstNode> lhsExpressions,
			List<? extends SqlAstNode> rhsExpressions,
			ComparisonOperator operator,
			boolean indexOptimized) {
		// Handle DISTINCT_FROM/NOT_DISTINCT_FROM tuple comparisons with Altibase's
		// null-safe equality emulation; delegate other tuple comparisons to Hibernate.
		if ( operator == ComparisonOperator.DISTINCT_FROM || operator == ComparisonOperator.NOT_DISTINCT_FROM ) {
			renderDistinctFromPredicate( lhsExpressions, operator, rhsExpressions );
		}
		else {
			super.emulateTupleComparison( lhsExpressions, rhsExpressions, operator, indexOptimized );
		}
	}

	/*
	 * a is not distinct from b
	 *   -> (a = b or a is null and b is null)
	 * a is distinct from b
	 *   -> (a <> b or a is null and b is not null or a is not null and b is null)
	 * (a, b) is not distinct from (x, y)
	 *   -> (a is not distinct from x) and (b is not distinct from y)
	 * (a, b) is distinct from (x, y)
	 *   -> (a is distinct from x) or (b is distinct from y)
	 */
	private void renderDistinctFromPredicate(
			List<? extends SqlAstNode> lhsExpressions,
			ComparisonOperator operator,
			List<? extends SqlAstNode> rhsExpressions) {
		appendSql( '(' );
		String separator = "";
		for ( int i = 0; i < lhsExpressions.size(); i++ ) {
			appendSql( separator );
			if ( operator == ComparisonOperator.DISTINCT_FROM ) {
				renderDistinctComparison( lhsExpressions.get( i ), rhsExpressions.get( i ) );
				separator = " or ";
			}
			else {
				renderNotDistinctComparison( lhsExpressions.get( i ), rhsExpressions.get( i ) );
				separator = " and ";
			}
		}
		appendSql( ')' );
	}

	private void renderNotDistinctComparison(SqlAstNode lhs, SqlAstNode rhs) {
		appendSql( '(' );
		lhs.accept( this );
		appendSql( '=' );
		rhs.accept( this );
		appendSql( " or " );
		lhs.accept( this );
		appendSql( " is null and " );
		rhs.accept( this );
		appendSql( " is null)" );
	}

	private void renderDistinctComparison(SqlAstNode lhs, SqlAstNode rhs) {
		appendSql( '(' );
		lhs.accept( this );
		appendSql( "<>" );
		rhs.accept( this );
		appendSql( " or " );
		lhs.accept( this );
		appendSql( " is null and " );
		rhs.accept( this );
		appendSql( " is not null or " );
		lhs.accept( this );
		appendSql( " is not null and " );
		rhs.accept( this );
		appendSql( " is null)" );
	}

	@Override
	public void visitOver(Over<?> over) {
		final Expression expression = over.getExpression();
		if ( expression instanceof FunctionExpression functionExpression
				&& "row_number".equals( functionExpression.getFunctionName() ) ) {
			if ( over.getPartitions().isEmpty() && over.getOrderList().isEmpty()
					&& over.getStartKind() == FrameKind.UNBOUNDED_PRECEDING
					&& over.getEndKind() == FrameKind.CURRENT_ROW
					&& over.getExclusion() == FrameExclusion.NO_OTHERS ) {
				// Altibase doesn't allow an empty over clause for the row_number() function,
				append( "row_number() over(order by 1)" );
				return;
			}
		}
		super.visitOver( over );
	}

	@Override
	protected LockStrategy determineLockingStrategy(QuerySpec querySpec, Locking.FollowOn followOnStrategy) {
		final LockStrategy lockStrategy = super.determineLockingStrategy( querySpec, followOnStrategy );
		final LockingClauseStrategy lockingClauseStrategy = getLockingClauseStrategy();
		if ( lockingClauseStrategy != null && lockingClauseStrategy.containsJoins() ) {
			// Altibase does not allow FOR UPDATE when the query also contains joins.
			if ( followOnStrategy == Locking.FollowOn.DISALLOW ) {
				throw new IllegalQueryOperationException( "Locking with joins is not supported" );
			}
			else if ( followOnStrategy == Locking.FollowOn.IGNORE ) {
				return LockStrategy.NONE;
			}
			return LockStrategy.FOLLOW_ON;
		}
		return lockStrategy;
	}

	@Override
	protected void renderPartitionItem(Expression expression) {
		if ( expression instanceof Literal ) {
			appendSql( "'0' || '0'" );
		}
		else if ( expression instanceof Summarization ) {
			// This could theoretically be emulated by rendering all grouping variations of the query and
			// connect them via union all but that's probably pretty inefficient and would have to happen
			// on the query spec level
			throw new UnsupportedOperationException( "Summarization is not supported by DBMS!" );
		}
		else {
			expression.accept( this );
		}
	}

	@Override
	protected void renderOffsetExpression(Expression offsetExpression) {
		// Altibase offset starts from 1
		appendSql( "1+" );
		offsetExpression.accept( this );
	}

	@Override
	protected DerivedTableRenderingSupport getDerivedTableRenderingSupport() {
		return StandardDerivedTableRenderingSupport.ALTIBASE;
	}

	@Override
	@Internal
	protected InsertConflictRenderingSupport getInsertConflictRenderingSupport() {
		return StandardInsertConflictRenderingSupport.MERGE;
	}

	@Override
	protected void renderSelectExpression(Expression expression) {
		// In INSERT SELECT/VALUES sources, inline simple values, version seeds and bound
		// parameters that Hibernate would otherwise render as SELECT-list parameter markers.
		final boolean renderingInsertSelectSource = !renderingSelectExpression
				&& getStatementStack().getCurrent() instanceof InsertSelectStatement insertStatement
				&& insertStatement.getSourceSelectStatement() != null;
		final boolean renderingDerivedValuesSource = isRenderingDerivedTable( DerivedTableKind.VALUES )
				&& !renderingSelectExpression;
		final boolean previousRenderingSelectExpression = renderingSelectExpression;
		renderingSelectExpression = true;
		try {
			// Apply INSERT SELECT/VALUES-specific inlining only while rendering their source SELECT-list.
			if ( renderingInsertSelectSource || renderingDerivedValuesSource ) {
				// Render literals directly instead of turning them into parameter markers.
				if ( expression instanceof Literal literal && renderInsertSelectLiteral( literal ) ) {
					return;
				}
				// Render generated version seed values as SQL expressions such as 0 or current timestamp.
				final EntityVersionMapping versionMapping = getVersionSeedMapping( expression );
				if ( versionMapping != null && renderVersionSeedParameter( versionMapping ) ) {
					return;
				}
				// Render remaining parameters in the source SELECT-list as SQL literals.
				withParameterRenderingMode(
						SqlAstNodeRenderingMode.INLINE_PARAMETERS,
						() -> super.renderSelectExpression( expression )
				);
				return;
			}
			// SELECT-list CASE expressions need the same Altibase-compatible parameter rendering,
			// including nested CASE result expressions reached during recursive SQL AST rendering.
			if ( expression instanceof CaseSearchedExpression caseSearchedExpression ) {
				visitCaseSearchedExpression( caseSearchedExpression, true );
				return;
			}
			if ( expression instanceof CaseSimpleExpression caseSimpleExpression ) {
				visitCaseSimpleExpression( caseSimpleExpression, true );
				return;
			}
			super.renderSelectExpression( expression );
		}
		finally {
			renderingSelectExpression = previousRenderingSelectExpression;
		}
	}

	// Handles searched CASE: "case when <predicate> then <result> end".
	// It inlines parameter markers in predicates and result expressions for Altibase.
	@Override
	protected void visitCaseSearchedExpression(CaseSearchedExpression caseSearchedExpression, boolean inSelect) {
		if ( !inSelect && !renderingSelectExpression ) {
			super.visitCaseSearchedExpression( caseSearchedExpression, false );
			return;
		}

		appendSql( "case" );
		for ( CaseSearchedExpression.WhenFragment whenFragment : caseSearchedExpression.getWhenFragments() ) {
			appendSql( " when " );
			withParameterRenderingMode(
					SqlAstNodeRenderingMode.INLINE_PARAMETERS,
					() -> whenFragment.getPredicate().accept( this )
			);
			appendSql( " then " );
			withParameterRenderingMode(
					SqlAstNodeRenderingMode.INLINE_PARAMETERS,
					() -> whenFragment.getResult().accept( this )
			);
		}

		final Expression otherwise = caseSearchedExpression.getOtherwise();
		if ( otherwise != null ) {
			appendSql( " else " );
			withParameterRenderingMode(
					SqlAstNodeRenderingMode.INLINE_PARAMETERS,
					() -> otherwise.accept( this )
			);
		}

		appendSql( " end" );
	}

	// Handles simple CASE: "case <fixture> when <value> then <result> end".
	// It inlines parameter markers in the fixture, check values and result expressions for Altibase.
	@Override
	protected void visitCaseSimpleExpression(CaseSimpleExpression caseSimpleExpression, boolean inSelect) {
		if ( !inSelect && !renderingSelectExpression ) {
			super.visitCaseSimpleExpression( caseSimpleExpression, false );
			return;
		}

		appendSql( "case " );
		withParameterRenderingMode(
				SqlAstNodeRenderingMode.INLINE_PARAMETERS,
				() -> caseSimpleExpression.getFixture().accept( this )
		);
		for ( CaseSimpleExpression.WhenFragment whenFragment : caseSimpleExpression.getWhenFragments() ) {
			appendSql( " when " );
			withParameterRenderingMode(
					SqlAstNodeRenderingMode.INLINE_PARAMETERS,
					() -> whenFragment.getCheckValue().accept( this )
			);
			appendSql( " then " );
			withParameterRenderingMode(
					SqlAstNodeRenderingMode.INLINE_PARAMETERS,
					() -> whenFragment.getResult().accept( this )
			);
		}

		final Expression otherwise = caseSimpleExpression.getOtherwise();
		if ( otherwise != null ) {
			appendSql( " else " );
			withParameterRenderingMode(
					SqlAstNodeRenderingMode.INLINE_PARAMETERS,
					() -> otherwise.accept( this )
			);
		}

		appendSql( " end" );
	}

	// Renders INSERT SELECT literals through Hibernate's type-aware literal rendering.
	private boolean renderInsertSelectLiteral(Literal literal) {
		renderLiteral( literal, true );
		return true;
	}

	// Renders optimistic-lock version seeds through the JDBC literal formatter when possible.
	@SuppressWarnings("unchecked")
	private boolean renderVersionSeedParameter(EntityVersionMapping versionMapping) {
		final JdbcLiteralFormatter<Object> literalFormatter = (JdbcLiteralFormatter<Object>) versionMapping.getJdbcMapping()
				.getJdbcType()
				.getJdbcLiteralFormatter( versionMapping.getJavaType() );
		if ( literalFormatter == null ) {
			return false;
		}
		final Object seedValue = getVersionSeedValue( versionMapping );
		literalFormatter.appendJdbcLiteral( this, seedValue, getDialect(), getWrapperOptions() );
		return true;
	}

	private Object getVersionSeedValue(EntityVersionMapping versionMapping) {
		final int precision = getVersionSeedPrecision( versionMapping );
		return versionMapping.getJavaType().seed(
				versionMapping.getLength(),
				precision,
				versionMapping.getScale(),
				getWrapperOptions()
		);
	}

	private int getVersionSeedPrecision(EntityVersionMapping versionMapping) {
		final Integer precision = versionMapping.getTemporalPrecision() != null
				? versionMapping.getTemporalPrecision()
				: versionMapping.getPrecision();
		return precision != null ? precision : getDialect().getTypeSizingProfile().defaultTimestampPrecision();
	}

	@Override
	protected void renderMergeUpdateClause(List<Assignment> assignments, Predicate wherePredicate) {
		// In Altibase, where condition in merge can be placed next to the set clause."
		appendSql( " then update" );
		renderSetClause( assignments );
		visitWhereClause( wherePredicate );
	}

	@Override
	protected void renderDeleteClause(DeleteStatement statement) {
		appendSql( "delete" );
		renderTableReferenceIdentificationVariable( statement.getTargetTable() );
		if ( statement.getFromClause().getRoots().isEmpty() ) {
			appendSql( " from " );
			renderDmlTargetTableExpression( statement.getTargetTable() );
		}
		else {
			visitFromClause( statement.getFromClause() );
		}
	}

	@Override
	protected void renderDmlTargetTableExpression(NamedTableReference tableReference) {
		super.renderDmlTargetTableExpression( tableReference );
		if ( getClauseStack().getCurrent() != Clause.INSERT ) {
			renderTableReferenceIdentificationVariable( tableReference );
		}
	}

	// Qualify assignment columns for multi-table updates to avoid ambiguity, but keep
	// INSERT SELECT merge emulation assignments unqualified for Altibase MERGE syntax.
	@Override
	protected void renderAssignmentColumn(ColumnReference column) {
		column.appendColumnForWrite(
				this,
				getAffectedTableNames().size() > 1 && !( getStatement() instanceof InsertSelectStatement )
						? determineColumnReferenceQualifier( column )
						: null
		);
	}

	@Override
	protected void renderUpdateClause(UpdateStatement updateStatement) {
		if ( updateStatement.getFromClause().getRoots().isEmpty() ) {
			super.renderUpdateClause( updateStatement );
		}
		else {
			appendSql( "update " );
			renderFromClauseSpaces( updateStatement.getFromClause() );
		}
	}

	@Override
	public void visitBinaryArithmeticExpression(BinaryArithmeticExpression arithmeticExpression) {
		if ( isIntegerDivisionEmulationRequired( arithmeticExpression ) ) {
			appendSql( "floor" );
		}
		super.visitBinaryArithmeticExpression(arithmeticExpression);
	}

}
