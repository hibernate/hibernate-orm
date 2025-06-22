/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function;

import java.sql.Types;
import java.util.List;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.CastType;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.function.FunctionRenderer;
import org.hibernate.query.sqm.function.SelfRenderingFunctionSqlAstExpression;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.produce.function.internal.PatternRenderer;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.CastTarget;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.type.BasicType;

/**
 * ANSI SQL-inspired {@code cast()} function, where the target types
 * are enumerated by {@link CastType}, and portability is achieved
 * by delegating to {@link Dialect#castPattern(CastType, CastType)}.
 *
 * @author Gavin King
 */
public class CastFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	private final Dialect dialect;
	private final CastType booleanCastType;

	public CastFunction(Dialect dialect, int preferredSqlTypeCodeForBoolean) {
		super(
				"cast",
				StandardArgumentsValidators.exactly( 2 ),
				StandardFunctionReturnTypeResolvers.useArgType( 2 ),
				StandardFunctionArgumentTypeResolvers.IMPLIED_RESULT_TYPE
		);
		this.dialect = dialect;
		this.booleanCastType = getBooleanCastType( preferredSqlTypeCodeForBoolean );
	}

	private CastType getBooleanCastType(int preferredSqlTypeCodeForBoolean) {
		return switch (preferredSqlTypeCodeForBoolean) {
			case Types.BIT, Types.SMALLINT, Types.TINYINT -> CastType.INTEGER_BOOLEAN;
			default -> CastType.BOOLEAN;
		};
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final Expression source = (Expression) arguments.get( 0 );
		final JdbcMapping sourceMapping = source.getExpressionType().getSingleJdbcMapping();
		final CastType sourceType = getCastType( sourceMapping );

		final CastTarget castTarget = (CastTarget) arguments.get( 1 );
		final JdbcMapping targetJdbcMapping = castTarget.getExpressionType().getSingleJdbcMapping();
		final CastType targetType = getCastType( targetJdbcMapping );

		if ( sourceType == CastType.OTHER && targetType == CastType.STRING
				&& sourceMapping.getJdbcType().isArray() ) {
			renderCastArrayToString( sqlAppender, arguments.get( 0 ), dialect, walker );
		}
		else {
			new PatternRenderer( dialect.castPattern( sourceType, targetType ) )
					.render( sqlAppender, arguments, walker );
		}
	}

	public static void renderCastArrayToString(
			SqlAppender sqlAppender,
			SqlAstNode arrayArgument,
			Dialect dialect,
			SqlAstTranslator<?> walker) {
		final SessionFactoryImplementor sessionFactory = walker.getSessionFactory();
		final BasicType<?> stringType = sessionFactory.getTypeConfiguration().getBasicTypeForJavaType( String.class );
		final SqmFunctionRegistry functionRegistry = sessionFactory.getQueryEngine().getSqmFunctionRegistry();
		final FunctionRenderer concatDescriptor =
				(FunctionRenderer) functionRegistry.findFunctionDescriptor( "concat" );
		final FunctionRenderer arrayToStringDescriptor =
				(FunctionRenderer) functionRegistry.findFunctionDescriptor( "array_to_string" );
		final boolean caseWhen = dialect.isEmptyStringTreatedAsNull();
		if ( caseWhen ) {
			sqlAppender.append( "case when " );
			arrayArgument.accept( walker );
			sqlAppender.append( " is null then null else " );
		}

		concatDescriptor.render(
				sqlAppender,
				List.of(
						new QueryLiteral<>( "[", stringType ),
						new SelfRenderingFunctionSqlAstExpression<>(
								"array_to_string",
								arrayToStringDescriptor,
								List.of(
										arrayArgument,
										new QueryLiteral<>( ",", stringType ),
										new QueryLiteral<>( "null", stringType )
								),
								stringType,
								stringType
						),
						new QueryLiteral<>( "]", stringType )
				),
				stringType,
				walker
		);
		if ( caseWhen ) {
			sqlAppender.append( " end" );
		}
	}

	private CastType getCastType(JdbcMapping sourceMapping) {
		final CastType castType = sourceMapping.getCastType();
		return castType == CastType.BOOLEAN ? booleanCastType : castType;
	}

//	@Override
//	protected <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
//			List<SqmTypedNode<?>> arguments,
//			ReturnableType<T> impliedResultType,
//			QueryEngine queryEngine,
//			TypeConfiguration typeConfiguration) {
//		SqmCastTarget<?> targetType = (SqmCastTarget<?>) arguments.get(1);
//		SqmExpression<?> arg = (SqmExpression<?>) arguments.get(0);
//
//		CastType to = CastType.from( targetType.getType() );
//		CastType from = CastType.from( arg.getNodeType() );
//
//		return queryEngine.getSqmFunctionRegistry()
//				.patternDescriptorBuilder( "cast", dialect.castPattern( from, to ) )
//				.setExactArgumentCount( 2 )
//				.setReturnTypeResolver( useArgType( 2 ) )
//				.descriptor()
//				.generateSqmExpression(
//						arguments,
//						impliedResultType,
//						queryEngine,
//						typeConfiguration
//				);
//	}

	@Override
	public String getArgumentListSignature() {
		return "(arg as Type)";
	}

}
