/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.produce.function;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.query.sqm.produce.function.internal.AbstractFunctionArgumentTypeResolver;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Christian Beikov
 */
public final class StandardFunctionArgumentTypeResolvers {
	/**
	 * Disallow instantiation
	 */
	private StandardFunctionArgumentTypeResolvers() {
	}

	public static final FunctionArgumentTypeResolver NULL =
			(AbstractFunctionArgumentTypeResolver)
					(arguments, argumentIndex, converter) -> null;

	public static final FunctionArgumentTypeResolver IMPLIED_RESULT_TYPE =
			(AbstractFunctionArgumentTypeResolver)
					(arguments, argumentIndex, converter)
							-> converter.resolveFunctionImpliedReturnType();

	public static final FunctionArgumentTypeResolver ARGUMENT_OR_IMPLIED_RESULT_TYPE =
			(AbstractFunctionArgumentTypeResolver)
					(arguments, argumentIndex, converter) -> {
						final int argumentsSize = arguments.size();
						for ( int i = 0; i < argumentIndex; i++ ) {
							if ( arguments.get( i ) instanceof SqmExpression<?> expression ) {
								final var expressible = converter.determineValueMapping( expression );
								if ( expressible != null ) {
									return expressible;
								}
							}
						}
						for ( int i = argumentIndex + 1; i < argumentsSize; i++ ) {
							if ( arguments.get( i ) instanceof SqmExpression<?> expression ) {
								final var expressible = converter.determineValueMapping( expression );
								if ( expressible != null ) {
									return expressible;
								}
							}
						}

		return converter.resolveFunctionImpliedReturnType();
	};

	public static FunctionArgumentTypeResolver invariant(
			TypeConfiguration typeConfiguration,
			FunctionParameterType type) {
		final var expressible = getMappingModelExpressible( typeConfiguration, type );
		return (AbstractFunctionArgumentTypeResolver)
				(arguments, argumentIndex, converter) -> expressible;
	}

	public static FunctionArgumentTypeResolver invariant(
			TypeConfiguration typeConfiguration,
			FunctionParameterType... types) {
		final var expressibles = new MappingModelExpressible[types.length];
		for ( int i = 0; i < types.length; i++ ) {
			expressibles[i] = getMappingModelExpressible( typeConfiguration, types[i] );
		}
		return (AbstractFunctionArgumentTypeResolver)
				(arguments, argumentIndex, converter)
						-> argumentIndex < expressibles.length ? expressibles[argumentIndex] : null;
	}

	public static FunctionArgumentTypeResolver invariant(FunctionParameterType... types) {
		return (AbstractFunctionArgumentTypeResolver)
				(arguments, argumentIndex, converter) -> {
					if ( argumentIndex >= types.length ) {
						return null;
					}
					return getMappingModelExpressible(
							converter.getCreationContext().getTypeConfiguration(),
							types[argumentIndex]
					);
				};
	}

	public static FunctionArgumentTypeResolver impliedOrInvariant(
			TypeConfiguration typeConfiguration,
			FunctionParameterType type) {
		final var expressible = getMappingModelExpressible( typeConfiguration, type );
		return (AbstractFunctionArgumentTypeResolver)
				(arguments, argumentIndex, converter) -> {
					final var mappingModelExpressible = converter.resolveFunctionImpliedReturnType();
					return mappingModelExpressible == null ? expressible : mappingModelExpressible;
				};
	}

	public static FunctionArgumentTypeResolver argumentsOrImplied(int... indices) {
		return (AbstractFunctionArgumentTypeResolver)
				(arguments, argumentIndex, converter) -> {
					final int argumentsSize = arguments.size();
					for ( int index : indices ) {
						if ( index >= argumentIndex || index >= argumentsSize ) {
							break;
						}
						if ( arguments.get( index ) instanceof SqmExpression<?> expression ) {
							final var expressible = converter.determineValueMapping( expression );
							if ( expressible != null ) {
								return expressible;
							}
						}
					}
					for ( int index : indices ) {
						if ( index <= argumentIndex || index >= argumentsSize ) {
							break;
						}
						if ( arguments.get( index ) instanceof SqmExpression<?> expression ) {
							final var expressible = converter.determineValueMapping( expression );
							if ( expressible != null ) {
								return expressible;
							}
						}
					}
					return converter.resolveFunctionImpliedReturnType();
				};
	}

	public static FunctionArgumentTypeResolver composite(FunctionArgumentTypeResolver... resolvers) {
		return (AbstractFunctionArgumentTypeResolver)
				(arguments, argumentIndex, converter) -> {
					for ( var resolver : resolvers ) {
						final var result = resolver.resolveFunctionArgumentType( arguments, argumentIndex, converter );
						if ( result != null ) {
							return result;
						}
					}
					return null;
				};
	}

	public static FunctionArgumentTypeResolver byArgument(FunctionArgumentTypeResolver... resolvers) {
		return (AbstractFunctionArgumentTypeResolver)
				(arguments, argumentIndex, converter)
						-> argumentIndex < resolvers.length
						? resolvers[argumentIndex].resolveFunctionArgumentType( arguments, argumentIndex, converter )
						: null;
	}

	private static MappingModelExpressible<?> getMappingModelExpressible(
			TypeConfiguration typeConfiguration,
			FunctionParameterType type) {
		return switch ( type ) {
			case STRING, STRING_OR_CLOB -> typeConfiguration.getBasicTypeForJavaType( String.class );
			case NUMERIC -> typeConfiguration.getBasicTypeForJavaType( BigDecimal.class );
			case INTEGER -> typeConfiguration.getBasicTypeForJavaType( Integer.class );
			case TEMPORAL -> typeConfiguration.getBasicTypeForJavaType( Timestamp.class );
			case DATE -> typeConfiguration.getBasicTypeForJavaType( Date.class );
			case TIME -> typeConfiguration.getBasicTypeForJavaType( Time.class );
			case BOOLEAN -> typeConfiguration.getBasicTypeForJavaType( Boolean.class );
			default -> null;
		};
	}
}
