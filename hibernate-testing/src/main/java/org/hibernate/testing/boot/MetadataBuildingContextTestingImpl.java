/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.boot;

import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.internal.InFlightMetadataCollectorImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.internal.RootMappingDefaults;
import org.hibernate.boot.internal.TypeDefinitionRegistryStandardImpl;
import org.hibernate.boot.model.naming.ObjectNameNormalizer;
import org.hibernate.boot.models.xml.internal.PersistenceUnitMetadataImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.EffectiveMappingDefaults;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataBuildingOptions;

/**
* @author Steve Ebersole
*/
public class MetadataBuildingContextTestingImpl implements MetadataBuildingContext {
	private final MetadataBuildingOptions buildingOptions;
	private final EffectiveMappingDefaults mappingDefaults;
	private final InFlightMetadataCollector metadataCollector;
	private final BootstrapContext bootstrapContext;
	private final ObjectNameNormalizer objectNameNormalizer;
	private final TypeDefinitionRegistryStandardImpl typeDefinitionRegistry;

	public MetadataBuildingContextTestingImpl(StandardServiceRegistry serviceRegistry) {
		MetadataBuilderImpl.MetadataBuildingOptionsImpl buildingOptions = new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry );
		this.buildingOptions = buildingOptions;
		buildingOptions.setBootstrapContext( bootstrapContext = new BootstrapContextImpl( serviceRegistry, buildingOptions ) );
		mappingDefaults = new RootMappingDefaults(
				new MetadataBuilderImpl.MappingDefaultsImpl( serviceRegistry ),
				new PersistenceUnitMetadataImpl()
		);
		metadataCollector = new InFlightMetadataCollectorImpl( bootstrapContext, buildingOptions );
		objectNameNormalizer = new ObjectNameNormalizer(this);
		typeDefinitionRegistry = new TypeDefinitionRegistryStandardImpl();
		bootstrapContext.getTypeConfiguration().scope( this );
	}

	@Override
	public BootstrapContext getBootstrapContext() {
		return bootstrapContext;
	}

	@Override
	public MetadataBuildingOptions getBuildingOptions() {
		return buildingOptions;
	}

	@Override
	public EffectiveMappingDefaults getEffectiveDefaults() {
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
		return "orm";
	}
}
