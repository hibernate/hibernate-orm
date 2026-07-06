/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.AssertionFailure;
import org.hibernate.boot.CacheRegionDefinition;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;

/// Explicit mapping-resolution contributions collected before metadata binding.
///
/// @since 9.0
/// @author Steve Ebersole
public record MappingResolutionContributions(
		Map<String, SqmFunctionDescriptor> sqlFunctions,
		List<AuxiliaryDatabaseObject> auxiliaryDatabaseObjects,
		List<ConverterDescriptor<?, ?>> attributeConverters,
		List<CacheRegionDefinition> cacheRegionDefinitions,
		SqmFunctionRegistry functionRegistry) {
	public static final MappingResolutionContributions EMPTY = builder().build();

	public MappingResolutionContributions {
		sqlFunctions = sqlFunctions == null ? Map.of() : Collections.unmodifiableMap( new LinkedHashMap<>( sqlFunctions ) );
		auxiliaryDatabaseObjects = auxiliaryDatabaseObjects == null ? List.of() : List.copyOf( auxiliaryDatabaseObjects );
		attributeConverters = attributeConverters == null ? List.of() : List.copyOf( attributeConverters );
		cacheRegionDefinitions = cacheRegionDefinitions == null ? List.of() : List.copyOf( cacheRegionDefinitions );
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private final Map<String, SqmFunctionDescriptor> sqlFunctions = new LinkedHashMap<>();
		private final List<AuxiliaryDatabaseObject> auxiliaryDatabaseObjects = new ArrayList<>();
		private final Map<Class<?>, ConverterDescriptor<?, ?>> attributeConverters = new LinkedHashMap<>();
		private final List<CacheRegionDefinition> cacheRegionDefinitions = new ArrayList<>();
		private SqmFunctionRegistry functionRegistry;

		public Builder functionRegistry(SqmFunctionRegistry functionRegistry) {
			this.functionRegistry = functionRegistry;
			return this;
		}

		public Builder addSqlFunction(String functionName, SqmFunctionDescriptor function) {
			sqlFunctions.put( functionName, function );
			return this;
		}

		public Builder addSqlFunctions(Map<String, SqmFunctionDescriptor> sqlFunctions) {
			if ( sqlFunctions != null ) {
				sqlFunctions.forEach( this::addSqlFunction );
			}
			return this;
		}

		public Builder addAuxiliaryDatabaseObject(AuxiliaryDatabaseObject auxiliaryDatabaseObject) {
			auxiliaryDatabaseObjects.add( auxiliaryDatabaseObject );
			return this;
		}

		public Builder addAuxiliaryDatabaseObjects(Collection<AuxiliaryDatabaseObject> auxiliaryDatabaseObjects) {
			if ( auxiliaryDatabaseObjects != null ) {
				auxiliaryDatabaseObjects.forEach( this::addAuxiliaryDatabaseObject );
			}
			return this;
		}

		public Builder addAttributeConverter(ConverterDescriptor<?, ?> descriptor) {
			final var attributeConverterClass = descriptor.getAttributeConverterClass();
			final Object previous = attributeConverters.put( attributeConverterClass, descriptor );
			if ( previous != null ) {
				throw new AssertionFailure(
						String.format(
								"AttributeConverter class [%s] registered multiple times",
								attributeConverterClass
						)
				);
			}
			return this;
		}

		public Builder addAttributeConverters(Collection<ConverterDescriptor<?, ?>> attributeConverters) {
			if ( attributeConverters != null ) {
				attributeConverters.forEach( this::addAttributeConverter );
			}
			return this;
		}

		public Builder addCacheRegionDefinition(CacheRegionDefinition cacheRegionDefinition) {
			cacheRegionDefinitions.add( cacheRegionDefinition );
			return this;
		}

		public Builder addCacheRegionDefinitions(Collection<CacheRegionDefinition> cacheRegionDefinitions) {
			if ( cacheRegionDefinitions != null ) {
				cacheRegionDefinitions.forEach( this::addCacheRegionDefinition );
			}
			return this;
		}

		public MappingResolutionContributions build() {
			return new MappingResolutionContributions(
					sqlFunctions,
					auxiliaryDatabaseObjects,
					List.copyOf( attributeConverters.values() ),
					cacheRegionDefinitions,
					functionRegistry
			);
		}
	}
}
