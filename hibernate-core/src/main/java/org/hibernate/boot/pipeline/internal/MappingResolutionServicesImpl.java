/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.internal;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.ClassLoaderAccess;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.temporal.spi.ChangesetCoordinator;
import org.hibernate.type.spi.TypeConfiguration;

/// BootstrapContext-backed [MappingResolutionServices].
///
/// @since 9.0
/// @author Steve Ebersole
public class MappingResolutionServicesImpl implements MappingResolutionServices {
	private final ServiceRegistry serviceRegistry;
	private final StandardServiceRegistry standardServiceRegistry;
	private final ConfigurationService configurationService;
	private final ClassLoaderService classLoaderService;
	private final ClassLoaderAccess classLoaderAccess;
	private final ManagedBeanRegistry managedBeanRegistry;
	private final BeanInstanceProducer customTypeProducer;
	private final StrategySelector strategySelector;
	private final ModelsContext modelsContext;
	private final TypeConfiguration typeConfiguration;
	private JdbcServices jdbcServices;
	private ChangesetCoordinator changesetCoordinator;

	public MappingResolutionServicesImpl(BootstrapContext bootstrapContext) {
		this.serviceRegistry = bootstrapContext.getServiceRegistry();
		this.standardServiceRegistry = bootstrapContext.getServiceRegistry();
		this.configurationService = bootstrapContext.getConfigurationService();
		this.classLoaderService = bootstrapContext.getClassLoaderService();
		this.classLoaderAccess = bootstrapContext.getClassLoaderAccess();
		this.managedBeanRegistry = bootstrapContext.getManagedBeanRegistry();
		this.customTypeProducer = bootstrapContext.getCustomTypeProducer();
		this.strategySelector = bootstrapContext.getServiceRegistry().requireService( StrategySelector.class );
		this.modelsContext = bootstrapContext.getModelsContext();
		this.typeConfiguration = bootstrapContext.getTypeConfiguration();
	}

	@Override
	public ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	@Override
	public StandardServiceRegistry getStandardServiceRegistry() {
		return standardServiceRegistry;
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
	public ClassLoaderAccess getClassLoaderAccess() {
		return classLoaderAccess;
	}

	@Override
	public ManagedBeanRegistry getManagedBeanRegistry() {
		return managedBeanRegistry;
	}

	@Override
	public BeanInstanceProducer getCustomTypeProducer() {
		return customTypeProducer;
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
	public JdbcServices getJdbcServices() {
		if ( jdbcServices == null ) {
			jdbcServices = serviceRegistry.requireService( JdbcServices.class );
		}
		return jdbcServices;
	}

	@Override
	public ChangesetCoordinator getChangesetCoordinator() {
		if ( changesetCoordinator == null ) {
			changesetCoordinator = serviceRegistry.requireService( ChangesetCoordinator.class );
		}
		return changesetCoordinator;
	}

	@Override
	public StrategySelector getStrategySelector() {
		return strategySelector;
	}
}
