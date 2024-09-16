/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.produce.function;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.List;

import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.query.sqm.tree.SqmTypedNode;
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

	public static final FunctionArgumentTypeResolver NULL = (function, argumentIndex, converter) -> {
		return null;
	};

	public static final FunctionArgumentTypeResolver IMPLIED_RESULT_TYPE = (function, argumentIndex, converter) -> {
		return converter.resolveFunctionImpliedReturnType();
	};

	public static final FunctionArgumentTypeResolver ARGUMENT_OR_IMPLIED_RESULT_TYPE = (function, argumentIndex, converter) -> {
		final List<? extends SqmTypedNode<?>> arguments = function.getArguments();
		final int argumentsSize = arguments.size();
		for ( int i = 0 ; i < argumentIndex; i++ ) {
			final SqmTypedNode<?> node = arguments.get( i );
			if ( node instanceof SqmExpression<?> ) {
				final MappingModelExpressible<?> expressible = converter.determineValueMapping( (SqmExpression<?>) node );
				if ( expressible != null ) {
					return expressible;
				}
			}
		}
		for ( int i = argumentIndex + 1 ; i < argumentsSize; i++ ) {
			final SqmTypedNode<?> node = arguments.get( i );
			if ( node instanceof SqmExpression<?> ) {
				final MappingModelExpressible<?> expressible = converter.determineValueMapping( (SqmExpression<?>) node );
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
		final MappingModelExpressible<?> expressible = getMappingModelExpressible( typeConfiguration, type );
		return (function, argumentIndex, converter) -> expressible;
	}

	public static FunctionArgumentTypeResolver invariant(
			TypeConfiguration typeConfiguration,
			FunctionParameterType... types) {
		final MappingModelExpressible<?>[] expressibles = new MappingModelExpressible[types.length];
		for ( int i = 0; i < types.length; i++ ) {
			expressibles[i] = getMappingModelExpressible( typeConfiguration, types[i] );
		}

		return (function, argumentIndex, converter) -> expressibles[argumentIndex];
	}

	public static FunctionArgumentTypeResolver invariant(FunctionParameterType... types) {
		return (function, argumentIndex, converter) -> getMappingModelExpressible(
				function.nodeBuilder().getTypeConfiguration(),
				types[argumentIndex]
		);
	}

	public static FunctionArgumentTypeResolver impliedOrInvariant(
			TypeConfiguration typeConfiguration,
			FunctionParameterType type) {
		final MappingModelExpressible<?> expressible = getMappingModelExpressible( typeConfiguration, type );
		return (function, argumentIndex, converter) -> {
			final MappingModelExpressible<?> mappingModelExpressible = converter.resolveFunctionImpliedReturnType();
			if ( mappingModelExpressible != null ) {
				return mappingModelExpressible;
			}
			return expressible;
		};
	}

	public static FunctionArgumentTypeResolver argumentsOrImplied(int... indices) {
		return (function, argumentIndex, converter) -> {
			final List<? extends SqmTypedNode<?>> arguments = function.getArguments();
			final int argumentsSize = arguments.size();
			for ( int index : indices ) {
				if ( index >= argumentIndex || index >= argumentsSize ) {
					break;
				}
				final SqmTypedNode<?> node = arguments.get( index );
				if ( node instanceof SqmExpression<?> ) {
					final MappingModelExpressible<?> expressible = converter.determineValueMapping( (SqmExpression<?>) node );
					if ( expressible != null ) {
						return expressible;
					}
				}
			}
			for ( int index : indices ) {
				if ( index <= argumentIndex || index >= argumentsSize ) {
					break;
				}
				final SqmTypedNode<?> node = arguments.get( index );
				if ( node instanceof SqmExpression<?> ) {
					final MappingModelExpressible<?> expressible = converter.determineValueMapping( (SqmExpression<?>) node );
					if ( expressible != null ) {
						return expressible;
					}
				}
			}

			return converter.resolveFunctionImpliedReturnType();
		};
	}

	public static FunctionArgumentTypeResolver composite(FunctionArgumentTypeResolver... resolvers) {
		return (function, argumentIndex, converter) -> {
			for ( FunctionArgumentTypeResolver resolver : resolvers ) {
				final MappingModelExpressible<?> result = resolver.resolveFunctionArgumentType(
						function,
						argumentIndex,
						converter
				);
				if ( result != null ) {
					return result;
				}
			}

			return null;
		};
	}

	public static FunctionArgumentTypeResolver byArgument(FunctionArgumentTypeResolver... resolvers) {
		return (function, argumentIndex, converter) -> {
			return resolvers[argumentIndex].resolveFunctionArgumentType( function, argumentIndex, converter );
		};
	}

	private static MappingModelExpressible<?> getMappingModelExpressible(
			TypeConfiguration typeConfiguration,
			FunctionParameterType type) {
		switch ( type ) {
			case STRING:
			case STRING_OR_CLOB:
				return typeConfiguration.getBasicTypeForJavaType( String.class );
			case NUMERIC:
				return typeConfiguration.getBasicTypeForJavaType( BigDecimal.class );
			case INTEGER:
				return typeConfiguration.getBasicTypeForJavaType( Integer.class );
			case TEMPORAL:
				return typeConfiguration.getBasicTypeForJavaType( Timestamp.class );
			case DATE:
				return typeConfiguration.getBasicTypeForJavaType( Date.class );
			case TIME:
				return typeConfiguration.getBasicTypeForJavaType( Time.class );
			case BOOLEAN:
				return typeConfiguration.getBasicTypeForJavaType( Boolean.class );
		}
		return null;
	}
}
