/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.context;

import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.pipeline.internal.MappingResolutionOptions;
import org.hibernate.boot.mapping.internal.categorize.CategorizedDomainModel;
import org.hibernate.boot.mapping.internal.categorize.GlobalRegistrations;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.SharedCacheMode;

/// Immutable binding context derived from the categorized model and metadata-building context.
///
/// The context exposes global source-model registrations and bootstrap services
/// that are stable for the whole binding run.  Per-run mutable state belongs in
/// [BindingStateImpl]; this object is for shared options such as naming
/// strategies, cache mode, and global registrations that later coordinator work
/// will bind into the metadata collector.
///
/// @since 9.0
/// @author Steve Ebersole
public class BindingContextImpl implements BindingContext {
	private final CategorizedDomainModel categorizedDomainModel;
	private final GlobalRegistrations globalRegistrations;

	private final ImplicitNamingStrategy implicitNamingStrategy;
	private final PhysicalNamingStrategy physicalNamingStrategy;
	private final SharedCacheMode sharedCacheMode;
	private final ServiceRegistry serviceRegistry;
	private final MappingResolutionOptions buildingPlan;
	private final ConfigurationService configurationService;
	private final ClassLoaderService classLoaderService;
	private final BeanInstanceProducer customTypeProducer;
	private final ManagedBeanRegistry managedBeanRegistry;
	private final ModelsContext modelsContext;
	private final TypeConfiguration typeConfiguration;

	public BindingContextImpl(
			CategorizedDomainModel categorizedDomainModel,
			MetadataBuildingContext metadataBuildingContext) {
		this(
				categorizedDomainModel,
				categorizedDomainModel.getGlobalRegistrations(),
				metadataBuildingContext.getBuildingPlan().getImplicitNamingStrategy(),
				metadataBuildingContext.getBuildingPlan().getPhysicalNamingStrategy(),
				metadataBuildingContext.getBuildingPlan().getSharedCacheMode(),
				metadataBuildingContext.getServiceRegistry(),
				metadataBuildingContext.getBuildingPlan(),
				metadataBuildingContext.getConfigurationService(),
				metadataBuildingContext.getClassLoaderService(),
				metadataBuildingContext.getCustomTypeProducer(),
				metadataBuildingContext.getManagedBeanRegistry(),
				metadataBuildingContext.getModelsContext(),
				metadataBuildingContext.getTypeConfiguration()
		);
	}

	public BindingContextImpl(
			CategorizedDomainModel categorizedDomainModel,
			GlobalRegistrations globalRegistrations,
			ImplicitNamingStrategy implicitNamingStrategy,
			PhysicalNamingStrategy physicalNamingStrategy,
			SharedCacheMode sharedCacheMode,
			ServiceRegistry serviceRegistry,
			MappingResolutionOptions buildingPlan,
			ConfigurationService configurationService,
			ClassLoaderService classLoaderService,
			BeanInstanceProducer customTypeProducer,
			ManagedBeanRegistry managedBeanRegistry,
			ModelsContext modelsContext,
			TypeConfiguration typeConfiguration) {
		this.categorizedDomainModel = categorizedDomainModel;
		this.implicitNamingStrategy = implicitNamingStrategy;
		this.physicalNamingStrategy = physicalNamingStrategy;
		this.globalRegistrations = globalRegistrations;
		this.sharedCacheMode = sharedCacheMode;
		this.serviceRegistry = serviceRegistry;
		this.buildingPlan = buildingPlan;
		this.configurationService = configurationService;
		this.classLoaderService = classLoaderService;
		this.customTypeProducer = customTypeProducer;
		this.managedBeanRegistry = managedBeanRegistry;
		this.modelsContext = modelsContext;
		this.typeConfiguration = typeConfiguration;
	}

	@Override
	public ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	@Override
	public MappingResolutionOptions getBuildingPlan() {
		return buildingPlan;
	}

	@Override
	public ConfigurationService getConfigurationService() {
		return configurationService;
	}

	@Override
	public ClassLoaderService getClassLoaderService() {
		return classLoaderService;
	}

	@Override
	public BeanInstanceProducer getCustomTypeProducer() {
		return customTypeProducer;
	}

	@Override
	public ManagedBeanRegistry getManagedBeanRegistry() {
		return managedBeanRegistry;
	}

	@Override
	public ModelsContext getModelsContext() {
		return modelsContext;
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return typeConfiguration;
	}

	@Override
	public GlobalRegistrations getGlobalRegistrations() {
		return globalRegistrations;
	}

	@Override
	public CategorizedDomainModel getCategorizedDomainModel() {
		return categorizedDomainModel;
	}

	@Override
	public SharedCacheMode getSharedCacheMode() {
		return sharedCacheMode;
	}

	@Override
	public ImplicitNamingStrategy getImplicitNamingStrategy() {
		return implicitNamingStrategy;
	}

	@Override
	public PhysicalNamingStrategy getPhysicalNamingStrategy() {
		return physicalNamingStrategy;
	}
}
