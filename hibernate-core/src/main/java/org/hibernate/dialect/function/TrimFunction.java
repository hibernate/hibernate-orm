/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.TrimSpec;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.FunctionArgumentException;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.produce.function.internal.PatternRenderer;
import org.hibernate.query.sqm.sql.internal.SqmParameterInterpretation;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.expression.TrimSpecification;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.STRING;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.TRIM_SPEC;
import static org.hibernate.type.SqlTypes.isCharacterType;

/**
 * ANSI SQL-standard {@code trim()} function, which has a funny syntax
 * involving a {@link TrimSpec}, and portability is achieved using
 * {@link Dialect#trimPattern(TrimSpec, boolean)}.
 * <p>
 * For example, {@code trim(leading ' ' from text)}.
 *
 * @author Gavin King
 */
public class TrimFunction extends AbstractSqmSelfRenderingFunctionDescriptor {
	private final Dialect dialect;
	private final SqlAstNodeRenderingMode argumentRenderingMode;

	public TrimFunction(Dialect dialect, TypeConfiguration typeConfiguration) {
		this( dialect, typeConfiguration, SqlAstNodeRenderingMode.DEFAULT );
	}

	public TrimFunction(
			Dialect dialect,
			TypeConfiguration typeConfiguration,
			SqlAstNodeRenderingMode argumentRenderingMode) {
		super(
				"trim",
				new ArgumentTypesValidator(
						StandardArgumentsValidators.exactly( 3 ),
						TRIM_SPEC, STRING, STRING
				),
				StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.STRING )
				),
				StandardFunctionArgumentTypeResolvers.invariant( typeConfiguration, TRIM_SPEC, STRING, STRING )
		);
		this.dialect = dialect;
		this.argumentRenderingMode = argumentRenderingMode;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final TrimSpec specification = ( (TrimSpecification) sqlAstArguments.get( 0 ) ).getSpecification();
		final SqlAstNode trimCharacter = sqlAstArguments.get( 1 );
		final boolean isWhitespace = isWhitespace( trimCharacter );
		final Expression sourceExpr = (Expression) sqlAstArguments.get( 2 );

		final String trim = dialect.trimPattern( specification, isWhitespace );

		final List<? extends SqlAstNode> args =
				isWhitespace ? List.of( sourceExpr ) : List.of( sourceExpr, trimCharacter );
		new PatternRenderer( trim, argumentRenderingMode ).render( sqlAppender, args, walker );
	}

	private static boolean isWhitespace(SqlAstNode trimCharacter) {
		if ( trimCharacter instanceof Literal literal ) {
			final char literalValue = (char) literal.getLiteralValue();
			return literalValue == ' ';
		}
		else if ( trimCharacter instanceof SqmParameterInterpretation parameterInterpretation ) {
			final JdbcType jdbcType =
					parameterInterpretation.getExpressionType().getSingleJdbcMapping().getJdbcType();
			if ( !isCharacterType( jdbcType.getJdbcTypeCode() ) ) {
				throw new FunctionArgumentException( String.format(
						"Expected parameter used as trim character to be character typed, instead was [%s]",
						jdbcType.getFriendlyName()
				) );
			}
			return false;
		}
		else {
			throw new IllegalArgumentException( "Trim character must be a literal string or parameter" );
		}
	}

//	@Override
//	@SuppressWarnings("unchecked")
//	public <T> SelfRenderingSqlFunctionExpression<T> generateSqmFunctionExpression(
//			List<SqmTypedNode<?>> arguments,
//			ReturnableType<T> impliedResultType,
//			QueryEngine queryEngine,
//			TypeConfiguration typeConfiguration) {
//		final TrimSpec specification = ( (SqmTrimSpecification) arguments.get( 0 ) ).getSpecification();
//		final char trimCharacter = ( (SqmLiteral<Character>) arguments.get( 1 ) ).getLiteralValue();
//		final SqmExpression sourceExpr = (SqmExpression) arguments.get( 2 );
//
//		String trim = dialect.trimPattern( specification, trimCharacter );
//		return queryEngine.getSqmFunctionRegistry()
//				.patternDescriptorBuilder( "trim", trim )
//				.setInvariantType( StandardBasicTypes.STRING )
//				.setExactArgumentCount( 1 )
//				.descriptor() //TODO: we could cache the 6 variations here
//				.generateSqmExpression(
//						Collections.singletonList( sourceExpr ),
//						impliedResultType,
//						queryEngine,
//						typeConfiguration
//				);
//	}

	@Override
	public String getArgumentListSignature() {
		return "([[{leading|trailing|both} ][STRING arg0 ]from] STRING arg1)";
	}
}
