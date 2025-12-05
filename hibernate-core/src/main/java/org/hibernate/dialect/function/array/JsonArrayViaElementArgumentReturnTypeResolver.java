/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import java.util.List;
import java.util.function.Supplier;

import org.hibernate.internal.build.AllowReflection;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.type.BasicType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.BasicPluralJavaType;
import org.hibernate.type.descriptor.jdbc.DelegatingJdbcTypeIndicators;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.spi.TypeConfiguration;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A {@link FunctionReturnTypeResolver} that resolves a JSON encoded array type based on the arguments,
 * which are supposed to be of the element type. The inferred type and implied type have precedence though.
 */
public class JsonArrayViaElementArgumentReturnTypeResolver implements FunctionReturnTypeResolver {

	public static final FunctionReturnTypeResolver INSTANCE = new JsonArrayViaElementArgumentReturnTypeResolver();

	private JsonArrayViaElementArgumentReturnTypeResolver() {
	}

	@Override
	public ReturnableType<?> resolveFunctionReturnType(
			ReturnableType<?> impliedType,
			@Nullable SqmToSqlAstConverter converter,
			List<? extends SqmTypedNode<?>> arguments,
			TypeConfiguration typeConfiguration) {
		if ( converter != null ) {
			if ( converter.isInTypeInference() ) {
				// Don't default to a Json array when in type inference mode.
				// Comparing e.g. `array() = (select array_agg() ...)` will trigger this resolver
				// while inferring the type for `array()`, which we want to avoid.
				return null;
			}
			final MappingModelExpressible<?> inferredType = converter.resolveFunctionImpliedReturnType();
			if ( inferredType != null ) {
				if ( inferredType instanceof ReturnableType<?> returnableType ) {
					return returnableType;
				}
				else if ( inferredType instanceof BasicValuedMapping basicValuedMapping ) {
					return (ReturnableType<?>) basicValuedMapping.getJdbcMapping();
				}
			}
		}
		if ( impliedType != null ) {
			return impliedType;
		}
		for ( SqmTypedNode<?> argument : arguments ) {
			final DomainType<?> sqmType = argument.getExpressible().getSqmType();
			if ( sqmType instanceof ReturnableType<?> ) {
				return resolveJsonArrayType( sqmType, typeConfiguration );
			}
		}
		return null;
	}

	@Override
	public BasicValuedMapping resolveFunctionReturnType(
			Supplier<BasicValuedMapping> impliedTypeAccess,
			List<? extends SqlAstNode> arguments) {
		return null;
	}

	@AllowReflection
	public static <T> BasicType<?> resolveJsonArrayType(DomainType<T> elementType, TypeConfiguration typeConfiguration) {
		@SuppressWarnings("unchecked")
		final var arrayJavaType =
				(BasicPluralJavaType<T>)
						typeConfiguration.getJavaTypeRegistry()
								.resolveArrayDescriptor( elementType.getJavaType() );
		final JdbcTypeIndicators currentBaseSqlTypeIndicators = typeConfiguration.getCurrentBaseSqlTypeIndicators();
		return arrayJavaType.resolveType(
				typeConfiguration,
				currentBaseSqlTypeIndicators.getDialect(),
				(BasicType<T>) elementType,
				null,
				new DelegatingJdbcTypeIndicators( currentBaseSqlTypeIndicators ) {
					@Override
					public Integer getExplicitJdbcTypeCode() {
						return SqlTypes.JSON_ARRAY;
					}

					@Override
					public  int getPreferredSqlTypeCodeForArray(int elementSqlTypeCode) {
						return SqlTypes.JSON_ARRAY;
					}
				}
		);
	}
}
