/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.internal;

import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.boot.scan.spi.ScanningProvider;
import org.hibernate.boot.models.internal.ClassLoaderServiceLoading;
import org.hibernate.boot.models.internal.ModelsHelper;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.ClassLoaderAccess;
import org.hibernate.boot.pipeline.internal.MappingResolutionOptions;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.jpa.internal.MutableJpaComplianceImpl;
import org.hibernate.jpa.spi.MutableJpaCompliance;
import org.hibernate.metamodel.internal.ManagedTypeRepresentationResolverStandard;
import org.hibernate.metamodel.spi.ManagedTypeRepresentationResolver;
import org.hibernate.models.spi.ModelsConfiguration;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.boot.BootLogging.BOOT_LOGGER;
import static org.hibernate.cfg.PersistenceSettings.SCANNER;
import static org.hibernate.cfg.PersistenceSettings.SCANNER_ARCHIVE_INTERPRETER;

/**
 * @author Andrea Boriero
 */
public class BootstrapContextImpl implements BootstrapContext {

	private final StandardServiceRegistry serviceRegistry;
	private final MappingResolutionOptions metadataBuildingOptions;

	private final TypeConfiguration typeConfiguration;
	private final MutableJpaCompliance jpaCompliance;

	private final ClassLoaderService classLoaderService;
	private final ClassLoaderAccessImpl classLoaderAccess;
	private final BeanInstanceProducer beanInstanceProducer;
	private final ManagedBeanRegistry managedBeanRegistry;

	private boolean isJpaBootstrap;

	private Object scanningSetting;
	private ArchiveDescriptorFactory archiveDescriptorFactory;

	private final ManagedTypeRepresentationResolver representationStrategySelector;
	private final ConfigurationService configurationService;

	private final ModelsContext modelsContext;

	public BootstrapContextImpl(
			StandardServiceRegistry serviceRegistry,
			MappingResolutionOptions metadataBuildingOptions) {
		this( serviceRegistry, metadataBuildingOptions, new TypeConfiguration() );
	}

	public BootstrapContextImpl(
			StandardServiceRegistry serviceRegistry,
			MappingResolutionOptions metadataBuildingOptions,
			TypeConfiguration typeConfiguration) {
		this( serviceRegistry, metadataBuildingOptions, typeConfiguration, null );
	}

	public BootstrapContextImpl(
			StandardServiceRegistry serviceRegistry,
			MappingResolutionOptions metadataBuildingOptions,
			TypeConfiguration typeConfiguration,
			ModelsContext restoredModelsContext) {
		this.serviceRegistry = serviceRegistry;
		this.metadataBuildingOptions = metadataBuildingOptions;

		classLoaderService = serviceRegistry.requireService( ClassLoaderService.class );
		classLoaderAccess = new ClassLoaderAccessImpl( classLoaderService );

		final var strategySelector = serviceRegistry.requireService( StrategySelector.class );
		final var configService = serviceRegistry.requireService( ConfigurationService.class );

		jpaCompliance = new MutableJpaComplianceImpl( configService.getSettings() );

		// Scanning environment
		scanningSetting = configService.getSettings().get( SCANNER );
		archiveDescriptorFactory = strategySelector.resolveStrategy(
				ArchiveDescriptorFactory.class,
				configService.getSettings().get( SCANNER_ARCHIVE_INTERPRETER )
		);

		representationStrategySelector = ManagedTypeRepresentationResolverStandard.INSTANCE;

		this.typeConfiguration = typeConfiguration;
		beanInstanceProducer = new TypeBeanInstanceProducer( configService, serviceRegistry );

		managedBeanRegistry = serviceRegistry.requireService( ManagedBeanRegistry.class );
		configurationService = serviceRegistry.requireService( ConfigurationService.class );

		modelsContext = restoredModelsContext == null
				? createModelBuildingContext( classLoaderService, configService )
				: restoredModelsContext;
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
	public MappingResolutionOptions getMappingResolutionOptions() {
		return metadataBuildingOptions;
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
		return scanningSetting;
	}

	@Override
	public void release() {
		classLoaderAccess.release();

		scanningSetting = null;
		archiveDescriptorFactory = null;

	}

	@Override
	public ManagedTypeRepresentationResolver getRepresentationStrategySelector() {
		return representationStrategySelector;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Mutations

	void injectJpaTempClassLoader(ClassLoader classLoader) {
		if ( BOOT_LOGGER.isTraceEnabled() && classLoader != getJpaTempClassLoader() ) {
			BOOT_LOGGER.injectingJpaTempClassLoader( classLoader, getJpaTempClassLoader() );
		}
		this.classLoaderAccess.injectTempClassLoader( classLoader );
	}

	public void injectScanning(ScanningProvider scanningProvider) {
		if ( scanningProvider != this.scanningSetting ) {
			BOOT_LOGGER.injectingScanner( scanningProvider, this.scanningSetting );
		}
		this.scanningSetting = scanningProvider;

	}

	void injectArchiveDescriptorFactory(ArchiveDescriptorFactory factory) {
		if ( factory != archiveDescriptorFactory ) {
			BOOT_LOGGER.injectingArchiveDescriptorFactory( factory, archiveDescriptorFactory );
		}
		this.archiveDescriptorFactory = factory;
	}

	public static ModelsContext createModelBuildingContext(
			ClassLoaderService classLoaderService,
			ConfigurationService configService) {
		final var classLoading = new ClassLoaderServiceLoading( classLoaderService );
		final var modelsConfiguration = new ModelsConfiguration();
		modelsConfiguration.setClassLoading( classLoading );
		modelsConfiguration.setRegistryPrimer( ModelsHelper::preFillRegistries );
		configService.getSettings().forEach( (key, value) -> {
			if ( key.startsWith( "hibernate.models." ) ) {
				modelsConfiguration.configValue( key, value );
			}
		} );
		return modelsConfiguration.bootstrap();
	}
}
