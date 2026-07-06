/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.internal;

import java.util.HashMap;

import org.hibernate.Internal;
import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.boot.pipeline.internal.settings.SettingsResolver;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.config.spi.ConfigurationService;

import jakarta.persistence.FetchType;

/// Helper for resolving boot metadata from the 9.0 mapping-source pipeline.
///
/// @since 9.0
/// @author Steve Ebersole
@Internal
public final class MetadataBuildingHelper {
	private MetadataBuildingHelper() {
	}

	public static MetadataImplementor buildMetadata(StandardServiceRegistry serviceRegistry, MappingSources mappingSources) {
		return buildMetadata( serviceRegistry, mappingSources, MappingCustomizations.NONE );
	}

	public static MetadataImplementor buildMetadata(
			StandardServiceRegistry serviceRegistry,
			MappingSources mappingSources,
			MappingCustomizations mappingCustomizations) {
		final var configurationValues = new HashMap<>();
		configurationValues.putAll( serviceRegistry.requireService( ConfigurationService.class ).getSettings() );
		final var bootstrapSettings = SettingsResolver.resolveBootstrapSettings( configurationValues, false );
		final var mappingSettings = SettingsResolver.resolveMappingSettings( bootstrapSettings, FetchType.EAGER );
		return new ResolvedMappingImplementor( MappingResolutionPipeline.resolve(
				bootstrapSettings,
				mappingSettings,
				mappingSources,
				mappingCustomizations,
				serviceRegistry
		) );
	}
}
