/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.internal;

import org.hibernate.boot.model.naming.ObjectNameNormalizer;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataBuildingOptions;

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
}
