/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.produce.function;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.query.sqm.produce.function.internal.AbstractFunctionArgumentTypeResolver;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
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

	public static final FunctionArgumentTypeResolver NULL = new AbstractFunctionArgumentTypeResolver() {
		@Override
		public @Nullable MappingModelExpressible<?> resolveFunctionArgumentType(List<? extends SqmTypedNode<?>> arguments, int argumentIndex, SqmToSqlAstConverter converter) {
			return null;
		}
	};

	public static final FunctionArgumentTypeResolver IMPLIED_RESULT_TYPE = new AbstractFunctionArgumentTypeResolver() {
		@Override
		public @Nullable MappingModelExpressible<?> resolveFunctionArgumentType(List<? extends SqmTypedNode<?>> arguments, int argumentIndex, SqmToSqlAstConverter converter) {
			return converter.resolveFunctionImpliedReturnType();
		}
	};

	public static final FunctionArgumentTypeResolver ARGUMENT_OR_IMPLIED_RESULT_TYPE = new AbstractFunctionArgumentTypeResolver() {
		@Override
		public @Nullable MappingModelExpressible<?> resolveFunctionArgumentType(List<? extends SqmTypedNode<?>> arguments, int argumentIndex, SqmToSqlAstConverter converter) {
			final int argumentsSize = arguments.size();
			for ( int i = 0; i < argumentIndex; i++ ) {
				final SqmTypedNode<?> node = arguments.get( i );
				if ( node instanceof SqmExpression<?> ) {
					final MappingModelExpressible<?> expressible = converter.determineValueMapping(
							(SqmExpression<?>) node );
					if ( expressible != null ) {
						return expressible;
					}
				}
			}
			for ( int i = argumentIndex + 1; i < argumentsSize; i++ ) {
				final SqmTypedNode<?> node = arguments.get( i );
				if ( node instanceof SqmExpression<?> ) {
					final MappingModelExpressible<?> expressible = converter.determineValueMapping(
							(SqmExpression<?>) node );
					if ( expressible != null ) {
						return expressible;
					}
				}
			}

			return converter.resolveFunctionImpliedReturnType();
		}
	};

	public static FunctionArgumentTypeResolver invariant(
			TypeConfiguration typeConfiguration,
			FunctionParameterType type) {
		final MappingModelExpressible<?> expressible = getMappingModelExpressible( typeConfiguration, type );
		return new AbstractFunctionArgumentTypeResolver() {
			@Override
			public @Nullable MappingModelExpressible<?> resolveFunctionArgumentType(List<? extends SqmTypedNode<?>> arguments, int argumentIndex, SqmToSqlAstConverter converter) {
				return expressible;
			}
		};
	}

	public static FunctionArgumentTypeResolver invariant(
			TypeConfiguration typeConfiguration,
			FunctionParameterType... types) {
		final MappingModelExpressible<?>[] expressibles = new MappingModelExpressible[types.length];
		for ( int i = 0; i < types.length; i++ ) {
			expressibles[i] = getMappingModelExpressible( typeConfiguration, types[i] );
		}

		return new AbstractFunctionArgumentTypeResolver() {
			@Override
			public @Nullable MappingModelExpressible<?> resolveFunctionArgumentType(List<? extends SqmTypedNode<?>> arguments, int argumentIndex, SqmToSqlAstConverter converter) {
				return argumentIndex < expressibles.length ? expressibles[argumentIndex] : null;
			}
		};
	}

	public static FunctionArgumentTypeResolver invariant(FunctionParameterType... types) {
		return new AbstractFunctionArgumentTypeResolver() {
			@Override
			public @Nullable MappingModelExpressible<?> resolveFunctionArgumentType(List<? extends SqmTypedNode<?>> arguments, int argumentIndex, SqmToSqlAstConverter converter) {
				if ( argumentIndex >= types.length ) {
					return null;
				}
				return getMappingModelExpressible(
						converter.getCreationContext().getTypeConfiguration(),
						types[argumentIndex]
				);
			}
		};
	}

	public static FunctionArgumentTypeResolver impliedOrInvariant(
			TypeConfiguration typeConfiguration,
			FunctionParameterType type) {
		final MappingModelExpressible<?> expressible = getMappingModelExpressible( typeConfiguration, type );
		return new AbstractFunctionArgumentTypeResolver() {
			@Override
			public @Nullable MappingModelExpressible<?> resolveFunctionArgumentType(List<? extends SqmTypedNode<?>> arguments, int argumentIndex, SqmToSqlAstConverter converter) {
				final MappingModelExpressible<?> mappingModelExpressible = converter.resolveFunctionImpliedReturnType();
				if ( mappingModelExpressible != null ) {
					return mappingModelExpressible;
				}
				return expressible;
			}
		};
	}

	public static FunctionArgumentTypeResolver argumentsOrImplied(int... indices) {
		return new AbstractFunctionArgumentTypeResolver() {
			@Override
			public @Nullable MappingModelExpressible<?> resolveFunctionArgumentType(List<? extends SqmTypedNode<?>> arguments, int argumentIndex, SqmToSqlAstConverter converter) {
				final int argumentsSize = arguments.size();
				for ( int index : indices ) {
					if ( index >= argumentIndex || index >= argumentsSize ) {
						break;
					}
					final SqmTypedNode<?> node = arguments.get( index );
					if ( node instanceof SqmExpression<?> ) {
						final MappingModelExpressible<?> expressible = converter.determineValueMapping(
								(SqmExpression<?>) node );
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
						final MappingModelExpressible<?> expressible = converter.determineValueMapping(
								(SqmExpression<?>) node );
						if ( expressible != null ) {
							return expressible;
						}
					}
				}

				return converter.resolveFunctionImpliedReturnType();
			}
		};
	}

	public static FunctionArgumentTypeResolver composite(FunctionArgumentTypeResolver... resolvers) {
		return new AbstractFunctionArgumentTypeResolver() {
			@Override
			public @Nullable MappingModelExpressible<?> resolveFunctionArgumentType(List<? extends SqmTypedNode<?>> arguments, int argumentIndex, SqmToSqlAstConverter converter) {
				for ( FunctionArgumentTypeResolver resolver : resolvers ) {
					final MappingModelExpressible<?> result = resolver.resolveFunctionArgumentType(
							arguments,
							argumentIndex,
							converter
					);
					if ( result != null ) {
						return result;
					}
				}

				return null;
			}
		};
	}

	public static FunctionArgumentTypeResolver byArgument(FunctionArgumentTypeResolver... resolvers) {
		return new AbstractFunctionArgumentTypeResolver() {
			@Override
			public @Nullable MappingModelExpressible<?> resolveFunctionArgumentType(List<? extends SqmTypedNode<?>> arguments, int argumentIndex, SqmToSqlAstConverter converter) {
				return argumentIndex < resolvers.length
						? resolvers[argumentIndex].resolveFunctionArgumentType( arguments, argumentIndex, converter )
						: null;
			}
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
