/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.xml;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.dialect.function.json.ExpressionTypeHelper;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.function.SelfRenderingFunctionSqlAstExpression;
import org.hibernate.query.sqm.function.SelfRenderingOrderedSetAggregateFunctionSqlAstExpression;
import org.hibernate.query.sqm.function.SelfRenderingSqmOrderedSetAggregateFunction;
import org.hibernate.query.sqm.spi.SqmCreationHelper;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.select.SqmOrderByClause;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.AbstractSqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Distinct;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.FunctionExpression;
import org.hibernate.sql.ast.tree.expression.SelfRenderingExpression;
import org.hibernate.sql.ast.tree.from.FunctionTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.SortSpecification;
import org.hibernate.type.spi.TypeConfiguration;


/**
 * SQL Server xmlagg function.
 */
public class SQLServerXmlAggFunction extends XmlAggFunction {

	public SQLServerXmlAggFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
	}

	@Override
	public <T> SelfRenderingSqmOrderedSetAggregateFunction<T> generateSqmOrderedSetAggregateFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			SqmPredicate filter,
			SqmOrderByClause withinGroupClause,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine) {
		return new SelfRenderingSqmOrderedSetAggregateFunction<>(
				this,
				this,
				arguments,
				filter,
				withinGroupClause,
				impliedResultType,
				getArgumentsValidator(),
				getReturnTypeResolver(),
				queryEngine.getCriteriaBuilder(),
				getName()
		) {
			@Override
			public Expression convertToSqlAst(SqmToSqlAstConverter walker) {
				// SQL Server can't aggregate an argument that contains a subquery,
				// which is a bummer because xmlelement and xmlforest implementations require subqueries,
				// but we can apply a trick to make this still work.
				// Essentially, we try to move the subquery into the from clause and mark it as lateral.
				// Then we can replace the original expression with that new table reference.
				final SelfRenderingOrderedSetAggregateFunctionSqlAstExpression expression = (SelfRenderingOrderedSetAggregateFunctionSqlAstExpression) super.convertToSqlAst( walker );
				final Expression xml = (Expression) expression.getArguments().get( 0 );
				final Set<String> qualifiers = ColumnQualifierCollectorSqlAstWalker.determineColumnQualifiers( xml );
				// If the argument contains a subquery, we will receive the column qualifiers that are used
				if ( !qualifiers.isEmpty() ) {
					// Register a query transformer to register the lateral table group join
					walker.registerQueryTransformer( (cteContainer, querySpec, converter) -> {
						// Find the table group which the subquery refers to
						final TableGroup tableGroup = querySpec.getFromClause().queryTableGroups(
								tg -> {
									final String primaryVariable = tg.getPrimaryTableReference()
											.getIdentificationVariable();
									if ( qualifiers.contains( primaryVariable ) ) {
										return tg;
									}
									for ( TableReferenceJoin tableReferenceJoin : tg.getTableReferenceJoins() ) {
										final String variable = tableReferenceJoin.getJoinedTableReference()
												.getIdentificationVariable();
										if ( qualifiers.contains( variable ) ) {
											return tg;
										}
									}
									return null;
								}
						);
						if ( tableGroup != null ) {
							// Generate the lateral subquery
							final String alias = "gen" + SqmCreationHelper.acquireUniqueAlias();
							final FunctionTableGroup lateralGroup = new FunctionTableGroup(
									new NavigablePath( "generated", alias ),
									null,
									new SelfRenderingFunctionSqlAstExpression(
											"helper",
											(sqlAppender, sqlAstArguments, returnType, walker1) -> {
												sqlAppender.appendSql( "(select " );
												xml.accept( walker1 );
												sqlAppender.appendSql( " v)" );
											},
											List.of(),
											null,
											null
									),
									alias,
									List.of("v"),
									Set.of(),
									true,
									true,
									false,
									null
							);
							tableGroup.addTableGroupJoin(
									new TableGroupJoin(
											lateralGroup.getNavigablePath(),
											SqlAstJoinType.INNER,
											lateralGroup
									)
							);
							// Replace the original expression that contains a subquery with a simple column reference,
							// that points to the newly created lateral table group
							//noinspection unchecked
							( (List<SqlAstNode>) expression.getArguments() ).set(
									0,
									new ColumnReference(
											lateralGroup.getPrimaryTableReference(),
											"v",
											expression.getJdbcMapping()
									)
							);
						}
						return querySpec;
					} );
				}
				return expression;
			}
		};
	}

	static class ColumnQualifierCollectorSqlAstWalker extends AbstractSqlAstWalker {

		private static final Set<String> POTENTIAL_SUBQUERY_FUNCTIONS = Set.of(
				"xmlelement",
				"xmlforest"
		);
		private final Set<String> columnQualifiers = new HashSet<>();
		private boolean potentialSubquery;

		public static Set<String> determineColumnQualifiers(SqlAstNode node) {
			final ColumnQualifierCollectorSqlAstWalker walker = new ColumnQualifierCollectorSqlAstWalker();
			node.accept( walker );
			return walker.potentialSubquery ? walker.columnQualifiers : Set.of();
		}

		@Override
		public void visitSelfRenderingExpression(SelfRenderingExpression expression) {
			if ( expression instanceof FunctionExpression functionExpression
					&& POTENTIAL_SUBQUERY_FUNCTIONS.contains( functionExpression.getFunctionName() ) ) {
				potentialSubquery = true;
			}
			super.visitSelfRenderingExpression( expression );
		}

		@Override
		public void visitColumnReference(ColumnReference columnReference) {
			if ( columnReference.getQualifier() != null ) {
				columnQualifiers.add( columnReference.getQualifier() );
			}
		}
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			Predicate filter,
			List<SortSpecification> withinGroup,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> translator) {
		final boolean caseWrapper = filter != null && !translator.getSessionFactory().getJdbcServices().getDialect().supportsFilterClause();
		sqlAppender.appendSql( "cast(string_agg(" );
		final SqlAstNode firstArg = sqlAstArguments.get( 0 );
		final Expression arg;
		if ( firstArg instanceof Distinct distinct ) {
			sqlAppender.appendSql( "distinct " );
			arg = distinct.getExpression();
		}
		else {
			arg = (Expression) firstArg;
		}
		final boolean needsCast = ExpressionTypeHelper.isXml( arg );
		if ( needsCast ) {
			sqlAppender.appendSql( "cast(" );
		}
		if ( caseWrapper ) {
			translator.getCurrentClauseStack().push( Clause.WHERE );
			sqlAppender.appendSql( "case when " );
			filter.accept( translator );
			sqlAppender.appendSql( " then " );
			arg.accept( translator );
			sqlAppender.appendSql( " else null end" );
			translator.getCurrentClauseStack().pop();
		}
		else {
			arg.accept( translator );
		}
		if ( needsCast ) {
			sqlAppender.appendSql( " as nvarchar(max))" );
		}
		sqlAppender.appendSql( ",'')" );
		if ( withinGroup != null && !withinGroup.isEmpty() ) {
			translator.getCurrentClauseStack().push( Clause.WITHIN_GROUP );
			sqlAppender.appendSql( " within group (order by " );
			withinGroup.get( 0 ).accept( translator );
			for ( int i = 1; i < withinGroup.size(); i++ ) {
				sqlAppender.appendSql( ',' );
				withinGroup.get( i ).accept( translator );
			}
			sqlAppender.appendSql( ')' );
			translator.getCurrentClauseStack().pop();
		}
		if ( !caseWrapper && filter != null ) {
			translator.getCurrentClauseStack().push( Clause.WHERE );
			sqlAppender.appendSql( " filter (where " );
			filter.accept( translator );
			sqlAppender.appendSql( ')' );
			translator.getCurrentClauseStack().pop();
		}
		sqlAppender.appendSql( " as xml)" );
	}
}
