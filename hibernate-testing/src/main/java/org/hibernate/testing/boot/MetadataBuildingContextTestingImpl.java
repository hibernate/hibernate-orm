/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.boot;

import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.internal.InFlightMetadataCollectorImpl;
import org.hibernate.boot.mapping.internal.context.MappingResolutionServicesImpl;
import org.hibernate.boot.mapping.internal.context.RootMappingDefaults;
import org.hibernate.boot.mapping.internal.context.TypeDefinitionRegistryStandardImpl;
import org.hibernate.boot.model.naming.ObjectNameNormalizer;
import org.hibernate.boot.mapping.internal.xml.PersistenceUnitMetadataImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.EffectiveMappingDefaults;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.pipeline.internal.MappingResolutionOptions;
import org.hibernate.boot.mapping.internal.context.MappingResolutionServices;

/**
* @author Steve Ebersole
*/
public class MetadataBuildingContextTestingImpl implements MetadataBuildingContext {
	private final MappingResolutionOptions buildingPlan;
	private final EffectiveMappingDefaults mappingDefaults;
	private final InFlightMetadataCollector metadataCollector;
	private final BootstrapContext bootstrapContext;
	private final MappingResolutionServices serviceComponents;
	private final ObjectNameNormalizer objectNameNormalizer;
	private final TypeDefinitionRegistryStandardImpl typeDefinitionRegistry;

	public MetadataBuildingContextTestingImpl(StandardServiceRegistry serviceRegistry) {
		org.hibernate.boot.pipeline.internal.MappingResolutionOptionsImpl buildingPlan = new org.hibernate.boot.pipeline.internal.MappingResolutionOptionsImpl( serviceRegistry );
		this.buildingPlan = buildingPlan;
		buildingPlan.setBootstrapContext( bootstrapContext = new BootstrapContextImpl( serviceRegistry, buildingPlan ) );
		serviceComponents = new MappingResolutionServicesImpl( bootstrapContext );
		mappingDefaults = new RootMappingDefaults(
				new org.hibernate.boot.mapping.internal.context.GlobalMappingDefaultsImpl( serviceRegistry ),
				new PersistenceUnitMetadataImpl()
		);
		metadataCollector = new InFlightMetadataCollectorImpl( bootstrapContext, buildingPlan );
		objectNameNormalizer = new ObjectNameNormalizer(this);
		typeDefinitionRegistry = new TypeDefinitionRegistryStandardImpl();
		bootstrapContext.getTypeConfiguration().scope( this );
	}

	@Override
	public BootstrapContext getBootstrapContext() {
		return bootstrapContext;
	}

	@Override
	public MappingResolutionServices getServiceComponents() {
		return serviceComponents;
	}

	@Override
	public MappingResolutionOptions getBuildingPlan() {
		return buildingPlan;
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
