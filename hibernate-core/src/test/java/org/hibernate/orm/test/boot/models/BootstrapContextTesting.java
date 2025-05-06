/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models;

import org.hibernate.boot.CacheRegionDefinition;
import org.hibernate.boot.archive.scan.internal.StandardScanOptions;
import org.hibernate.boot.archive.scan.spi.ScanEnvironment;
import org.hibernate.boot.archive.scan.spi.ScanOptions;
import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.boot.internal.ClassLoaderAccessImpl;
import org.hibernate.boot.internal.TypeBeanInstanceProducer;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
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
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.type.BasicType;
import org.hibernate.type.internal.BasicTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;
import org.jboss.jandex.IndexView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
	private final MetadataBuildingOptions metadataBuildingOptions;

	private final TypeConfiguration typeConfiguration;
	private final SqmFunctionRegistry sqmFunctionRegistry;
	private final MutableJpaCompliance jpaCompliance;

	private final ClassLoaderService classLoaderService;
	private final ClassLoaderAccessImpl classLoaderAccess;
	private final BeanInstanceProducer beanInstanceProducer;
	private final ManagedBeanRegistry managedBeanRegistry;
	private final ConfigurationService configurationService;

	private boolean isJpaBootstrap;

	private final ClassmateContext classmateContext;

	private ScanOptions scanOptions;
	private ScanEnvironment scanEnvironment;
	private Object scannerSetting;
	private ArchiveDescriptorFactory archiveDescriptorFactory;

	private HashMap<String, SqmFunctionDescriptor> sqlFunctionMap;
	private ArrayList<AuxiliaryDatabaseObject> auxiliaryDatabaseObjectList;
	private HashMap<Class<?>, ConverterDescriptor<?,?>> attributeConverterDescriptorMap;
	private ArrayList<CacheRegionDefinition> cacheRegionDefinitions;
	private final ManagedTypeRepresentationResolver representationStrategySelector;
	private ModelsContext modelsContext;

	public BootstrapContextTesting(
			IndexView jandexIndex,
			StandardServiceRegistry serviceRegistry,
			MetadataBuildingOptions metadataBuildingOptions) {
		this.serviceRegistry = serviceRegistry;
		this.classmateContext = new ClassmateContext();
		this.metadataBuildingOptions = metadataBuildingOptions;

		this.classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
		this.classLoaderAccess = new ClassLoaderAccessImpl( classLoaderService );

		final StrategySelector strategySelector = serviceRegistry.getService( StrategySelector.class );
		final ConfigurationService configService = serviceRegistry.getService( ConfigurationService.class );

		this.jpaCompliance = new MutableJpaComplianceImpl( configService.getSettings() );
		this.scanOptions = new StandardScanOptions(
				(String) configService.getSettings().get( AvailableSettings.SCANNER_DISCOVERY ),
				false
		);

		// ScanEnvironment must be set explicitly
		this.scannerSetting = configService.getSettings().get( AvailableSettings.SCANNER );
		this.archiveDescriptorFactory = strategySelector.resolveStrategy(
				ArchiveDescriptorFactory.class,
				configService.getSettings().get( AvailableSettings.SCANNER_ARCHIVE_INTERPRETER )
		);

		this.representationStrategySelector = ManagedTypeRepresentationResolverStandard.INSTANCE;

		this.typeConfiguration = new TypeConfiguration();
		this.beanInstanceProducer = new TypeBeanInstanceProducer( configService, serviceRegistry );
		this.sqmFunctionRegistry = new SqmFunctionRegistry();

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
	public IndexView getJandexView() {
		return null;
	}

	@Override
	public Map<String, SqmFunctionDescriptor> getSqlFunctions() {
		return sqlFunctionMap == null ? Collections.emptyMap() : sqlFunctionMap;
	}

	@Override
	public Collection<AuxiliaryDatabaseObject> getAuxiliaryDatabaseObjectList() {
		return auxiliaryDatabaseObjectList == null ? Collections.emptyList() : auxiliaryDatabaseObjectList;
	}

	@Override
	public Collection<ConverterDescriptor<?, ?>> getAttributeConverters() {
		return attributeConverterDescriptorMap != null
				? attributeConverterDescriptorMap.values()
				: Collections.emptyList();
	}

	@Override
	public Collection<CacheRegionDefinition> getCacheRegionDefinitions() {
		return cacheRegionDefinitions == null ? Collections.emptyList() : cacheRegionDefinitions;
	}

	private final Map<String, BasicTypeImpl<?>> adHocBasicTypeRegistrations = new HashMap<>();

	@Override
	public void registerAdHocBasicType(BasicType<?> basicType) {
		adHocBasicTypeRegistrations.put( basicType.getName(), (BasicTypeImpl<?>) basicType );
	}

	@Override
	public <T> BasicTypeImpl<T> resolveAdHocBasicType(String key) {
		//noinspection unchecked
		return (BasicTypeImpl<T>) adHocBasicTypeRegistrations.get( key );
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
}
