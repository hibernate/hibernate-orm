/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function.array;

import java.util.List;

import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.query.ReturnableType;
import org.hibernate.query.SemanticException;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.function.FunctionRenderingSupport;
import org.hibernate.query.sqm.function.SelfRenderingFunctionSqlAstExpression;
import org.hibernate.query.sqm.function.SelfRenderingSqmOrderedSetAggregateFunction;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmDistinct;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.select.SqmOrderByClause;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Distinct;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.SortSpecification;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.descriptor.sql.DdlType;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Christian Beikov
 */
public class OracleCollectArrayAggEmulation extends ArrayAggFunction {

	public OracleCollectArrayAggEmulation() {
		super( "collect", false, false );
	}
	@Override
	public <T> SelfRenderingSqmOrderedSetAggregateFunction<T> generateSqmOrderedSetAggregateFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			SqmPredicate filter,
			SqmOrderByClause withinGroupClause,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine) {
		if ( arguments.get( 0 ) instanceof SqmDistinct<?> ) {
			throw new SemanticException( "Can't emulate distinct clause for Oracle array_agg emulation" );
		}
		if ( filter != null ) {
			throw new SemanticException( "Can't emulate filter clause for Oracle array_agg emulation" );
		}
		return super.generateSqmOrderedSetAggregateFunctionExpression(
				arguments,
				filter,
				withinGroupClause,
				impliedResultType,
				queryEngine
		);
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			Predicate filter,
			List<SortSpecification> withinGroup,
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
		sqlAppender.appendSql( " null on null returning clob" );
		sqlAppender.appendSql( ')' );
		if ( filter != null ) {
			translator.getCurrentClauseStack().push( Clause.WHERE );
			sqlAppender.appendSql( " filter (where " );
			filter.accept( translator );
			sqlAppender.appendSql( ')' );
			translator.getCurrentClauseStack().pop();
		}
	}

//	@Override
//	public <T> SelfRenderingSqmOrderedSetAggregateFunction<T> generateSqmOrderedSetAggregateFunctionExpression(
//			List<? extends SqmTypedNode<?>> arguments,
//			SqmPredicate filter,
//			SqmOrderByClause withinGroupClause,
//			ReturnableType<T> impliedResultType,
//			QueryEngine queryEngine) {
//		return new OracleArrayAggSqmFunction<>(
//				this,
//				this,
//				arguments,
//				filter,
//				withinGroupClause,
//				impliedResultType,
//				getArgumentsValidator(),
//				getReturnTypeResolver(),
//				queryEngine.getCriteriaBuilder(),
//				getName()
//		);
//	}

	protected static class OracleArrayAggSqmFunction<T> extends SelfRenderingSqmOrderedSetAggregateFunction<T> {
		public OracleArrayAggSqmFunction(
				OracleCollectArrayAggEmulation descriptor,
				FunctionRenderingSupport renderingSupport,
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
		public Expression convertToSqlAst(SqmToSqlAstConverter walker) {
			final ReturnableType<?> resultType = resolveResultType( walker );

			List<SqlAstNode> arguments = resolveSqlAstArguments( getArguments(), walker );
			if ( getArgumentsValidator() != null ) {
				getArgumentsValidator().validateSqlTypes( arguments, getFunctionName() );
			}
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
			final TypeConfiguration typeConfiguration = walker.getCreationContext().getSessionFactory().getTypeConfiguration();
			final DdlTypeRegistry ddlTypeRegistry = typeConfiguration.getDdlTypeRegistry();
			final DdlType ddlType = ddlTypeRegistry.getDescriptor(
					pluralType.getJdbcType().getDdlTypeCode()
			);
			final String arrayTypeName = ddlType.getCastTypeName( Size.nil(), pluralType, ddlTypeRegistry );
			return new SelfRenderingFunctionSqlAstExpression(
					getFunctionName(),
					getRenderingSupport(),
					arguments,
					resultType,
					getMappingModelExpressible( walker, resultType, arguments )
			) {
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
					sqlAppender.append( "case when cast(" );
					super.renderToSql( sqlAppender, walker, sessionFactory );
					sqlAppender.appendSql( " as " );
					sqlAppender.appendSql( arrayTypeName );
					sqlAppender.appendSql( ")=" );
					sqlAppender.appendSql( arrayTypeName );
					sqlAppender.appendSql( "() then null else cast(" );
					super.renderToSql( sqlAppender, walker, sessionFactory );
					sqlAppender.appendSql( " as " );
					sqlAppender.appendSql( arrayTypeName );
					sqlAppender.appendSql( ") end" );
				}
			};
		}
	}
}
