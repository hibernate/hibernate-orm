/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.boot;

import java.util.Collection;
import java.util.Map;

import org.hibernate.boot.CacheRegionDefinition;
import org.hibernate.boot.archive.scan.spi.ScanEnvironment;
import org.hibernate.boot.archive.scan.spi.ScanOptions;
import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.boot.internal.ClassmateContext;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.ClassLoaderAccess;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.jpa.spi.MutableJpaCompliance;
import org.hibernate.metamodel.internal.ManagedTypeRepresentationResolverStandard;
import org.hibernate.metamodel.spi.ManagedTypeRepresentationResolver;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;
import org.hibernate.type.BasicType;
import org.hibernate.type.spi.TypeConfiguration;


/**
 * @author Andrea Boriero
 */
public class BootstrapContextImpl implements BootstrapContext {

	private final BootstrapContext delegate;

	public BootstrapContextImpl() {
		StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build();
		MetadataBuildingOptions buildingOptions = new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry );

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
	public Collection<ConverterDescriptor> getAttributeConverters() {
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

	public void close() {
		delegate.release();
		delegate.getServiceRegistry().close();
	}
}
