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
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.boot.model.TypeContributor;

/// Programmatic metadata-build customizations supplied by a bootstrap entry point.
///
/// This descriptor carries instructions that are not mapping sources and are not
/// resolved configuration settings, but still need to be applied while resolving
/// boot metadata.
///
/// @since 9.0
/// @author Steve Ebersole
@Internal
public record MetadataCustomizations(
		Map<String, Class<?>> queryImports,
		List<TypeContributor> typeContributors,
		List<FunctionContributor> functionContributors,
		List<CacheRegionDefinition> cacheRegionDefinitions) {
	public static final MetadataCustomizations NONE = new MetadataCustomizations(
			Map.of(),
			List.of(),
			List.of(),
			List.of()
	);

	public MetadataCustomizations {
		queryImports = queryImports == null ? Map.of() : Collections.unmodifiableMap( new LinkedHashMap<>( queryImports ) );
		typeContributors = typeContributors == null ? List.of() : List.copyOf( typeContributors );
		functionContributors = functionContributors == null ? List.of() : List.copyOf( functionContributors );
		cacheRegionDefinitions = cacheRegionDefinitions == null ? List.of() : List.copyOf( cacheRegionDefinitions );
	}
}
