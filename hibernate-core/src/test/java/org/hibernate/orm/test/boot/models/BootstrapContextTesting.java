/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models;

import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.boot.internal.ClassLoaderAccessImpl;
import org.hibernate.boot.internal.TypeBeanInstanceProducer;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.ClassLoaderAccess;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.jpa.internal.MutableJpaComplianceImpl;
import org.hibernate.jpa.spi.MutableJpaCompliance;
import org.hibernate.metamodel.internal.ManagedTypeRepresentationResolverStandard;
import org.hibernate.metamodel.spi.ManagedTypeRepresentationResolver;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.type.spi.TypeConfiguration;
import org.jboss.jandex.IndexView;

import static org.hibernate.boot.internal.BootstrapContextImpl.createModelBuildingContext;

/**
 * BootstrapContext impl to be able to inject a Jandex index
 *
 * @todo Consider making this easier upstream in BootstrapContextImpl
 *
 * @author Steve Ebersole
 */
public class BootstrapContextTesting implements BootstrapContext {

	private final StandardServiceRegistry serviceRegistry;

	private final TypeConfiguration typeConfiguration;
	private final MutableJpaCompliance jpaCompliance;

	private final ClassLoaderService classLoaderService;
	private final ClassLoaderAccessImpl classLoaderAccess;
	private final BeanInstanceProducer beanInstanceProducer;
	private final ManagedBeanRegistry managedBeanRegistry;
	private final ConfigurationService configurationService;

	private boolean isJpaBootstrap;

	private Object scannerSetting;
	private ArchiveDescriptorFactory archiveDescriptorFactory;

	private final ManagedTypeRepresentationResolver representationStrategySelector;
	private ModelsContext modelsContext;

	public BootstrapContextTesting(
			IndexView jandexIndex,
			StandardServiceRegistry serviceRegistry,
			TypeConfiguration typeConfiguration) {
		this.serviceRegistry = serviceRegistry;

		this.classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
		this.classLoaderAccess = new ClassLoaderAccessImpl( classLoaderService );

		final StrategySelector strategySelector = serviceRegistry.getService( StrategySelector.class );
		final ConfigurationService configService = serviceRegistry.getService( ConfigurationService.class );

		this.jpaCompliance = new MutableJpaComplianceImpl( configService.getSettings() );

		// ScanEnvironment must be set explicitly
		this.scannerSetting = configService.getSettings().get( AvailableSettings.SCANNER );
		this.archiveDescriptorFactory = strategySelector.resolveStrategy(
				ArchiveDescriptorFactory.class,
				configService.getSettings().get( AvailableSettings.SCANNER_ARCHIVE_INTERPRETER )
		);

		this.representationStrategySelector = ManagedTypeRepresentationResolverStandard.INSTANCE;

		this.typeConfiguration = typeConfiguration;
		this.beanInstanceProducer = new TypeBeanInstanceProducer( configService, serviceRegistry );

		this.managedBeanRegistry = serviceRegistry.requireService( ManagedBeanRegistry.class );
		this.configurationService = serviceRegistry.requireService( ConfigurationService.class );

		this.modelsContext = createModelBuildingContext( classLoaderService, configService );
	}

	@Override
	public StandardServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	@Override
	public MutableJpaCompliance getJpaCompliance() {
		return jpaCompliance;
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return typeConfiguration;
	}

	@Override
	public ModelsContext getModelsContext() {
		return modelsContext;
	}

	@Override
	public BeanInstanceProducer getCustomTypeProducer() {
		return beanInstanceProducer;
	}

	@Override
	public ClassLoaderService getClassLoaderService() {
		return classLoaderService;
	}

	@Override
	public ManagedBeanRegistry getManagedBeanRegistry() {
		return managedBeanRegistry;
	}

	@Override
	public ConfigurationService getConfigurationService() {
		return configurationService;
	}

	@Override
	public boolean isJpaBootstrap() {
		return isJpaBootstrap;
	}

	@Override
	public void markAsJpaBootstrap() {
		isJpaBootstrap = true;
	}

	@Override
	public ClassLoader getJpaTempClassLoader() {
		return classLoaderAccess.getJpaTempClassLoader();
	}

	@Override
	public ClassLoaderAccess getClassLoaderAccess() {
		return classLoaderAccess;
	}

	@Override
	public ArchiveDescriptorFactory getArchiveDescriptorFactory() {
		return archiveDescriptorFactory;
	}

	@Override
	public Object getScanning() {
		return scannerSetting;
	}

	@Override
	public void release() {
		classLoaderAccess.release();

		scannerSetting = null;
		archiveDescriptorFactory = null;

	}

	@Override
	public ManagedTypeRepresentationResolver getRepresentationStrategySelector() {
		return representationStrategySelector;
	}
}
