/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.boot;

import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.ClassLoaderAccess;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.jpa.spi.MutableJpaCompliance;
import org.hibernate.metamodel.internal.ManagedTypeRepresentationResolverStandard;
import org.hibernate.metamodel.spi.ManagedTypeRepresentationResolver;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Andrea Boriero
 */
public class BootstrapContextImpl implements BootstrapContext, AutoCloseable {
	private final BootstrapContext delegate;

	public BootstrapContextImpl() {
		this( new StandardServiceRegistryBuilder().build() );
	}

	public BootstrapContextImpl(StandardServiceRegistry serviceRegistry) {
		delegate = new org.hibernate.boot.internal.BootstrapContextImpl( serviceRegistry );
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
	public BeanInstanceProducer getCustomTypeProducer() {
		return delegate.getCustomTypeProducer();
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
	public ArchiveDescriptorFactory getArchiveDescriptorFactory() {
		return delegate.getArchiveDescriptorFactory();
	}

	@Override
	public Object getScanning() {
		return delegate.getScanning();
	}

	@Override
	public ManagedTypeRepresentationResolver getRepresentationStrategySelector() {
		return ManagedTypeRepresentationResolverStandard.INSTANCE;
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
