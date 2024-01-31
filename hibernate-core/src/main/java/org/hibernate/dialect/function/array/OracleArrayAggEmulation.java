/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function.array;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.query.ReturnableType;
import org.hibernate.query.SemanticException;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.function.FunctionKind;
import org.hibernate.query.sqm.function.FunctionRenderer;
import org.hibernate.query.sqm.function.SelfRenderingOrderedSetAggregateFunctionSqlAstExpression;
import org.hibernate.query.sqm.function.SelfRenderingSqmOrderedSetAggregateFunction;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.select.SqmOrderByClause;
import org.hibernate.query.sqm.tree.select.SqmSortSpecification;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.cte.CteContainer;
import org.hibernate.sql.ast.tree.cte.SelfRenderingCteObject;
import org.hibernate.sql.ast.tree.expression.Distinct;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.QueryTransformer;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SortSpecification;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.sql.DdlType;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Christian Beikov
 */
public class OracleArrayAggEmulation extends AbstractSqmSelfRenderingFunctionDescriptor {

	public static final String FUNCTION_NAME = "array_agg";

	public OracleArrayAggEmulation() {
		super(
				FUNCTION_NAME,
				FunctionKind.ORDERED_SET_AGGREGATE,
				StandardArgumentsValidators.exactly( 1 ),
				JsonArrayViaElementArgumentReturnTypeResolver.INSTANCE,
				StandardFunctionArgumentTypeResolvers.NULL
		);
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		render( sqlAppender, sqlAstArguments, null, Collections.emptyList(), returnType, walker );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			Predicate filter,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		render( sqlAppender, sqlAstArguments, filter, Collections.emptyList(), returnType, walker );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			Predicate filter,
			List<SortSpecification> withinGroup,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> translator) {
		sqlAppender.appendSql( "json_arrayagg" );
		sqlAppender.appendSql( '(' );
		final SqlAstNode firstArg = sqlAstArguments.get( 0 );
		final Expression arg;
		if ( firstArg instanceof Distinct ) {
			sqlAppender.appendSql( "distinct " );
			arg = ( (Distinct) firstArg ).getExpression();
		}
		else {
			arg = (Expression) firstArg;
		}
		arg.accept( translator );
		if ( withinGroup != null && !withinGroup.isEmpty() ) {
			translator.getCurrentClauseStack().push( Clause.WITHIN_GROUP );
			sqlAppender.appendSql( " order by " );
			withinGroup.get( 0 ).accept( translator );
			for ( int i = 1; i < withinGroup.size(); i++ ) {
				sqlAppender.appendSql( ',' );
				withinGroup.get( i ).accept( translator );
			}
			translator.getCurrentClauseStack().pop();
		}
		sqlAppender.appendSql( " null on null returning " );
		sqlAppender.appendSql(
				translator.getSessionFactory().getTypeConfiguration().getDdlTypeRegistry()
						.getTypeName( SqlTypes.JSON, translator.getSessionFactory().getJdbcServices().getDialect() )
		);
		sqlAppender.appendSql( ')' );
		if ( filter != null ) {
			translator.getCurrentClauseStack().push( Clause.WHERE );
			sqlAppender.appendSql( " filter (where " );
			filter.accept( translator );
			sqlAppender.appendSql( ')' );
			translator.getCurrentClauseStack().pop();
		}
	}

	@Override
	public <T> SelfRenderingSqmOrderedSetAggregateFunction<T> generateSqmOrderedSetAggregateFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			SqmPredicate filter,
			SqmOrderByClause withinGroupClause,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine) {
		return new OracleArrayAggSqmFunction<>(
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
		);
	}

	protected static class OracleArrayAggSqmFunction<T> extends SelfRenderingSqmOrderedSetAggregateFunction<T> {
		public OracleArrayAggSqmFunction(
				OracleArrayAggEmulation descriptor,
				FunctionRenderer renderingSupport,
				List<? extends SqmTypedNode<?>> arguments,
				SqmPredicate filter,
				SqmOrderByClause withinGroupClause,
				ReturnableType<T> impliedResultType,
				ArgumentsValidator argumentsValidator,
				FunctionReturnTypeResolver returnTypeResolver,
				NodeBuilder nodeBuilder,
				String name) {
			super(
					descriptor,
					renderingSupport,
					arguments,
					filter,
					withinGroupClause,
					impliedResultType,
					argumentsValidator,
					returnTypeResolver,
					nodeBuilder,
					name
			);
		}

		@Override
		protected ReturnableType<?> resolveResultType(TypeConfiguration typeConfiguration) {
			return getReturnTypeResolver().resolveFunctionReturnType(
					getImpliedResultType(),
					() -> null,
					getArguments(),
					nodeBuilder().getTypeConfiguration()
			);
		}

		@Override
		public Expression convertToSqlAst(SqmToSqlAstConverter walker) {
			final ReturnableType<?> resultType = resolveResultType( walker );
			if ( resultType == null ) {
				throw new SemanticException(
						"Oracle array_agg emulation requires knowledge about the return type, but resolved return type could not be determined"
				);
			}
			final DomainType<?> type = resultType.getSqmType();
			if ( !( type instanceof BasicPluralType<?, ?> ) ) {
				throw new SemanticException(
						"Oracle array_agg emulation requires a basic plural return type, but resolved return type was: " + type
				);
			}
			final BasicPluralType<?, ?> pluralType = (BasicPluralType<?, ?>) type;
			if ( pluralType.getJdbcType().getDefaultSqlTypeCode() == SqlTypes.JSON ) {
				// If we can return the result as JSON, we don't need further special handling
				return super.convertToSqlAst( walker );
			}
			// If we have to return an array type, then we must apply some further magic to transform the json array
			// into an array of the desired array type via a with-clause defined function
			final TypeConfiguration typeConfiguration = walker.getCreationContext().getSessionFactory().getTypeConfiguration();
			final DdlTypeRegistry ddlTypeRegistry = typeConfiguration.getDdlTypeRegistry();
			final DdlType ddlType = ddlTypeRegistry.getDescriptor(
					pluralType.getJdbcType().getDdlTypeCode()
			);
			final String arrayTypeName = ddlType.getCastTypeName( Size.nil(), pluralType, ddlTypeRegistry );

			List<SqlAstNode> arguments = resolveSqlAstArguments( getArguments(), walker );
			if ( getArgumentsValidator() != null ) {
				getArgumentsValidator().validateSqlTypes( arguments, getFunctionName() );
			}
			List<SortSpecification> withinGroup;
			if ( getWithinGroup() == null ) {
				withinGroup = Collections.emptyList();
			}
			else {
				walker.getCurrentClauseStack().push( Clause.WITHIN_GROUP );
				try {
					final List<SqmSortSpecification> sortSpecifications = getWithinGroup().getSortSpecifications();
					withinGroup = new ArrayList<>( sortSpecifications.size() );
					for ( SqmSortSpecification sortSpecification : sortSpecifications ) {
						final SortSpecification specification = (SortSpecification) walker.visitSortSpecification( sortSpecification );
						if ( specification != null ) {
							withinGroup.add( specification );
						}
					}
				}
				finally {
					walker.getCurrentClauseStack().pop();
				}
			}
			final OracleArrayAggEmulationSqlAstExpression expression = new OracleArrayAggEmulationSqlAstExpression(
					getFunctionName(),
					getFunctionRenderer(),
					arguments,
					getFilter() == null ? null : walker.visitNestedTopLevelPredicate( getFilter() ),
					withinGroup,
					resultType,
					getMappingModelExpressible( walker, resultType, arguments ),
					arrayTypeName
			);
			walker.registerQueryTransformer( expression );
			return expression;
		}

		private static class OracleArrayAggEmulationSqlAstExpression
				extends SelfRenderingOrderedSetAggregateFunctionSqlAstExpression
				implements QueryTransformer {
			private final String arrayTypeName;
			private final String functionName;

			public OracleArrayAggEmulationSqlAstExpression(
					String functionName,
					FunctionRenderer renderer,
					List<? extends SqlAstNode> sqlAstArguments,
					Predicate filter,
					List<SortSpecification> withinGroup,
					ReturnableType<?> type,
					JdbcMappingContainer expressible,
					String arrayTypeName) {
				super(
						functionName,
						renderer,
						sqlAstArguments,
						filter,
						withinGroup,
						type,
						expressible
				);
				this.arrayTypeName = arrayTypeName;
				this.functionName = "json_to_" + arrayTypeName;
			}

			@Override
			public QuerySpec transform(CteContainer cteContainer, QuerySpec querySpec, SqmToSqlAstConverter converter) {
				if ( cteContainer.getCteStatement( functionName ) == null ) {
					cteContainer.addCteObject(
							new SelfRenderingCteObject() {
								@Override
								public String getName() {
									return functionName;
								}

								@Override
								public void render(
										SqlAppender sqlAppender,
										SqlAstTranslator<?> walker,
										SessionFactoryImplementor sessionFactory) {
									sqlAppender.appendSql( "function " );
									sqlAppender.appendSql( functionName );
									sqlAppender.appendSql( "(p_json_array in " );
									sqlAppender.appendSql(
											sessionFactory.getTypeConfiguration().getDdlTypeRegistry()
													.getTypeName(
															SqlTypes.JSON,
															sessionFactory.getJdbcServices().getDialect()
													)
									);
									sqlAppender.appendSql( ") return " );
									sqlAppender.appendSql( arrayTypeName );
									sqlAppender.appendSql( " is v_result " );
									sqlAppender.appendSql( arrayTypeName );
									sqlAppender.appendSql( "; begin select t.value bulk collect into v_result " );
									sqlAppender.appendSql( "from json_table(p_json_array,'$[*]' columns (value path '$')) t;" );
									sqlAppender.appendSql( "return v_result; end; " );
								}
							}
					);
				}
				return querySpec;
			}

			@Override
			public void renderToSql(
					SqlAppender sqlAppender,
					SqlAstTranslator<?> walker,
					SessionFactoryImplementor sessionFactory) {
				// Oracle doesn't have an array_agg function, so we must use the collect function,
				// which requires that we cast the result to the array type.
				// On empty results, we require that array_agg returns null,
				// but Oracle rather returns an empty collection, so we have to handle that.
				// Unfortunately, nullif doesn't work with collection types,
				// so we have to render a case when expression instead
				sqlAppender.append( functionName );
				sqlAppender.append( '(' );
				super.renderToSql( sqlAppender, walker, sessionFactory );
				sqlAppender.appendSql( ')' );
			}
		}
	}

}
