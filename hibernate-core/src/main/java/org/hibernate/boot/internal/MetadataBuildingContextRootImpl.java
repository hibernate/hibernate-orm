/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.internal;

import org.hibernate.boot.model.internal.TemporalHelper;
import org.hibernate.boot.model.naming.ObjectNameNormalizer;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.cfg.TemporalTableStrategy;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;

/**
 * Root {@link MetadataBuildingContext}.
 */
public class MetadataBuildingContextRootImpl implements MetadataBuildingContext {
	private final String contributor;
	private final BootstrapContext bootstrapContext;
	private final MetadataBuildingOptions options;
	private final RootMappingDefaults mappingDefaults;
	private final InFlightMetadataCollector metadataCollector;
	private final ObjectNameNormalizer objectNameNormalizer;
	private final TypeDefinitionRegistryStandardImpl typeDefinitionRegistry;
	private final TemporalTableStrategy temporalTableStrategy;

	public MetadataBuildingContextRootImpl(
			String contributor,
			BootstrapContext bootstrapContext,
			MetadataBuildingOptions options,
			InFlightMetadataCollector metadataCollector,
			RootMappingDefaults mappingDefaults) {
		this.contributor = contributor;
		this.bootstrapContext = bootstrapContext;
		this.options = options;
		this.mappingDefaults = mappingDefaults;
		this.metadataCollector = metadataCollector;
		this.objectNameNormalizer = new ObjectNameNormalizer(this);
		this.typeDefinitionRegistry = new TypeDefinitionRegistryStandardImpl();
		this.temporalTableStrategy = temporalTableStrategy( bootstrapContext );
	}

	private TemporalTableStrategy temporalTableStrategy(BootstrapContext bootstrapContext) {
		final var settings =
				bootstrapContext.getServiceRegistry()
						.requireService( ConfigurationService.class )
						.getSettings();
		return TemporalHelper.determineTemporalTableStrategy( settings );
	}

	@Override
	public BootstrapContext getBootstrapContext() {
		return bootstrapContext;
	}

	@Override
	public MetadataBuildingOptions getBuildingOptions() {
		return options;
	}

	@Override
	public RootMappingDefaults getEffectiveDefaults() {
		return mappingDefaults;
	}

	@Override
	public InFlightMetadataCollector getMetadataCollector() {
		return metadataCollector;
	}

	@Override
	public ObjectNameNormalizer getObjectNameNormalizer() {
		return objectNameNormalizer;
	}

	@Override
	public TypeDefinitionRegistryStandardImpl getTypeDefinitionRegistry() {
		return typeDefinitionRegistry;
	}

	@Override
	public String getCurrentContributorName() {
		return contributor;
	}

	@Override
	public TemporalTableStrategy getTemporalTableStrategy(Dialect dialect) {
		return temporalTableStrategy == TemporalTableStrategy.AUTO
				? dialect.getDefaultTemporalTableStrategy()
				: temporalTableStrategy;
	}
}
