/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.query.ReturnableType;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.function.FunctionKind;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionArgumentException;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.CastTarget;
import org.hibernate.sql.ast.tree.expression.Distinct;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.BasicType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.ArrayJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.ObjectJdbcType;
import org.hibernate.type.spi.TypeConfiguration;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.NUMERIC;

/**
 * @author Christian Beikov
 */
public class AvgFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	private final SqlAstNodeRenderingMode defaultArgumentRenderingMode;
	private final CastFunction castFunction;
	private final BasicType<Double> doubleType;

	public AvgFunction(
			Dialect dialect,
			TypeConfiguration typeConfiguration,
			SqlAstNodeRenderingMode defaultArgumentRenderingMode) {
		super(
				"avg",
				FunctionKind.AGGREGATE,
				new Validator(),
				new ReturnTypeResolver( typeConfiguration ),
				StandardFunctionArgumentTypeResolvers.invariant( typeConfiguration, NUMERIC )
		);
		this.defaultArgumentRenderingMode = defaultArgumentRenderingMode;
		doubleType = typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.DOUBLE );
		//This is kinda wrong, we're supposed to use findFunctionDescriptor("cast"), not instantiate CastFunction
		//However, since no Dialects currently override the cast() function, it's OK for now
		castFunction = new CastFunction( dialect, dialect.getPreferredSqlTypeCodeForBoolean() );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		render( sqlAppender, sqlAstArguments, null, returnType, walker );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			Predicate filter,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> translator) {
		final boolean caseWrapper = filter != null && !translator.supportsFilterClause();
		sqlAppender.appendSql( "avg(" );
		final Expression arg;
		if ( sqlAstArguments.get( 0 ) instanceof Distinct ) {
			sqlAppender.appendSql( "distinct " );
			arg = ( (Distinct) sqlAstArguments.get( 0 ) ).getExpression();
		}
		else {
			arg = (Expression) sqlAstArguments.get( 0 );
		}
		if ( caseWrapper ) {
			translator.getCurrentClauseStack().push( Clause.WHERE );
			sqlAppender.appendSql( "case when " );
			filter.accept( translator );
			translator.getCurrentClauseStack().pop();
			sqlAppender.appendSql( " then " );
			renderArgument( sqlAppender, translator, arg );
			sqlAppender.appendSql( " else null end)" );
		}
		else {
			renderArgument( sqlAppender, translator, arg );
			sqlAppender.appendSql( ')' );
			if ( filter != null ) {
				translator.getCurrentClauseStack().push( Clause.WHERE );
				sqlAppender.appendSql( " filter (where " );
				filter.accept( translator );
				sqlAppender.appendSql( ')' );
				translator.getCurrentClauseStack().pop();
			}
		}
	}

	private void renderArgument(SqlAppender sqlAppender, SqlAstTranslator<?> translator, Expression realArg) {
		final JdbcMapping sourceMapping = realArg.getExpressionType().getSingleJdbcMapping();
		// Only cast to float/double if this is an integer
		if ( sourceMapping.getJdbcType().isInteger() ) {
			castFunction.render(
					sqlAppender,
					Arrays.asList( realArg, new CastTarget( doubleType ) ),
					doubleType,
					translator
			);
		}
		else {
			translator.render( realArg, defaultArgumentRenderingMode );
		}
	}

	@Override
	public String getArgumentListSignature() {
		return "(NUMERIC arg)";
	}

	public static class Validator implements ArgumentsValidator {

		public static final ArgumentsValidator INSTANCE = new Validator();

		@Override
		public void validate(
				List<? extends SqmTypedNode<?>> arguments,
				String functionName,
				TypeConfiguration typeConfiguration) {
			if ( arguments.size() != 1 ) {
				throw new FunctionArgumentException(
						String.format(
								Locale.ROOT,
								"Function %s() has %d parameters, but %d arguments given",
								functionName,
								1,
								arguments.size()
						)
				);
			}
			final SqmTypedNode<?> argument = arguments.get( 0 );
			final SqmExpressible<?> expressible = argument.getExpressible();
			final DomainType<?> domainType;
			if ( expressible != null && ( domainType = expressible.getSqmType() ) != null ) {
				final JdbcType jdbcType = getJdbcType( domainType, typeConfiguration );
				if ( !isNumeric( jdbcType ) ) {
					throw new FunctionArgumentException(
							String.format(
									"Parameter %d of function '%s()' has type '%s', but argument is of type '%s'",
									1,
									functionName,
									NUMERIC,
									domainType.getTypeName()
							)
					);
				}
			}
		}

		private static boolean isNumeric(JdbcType jdbcType) {
			final int sqlTypeCode = jdbcType.getDefaultSqlTypeCode();
			if ( SqlTypes.isNumericType( sqlTypeCode ) ) {
				return true;
			}
			if ( jdbcType instanceof ArrayJdbcType ) {
				return isNumeric( ( (ArrayJdbcType) jdbcType ).getElementJdbcType() );
			}
			return false;
		}

		private static JdbcType getJdbcType(DomainType<?> domainType, TypeConfiguration typeConfiguration) {
			if ( domainType instanceof JdbcMapping ) {
				return ( (JdbcMapping) domainType ).getJdbcType();
			}
			else {
				final JavaType<?> javaType = domainType.getExpressibleJavaType();
				if ( javaType.getJavaTypeClass().isEnum() ) {
					// we can't tell if the enum is mapped STRING or ORDINAL
					return ObjectJdbcType.INSTANCE;
				}
				else {
					return javaType.getRecommendedJdbcType( typeConfiguration.getCurrentBaseSqlTypeIndicators() );
				}
			}
		}

		@Override
		public String getSignature() {
			return "(arg)";
		}
	}

	public static class ReturnTypeResolver implements FunctionReturnTypeResolver {

		private final BasicType<Double> doubleType;

		public ReturnTypeResolver(TypeConfiguration typeConfiguration) {
			this.doubleType = typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.DOUBLE );
		}

		@Override
		public BasicValuedMapping resolveFunctionReturnType(
				Supplier<BasicValuedMapping> impliedTypeAccess,
				List<? extends SqlAstNode> arguments) {
			final BasicValuedMapping impliedType = impliedTypeAccess.get();
			if ( impliedType != null ) {
				return impliedType;
			}
			final JdbcMapping jdbcMapping = ( (Expression) arguments.get( 0 ) ).getExpressionType().getSingleJdbcMapping();
			if ( jdbcMapping instanceof BasicPluralType<?, ?> ) {
				return (BasicValuedMapping) jdbcMapping;
			}
			return doubleType;
		}

		@Override
		public ReturnableType<?> resolveFunctionReturnType(
				ReturnableType<?> impliedType,
				@Nullable SqmToSqlAstConverter converter,
				List<? extends SqmTypedNode<?>> arguments,
				TypeConfiguration typeConfiguration) {
			final SqmExpressible<?> expressible = arguments.get( 0 ).getExpressible();
			final DomainType<?> domainType;
			if ( expressible != null && ( domainType = expressible.getSqmType() ) != null ) {
				if ( domainType instanceof BasicPluralType<?, ?> ) {
					return (ReturnableType<?>) domainType;
				}
			}
			return typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.DOUBLE );
		}
	}

}
