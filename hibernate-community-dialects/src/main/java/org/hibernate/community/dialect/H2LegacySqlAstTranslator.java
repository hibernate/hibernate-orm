/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.util.List;

import org.hibernate.Internal;
import org.hibernate.SPI;
import org.hibernate.dialect.sql.ast.spi.PaginationRenderingPlan;
import org.hibernate.dialect.sql.ast.spi.PaginationRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.NullOrderingSupport;
import org.hibernate.dialect.sql.ast.spi.PrimaryTableReferenceContext;
import org.hibernate.dialect.sql.ast.spi.QueryMutationRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.ReturningRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.SelectItemReferenceStrategy;
import org.hibernate.dialect.sql.ast.spi.StandardReturningRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardQueryMutationRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.UpdateRenderingPlan;
import org.hibernate.query.common.FetchClauseType;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.sql.ast.spi.translation.Clause;
import org.hibernate.sql.ast.spi.translation.SqlAstNodeRenderingMode;
import org.hibernate.dialect.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.spi.query.select.SqlSelection;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.dialect.sql.ast.spi.InsertConflictRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardInsertConflictRenderingSupport;
import org.hibernate.sql.ast.spi.query.cte.CteTableGroup;
import org.hibernate.sql.ast.spi.query.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.Literal;
import org.hibernate.sql.ast.spi.query.expression.SqlTuple;
import org.hibernate.sql.ast.spi.query.expression.SqlTupleContainer;
import org.hibernate.sql.ast.spi.query.expression.Summarization;
import org.hibernate.sql.ast.spi.query.from.NamedTableReference;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.query.predicate.BooleanExpressionPredicate;
import org.hibernate.sql.ast.spi.query.predicate.InSubQueryPredicate;
import org.hibernate.sql.ast.spi.query.predicate.LikePredicate;
import org.hibernate.sql.ast.spi.query.select.SelectClause;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.ast.spi.model.TableInsertStandard;


/**
 * A legacy SQL AST translator for H2.
 *
 * @author Christian Beikov
 */
@Internal
public class H2LegacySqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {

	private boolean renderAsArray;

	public H2LegacySqlAstTranslator(SqlAstTranslationRequest<? extends Statement, T> request) {
		super( request );
	}

	@Override
	@SPI({ SPI.Role.IMPLEMENT, SPI.Role.SUPPLY })
	protected SelectItemReferenceStrategy getGroupBySelectItemReferenceStrategy() {
		return SelectItemReferenceStrategy.ALIAS;
	}

	@Override
	protected ReturningRenderingSupport getReturningRenderingSupport() {
		return StandardReturningRenderingSupport.H2;
	}

	@Override
	protected boolean renderLegacyReturningInsert(TableInsertStandard tableInsert) {
		if ( getDialect().getVersion().isSameOrAfter( 2 ) ) {
			return false;
		}
		assert tableInsert.getReturningColumns() != null
				&& !tableInsert.getReturningColumns().isEmpty();

		renderStandardTableInsert( tableInsert );
		appendSql( " call identity();" );
		return true;
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
	protected PaginationRenderingSupport getPaginationRenderingSupport() {
		return request -> {
			if ( request.hasFetch() && request.fetchClauseType() != FetchClauseType.ROWS_ONLY ) {
				if ( !supportsOffsetFetchClausePercentWithTies() ) {
					throw new IllegalArgumentException(
							"Can't emulate fetch clause type: " + request.fetchClauseType() );
				}
				return new PaginationRenderingPlan.OffsetFetch( true );
			}
			return supportsOffsetFetchClause()
					? new PaginationRenderingPlan.OffsetFetch( true )
					: new PaginationRenderingPlan.LimitOffset();
		};
	}

	@Override
	protected void renderSelectTupleComparison(
			List<SqlSelection> lhsExpressions,
			SqlTuple tuple,
			ComparisonOperator operator) {
		emulateSelectTupleComparison( lhsExpressions, tuple.getExpressions(), operator, true );
	}

	@Override
	public void visitInSubQueryPredicate(InSubQueryPredicate inSubQueryPredicate) {
		final SqlTuple lhsTuple;
		// As of 1.4.200 this is supported
		if ( getDialect().getVersion().isBefore( 1, 4, 200 )
				&& ( lhsTuple = SqlTupleContainer.getSqlTuple( inSubQueryPredicate.getTestExpression() ) ) != null
				&& lhsTuple.getExpressions().size() != 1 ) {
			inSubQueryPredicate.getTestExpression().accept( this );
			if ( inSubQueryPredicate.isNegated() ) {
				appendSql( " not" );
			}
			appendSql( " in" );
			final boolean renderAsArray = this.renderAsArray;
			this.renderAsArray = true;
			inSubQueryPredicate.getSubQuery().accept( this );
			this.renderAsArray = renderAsArray;
		}
		else {
			super.visitInSubQueryPredicate( inSubQueryPredicate );
		}
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

	protected boolean allowsNullPrecedence() {
		return getClauseStack().getCurrent() != Clause.WITHIN_GROUP
				|| getDialect().getNullOrderingSupport()
						.supports( NullOrderingSupport.Capability.NULLS_FIRST_LAST );
	}

	private boolean supportsOffsetFetchClause() {
		return getDialect().getVersion().isSameOrAfter( 1, 4, 195 );
	}

	private boolean supportsOffsetFetchClausePercentWithTies() {
		// Introduction of TIES clause https://github.com/h2database/h2database/commit/876e9fbe7baf11d01675bfe871aac2cf1b6104ce
		// Introduction of PERCENT support https://github.com/h2database/h2database/commit/f45913302e5f6ad149155a73763c0c59d8205849
		return getDialect().getVersion().isSameOrAfter( 1, 4, 198 );
	}
}
