/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.internal;

import org.hibernate.AssertionFailure;
import org.hibernate.boot.CacheRegionDefinition;
import org.hibernate.boot.archive.scan.internal.StandardScanOptions;
import org.hibernate.boot.archive.scan.spi.ScanEnvironment;
import org.hibernate.boot.archive.scan.spi.ScanOptions;
import org.hibernate.boot.archive.scan.spi.Scanner;
import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.models.internal.ClassLoaderServiceLoading;
import org.hibernate.boot.models.internal.ModelsHelper;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.ClassLoaderAccess;
import org.hibernate.boot.spi.ClassmateContext;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.jpa.internal.MutableJpaComplianceImpl;
import org.hibernate.jpa.spi.MutableJpaCompliance;
import org.hibernate.metamodel.internal.ManagedTypeRepresentationResolverStandard;
import org.hibernate.metamodel.spi.ManagedTypeRepresentationResolver;
import org.hibernate.models.spi.ModelsConfiguration;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.type.BasicType;
import org.hibernate.type.spi.TypeConfiguration;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

/**
 * @author Andrea Boriero
 */
public class BootstrapContextImpl implements BootstrapContext {
	private static final Logger LOG = Logger.getLogger( BootstrapContextImpl.class );

	private final StandardServiceRegistry serviceRegistry;
	private final MetadataBuildingOptions metadataBuildingOptions;

	private final TypeConfiguration typeConfiguration;
	private final SqmFunctionRegistry sqmFunctionRegistry;
	private final MutableJpaCompliance jpaCompliance;

	private final ClassLoaderService classLoaderService;
	private final ClassLoaderAccessImpl classLoaderAccess;
	private final BeanInstanceProducer beanInstanceProducer;
	private final ManagedBeanRegistry managedBeanRegistry;

	private boolean isJpaBootstrap;

	private final ClassmateContext classmateContext;

	private ScanOptions scanOptions;
	private ScanEnvironment scanEnvironment;
	private Object scannerSetting;
	private ArchiveDescriptorFactory archiveDescriptorFactory;

	private HashMap<String,SqmFunctionDescriptor> sqlFunctionMap;
	private ArrayList<AuxiliaryDatabaseObject> auxiliaryDatabaseObjectList;
	private HashMap<Class<?>, ConverterDescriptor<?,?>> attributeConverterDescriptorMap;
	private ArrayList<CacheRegionDefinition> cacheRegionDefinitions;
	private final ManagedTypeRepresentationResolver representationStrategySelector;
	private final ConfigurationService configurationService;

	private final ModelsContext modelsContext;

	public BootstrapContextImpl(
			StandardServiceRegistry serviceRegistry,
			MetadataBuildingOptions metadataBuildingOptions) {
		this.serviceRegistry = serviceRegistry;
		this.metadataBuildingOptions = metadataBuildingOptions;

		classmateContext = new ClassmateContext();
		classLoaderService = serviceRegistry.requireService( ClassLoaderService.class );
		classLoaderAccess = new ClassLoaderAccessImpl( classLoaderService );

		final StrategySelector strategySelector = serviceRegistry.requireService( StrategySelector.class );
		final ConfigurationService configService = serviceRegistry.requireService( ConfigurationService.class );

		jpaCompliance = new MutableJpaComplianceImpl( configService.getSettings() );
		scanOptions = new StandardScanOptions(
				(String) configService.getSettings().get( AvailableSettings.SCANNER_DISCOVERY ),
				false
		);

		// ScanEnvironment must be set explicitly
		scannerSetting = configService.getSettings().get( AvailableSettings.SCANNER );
		archiveDescriptorFactory = strategySelector.resolveStrategy(
				ArchiveDescriptorFactory.class,
				configService.getSettings().get( AvailableSettings.SCANNER_ARCHIVE_INTERPRETER )
		);

		representationStrategySelector = ManagedTypeRepresentationResolverStandard.INSTANCE;

		typeConfiguration = new TypeConfiguration();
		beanInstanceProducer = new TypeBeanInstanceProducer( configService, serviceRegistry );
		sqmFunctionRegistry = new SqmFunctionRegistry();

		managedBeanRegistry = serviceRegistry.requireService( ManagedBeanRegistry.class );
		configurationService = serviceRegistry.requireService( ConfigurationService.class );

		modelsContext = createModelBuildingContext( classLoaderService, configService );
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
	public SqmFunctionRegistry getFunctionRegistry() {
		return sqmFunctionRegistry;
	}

	@Override
	public BeanInstanceProducer getCustomTypeProducer() {
		return beanInstanceProducer;
	}

	@Override
	public MetadataBuildingOptions getMetadataBuildingOptions() {
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
	public ClassmateContext getClassmateContext() {
		return classmateContext;
	}

	@Override
	public ArchiveDescriptorFactory getArchiveDescriptorFactory() {
		return archiveDescriptorFactory;
	}

	@Override
	public ScanOptions getScanOptions() {
		return scanOptions;
	}

	@Override
	public ScanEnvironment getScanEnvironment() {
		return scanEnvironment;
	}

	@Override
	public Object getScanner() {
		return scannerSetting;
	}

	@Override
	public Object getJandexView() {
		return null;
	}

	@Override
	public Map<String, SqmFunctionDescriptor> getSqlFunctions() {
		return sqlFunctionMap == null ? emptyMap() : sqlFunctionMap;
	}

	@Override
	public Collection<AuxiliaryDatabaseObject> getAuxiliaryDatabaseObjectList() {
		return auxiliaryDatabaseObjectList == null ? emptyList() : auxiliaryDatabaseObjectList;
	}

	@Override
	public Collection<ConverterDescriptor<?, ?>> getAttributeConverters() {
		return attributeConverterDescriptorMap != null
				? attributeConverterDescriptorMap.values()
				: emptyList();
	}

	@Override
	public Collection<CacheRegionDefinition> getCacheRegionDefinitions() {
		return cacheRegionDefinitions == null ? emptyList() : cacheRegionDefinitions;
	}

	private final Map<String,BasicType<?>> adHocBasicTypeRegistrations = new HashMap<>();

	@Override
	public void registerAdHocBasicType(BasicType<?> basicType) {
		adHocBasicTypeRegistrations.put( basicType.getName(), basicType );
	}

	@Override
	public <T> BasicType<T> resolveAdHocBasicType(String key) {
		//noinspection unchecked
		return (BasicType<T>) adHocBasicTypeRegistrations.get( key );
	}

	@Override
	public void release() {
		classmateContext.release();
		classLoaderAccess.release();

		scanOptions = null;
		scanEnvironment = null;
		scannerSetting = null;
		archiveDescriptorFactory = null;

		if ( sqlFunctionMap != null ) {
			sqlFunctionMap.clear();
		}

		if ( auxiliaryDatabaseObjectList != null ) {
			auxiliaryDatabaseObjectList.clear();
		}

		if ( attributeConverterDescriptorMap != null ) {
			attributeConverterDescriptorMap.clear();
		}

		if ( cacheRegionDefinitions != null ) {
			cacheRegionDefinitions.clear();
		}
	}

	@Override
	public ManagedTypeRepresentationResolver getRepresentationStrategySelector() {
		return representationStrategySelector;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Mutations

	public void addAttributeConverterDescriptor(ConverterDescriptor<?,?> descriptor) {
		if ( attributeConverterDescriptorMap == null ) {
			attributeConverterDescriptorMap = new HashMap<>();
		}

		final Object old = attributeConverterDescriptorMap.put( descriptor.getAttributeConverterClass(), descriptor );
		if ( old != null ) {
			throw new AssertionFailure(
					String.format(
							"AttributeConverter class [%s] registered multiple times",
							descriptor.getAttributeConverterClass()
					)
			);
		}
	}

	void injectJpaTempClassLoader(ClassLoader classLoader) {
		if ( LOG.isTraceEnabled() && classLoader != getJpaTempClassLoader() ) {
			LOG.tracef( "Injecting JPA temp ClassLoader [%s] into BootstrapContext; was [%s]",
					classLoader, getJpaTempClassLoader() );
		}
		this.classLoaderAccess.injectTempClassLoader( classLoader );
	}

	void injectScanOptions(ScanOptions scanOptions) {
		if ( LOG.isTraceEnabled() && scanOptions != this.scanOptions ) {
			LOG.tracef( "Injecting ScanOptions [%s] into BootstrapContext; was [%s]",
					scanOptions, this.scanOptions );
		}
		this.scanOptions = scanOptions;
	}

	void injectScanEnvironment(ScanEnvironment scanEnvironment) {
		if ( LOG.isTraceEnabled() && scanEnvironment != this.scanEnvironment ) {
			LOG.tracef( "Injecting ScanEnvironment [%s] into BootstrapContext; was [%s]",
					scanEnvironment, this.scanEnvironment );
		}
		this.scanEnvironment = scanEnvironment;
	}

	void injectScanner(Scanner scanner) {
		if ( LOG.isTraceEnabled() && scanner != this.scannerSetting ) {
			LOG.tracef( "Injecting Scanner [%s] into BootstrapContext; was [%s]",
					scanner, scannerSetting );
		}
		this.scannerSetting = scanner;
	}

	void injectArchiveDescriptorFactory(ArchiveDescriptorFactory factory) {
		if ( LOG.isTraceEnabled() && factory != archiveDescriptorFactory ) {
			LOG.tracef( "Injecting ArchiveDescriptorFactory [%s] into BootstrapContext; was [%s]",
					factory, archiveDescriptorFactory );
		}
		this.archiveDescriptorFactory = factory;
	}

	public void addSqlFunction(String functionName, SqmFunctionDescriptor function) {
		if ( sqlFunctionMap == null ) {
			sqlFunctionMap = new HashMap<>();
		}
		sqlFunctionMap.put( functionName, function );
	}

	public void addAuxiliaryDatabaseObject(AuxiliaryDatabaseObject auxiliaryDatabaseObject) {
		if ( auxiliaryDatabaseObjectList == null ) {
			auxiliaryDatabaseObjectList = new ArrayList<>();
		}
		auxiliaryDatabaseObjectList.add( auxiliaryDatabaseObject );
	}


	public void addCacheRegionDefinition(CacheRegionDefinition cacheRegionDefinition) {
		if ( cacheRegionDefinitions == null ) {
			cacheRegionDefinitions = new ArrayList<>();
		}
		cacheRegionDefinitions.add( cacheRegionDefinition );
	}

	public static ModelsContext createModelBuildingContext(
			ClassLoaderService classLoaderService,
			ConfigurationService configService) {
		final ClassLoaderServiceLoading classLoading = new ClassLoaderServiceLoading( classLoaderService );

		final ModelsConfiguration modelsConfiguration = new ModelsConfiguration();
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
