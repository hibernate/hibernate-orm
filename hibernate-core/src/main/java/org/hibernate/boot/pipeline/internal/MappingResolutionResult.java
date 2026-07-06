/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.internal;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/// Mapping resolved through an entry-point bootstrap adapter, together with
/// lifecycle cleanup for any bootstrap-owned resources.
///
/// This is an internal result for callers that need the finalized boot metadata.
/// It lets those callers use the same descriptor/settings/source-resolution path
/// as normal [BootstrapPipeline] construction, then stop before
/// building a [org.hibernate.SessionFactory].
///
/// The returned [metadata][#metadata()] is a [ResolvedMappingImplementor]
/// so that internal SessionFactory construction can reuse the pipeline-aware
/// resolved mapping rather than rebuilding intermediate boot-model products.
///
/// Some resolution entry points create service registries owned by this result.
/// If the metadata is used only for inspection, callers should close this object
/// to release those registries.  Callers that transfer this metadata into a
/// SessionFactory-building path are responsible for carrying the cleanup action
/// into that path.
///
/// This is a transitional shape.  It is expected to collapse into the eventual
/// internal bootstrap-result abstraction as the pipeline products settle.
///
/// @since 9.0
/// @author Steve Ebersole
public record MappingResolutionResult(
		/// Finalized boot metadata resolved by the pipeline.
		ResolvedMappingImplementor metadata,

		/// Resolved configuration values used for mapping resolution.
		Map<String, Object> configurationValues,

		/// Cleanup action for bootstrap-owned services.
		Runnable cleanup) implements AutoCloseable {

	public MappingResolutionResult {
		configurationValues = configurationValues == null
				? Map.of()
				: Collections.unmodifiableMap( new LinkedHashMap<>( configurationValues ) );
		cleanup = cleanup == null ? () -> {} : cleanup;
	}

	public MappingResolutionResult(
			ResolvedMappingImplementor metadata,
			Map<String, Object> configurationValues) {
		this( metadata, configurationValues, null );
	}

	@Override
	public void close() {
		cleanup.run();
	}
}
