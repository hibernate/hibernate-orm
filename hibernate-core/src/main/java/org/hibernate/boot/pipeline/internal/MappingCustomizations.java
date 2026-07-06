/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.internal;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Internal;
import org.hibernate.boot.CacheRegionDefinition;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.ColumnOrderingStrategy;
import org.hibernate.boot.spi.BasicTypeRegistration;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.usertype.UserType;

import jakarta.persistence.SharedCacheMode;

/// Programmatic mapping-resolution customizations supplied by a bootstrap entry point.
///
/// This descriptor carries instructions that are not mapping sources and are not
/// resolved configuration settings, but still need to be applied while resolving
/// mapping details.
///
/// @since 9.0
/// @author Steve Ebersole
@Internal
public record MappingCustomizations(
		Map<String, Class<?>> queryImports,
		List<TypeContributor> typeContributors,
		List<FunctionContributor> functionContributors,
		List<CacheRegionDefinition> cacheRegionDefinitions,
		List<BasicTypeRegistration> basicTypeRegistrations,
		List<UserTypeRegistration> userTypeRegistrations,
		Map<String, SqmFunctionDescriptor> sqlFunctions,
		List<AuxiliaryDatabaseObject> auxiliaryDatabaseObjects,
		List<ConverterDescriptor<?, ?>> attributeConverters,
		ImplicitNamingStrategy implicitNamingStrategy,
		PhysicalNamingStrategy physicalNamingStrategy,
		ColumnOrderingStrategy columnOrderingStrategy,
		SharedCacheMode sharedCacheMode) {
	public static final MappingCustomizations NONE = new MappingCustomizations(
			Map.of(),
			List.of(),
			List.of(),
			List.of(),
			List.of(),
			List.of(),
			Map.of(),
			List.of(),
			List.of(),
			null,
			null,
			null,
			null
	);

	public MappingCustomizations(
			Map<String, Class<?>> queryImports,
			List<TypeContributor> typeContributors,
			List<FunctionContributor> functionContributors,
			List<CacheRegionDefinition> cacheRegionDefinitions) {
		this(
				queryImports,
				typeContributors,
				functionContributors,
				cacheRegionDefinitions,
				List.of(),
				List.of(),
				Map.of(),
				List.of(),
				List.of(),
				null,
				null,
				null,
				null
		);
	}

	public MappingCustomizations {
		queryImports = queryImports == null ? Map.of() : Collections.unmodifiableMap( new LinkedHashMap<>( queryImports ) );
		typeContributors = typeContributors == null ? List.of() : List.copyOf( typeContributors );
		functionContributors = functionContributors == null ? List.of() : List.copyOf( functionContributors );
		cacheRegionDefinitions = cacheRegionDefinitions == null ? List.of() : List.copyOf( cacheRegionDefinitions );
		basicTypeRegistrations = basicTypeRegistrations == null ? List.of() : List.copyOf( basicTypeRegistrations );
		userTypeRegistrations = userTypeRegistrations == null ? List.of() : List.copyOf( userTypeRegistrations );
		sqlFunctions = sqlFunctions == null ? Map.of() : Collections.unmodifiableMap( new LinkedHashMap<>( sqlFunctions ) );
		auxiliaryDatabaseObjects = auxiliaryDatabaseObjects == null ? List.of() : List.copyOf( auxiliaryDatabaseObjects );
		attributeConverters = attributeConverters == null ? List.of() : List.copyOf( attributeConverters );
	}

	public record UserTypeRegistration(UserType<?> type, String[] keys) {
		public UserTypeRegistration {
			keys = keys == null ? new String[0] : keys.clone();
		}

		@Override
		public String[] keys() {
			return keys.clone();
		}
	}
}
