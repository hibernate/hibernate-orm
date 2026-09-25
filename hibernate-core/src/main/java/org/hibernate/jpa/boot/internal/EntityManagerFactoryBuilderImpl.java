/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.boot.internal;

import org.hibernate.boot.model.process.internal.EnhancementCandidates;
import org.hibernate.boot.model.process.internal.MappingSourceHelper;
import org.hibernate.boot.scan.spi.ScanningResult;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import org.hibernate.Internal;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.internal.BootstrapRegistryLifecycle;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.beanvalidation.BeanValidationIntegrator;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.model.process.spi.MetadataBuildingProcess;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.internal.TcclLookupPrecedence;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.boot.spi.MetadataBuilderContributor;
import org.hibernate.boot.spi.MetadataBuilderImplementor;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryBuilderImplementor;
import org.hibernate.bytecode.enhance.spi.EnhancementModel;
import org.hibernate.bytecode.enhance.spi.EnhancementOptions;
import org.hibernate.jpa.internal.enhance.PersistenceUnitEnhancementModel;
import org.hibernate.bytecode.enhance.spi.EnhancementException;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.bytecode.spi.ClassTransformer;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.hibernate.jpa.boot.spi.JpaSettings;
import org.hibernate.jpa.boot.spi.PersistenceConfigurationDescriptor;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.jpa.boot.spi.StrategyRegistrationProviderList;
import org.hibernate.jpa.boot.spi.TypeContributorList;
import org.hibernate.jpa.internal.JpaEntityNotFoundDelegate;
import org.hibernate.tool.schema.spi.DelayedDropRegistryNotAvailableImpl;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static java.lang.Boolean.parseBoolean;
import static java.util.Collections.unmodifiableMap;
import static org.hibernate.boot.scan.internal.ScanningHelper.performScanning;
import static org.hibernate.cfg.AvailableSettings.CLASSLOADERS;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_VALIDATION_FACTORY;
import static org.hibernate.cfg.AvailableSettings.JPA_VALIDATION_FACTORY;
import static org.hibernate.cfg.BytecodeSettings.BYTECODE_PROVIDER_INSTANCE;
import static org.hibernate.cfg.BytecodeSettings.ENHANCER_ENABLE_ASSOCIATION_MANAGEMENT;
import static org.hibernate.cfg.BytecodeSettings.ENHANCER_ENABLE_DIRTY_TRACKING;
import static org.hibernate.cfg.BytecodeSettings.ENHANCER_ENABLE_LAZY_INITIALIZATION;
import static org.hibernate.internal.log.DeprecationLogger.DEPRECATION_LOGGER;
import static org.hibernate.jpa.internal.JpaLogger.JPA_LOGGER;
import static org.hibernate.jpa.internal.util.LogHelper.logPersistenceUnitInformation;
/**
 * @author Steve Ebersole
 */
@SuppressWarnings({"deprecation", "removal"})
public class EntityManagerFactoryBuilderImpl implements EntityManagerFactoryBuilder {


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// New settings

	/**
	 * Names a {@link IntegratorProvider}
	 */
	public static final String INTEGRATOR_PROVIDER = JpaSettings.INTEGRATOR_PROVIDER;

	/**
	 * Names a {@link StrategyRegistrationProviderList}
	 */
	public static final String STRATEGY_REGISTRATION_PROVIDERS = JpaSettings.STRATEGY_REGISTRATION_PROVIDERS;

	/**
	 * Names a {@link TypeContributorList}
	 */
	public static final String TYPE_CONTRIBUTORS = JpaSettings.TYPE_CONTRIBUTORS;

	/**
	 * Names a {@link MetadataBuilderContributor}
	 */
	public static final String METADATA_BUILDER_CONTRIBUTOR = JpaSettings.METADATA_BUILDER_CONTRIBUTOR;


	private final PersistenceUnitDescriptor persistenceUnit;
	private final BootstrapRegistryLifecycle registryLifecycle;

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// things built in first phase, needed for second phase
	private final Map<String,Object> configurationValues;
	private final StandardServiceRegistry standardServiceRegistry;
	private final ManagedResources managedResources;
	private final MetadataBuilderImplementor metamodelBuilder;

	public  EntityManagerFactoryBuilderImpl(HibernatePersistenceConfiguration cfg) {
		var bootRegistry = buildBootstrapServiceRegistry( cfg.properties(), null, null );
		registryLifecycle = new BootstrapRegistryLifecycle( bootRegistry );
		try {
			var registryBuilder = StandardServiceRegistryBuilder.forJpa( bootRegistry );

			final var mergedSettings = new JpaSettingsAssembler( null, dataSource, this::exceptionHeader ).assemble( cfg, registryBuilder );
			// keep the merged config values for phase-2
			configurationValues = mergedSettings.getConfigurationValues();

			// Build the "standard" service registry
			registryBuilder.applySettings( configurationValues );
			standardServiceRegistry = registryLifecycle.register( registryBuilder.build() );

			final var discovery = performScanning( cfg, standardServiceRegistry );
			persistenceUnit = new PersistenceConfigurationDescriptor( cfg, discovery );

			final var metadataSources = new MetadataSources( standardServiceRegistry );
			metamodelBuilder =
					(MetadataBuilderImplementor)
							metadataSources.getMetadataBuilder( standardServiceRegistry );
			managedResources = prepareManagedResources( metadataSources, mergedSettings, discovery );
			completePreparation();
		}
		catch (Throwable throwable) {
			registryLifecycle.close( throwable );
			throw throwable;
		}
	}



	public EntityManagerFactoryBuilderImpl(
			PersistenceUnitDescriptor persistenceUnit,
			Map<String, Object> integrationSettings) {
		this( persistenceUnit, integrationSettings, null, null, null );
	}

	public EntityManagerFactoryBuilderImpl(
			PersistenceUnitDescriptor persistenceUnit,
			Map<String, Object> integrationSettings,
			ClassLoader providedClassLoader ) {
		this( persistenceUnit, integrationSettings, providedClassLoader, null, null );
	}

	public EntityManagerFactoryBuilderImpl(
			PersistenceUnitDescriptor persistenceUnit,
			Map<String, Object> integrationSettings,
			ClassLoaderService providedClassLoaderService ) {
		this( persistenceUnit, integrationSettings, null, providedClassLoaderService, null );
	}

	/**
	 * For tests only
	 */
	@Internal
	public EntityManagerFactoryBuilderImpl(
			PersistenceUnitDescriptor persistenceUnitDescriptor,
			Map<String, Object> integration,
			Consumer<MergedSettings> mergedSettingsBaseline) {
		this( persistenceUnitDescriptor, integration, null, null, mergedSettingsBaseline );
	}

	private EntityManagerFactoryBuilderImpl(
			PersistenceUnitDescriptor persistenceUnit,
			Map<String,Object> integrationSettings,
			ClassLoader providedClassLoader,
			ClassLoaderService providedClassLoaderService,
			Consumer<MergedSettings> mergedSettingsBaseline) {
		if ( integrationSettings == null ) {
			integrationSettings = new HashMap<>();
		}

		logPersistenceUnitInformation( persistenceUnit );

		this.persistenceUnit = persistenceUnit;

		// Build the bootstrap service registry, which mainly handles classloader interactions
		final var bootstrapServiceRegistry =
				buildBootstrapServiceRegistry( mergedIntegrationSettings( persistenceUnit, integrationSettings ),
						providedClassLoader, providedClassLoaderService );
		registryLifecycle = new BootstrapRegistryLifecycle( bootstrapServiceRegistry );
		try {
			// merge configuration sources and build the "standard" service registry
			final var registryBuilder = getStandardServiceRegistryBuilder( bootstrapServiceRegistry );
			final var mergedSettings =
					new JpaSettingsAssembler( persistenceUnit, dataSource, this::exceptionHeader )
							.assemble( persistenceUnit, integrationSettings, registryBuilder, mergedSettingsBaseline );
			// keep the merged config values for phase-2
			configurationValues = mergedSettings.getConfigurationValues();
			// Build the "standard" service registry
			registryBuilder.applySettings( configurationValues );
			standardServiceRegistry = registryLifecycle.register( registryBuilder.build() );
			final var metadataSources = new MetadataSources( standardServiceRegistry );
			metamodelBuilder =
					(MetadataBuilderImplementor)
							metadataSources.getMetadataBuilder( standardServiceRegistry );
			managedResources = prepareManagedResources( metadataSources, mergedSettings, ScanningResult.NONE );
			completePreparation();
		}
		catch (Throwable throwable) {
			registryLifecycle.close( throwable );
			throw throwable;
		}
	}

	private ManagedResources prepareManagedResources(
			MetadataSources metadataSources,
			MergedSettings mergedSettings,
			ScanningResult discovery) {
		final var sourcesCollector = new JpaMappingSourcesCollector(
				persistenceUnit, standardServiceRegistry, configurationValues,
				metamodelBuilder.getMetadataBuildingOptions().isXmlMappingEnabled() );
		applyMetamodelBuilderSettings( mergedSettings, sourcesCollector.collect( metadataSources, discovery ) );
		applyMetadataBuilderContributor();
		setupMappingReferences( metadataSources );
		return MetadataBuildingProcess.prepare( metadataSources, metamodelBuilder.getBootstrapContext() );
	}

	private void completePreparation() {
		// The constructors must assign managedResources before validation and enhancement callbacks run.
		setupValidation();
		setupEnhancement( persistenceUnit );
		// Revoke access to the temporary ClassLoader once preparation has completed successfully.
		metamodelBuilder.applyTempClassLoader( null );
	}

	private static Map<String, Object> mergedIntegrationSettings(
			PersistenceUnitDescriptor persistenceUnit,
			Map<String, Object> integrationSettings) {
		final var properties = persistenceUnit.getProperties();
		if ( properties != null ) {
			// original integration setting entries take precedence
			final Map<String,Object> mergedIntegrationSettings =
					new HashMap<>( properties.size() + integrationSettings.size() );
			properties.forEach( (key, value) -> {
				if ( key instanceof String name ) {
					mergedIntegrationSettings.put( name, value );
				}
			} );
			mergedIntegrationSettings.putAll( integrationSettings );
			return mergedIntegrationSettings;
		}
		else {
			return integrationSettings;
		}
	}


	private void setupMappingReferences(MetadataSources metadataSources) {
		MappingSourceHelper.applyConfigurationMappings( metadataSources, standardServiceRegistry );
	}

	private void setupValidation() {
		final Object validatorFactory = configurationValues.get( JAKARTA_VALIDATION_FACTORY );
		if ( validatorFactory == null ) {
			final Object legacyValidatorFactory = configurationValues.get( JPA_VALIDATION_FACTORY );
			if ( legacyValidatorFactory != null ) {
				DEPRECATION_LOGGER.deprecatedSetting( JPA_VALIDATION_FACTORY, JAKARTA_VALIDATION_FACTORY );
			}
			withValidatorFactory( legacyValidatorFactory );
		}
		else {
			withValidatorFactory( validatorFactory );
		}
	}

	private void setupEnhancement(PersistenceUnitDescriptor persistenceUnit) {
		if ( persistenceUnit.isClassTransformerRegistrationDisabled() ) {
			return;
		}

		final boolean dirtyTrackingEnabled =
				readBooleanConfigurationValueDefaultTrue( ENHANCER_ENABLE_DIRTY_TRACKING );
		final boolean lazyInitializationEnabled =
				readBooleanConfigurationValueDefaultTrue( ENHANCER_ENABLE_LAZY_INITIALIZATION );
		final boolean associationManagementEnabled =
				readBooleanConfigurationValue( ENHANCER_ENABLE_ASSOCIATION_MANAGEMENT );

		if ( !lazyInitializationEnabled ) {
			DEPRECATION_LOGGER.deprecatedSettingForRemoval( ENHANCER_ENABLE_LAZY_INITIALIZATION, "true" );
		}
		if ( !dirtyTrackingEnabled ) {
			DEPRECATION_LOGGER.deprecatedSettingForRemoval( ENHANCER_ENABLE_DIRTY_TRACKING, "true" );
		}

		if ( dirtyTrackingEnabled || lazyInitializationEnabled || associationManagementEnabled ) {
			final var classTransformer = persistenceUnit.pushClassTransformer(
					getEnhancementModel(),
					EnhancementOptions.of(dirtyTrackingEnabled, lazyInitializationEnabled, associationManagementEnabled),
					getEnhancementBytecodeProvider());
			if ( classTransformer != null ) {
				final var classLoader = persistenceUnit.getTempClassLoader();
				if ( classLoader == null ) {
					throw new PersistenceException( "Enhancement requires a temp class loader, but none was given"
							+ exceptionHeader() );
				}
				discoverTypesToTransform( classTransformer, classLoader );
			}
		}
	}

	private void discoverTypesToTransform(ClassTransformer transformer, ClassLoader loader) {
		if ( persistenceUnit instanceof PersistenceUnitInfoDescriptor ) {
			EnhancementCandidates.forContainer( persistenceUnit.getAllClassNames() )
					.forEach( name -> transformer.discoverTypes( loader, name ) );
		}
		else {
			EnhancementCandidates.forResources( managedResources ).forEach( (name, hbm) -> {
				try {
					transformer.discoverTypes( loader, name );
				}
				catch (EnhancementException e) {
					if ( !hbm ) {
						throw e;
					}
					JPA_LOGGER.enhancementDiscoveryFailed( name, e );
				}
			} );
		}
	}

	private boolean readBooleanConfigurationValueDefaultTrue(String propertyName) {
		final Object propertyValue = configurationValues.remove( propertyName );
		return propertyValue == null || parseBoolean( propertyValue.toString() );
	}

	/**
	 * Extension point for subclasses. Used by Hibernate Reactive
	 */
	protected StandardServiceRegistryBuilder getStandardServiceRegistryBuilder(BootstrapServiceRegistry bsr) {
		return StandardServiceRegistryBuilder.forJpa( bsr );
	}

	private void applyMetadataBuilderContributor() {
		final Object metadataBuilderContributorSetting = configurationValues.get( METADATA_BUILDER_CONTRIBUTOR );
		if ( metadataBuilderContributorSetting != null ) {
			final var metadataBuilderContributor = loadSettingInstance(
					METADATA_BUILDER_CONTRIBUTOR,
					metadataBuilderContributorSetting,
					MetadataBuilderContributor.class
			);

			if ( metadataBuilderContributor != null ) {
				metadataBuilderContributor.contribute( metamodelBuilder );
			}
		}

		metamodelBuilder.getBootstrapContext().getClassLoaderService()
				.loadJavaServices( MetadataBuilderContributor.class )
				.forEach( contributor -> contributor.contribute( metamodelBuilder ) );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// temporary!
	public Map<Object,Object> getConfigurationValues() {
		return unmodifiableMap( configurationValues );
	}
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private boolean readBooleanConfigurationValue(String propertyName) {
		final Object propertyValue = configurationValues.remove( propertyName );
		return propertyValue != null && parseBoolean( propertyValue.toString() );
	}

	/**
	 * Builds the representation of the domain model used by runtime bytecode enhancement
	 */
	protected EnhancementModel getEnhancementModel() {
		return new PersistenceUnitEnhancementModel(managedResources.getAnnotatedClassNames());
	}

	private BytecodeProvider getEnhancementBytecodeProvider() {
		final Object value = configurationValues.get(BYTECODE_PROVIDER_INSTANCE);
		if (value != null && !(value instanceof BytecodeProvider)) {
			throw new PersistenceException("Property " + BYTECODE_PROVIDER_INSTANCE + " was set to '" + value
					+ "', which is not compatible with the expected type " + BytecodeProvider.class);
		}
		return (BytecodeProvider) value;
	}

	/**
	 * Builds the {@link BootstrapServiceRegistry} used to eventually build the {@link StandardServiceRegistryBuilder}; mainly
	 * used here during instantiation to define class-loading behavior.
	 *
	 * @param integrationSettings Any integration settings passed by the EE container or SE application
	 *
	 * @return The built BootstrapServiceRegistry
	 */
	private BootstrapServiceRegistry buildBootstrapServiceRegistry(
			Map<?,?> integrationSettings,
			ClassLoader providedClassLoader,
			ClassLoaderService providedClassLoaderService) {
		final var builder = new BootstrapServiceRegistryBuilder();
		applyIntegrationProvider( integrationSettings, builder );
		final var strategyRegistrationProviderList =
				(StrategyRegistrationProviderList)
						integrationSettings.get( STRATEGY_REGISTRATION_PROVIDERS );
		if ( strategyRegistrationProviderList != null ) {
			for ( var strategyRegistrationProvider :
					strategyRegistrationProviderList.getStrategyRegistrationProviders() ) {
				builder.applyStrategySelectors( strategyRegistrationProvider );
			}
		}
		configureClassLoading( integrationSettings, providedClassLoader, providedClassLoaderService, builder );
		return builder.build();
	}

	/**
	 * @implNote {@code providedClassLoaderService} and {@code providedClassLoaders}
	 * are mutually exclusive concepts, with priority given to the former.
	 *
	 * @see BootstrapServiceRegistryBuilder#build
	 */
	private void configureClassLoading(
			Map<?, ?> integrationSettings,
			ClassLoader providedClassLoader,
			ClassLoaderService providedClassLoaderService,
			BootstrapServiceRegistryBuilder registryBuilder) {
		if ( providedClassLoaderService != null ) {
			registryBuilder.applyClassLoaderService( providedClassLoaderService );
		}
		else {
			if ( persistenceUnit != null && persistenceUnit.getClassLoader() != null ) {
				registryBuilder.applyClassLoader( persistenceUnit.getClassLoader() );
			}
			if ( providedClassLoader != null ) {
				registryBuilder.applyClassLoader( providedClassLoader );
			}
			applyConfiguredClassLoaders( integrationSettings, registryBuilder );
			//configurationValues not assigned yet, using directly the properties of the PU
			if ( persistenceUnit != null ) {
				final var unitProperties = persistenceUnit.getProperties();
				if ( unitProperties != null ) {
					final TcclLookupPrecedence tcclLookupPrecedence = TcclLookupPrecedence.from( unitProperties );
					if ( tcclLookupPrecedence != null ) {
						registryBuilder.applyTcclLookupPrecedence( tcclLookupPrecedence );
					}
				}
			}
		}
	}

	private static void applyConfiguredClassLoaders(
			Map<?, ?> integrationSettings, BootstrapServiceRegistryBuilder registryBuilder) {
		final Object classLoadersSetting = integrationSettings.get( CLASSLOADERS );
		if ( classLoadersSetting != null ) {
			if ( classLoadersSetting instanceof Collection ) {
				@SuppressWarnings("unchecked")
				final var classLoaders = (Collection<ClassLoader>) classLoadersSetting;
				for ( ClassLoader classLoader : classLoaders ) {
					registryBuilder.applyClassLoader( classLoader );
				}
			}
			else if ( classLoadersSetting.getClass().isArray() ) {
				for ( ClassLoader classLoader : (ClassLoader[]) classLoadersSetting ) {
					registryBuilder.applyClassLoader( classLoader );
				}
			}
			else if ( classLoadersSetting instanceof ClassLoader classLoader ) {
				registryBuilder.applyClassLoader( classLoader );
			}
		}
	}

	private void applyIntegrationProvider(
			Map<?,?> integrationSettings,
			BootstrapServiceRegistryBuilder registryBuilder) {
		if ( integrationSettings == null ) {
			return;
		}

		final Object integrationSetting = integrationSettings.get( INTEGRATOR_PROVIDER );
		if ( integrationSetting != null ) {
			final var integratorProvider =
					loadSettingInstance( INTEGRATOR_PROVIDER, integrationSetting, IntegratorProvider.class );
			if ( integratorProvider != null ) {
				for ( var integrator : integratorProvider.getIntegrators() ) {
					registryBuilder.applyIntegrator( integrator );
				}
			}
		}
	}



	private void applyMetamodelBuilderSettings(
			MergedSettings mergedSettings,
			List<ConverterDescriptor<?,?>> converterDescriptors) {
		metamodelBuilder.getBootstrapContext().markAsJpaBootstrap();

		final var tempClassLoader = persistenceUnit.getTempClassLoader();
		if ( tempClassLoader != null ) {
			metamodelBuilder.applyTempClassLoader( tempClassLoader );
		}

		metamodelBuilder.applyDefaultToOneFetchType( persistenceUnit.getDefaultToOneFetchType() );

		final var cacheRegionDefinitions = mergedSettings.getCacheRegionDefinitions();
		if ( cacheRegionDefinitions != null ) {
			cacheRegionDefinitions.forEach( metamodelBuilder::applyCacheRegionDefinition );
		}

		applyTypeContributors();

		if ( converterDescriptors != null ) {
			converterDescriptors.forEach( metamodelBuilder::applyAttributeConverter );
		}
	}

	private void applyTypeContributors() {
		final var typeContributorList =
				(TypeContributorList)
						configurationValues.remove( TYPE_CONTRIBUTORS );
		if ( typeContributorList != null ) {
			typeContributorList.getTypeContributors().forEach( metamodelBuilder::applyTypes );
		}
		// Service-loaded contributors are applied by MetadataBuildingProcess for every bootstrap entry point.
	}


	// Phase 2 concerns ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private Object validatorFactory;
	private DataSource dataSource;
	private MetadataImplementor metadata;

	/**
	 * Intended for internal testing only...
	 */
	public MetadataImplementor getMetadata() {
		return metadata;
	}

	@Override
	public ManagedResources getManagedResources() {
		return managedResources;
	}

	/**
	 * Used by extensions : Hibernate Reactive
	 */
	@Override
	public MetadataImplementor metadata() {
		if ( metadata == null ) {
			metadata =
					MetadataBuildingProcess.complete( managedResources,
							metamodelBuilder.getBootstrapContext(),
							metamodelBuilder.getMetadataBuildingOptions() );
		}
		return metadata;
	}

	@Override
	public EntityManagerFactoryBuilder withValidatorFactory(Object validatorFactory) {
		this.validatorFactory = validatorFactory;
		if ( validatorFactory != null ) {
			BeanValidationIntegrator.validateFactory( validatorFactory );
		}
		return this;
	}

	@Override
	public EntityManagerFactoryBuilder withDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
		return this;
	}

	@Override
	public void cancel() {
		registryLifecycle.close();
	}

	@Override
	public void generateSchema() {
		Throwable failure = null;
		try {
			populateSessionFactoryBuilder();
			SchemaManagementToolCoordinator.process( metadata, standardServiceRegistry,
					configurationValues, DelayedDropRegistryNotAvailableImpl.INSTANCE );
		}
		catch (Exception e) {
			final var exception = new PersistenceException( "Error performing schema management " + exceptionHeader(), e );
			failure = exception;
			throw exception;
		}
		catch (Error e) {
			failure = e;
			throw e;
		}
		finally {
			try {
				cancel();
			}
			catch (RuntimeException | Error cleanupFailure) {
				if ( failure == null ) {
					throw cleanupFailure;
				}
				if ( failure != cleanupFailure ) {
					failure.addSuppressed( cleanupFailure );
				}
			}
		}
	}

	@Override
	public EntityManagerFactory build() {
		try {
			final var sessionFactoryBuilder = populateSessionFactoryBuilder();
			try {
				final var entityManagerFactory = sessionFactoryBuilder.build();
				registryLifecycle.transferOwnership();
				return entityManagerFactory;
			}
			catch (Exception e) {
				throw new PersistenceException( "Unable to build Hibernate SessionFactory " + exceptionHeader() , e );
			}
		}
		catch (Throwable failure) {
			registryLifecycle.close( failure );
			throw failure;
		}
	}

	protected SessionFactoryBuilder populateSessionFactoryBuilder() {
		final var builder = metadata().getSessionFactoryBuilder();
//		// Locate and apply the requested SessionFactory-level interceptor (if one)
//		final Object sessionFactoryInterceptorSetting = configurationValues.remove( AvailableSettings.INTERCEPTOR );
//		if ( sessionFactoryInterceptorSetting != null ) {
//			final Interceptor sessionFactoryInterceptor =
//					strategySelector.resolveStrategy( Interceptor.class, sessionFactoryInterceptorSetting );
//			builder.applyInterceptor( sessionFactoryInterceptor );
//		}
		handleAllowJtaTransactionAccess( builder );
		addConfiguredSessionFactoryObserver( builder );
		builder.addSessionFactoryObservers( ServiceRegistryCloser.INSTANCE );
		builder.applyEntityNotFoundDelegate( JpaEntityNotFoundDelegate.INSTANCE );
		if ( validatorFactory != null ) {
			builder.applyValidatorFactory( validatorFactory );
		}
		return builder;
	}

	private void addConfiguredSessionFactoryObserver(SessionFactoryBuilder builder) {
		final Object sessionFactoryObserverSetting =
				configurationValues.remove( AvailableSettings.SESSION_FACTORY_OBSERVER );
		if ( sessionFactoryObserverSetting != null ) {
			final SessionFactoryObserver suppliedSessionFactoryObserver =
					standardServiceRegistry.requireService( StrategySelector.class )
							.resolveStrategy( SessionFactoryObserver.class, sessionFactoryObserverSetting );
			builder.addSessionFactoryObservers( suppliedSessionFactoryObserver );
		}
	}

	// will use user override value or default to false if not supplied to follow JPA spec
	private void handleAllowJtaTransactionAccess(SessionFactoryBuilder builder) {
		final boolean jtaTransactionAccessEnabled =
				readBooleanConfigurationValue( AvailableSettings.ALLOW_JTA_TRANSACTION_ACCESS );
		if ( !jtaTransactionAccessEnabled
				&& builder instanceof SessionFactoryBuilderImplementor implementor ) {
			implementor.disableJtaTransactionAccess();
		}
	}

	private String exceptionHeader() {
		return " [persistence unit: " + persistenceUnit.getName() + "] ";
	}

	@SuppressWarnings("unchecked")
	private <T> T loadSettingInstance(String settingName, Object settingValue, Class<T> clazz) {
		final Class<? extends T> instanceClass;
		if ( clazz.isInstance( settingValue ) ) {
			return clazz.cast( settingValue );
		}
		else if ( settingValue instanceof Class ) {
			instanceClass = (Class<? extends T>) settingValue;
		}
		else if ( settingValue instanceof String className ) {
			if ( standardServiceRegistry != null ) {
				instanceClass =
						standardServiceRegistry.requireService( ClassLoaderService.class )
								.classForName( className );
			}
			else {
				try {
					instanceClass = (Class<? extends T>) Class.forName( className );
				}
				catch (ClassNotFoundException e) {
					throw new IllegalArgumentException( "Can't load class: " + className, e );
				}
			}
		}
		else {
			throw new IllegalArgumentException( "The provided " + settingName
					+ " setting value [" + settingValue + "] is not supported" );
		}

		if ( instanceClass != null ) {
			try {
				return instanceClass.newInstance();
			}
			catch (InstantiationException | IllegalAccessException e) {
				throw new IllegalArgumentException(
						"The " + clazz.getSimpleName() +" class [" + instanceClass + "] could not be instantiated",
						e
				);
			}
		}
		else {
			return null;
		}
	}

	/**
	 * Exposed to extensions: see Hibernate Reactive
	 */
	public StandardServiceRegistry getStandardServiceRegistry() {
		return standardServiceRegistry;
	}
}
