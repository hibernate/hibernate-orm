/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.internal;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.boot.spi.MetadataImplementor;

/// Result of resolving ORM metadata through an entry-point bootstrap adapter.
///
/// This is an internal bridge for callers that need the finalized boot metadata.
/// It lets those callers use the same descriptor/settings/source-resolution path
/// as normal {@link SessionFactoryBootstrap} construction, then stop before
/// building a {@link org.hibernate.SessionFactory}.
///
/// The returned {@linkplain #metadata() metadata} is wrapped as a
/// {@link ResolvedMetadataImplementor}, so a later
/// {@link org.hibernate.boot.Metadata#buildSessionFactory()} call can still use
/// the pipeline-aware resolved metadata rather than rebuilding intermediate
/// boot-model products.
///
/// The bootstrap process creates service registries owned by this result.  If
/// the metadata is used only for inspection, callers should close this object to
/// release those registries.  If the metadata is used to build a
/// SessionFactory, the same cleanup hook is also attached as a SessionFactory
/// observer and will run when that factory is closed.
///
/// This is a transitional shape.  It is expected to collapse into the eventual
/// internal bootstrap-result abstraction as the pipeline products settle.
///
/// @since 9.0
public record MetadataBootstrap(
		/// Finalized boot metadata resolved by the pipeline.
		MetadataImplementor metadata,

		/// Resolved configuration values used for metadata bootstrap.
		Map<String, Object> configurationValues,

		/// Cleanup action for bootstrap-owned services.
		Runnable cleanup) implements AutoCloseable {

	public MetadataBootstrap {
		configurationValues = configurationValues == null
				? Map.of()
				: Collections.unmodifiableMap( new LinkedHashMap<>( configurationValues ) );
		cleanup = cleanup == null ? () -> {} : cleanup;
	}

	public MetadataBootstrap(
			MetadataImplementor metadata,
			Map<String, Object> configurationValues) {
		this( metadata, configurationValues, null );
	}

	@Override
	public void close() {
		cleanup.run();
	}
}
