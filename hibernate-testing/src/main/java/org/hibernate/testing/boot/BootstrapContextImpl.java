/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.boot;

import org.hibernate.boot.CacheRegionDefinition;
import org.hibernate.boot.archive.scan.spi.ScanEnvironment;
import org.hibernate.boot.archive.scan.spi.ScanOptions;
import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.ClassLoaderAccess;
import org.hibernate.boot.spi.ClassmateContext;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.jpa.spi.MutableJpaCompliance;
import org.hibernate.metamodel.internal.ManagedTypeRepresentationResolverStandard;
import org.hibernate.metamodel.spi.ManagedTypeRepresentationResolver;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.type.BasicType;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.Collection;
import java.util.Map;


/**
 * @author Andrea Boriero
 */
public class BootstrapContextImpl implements BootstrapContext, AutoCloseable {
	private final BootstrapContext delegate;

	public BootstrapContextImpl() {
		this( new StandardServiceRegistryBuilder().build() );
	}

	public BootstrapContextImpl(StandardServiceRegistry serviceRegistry) {
		final MetadataBuildingOptions buildingOptions = new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry );
		delegate = new org.hibernate.boot.internal.BootstrapContextImpl( serviceRegistry, buildingOptions );
	}

	@Override
	public StandardServiceRegistry getServiceRegistry() {
		return delegate.getServiceRegistry();
	}

	@Override
	public MutableJpaCompliance getJpaCompliance() {
		return delegate.getJpaCompliance();
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return delegate.getTypeConfiguration();
	}

	@Override
	public ModelsContext getModelsContext() {
		return delegate.getModelsContext();
	}

	@Override
	public SqmFunctionRegistry getFunctionRegistry() {
		return delegate.getFunctionRegistry();
	}

	@Override
	public BeanInstanceProducer getCustomTypeProducer() {
		return delegate.getCustomTypeProducer();
	}

	@Override
	public MetadataBuildingOptions getMetadataBuildingOptions() {
		return delegate.getMetadataBuildingOptions();
	}

	@Override
	public ClassLoaderService getClassLoaderService() {
		return delegate.getClassLoaderService();
	}

	@Override
	public ManagedBeanRegistry getManagedBeanRegistry() {
		return delegate.getManagedBeanRegistry();
	}

	@Override
	public ConfigurationService getConfigurationService() {
		return delegate.getConfigurationService();
	}

	@Override
	public boolean isJpaBootstrap() {
		return delegate.isJpaBootstrap();
	}

	@Override
	public void markAsJpaBootstrap() {
		delegate.markAsJpaBootstrap();
	}

	@Override
	public ClassLoader getJpaTempClassLoader() {
		return delegate.getJpaTempClassLoader();
	}

	@Override
	public ClassLoaderAccess getClassLoaderAccess() {
		return delegate.getClassLoaderAccess();
	}

	@Override
	public ClassmateContext getClassmateContext() {
		return delegate.getClassmateContext();
	}

	@Override
	public ArchiveDescriptorFactory getArchiveDescriptorFactory() {
		return delegate.getArchiveDescriptorFactory();
	}

	@Override
	public ScanOptions getScanOptions() {
		return delegate.getScanOptions();
	}

	@Override
	public ScanEnvironment getScanEnvironment() {
		return delegate.getScanEnvironment();
	}

	@Override
	public Object getScanner() {
		return delegate.getScanner();
	}

	@Override
	public Object getJandexView() {
		return delegate.getJandexView();
	}

	@Override
	public Map<String, SqmFunctionDescriptor> getSqlFunctions() {
		return delegate.getSqlFunctions();
	}

	@Override
	public Collection<AuxiliaryDatabaseObject> getAuxiliaryDatabaseObjectList() {
		return delegate.getAuxiliaryDatabaseObjectList();
	}

	@Override
	public Collection<ConverterDescriptor<?, ?>> getAttributeConverters() {
		return delegate.getAttributeConverters();
	}

	@Override
	public Collection<CacheRegionDefinition> getCacheRegionDefinitions() {
		return delegate.getCacheRegionDefinitions();
	}

	@Override
	public ManagedTypeRepresentationResolver getRepresentationStrategySelector() {
		return ManagedTypeRepresentationResolverStandard.INSTANCE;
	}

	@Override
	public void registerAdHocBasicType(BasicType<?> basicType) {
	}

	@Override
	public <T> BasicType<T> resolveAdHocBasicType(String key) {
		return null;
	}

	@Override
	public void release() {
		delegate.release();
	}

	@Override
	public void close() {
		delegate.release();
		delegate.getServiceRegistry().close();
	}
}
