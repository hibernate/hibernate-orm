/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.internal;

import java.util.ArrayDeque;
import java.util.List;

import org.hibernate.SPI;
import org.hibernate.dialect.sql.ast.spi.PaginationRenderingPlan;
import org.hibernate.dialect.sql.ast.spi.PaginationRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.InsertConflictRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.PrimaryTableReferenceContext;
import org.hibernate.dialect.sql.ast.spi.QueryMutationRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.ReturningRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.SelectItemReferenceStrategy;
import org.hibernate.dialect.sql.ast.spi.StandardReturningRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardInsertConflictRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardQueryMutationRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.UpdateRenderingPlan;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.sql.ast.spi.translation.Clause;
import org.hibernate.sql.ast.spi.translation.SqlAstNodeRenderingMode;
import org.hibernate.dialect.sql.ast.spi.FullJoinEmulation;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorWithMerge;
import org.hibernate.sql.ast.spi.query.select.SqlSelection;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.sql.ast.spi.query.cte.CteTableGroup;
import org.hibernate.sql.ast.spi.query.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.Literal;
import org.hibernate.sql.ast.spi.query.expression.SqlTuple;
import org.hibernate.sql.ast.spi.query.expression.Summarization;
import org.hibernate.sql.ast.spi.query.from.NamedTableReference;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.query.predicate.BooleanExpressionPredicate;
import org.hibernate.sql.ast.spi.query.predicate.LikePredicate;
import org.hibernate.sql.ast.spi.query.select.SelectClause;
import org.hibernate.sql.ast.spi.query.select.SortSpecification;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.exec.spi.JdbcOperation;


/**
 * A SQL AST translator for H2.
 *
 * @author Christian Beikov
 */
public class H2SqlAstTranslator<T extends JdbcOperation> extends SqlAstTranslatorWithMerge<T> {

	private boolean renderAsArray;
	private final ArrayDeque<FullJoinEmulation> fullJoinEmulations = new ArrayDeque<>();

	public H2SqlAstTranslator(SqlAstTranslationRequest<? extends Statement, T> request) {
		super( request );
		this.fullJoinEmulations.push( new FullJoinEmulation( this ) );
	}

	@Override
	@SPI({ SPI.Role.IMPLEMENT, SPI.Role.SUPPLY })
	protected SelectItemReferenceStrategy getGroupBySelectItemReferenceStrategy() {
		return SelectItemReferenceStrategy.ALIAS;
	}

	private FullJoinEmulation currentFullJoinEmulationHelper() {
		return fullJoinEmulations.getFirst();
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
	protected ReturningRenderingSupport getReturningRenderingSupport() {
		return StandardReturningRenderingSupport.H2;
	}

	@Override
	protected InsertConflictRenderingSupport getInsertConflictRenderingSupport() {
		return StandardInsertConflictRenderingSupport.MERGE;
	}

	@Override
	protected QueryMutationRenderingSupport getQueryMutationRenderingSupport() {
		return StandardQueryMutationRenderingSupport.withTargetAliasedDelete(
				new UpdateRenderingPlan.Merge(),
				"dml_target_"
		);
	}

	@Override
	protected void renderDmlTargetTableExpression(NamedTableReference tableReference) {
		super.renderDmlTargetTableExpression( tableReference );
		if ( getClauseStack().getCurrent() != Clause.INSERT ) {
			renderTableReferenceIdentificationVariable( tableReference );
		}
	}

	@Override
	protected SqlAstNodeRenderingMode getCteParameterRenderingMode() {
		// H2 has various bugs in different versions that make it impossible to use CTEs with parameters reliably
		return SqlAstNodeRenderingMode.INLINE_PARAMETERS;
	}

	@Override
	protected boolean needsCteInlining() {
		// CTEs in H2 are just so buggy, that we can't reliably use them
		return true;
	}

	@Override
	protected boolean shouldInlineCte(TableGroup tableGroup) {
		return tableGroup instanceof CteTableGroup
			&& !getCteStatement( tableGroup.getPrimaryTableReference().getTableId() ).isRecursive();
	}

	@Override
	protected String getArrayContainsFunction() {
		return "array_contains";
	}

	@Override
	protected void renderExpressionAsClauseItem(Expression expression) {
		expression.accept( this );
	}

	@Override
	public void visitBooleanExpressionPredicate(BooleanExpressionPredicate booleanExpressionPredicate) {
		final boolean isNegated = booleanExpressionPredicate.isNegated();
		if ( isNegated ) {
			appendSql( "not(" );
		}
		booleanExpressionPredicate.getExpression().accept( this );
		if ( isNegated ) {
			appendSql( CLOSE_PARENTHESIS );
		}
	}

	@Override
	protected void visitOrderBy(List<SortSpecification> sortSpecifications) {
		currentFullJoinEmulationHelper().renderOrderByIfNeeded( getCurrentQueryPart(), sortSpecifications, super::visitOrderBy );
	}

	@Override
	protected PaginationRenderingSupport getPaginationRenderingSupport() {
		return request -> currentFullJoinEmulationHelper().isFullJoinEmulationQueryPart( request.queryPart() )
				? new PaginationRenderingPlan.None()
				: new PaginationRenderingPlan.OffsetFetch( true );
	}

	@Override
	protected void renderSelectTupleComparison(
			List<SqlSelection> lhsExpressions,
			SqlTuple tuple,
			ComparisonOperator operator) {
		emulateSelectTupleComparison( lhsExpressions, tuple.getExpressions(), operator, true );
	}

	@Override
	protected void renderSelectItems(SelectClause selectClause) {
		final boolean renderAsArray = this.renderAsArray;
		this.renderAsArray = false;
		if ( renderAsArray ) {
			append( OPEN_PARENTHESIS );
		}
		super.renderSelectItems( selectClause );
		if ( renderAsArray ) {
			append( CLOSE_PARENTHESIS );
		}
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
	protected void visitArithmeticOperand(Expression expression) {
		render( expression, SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
	}

	@Override
	protected void renderPrimaryTableReferencePrefix(PrimaryTableReferenceContext context) {
		// The H2 parser can't handle a sub-query as first element in a nested join
		// i.e. `join ( (select ...) alias join ... )`, so we have to introduce a dummy table reference
		if ( context.beginsNestedJoinGroup() && context.subqueryLike() ) {
			appendSql( "dual cross join " );
		}
	}

	@Override
	public void visitLikePredicate(LikePredicate likePredicate) {
		super.visitLikePredicate( likePredicate );
		// Custom implementation because H2 uses backslash as the default escape character
		// We can override this by specifying an empty escape character
		// See http://www.h2database.com/html/grammar.html#like_predicate_right_hand_side
		if ( likePredicate.getEscapeCharacter() == null ) {
			appendSql( " escape ''" );
		}
	}

}
