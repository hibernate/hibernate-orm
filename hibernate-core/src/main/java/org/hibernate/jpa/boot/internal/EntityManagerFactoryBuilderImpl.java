/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.boot.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import javax.persistence.AttributeConverter;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceException;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.CacheRegionDefinition;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.archive.scan.internal.StandardScanOptions;
import org.hibernate.boot.cfgxml.spi.CfgXmlAccessService;
import org.hibernate.boot.cfgxml.spi.LoadedConfig;
import org.hibernate.boot.cfgxml.spi.MappingReference;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.model.process.spi.MetadataBuildingProcess;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.internal.TcclLookupPrecedence;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.selector.StrategyRegistrationProvider;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.boot.spi.MetadataBuilderContributor;
import org.hibernate.boot.spi.MetadataBuilderImplementor;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryBuilderImplementor;
import org.hibernate.bytecode.enhance.spi.DefaultEnhancementContext;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.UnloadedClass;
import org.hibernate.bytecode.enhance.spi.UnloadedField;
import org.hibernate.cfg.AttributeConverterDefinition;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.beanvalidation.BeanValidationIntegrator;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.factory.spi.MutableIdentifierGeneratorFactory;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.internal.EntityManagerMessageLogger;
import org.hibernate.internal.util.NullnessHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.jpa.boot.spi.StrategyRegistrationProviderList;
import org.hibernate.jpa.boot.spi.TypeContributorList;
import org.hibernate.jpa.internal.util.LogHelper;
import org.hibernate.jpa.internal.util.PersistenceUnitTransactionTypeHelper;
import org.hibernate.jpa.spi.IdentifierGeneratorStrategyProvider;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorBuilderImpl;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorBuilderImpl;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.secure.spi.GrantedPermission;
import org.hibernate.secure.spi.JaccPermissionDeclarations;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tool.schema.spi.DelayedDropRegistryNotAvailableImpl;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;

import org.jboss.jandex.Index;

import static org.hibernate.cfg.AvailableSettings.DATASOURCE;
import static org.hibernate.cfg.AvailableSettings.DRIVER;
import static org.hibernate.cfg.AvailableSettings.JACC_CONTEXT_ID;
import static org.hibernate.cfg.AvailableSettings.JACC_ENABLED;
import static org.hibernate.cfg.AvailableSettings.JACC_PREFIX;
import static org.hibernate.cfg.AvailableSettings.JPA_JDBC_DRIVER;
import static org.hibernate.cfg.AvailableSettings.JPA_JDBC_PASSWORD;
import static org.hibernate.cfg.AvailableSettings.JPA_JDBC_URL;
import static org.hibernate.cfg.AvailableSettings.JPA_JDBC_USER;
import static org.hibernate.cfg.AvailableSettings.JPA_JTA_DATASOURCE;
import static org.hibernate.cfg.AvailableSettings.JPA_NON_JTA_DATASOURCE;
import static org.hibernate.cfg.AvailableSettings.JPA_SHARED_CACHE_MODE;
import static org.hibernate.cfg.AvailableSettings.JPA_TRANSACTION_TYPE;
import static org.hibernate.cfg.AvailableSettings.JPA_VALIDATION_MODE;
import static org.hibernate.cfg.AvailableSettings.PASS;
import static org.hibernate.cfg.AvailableSettings.SESSION_FACTORY_NAME;
import static org.hibernate.cfg.AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY;
import static org.hibernate.cfg.AvailableSettings.URL;
import static org.hibernate.cfg.AvailableSettings.USER;
import static org.hibernate.internal.HEMLogging.messageLogger;
import static org.hibernate.jpa.AvailableSettings.CFG_FILE;
import static org.hibernate.jpa.AvailableSettings.CLASS_CACHE_PREFIX;
import static org.hibernate.jpa.AvailableSettings.COLLECTION_CACHE_PREFIX;
import static org.hibernate.jpa.AvailableSettings.PERSISTENCE_UNIT_NAME;

/**
 * @author Steve Ebersole
 */
public class EntityManagerFactoryBuilderImpl implements EntityManagerFactoryBuilder {
	private static final EntityManagerMessageLogger LOG = messageLogger( EntityManagerFactoryBuilderImpl.class );


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// New settings

	/**
	 * Names a {@link IntegratorProvider}
	 */
	public static final String INTEGRATOR_PROVIDER = "hibernate.integrator_provider";
	
	/**
	 * Names a {@link StrategyRegistrationProviderList}
	 */
	public static final String STRATEGY_REGISTRATION_PROVIDERS = "hibernate.strategy_registration_provider";
	
	/**
	 * Names a {@link TypeContributorList}
	 */
	public static final String TYPE_CONTRIBUTORS = "hibernate.type_contributors";

	/**
	 * Names a {@link MetadataBuilderImplementor}
	 */
	public static final String METADATA_BUILDER_CONTRIBUTOR = "hibernate.metadata_builder_contributor";

	/**
	 * Names a Jandex {@link Index} instance to use.
	 */
	public static final String JANDEX_INDEX = "hibernate.jandex_index";


	private final PersistenceUnitDescriptor persistenceUnit;

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// things built in first phase, needed for second phase..
	private final Map configurationValues;
	private final StandardServiceRegistry standardServiceRegistry;
	private final ManagedResources managedResources;
	private final MetadataBuilderImplementor metamodelBuilder;

	private static class JpaEntityNotFoundDelegate implements EntityNotFoundDelegate, Serializable {
		/**
		 * Singleton access
		 */
		public static final JpaEntityNotFoundDelegate INSTANCE = new JpaEntityNotFoundDelegate();

		public void handleEntityNotFound(String entityName, Serializable id) {
			throw new EntityNotFoundException( "Unable to find " + entityName  + " with id " + id );
		}
	}

	public EntityManagerFactoryBuilderImpl(PersistenceUnitDescriptor persistenceUnit, Map integrationSettings) {
		this( persistenceUnit, integrationSettings, null, null );
	}

	public EntityManagerFactoryBuilderImpl(
			PersistenceUnitDescriptor persistenceUnit,
			Map integrationSettings,
			ClassLoader providedClassLoader ) {
		this( persistenceUnit, integrationSettings, providedClassLoader, null);
	}

	public EntityManagerFactoryBuilderImpl(
			PersistenceUnitDescriptor persistenceUnit,
			Map integrationSettings,
			ClassLoaderService providedClassLoaderService ) {
		this( persistenceUnit, integrationSettings, null, providedClassLoaderService);
	}

	private EntityManagerFactoryBuilderImpl(
			PersistenceUnitDescriptor persistenceUnit,
			Map integrationSettings,
			ClassLoader providedClassLoader,
			ClassLoaderService providedClassLoaderService) {

		LogHelper.logPersistenceUnitInformation( persistenceUnit );

		this.persistenceUnit = persistenceUnit;

		if ( integrationSettings == null ) {
			integrationSettings = new HashMap();
		}

		Map mergedIntegrationSettings = null;
		Properties properties = persistenceUnit.getProperties();
		if ( properties != null ) {
			// original integratin setting entries take precedence
			mergedIntegrationSettings = new HashMap( properties );
			mergedIntegrationSettings.putAll( integrationSettings );
		}

		// Build the boot-strap service registry, which mainly handles class loader interactions
		final BootstrapServiceRegistry bsr = buildBootstrapServiceRegistry(
				mergedIntegrationSettings != null ? mergedIntegrationSettings : integrationSettings,
				providedClassLoader,
				providedClassLoaderService
		);

		// merge configuration sources and build the "standard" service registry
		final StandardServiceRegistryBuilder ssrBuilder = getStandardServiceRegistryBuilder( bsr );

		final MergedSettings mergedSettings = mergeSettings( persistenceUnit, integrationSettings, ssrBuilder );

		// flush before completion validation
		if ( "true".equals( mergedSettings.configurationValues.get( Environment.FLUSH_BEFORE_COMPLETION ) ) ) {
			LOG.definingFlushBeforeCompletionIgnoredInHem( Environment.FLUSH_BEFORE_COMPLETION );
			mergedSettings.configurationValues.put( Environment.FLUSH_BEFORE_COMPLETION, "false" );
		}

		// keep the merged config values for phase-2
		this.configurationValues = mergedSettings.getConfigurationValues();

		// Build the "standard" service registry
		ssrBuilder.applySettings( configurationValues );

		this.standardServiceRegistry = ssrBuilder.build();

		configureIdentifierGenerators( standardServiceRegistry );

		final MetadataSources metadataSources = new MetadataSources( bsr );
		List<AttributeConverterDefinition> attributeConverterDefinitions = applyMappingResources( metadataSources );

		this.metamodelBuilder = (MetadataBuilderImplementor) metadataSources.getMetadataBuilder( standardServiceRegistry );
		applyMetamodelBuilderSettings( mergedSettings, attributeConverterDefinitions );

		applyMetadataBuilderContributor();

		// todo : would be nice to have MetadataBuilder still do the handling of CfgXmlAccessService here
		//		another option is to immediately handle them here (probably in mergeSettings?) as we encounter them...
		final CfgXmlAccessService cfgXmlAccessService = standardServiceRegistry.getService( CfgXmlAccessService.class );
		if ( cfgXmlAccessService.getAggregatedConfig() != null ) {
			if ( cfgXmlAccessService.getAggregatedConfig().getMappingReferences() != null ) {
				for ( MappingReference mappingReference : cfgXmlAccessService.getAggregatedConfig().getMappingReferences() ) {
					mappingReference.apply( metadataSources );
				}
			}
		}

		this.managedResources = MetadataBuildingProcess.prepare(
				metadataSources,
				metamodelBuilder.getBootstrapContext()
		);

		withValidatorFactory( configurationValues.get( org.hibernate.cfg.AvailableSettings.JPA_VALIDATION_FACTORY ) );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// push back class transformation to the environment; for the time being this only has any effect in EE
		// container situations, calling back into PersistenceUnitInfo#addClassTransformer

		final boolean dirtyTrackingEnabled = readBooleanConfigurationValue( AvailableSettings.ENHANCER_ENABLE_DIRTY_TRACKING );
		final boolean lazyInitializationEnabled = readBooleanConfigurationValue( AvailableSettings.ENHANCER_ENABLE_LAZY_INITIALIZATION );
		final boolean associationManagementEnabled = readBooleanConfigurationValue( AvailableSettings.ENHANCER_ENABLE_ASSOCIATION_MANAGEMENT );

		if ( dirtyTrackingEnabled || lazyInitializationEnabled || associationManagementEnabled ) {
			EnhancementContext enhancementContext = getEnhancementContext(
					dirtyTrackingEnabled,
					lazyInitializationEnabled,
					associationManagementEnabled
			);

			persistenceUnit.pushClassTransformer( enhancementContext );
		}

		// for the time being we want to revoke access to the temp ClassLoader if one was passed
		metamodelBuilder.applyTempClassLoader( null );
	}

	/**
	 * Extension point for subclasses. Used by Hibernate Reactive
	 */
	protected StandardServiceRegistryBuilder getStandardServiceRegistryBuilder(BootstrapServiceRegistry bsr) {
		return StandardServiceRegistryBuilder.forJpa( bsr );
	}

	private void applyMetadataBuilderContributor() {

		Object metadataBuilderContributorSetting = configurationValues.get( METADATA_BUILDER_CONTRIBUTOR );

		if ( metadataBuilderContributorSetting == null ) {
			return;
		}

		MetadataBuilderContributor metadataBuilderContributor = loadSettingInstance(
				METADATA_BUILDER_CONTRIBUTOR,
				metadataBuilderContributorSetting,
				MetadataBuilderContributor.class
		);

		if ( metadataBuilderContributor != null ) {
			metadataBuilderContributor.contribute( metamodelBuilder );
		}
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// temporary!
	public Map getConfigurationValues() {
		return Collections.unmodifiableMap( configurationValues );
	}
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private boolean readBooleanConfigurationValue(String propertyName) {
		Object propertyValue = configurationValues.remove( propertyName );
		return propertyValue != null && Boolean.parseBoolean( propertyValue.toString() );
	}

	/**
	 * Builds the context to be used in runtime bytecode enhancement
	 *
	 * @param dirtyTrackingEnabled To enable dirty tracking feature
	 * @param lazyInitializationEnabled To enable lazy initialization feature
	 * @param associationManagementEnabled To enable association management feature
	 * @return An enhancement context for classes managed by this EM
	 */
	protected EnhancementContext getEnhancementContext(
			final boolean dirtyTrackingEnabled,
			final boolean lazyInitializationEnabled,
			final boolean associationManagementEnabled ) {
		return new DefaultEnhancementContext() {

			@Override
			public boolean isEntityClass(UnloadedClass classDescriptor) {
				return managedResources.getAnnotatedClassNames().contains( classDescriptor.getName() )
						&& super.isEntityClass( classDescriptor );
			}

			@Override
			public boolean isCompositeClass(UnloadedClass classDescriptor) {
				return managedResources.getAnnotatedClassNames().contains( classDescriptor.getName() )
						&& super.isCompositeClass( classDescriptor );
			}

			@Override
			public boolean doBiDirectionalAssociationManagement(UnloadedField field) {
				return associationManagementEnabled;
			}

			@Override
			public boolean doDirtyCheckingInline(UnloadedClass classDescriptor) {
				return dirtyTrackingEnabled;
			}

			@Override
			public boolean hasLazyLoadableAttributes(UnloadedClass classDescriptor) {
				return lazyInitializationEnabled;
			}

			@Override
			public boolean isLazyLoadable(UnloadedField field) {
				return lazyInitializationEnabled;
			}

			@Override
			public boolean doExtendedEnhancement(UnloadedClass classDescriptor) {
				// doesn't make any sense to have extended enhancement enabled at runtime. we only enhance entities anyway.
				return false;
			}

		};
	}

	/**
	 * Builds the {@link BootstrapServiceRegistry} used to eventually build the {@link org.hibernate.boot.registry.StandardServiceRegistryBuilder}; mainly
	 * used here during instantiation to define class-loading behavior.
	 *
	 * @param integrationSettings Any integration settings passed by the EE container or SE application
	 *
	 * @return The built BootstrapServiceRegistry
	 */
	private BootstrapServiceRegistry buildBootstrapServiceRegistry(
			Map integrationSettings,
			ClassLoader providedClassLoader,
			ClassLoaderService providedClassLoaderService) {
		final BootstrapServiceRegistryBuilder bsrBuilder = new BootstrapServiceRegistryBuilder();

		applyIntegrationProvider( integrationSettings, bsrBuilder );

		final StrategyRegistrationProviderList strategyRegistrationProviderList
				= (StrategyRegistrationProviderList) integrationSettings.get( STRATEGY_REGISTRATION_PROVIDERS );
		if ( strategyRegistrationProviderList != null ) {
			for ( StrategyRegistrationProvider strategyRegistrationProvider : strategyRegistrationProviderList.getStrategyRegistrationProviders() ) {
				bsrBuilder.applyStrategySelectors( strategyRegistrationProvider );
			}
		}


		// ClassLoaders ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// NOTE: See BootstrapServiceRegistryBuilder#build.  providedClassLoaderService and providedClassLoaders are
		// mutually exclusive concepts, with priority given to the former

		if ( providedClassLoaderService != null ) {
			bsrBuilder.applyClassLoaderService( providedClassLoaderService );
		}
		else {
			if ( persistenceUnit.getClassLoader() != null ) {
				bsrBuilder.applyClassLoader( persistenceUnit.getClassLoader() );
			}

			if ( providedClassLoader != null ) {
				bsrBuilder.applyClassLoader( providedClassLoader );
			}

			final ClassLoader appClassLoader = (ClassLoader) integrationSettings.get( org.hibernate.cfg.AvailableSettings.APP_CLASSLOADER );
			if ( appClassLoader != null ) {
				LOG.debugf(
						"Found use of deprecated `%s` setting; use `%s` instead.",
						org.hibernate.cfg.AvailableSettings.APP_CLASSLOADER,
						org.hibernate.cfg.AvailableSettings.CLASSLOADERS
				);
			}
			final Object classLoadersSetting = integrationSettings.get( org.hibernate.cfg.AvailableSettings.CLASSLOADERS );
			if ( classLoadersSetting != null ) {
				if ( java.util.Collection.class.isInstance( classLoadersSetting ) ) {
					for ( ClassLoader classLoader : (java.util.Collection<ClassLoader>) classLoadersSetting ) {
						bsrBuilder.applyClassLoader( classLoader );
					}
				}
				else if ( classLoadersSetting.getClass().isArray() ) {
					for ( ClassLoader classLoader : (ClassLoader[]) classLoadersSetting ) {
						bsrBuilder.applyClassLoader( classLoader );
					}
				}
				else if ( ClassLoader.class.isInstance( classLoadersSetting ) ) {
					bsrBuilder.applyClassLoader( (ClassLoader) classLoadersSetting );
				}
			}
                        
			//configurationValues not assigned yet, using directly the properties of the PU
			Properties puProperties = persistenceUnit.getProperties();
			if( puProperties != null ) {
				final String tcclLookupPrecedence = puProperties.getProperty( org.hibernate.cfg.AvailableSettings.TC_CLASSLOADER );
				if( tcclLookupPrecedence != null ) {
					bsrBuilder.applyTcclLookupPrecedence( TcclLookupPrecedence.valueOf( tcclLookupPrecedence.toUpperCase( Locale.ROOT ) ) );
				}
			}
		}

		return bsrBuilder.build();
	}

	private void applyIntegrationProvider(Map integrationSettings, BootstrapServiceRegistryBuilder bsrBuilder) {
		Object integrationSetting = integrationSettings.get( INTEGRATOR_PROVIDER );
		if ( integrationSetting == null ) {
			return;
		}
		final IntegratorProvider integratorProvider = loadSettingInstance(
				INTEGRATOR_PROVIDER,
				integrationSetting,
				IntegratorProvider.class
		);

		if ( integratorProvider != null ) {
			for ( Integrator integrator : integratorProvider.getIntegrators() ) {
				bsrBuilder.applyIntegrator( integrator );
			}
		}
	}

	@SuppressWarnings("unchecked")
	private MergedSettings mergeSettings(
			PersistenceUnitDescriptor persistenceUnit,
			Map<?,?> integrationSettings,
			StandardServiceRegistryBuilder ssrBuilder) {
		final MergedSettings mergedSettings = new MergedSettings();
		mergedSettings.processPersistenceUnitDescriptorProperties( persistenceUnit );

		// see if the persistence.xml settings named a Hibernate config file....
		String cfgXmlResourceName = (String) mergedSettings.configurationValues.remove( CFG_FILE );
		if ( StringHelper.isEmpty( cfgXmlResourceName ) ) {
			// see if integration settings named a Hibernate config file....
			cfgXmlResourceName = (String) integrationSettings.get( CFG_FILE );
		}

		if ( StringHelper.isNotEmpty( cfgXmlResourceName ) ) {
			processHibernateConfigXmlResources( ssrBuilder, mergedSettings, cfgXmlResourceName );
		}

		normalizeSettings( persistenceUnit, integrationSettings, mergedSettings );

		final String jaccContextId = (String) mergedSettings.configurationValues.get( JACC_CONTEXT_ID );

		// here we are going to iterate the merged config settings looking for:
		//		1) additional JACC permissions
		//		2) additional cache region declarations
		//
		// we will also clean up any references with null entries
		Iterator itr = mergedSettings.configurationValues.entrySet().iterator();
		while ( itr.hasNext() ) {
			final Map.Entry entry = (Map.Entry) itr.next();
			if ( entry.getValue() == null ) {
				// remove entries with null values
				itr.remove();
				break;
			}

			if ( String.class.isInstance( entry.getKey() ) && String.class.isInstance( entry.getValue() ) ) {
				final String keyString = (String) entry.getKey();
				final String valueString = (String) entry.getValue();

				if ( keyString.startsWith( JACC_PREFIX ) ) {
					if( !JACC_CONTEXT_ID.equals( keyString ) && !JACC_ENABLED.equals( keyString )) {
						if ( jaccContextId == null ) {
							LOG.debug(
									"Found JACC permission grant [%s] in properties, but no JACC context id was specified; ignoring"
							);
						}
						else {
							mergedSettings.getJaccPermissions( jaccContextId ).addPermissionDeclaration(
									parseJaccConfigEntry( keyString, valueString )
							);
						}
					}
				}
				else if ( keyString.startsWith( CLASS_CACHE_PREFIX ) ) {
					mergedSettings.addCacheRegionDefinition(
							parseCacheRegionDefinitionEntry(
									keyString.substring( CLASS_CACHE_PREFIX.length() + 1 ),
									valueString,
									CacheRegionDefinition.CacheRegionType.ENTITY
							)
					);
				}
				else if ( keyString.startsWith( COLLECTION_CACHE_PREFIX ) ) {
					mergedSettings.addCacheRegionDefinition(
							parseCacheRegionDefinitionEntry(
									keyString.substring( COLLECTION_CACHE_PREFIX.length() + 1 ),
									(String) entry.getValue(),
									CacheRegionDefinition.CacheRegionType.COLLECTION
							)
					);
				}
			}

		}

		return mergedSettings;
	}

	/**
	 * Handles normalizing the settings coming from multiple sources, applying proper precedences
	 */
	@SuppressWarnings("unchecked")
	private void normalizeSettings(
			PersistenceUnitDescriptor persistenceUnit,
			Map<?, ?> integrationSettings,
			MergedSettings mergedSettings) {
		// make a copy so we can remove things as we process them
		final HashMap<?, ?> integrationSettingsCopy = new HashMap<>( integrationSettings );

		normalizeConnectionAccessUserAndPass( integrationSettingsCopy, mergedSettings );

		normalizeTransactionCoordinator( persistenceUnit, integrationSettingsCopy, mergedSettings );

		normalizeDataAccess( integrationSettingsCopy, mergedSettings, persistenceUnit );

		// normalize ValidationMode
		final Object intgValidationMode = integrationSettingsCopy.remove( JPA_VALIDATION_MODE );
		if ( intgValidationMode != null ) {
			mergedSettings.configurationValues.put( JPA_VALIDATION_MODE, intgValidationMode );
		}
		else if ( persistenceUnit.getValidationMode() != null ) {
			mergedSettings.configurationValues.put( JPA_VALIDATION_MODE, persistenceUnit.getValidationMode() );
		}

		// normalize SharedCacheMode
		final Object intgCacheMode = integrationSettingsCopy.remove( JPA_SHARED_CACHE_MODE );
		if ( intgCacheMode != null ) {
			mergedSettings.configurationValues.put( JPA_SHARED_CACHE_MODE, intgCacheMode );
		}
		else if ( persistenceUnit.getSharedCacheMode() != null ) {
			mergedSettings.configurationValues.put( JPA_SHARED_CACHE_MODE, persistenceUnit.getSharedCacheMode() );
		}

		// Apply all "integration overrides" as the last step.  By specification,
		// these should have precedence.
		//
		// NOTE that this occurs after the specialized normalize calls above which remove
		// any specially-handled settings.
		for ( Map.Entry<?,?> entry : integrationSettingsCopy.entrySet() ) {
			if ( entry.getKey() == null ) {
				continue;
			}

			if ( entry.getValue() == null ) {
				mergedSettings.configurationValues.remove( entry.getKey() );
			}
			else {
				mergedSettings.configurationValues.put( entry.getKey(), entry.getValue() );
			}
		}
	}

	/**
	 * Because a DataSource can be secured (requiring Hibernate to pass the USER/PASSWORD when accessing the DataSource)
	 * we apply precedence to the USER and PASS separately
	 */
	private void normalizeConnectionAccessUserAndPass(
			HashMap<?, ?> integrationSettingsCopy,
			MergedSettings mergedSettings) {
		//noinspection unchecked
		final Object effectiveUser = NullnessHelper.coalesceSuppliedValues(
				() -> integrationSettingsCopy.remove( USER ),
				() -> integrationSettingsCopy.remove( JPA_JDBC_USER ),
				() -> extractPuProperty( persistenceUnit, USER ),
				() -> extractPuProperty( persistenceUnit, JPA_JDBC_USER )
		);

		//noinspection unchecked
		final Object effectivePass = NullnessHelper.coalesceSuppliedValues(
				() -> integrationSettingsCopy.remove( PASS ),
				() -> integrationSettingsCopy.remove( JPA_JDBC_PASSWORD ),
				() -> extractPuProperty( persistenceUnit, PASS ),
				() -> extractPuProperty( persistenceUnit, JPA_JDBC_PASSWORD )
		);

		if ( effectiveUser != null || effectivePass != null ) {
			applyUserAndPass( effectiveUser, effectivePass, mergedSettings );
		}
	}

	private <T> T extractPuProperty(PersistenceUnitDescriptor persistenceUnit, String propertyName) {
		//noinspection unchecked
		return persistenceUnit.getProperties() == null ? null : (T) persistenceUnit.getProperties().get( propertyName );
	}

	@SuppressWarnings("unchecked")
	private void applyUserAndPass(Object effectiveUser, Object effectivePass, MergedSettings mergedSettings) {
		if ( effectiveUser != null ) {
			mergedSettings.configurationValues.put( USER, effectiveUser );
			mergedSettings.configurationValues.put( JPA_JDBC_USER, effectiveUser );
		}

		if ( effectivePass != null ) {
			mergedSettings.configurationValues.put( PASS, effectivePass );
			mergedSettings.configurationValues.put( JPA_JDBC_PASSWORD, effectivePass );
		}
	}

	private static final String IS_JTA_TXN_COORD = "local.setting.IS_JTA_TXN_COORD";

	@SuppressWarnings("unchecked")
	private void normalizeTransactionCoordinator(
			PersistenceUnitDescriptor persistenceUnit,
			HashMap<?, ?> integrationSettingsCopy,
			MergedSettings mergedSettings) {
		PersistenceUnitTransactionType txnType = null;

		final Object intgTxnType = integrationSettingsCopy.remove( JPA_TRANSACTION_TYPE );

		if ( intgTxnType != null ) {
			txnType = PersistenceUnitTransactionTypeHelper.interpretTransactionType( intgTxnType );
		}
		else if ( persistenceUnit.getTransactionType() != null ) {
			txnType = persistenceUnit.getTransactionType();
		}
		else {
			final Object puPropTxnType = mergedSettings.configurationValues.get( JPA_TRANSACTION_TYPE );
			if ( puPropTxnType != null ) {
				txnType = PersistenceUnitTransactionTypeHelper.interpretTransactionType( puPropTxnType );
			}
		}

		if ( txnType == null ) {
			// is it more appropriate to have this be based on bootstrap entry point (EE vs SE)?
			LOG.debugf( "PersistenceUnitTransactionType not specified - falling back to RESOURCE_LOCAL" );
			txnType = PersistenceUnitTransactionType.RESOURCE_LOCAL;
		}

		boolean hasTxStrategy = mergedSettings.configurationValues.containsKey( TRANSACTION_COORDINATOR_STRATEGY );
		final Boolean definiteJtaCoordinator;

		if ( hasTxStrategy ) {
			LOG.overridingTransactionStrategyDangerous( TRANSACTION_COORDINATOR_STRATEGY );

			// see if we can tell whether it is a JTA coordinator
			final Object strategy = mergedSettings.configurationValues.get( TRANSACTION_COORDINATOR_STRATEGY );
			if ( strategy instanceof TransactionCoordinatorBuilder ) {
				definiteJtaCoordinator = ( (TransactionCoordinatorBuilder) strategy ).isJta();
			}
			else {
				definiteJtaCoordinator = false;
			}
		}
		else {
			if ( txnType == PersistenceUnitTransactionType.JTA ) {
				mergedSettings.configurationValues.put( TRANSACTION_COORDINATOR_STRATEGY, JtaTransactionCoordinatorBuilderImpl.class );
				definiteJtaCoordinator = true;
			}
			else if ( txnType == PersistenceUnitTransactionType.RESOURCE_LOCAL ) {
				mergedSettings.configurationValues.put( TRANSACTION_COORDINATOR_STRATEGY, JdbcResourceLocalTransactionCoordinatorBuilderImpl.class );
				definiteJtaCoordinator = false;
			}
			else {
				throw new IllegalStateException( "Could not determine TransactionCoordinator strategy to use" );
			}
		}

		mergedSettings.configurationValues.put( IS_JTA_TXN_COORD, definiteJtaCoordinator );
	}

	private void normalizeDataAccess(
			HashMap<?, ?> integrationSettingsCopy,
			MergedSettings mergedSettings,
			PersistenceUnitDescriptor persistenceUnit) {
		if ( dataSource != null ) {
			applyDataSource(
					dataSource,
					// we don't explicitly know
					null,
					integrationSettingsCopy,
					mergedSettings
			);

			// EARLY EXIT!!
			return;
		}

		if ( integrationSettingsCopy.containsKey( DATASOURCE ) ) {
			final Object dataSourceRef = integrationSettingsCopy.remove( DATASOURCE );
			if ( dataSourceRef != null ) {
				applyDataSource(
						dataSourceRef,
						null,
						integrationSettingsCopy,
						mergedSettings
				);

				// EARLY EXIT!!
				return;
			}
		}

		if ( integrationSettingsCopy.containsKey( JPA_JTA_DATASOURCE ) ) {
			final Object dataSourceRef = integrationSettingsCopy.remove( JPA_JTA_DATASOURCE );
			if ( dataSourceRef != null ) {
				applyDataSource(
						dataSourceRef,
						true,
						integrationSettingsCopy,
						mergedSettings
				);

				// EARLY EXIT!!
				return;
			}
		}

		if ( integrationSettingsCopy.containsKey( JPA_NON_JTA_DATASOURCE ) ) {
			final Object dataSourceRef = integrationSettingsCopy.remove( JPA_NON_JTA_DATASOURCE );

			applyDataSource(
					dataSourceRef,
					false,
					integrationSettingsCopy,
					mergedSettings
			);

			// EARLY EXIT!!
			return;
		}

		if ( integrationSettingsCopy.containsKey( URL ) ) {
			// these have precedence over the JPA ones
			final Object integrationJdbcUrl = integrationSettingsCopy.get( URL );
			if ( integrationJdbcUrl != null ) {
				//noinspection unchecked
				applyJdbcSettings(
						integrationJdbcUrl,
						NullnessHelper.coalesceSuppliedValues(
								() -> ConfigurationHelper.getString( DRIVER, integrationSettingsCopy ),
								() -> ConfigurationHelper.getString( JPA_JDBC_DRIVER, integrationSettingsCopy ),
								() -> ConfigurationHelper.getString( DRIVER, mergedSettings.configurationValues ),
								() -> ConfigurationHelper.getString( JPA_JDBC_DRIVER, mergedSettings.configurationValues )
						),
						integrationSettingsCopy,
						mergedSettings
				);

				// EARLY EXIT!!
				return;
			}
		}

		if ( integrationSettingsCopy.containsKey( JPA_JDBC_URL ) ) {
			final Object integrationJdbcUrl = integrationSettingsCopy.get( JPA_JDBC_URL );

			if ( integrationJdbcUrl != null ) {
				//noinspection unchecked
				applyJdbcSettings(
						integrationJdbcUrl,
						NullnessHelper.coalesceSuppliedValues(
								() -> ConfigurationHelper.getString( JPA_JDBC_DRIVER, integrationSettingsCopy ),
								() -> ConfigurationHelper.getString( JPA_JDBC_DRIVER, mergedSettings.configurationValues )
						),
						integrationSettingsCopy,
						mergedSettings
				);

				// EARLY EXIT!!
				return;
			}
		}

		if ( persistenceUnit.getJtaDataSource() != null ) {
			applyDataSource(
					persistenceUnit.getJtaDataSource(),
					true,
					integrationSettingsCopy,
					mergedSettings
			);

			// EARLY EXIT!!
			return;
		}

		if ( persistenceUnit.getNonJtaDataSource() != null ) {
			applyDataSource(
					persistenceUnit.getNonJtaDataSource(),
					false,
					integrationSettingsCopy,
					mergedSettings
			);

			// EARLY EXIT!!
			return;
		}

		if ( mergedSettings.configurationValues.containsKey( URL ) ) {
			final Object url = mergedSettings.configurationValues.get( URL );

			if ( url != null && ( ! ( url instanceof String ) || StringHelper.isNotEmpty( (String) url ) ) ) {
				applyJdbcSettings(
						url,
						ConfigurationHelper.getString( DRIVER, mergedSettings.configurationValues ),
						integrationSettingsCopy,
						mergedSettings
				);

				// EARLY EXIT!!
				return;
			}
		}

		if ( mergedSettings.configurationValues.containsKey( JPA_JDBC_URL ) ) {
			final Object url = mergedSettings.configurationValues.get( JPA_JDBC_URL );

			if ( url != null && ( ! ( url instanceof String ) || StringHelper.isNotEmpty( (String) url ) ) ) {
				applyJdbcSettings(
						url,
						ConfigurationHelper.getString( JPA_JDBC_DRIVER, mergedSettings.configurationValues ),
						integrationSettingsCopy,
						mergedSettings
				);

				// EARLY EXIT!!
				return;
			}
		}

		// any other conditions to account for?
	}

	@SuppressWarnings("unchecked")
	private void applyDataSource(
			Object dataSourceRef,
			Boolean useJtaDataSource,
			HashMap<?, ?> integrationSettingsCopy,
			MergedSettings mergedSettings) {

		// `IS_JTA_TXN_COORD` is a value set during `#normalizeTransactionCoordinator` to indicate whether
		// the execution environment "is JTA" as best as it can tell..
		//
		// we use this value when JTA was not explicitly specified in regards the DataSource
		final boolean isJtaTransactionCoordinator = (boolean) mergedSettings.configurationValues.remove( IS_JTA_TXN_COORD );
		final boolean isJta = useJtaDataSource == null ? isJtaTransactionCoordinator : useJtaDataSource;

		// add to EMF properties (questionable - see HHH-13432)
		final String emfKey;
		final String inverseEmfKey;
		if ( isJta ) {
			emfKey = JPA_JTA_DATASOURCE;
			inverseEmfKey = JPA_NON_JTA_DATASOURCE;
		}
		else {
			emfKey = JPA_NON_JTA_DATASOURCE;
			inverseEmfKey = JPA_JTA_DATASOURCE;
		}
		mergedSettings.configurationValues.put( emfKey, dataSourceRef );

		// clear any settings logically overridden by this datasource
		cleanUpConfigKeys(
				integrationSettingsCopy,
				mergedSettings,
				inverseEmfKey,
				JPA_JDBC_DRIVER,
				DRIVER,
				JPA_JDBC_URL,
				URL
		);


		// clean-up the entries in the "integration overrides" so they do not get get picked
		// up in the general "integration overrides" handling
		cleanUpConfigKeys( integrationSettingsCopy, DATASOURCE, JPA_JTA_DATASOURCE, JPA_NON_JTA_DATASOURCE );

		// add under Hibernate's DATASOURCE setting where the ConnectionProvider will find it
		mergedSettings.configurationValues.put( DATASOURCE, dataSourceRef );
	}

	private void cleanUpConfigKeys(HashMap<?, ?> integrationSettingsCopy, MergedSettings mergedSettings, String... keys) {
		for ( String key : keys ) {
			final Object removedIntgSetting = integrationSettingsCopy.remove( key );
			if ( removedIntgSetting != null ) {
				LOG.debugf( "Removed integration override setting [%s] due to normalization", key );
			}

			final Object removedMergedSetting = mergedSettings.configurationValues.remove( key );
			if ( removedMergedSetting != null ) {
				LOG.debugf( "Removed merged setting [%s] due to normalization", key );
			}
		}
	}

	private void cleanUpConfigKeys(Map<?, ?> settings, String... keys) {
		for ( String key : keys ) {
			settings.remove( key );
		}
	}

	@SuppressWarnings("unchecked")
	private void applyJdbcSettings(
			Object url,
			String driver,
			HashMap<?, ?> integrationSettingsCopy,
			MergedSettings mergedSettings) {
		mergedSettings.configurationValues.put( URL, url );
		mergedSettings.configurationValues.put( JPA_JDBC_URL, url );

		if ( driver != null ) {
			mergedSettings.configurationValues.put( DRIVER, driver );
			mergedSettings.configurationValues.put( JPA_JDBC_DRIVER, driver );
		}
		else {
			mergedSettings.configurationValues.remove( DRIVER );
			mergedSettings.configurationValues.remove( JPA_JDBC_DRIVER );
		}

		// clean up the integration-map values
		cleanUpConfigKeys(
				integrationSettingsCopy,
				DRIVER,
				JPA_JDBC_DRIVER,
				URL,
				JPA_JDBC_URL,
				USER,
				JPA_JDBC_USER,
				PASS,
				JPA_JDBC_PASSWORD
		);

		cleanUpConfigKeys(
				integrationSettingsCopy,
				mergedSettings,
				DATASOURCE,
				JPA_JTA_DATASOURCE,
				JPA_NON_JTA_DATASOURCE
		);
	}

	private void processHibernateConfigXmlResources(
			StandardServiceRegistryBuilder ssrBuilder,
			MergedSettings mergedSettings,
			String cfgXmlResourceName) {
		final LoadedConfig loadedConfig = ssrBuilder.getConfigLoader().loadConfigXmlResource( cfgXmlResourceName );

		mergedSettings.processHibernateConfigXmlResources( loadedConfig );

		ssrBuilder.getAggregatedCfgXml().merge( loadedConfig );
	}

	private GrantedPermission parseJaccConfigEntry(String keyString, String valueString) {
		try {
			final int roleStart = JACC_PREFIX.length() + 1;
			final String role = keyString.substring( roleStart, keyString.indexOf( '.', roleStart ) );
			final int classStart = roleStart + role.length() + 1;
			final String clazz = keyString.substring( classStart, keyString.length() );
			return new GrantedPermission( role, clazz, valueString );
		}
		catch ( IndexOutOfBoundsException e ) {
			throw persistenceException( "Illegal usage of " + JACC_PREFIX + ": " + keyString );
		}
	}

	private CacheRegionDefinition parseCacheRegionDefinitionEntry(String role, String value, CacheRegionDefinition.CacheRegionType cacheType) {
		final StringTokenizer params = new StringTokenizer( value, ";, " );
		if ( !params.hasMoreTokens() ) {
			StringBuilder error = new StringBuilder( "Illegal usage of " );
			if ( cacheType == CacheRegionDefinition.CacheRegionType.ENTITY ) {
				error.append( CLASS_CACHE_PREFIX )
						.append( ": " )
						.append( CLASS_CACHE_PREFIX );
			}
			else {
				error.append( COLLECTION_CACHE_PREFIX )
						.append( ": " )
						.append( COLLECTION_CACHE_PREFIX );
			}
			error.append( '.' )
					.append( role )
					.append( ' ' )
					.append( value )
					.append( ".  Was expecting configuration (usage[,region[,lazy]]), but found none" );
			throw persistenceException( error.toString() );
		}

		String usage = params.nextToken();
		String region = null;
		if ( params.hasMoreTokens() ) {
			region = params.nextToken();
		}
		boolean lazyProperty = true;
		if ( cacheType == CacheRegionDefinition.CacheRegionType.ENTITY ) {
			if ( params.hasMoreTokens() ) {
				lazyProperty = "all".equalsIgnoreCase( params.nextToken() );
			}
		}
		else {
			lazyProperty = false;
		}

		return new CacheRegionDefinition( cacheType, role, usage, region, lazyProperty );
	}

	private void configureIdentifierGenerators(StandardServiceRegistry ssr) {
		final StrategySelector strategySelector = ssr.getService( StrategySelector.class );

		// apply id generators
		final Object idGeneratorStrategyProviderSetting = configurationValues.remove( AvailableSettings.IDENTIFIER_GENERATOR_STRATEGY_PROVIDER );
		if ( idGeneratorStrategyProviderSetting != null ) {
			final IdentifierGeneratorStrategyProvider idGeneratorStrategyProvider =
					strategySelector.resolveStrategy( IdentifierGeneratorStrategyProvider.class, idGeneratorStrategyProviderSetting );
			final MutableIdentifierGeneratorFactory identifierGeneratorFactory = ssr.getService( MutableIdentifierGeneratorFactory.class );
			if ( identifierGeneratorFactory == null ) {
				throw persistenceException(
						"Application requested custom identifier generator strategies, " +
								"but the MutableIdentifierGeneratorFactory could not be found"
				);
			}
			for ( Map.Entry<String,Class<?>> entry : idGeneratorStrategyProvider.getStrategies().entrySet() ) {
				identifierGeneratorFactory.register( entry.getKey(), entry.getValue() );
			}
		}
	}

	@SuppressWarnings("unchecked")
	private List<AttributeConverterDefinition> applyMappingResources(MetadataSources metadataSources) {
		// todo : where in the heck are `org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor.getManagedClassNames` handled?!?

//		final ClassLoaderService classLoaderService = ssr.getService( ClassLoaderService.class );
//
//		// todo : make sure MetadataSources/Metadata are capable of handling duplicate sources
//
//		// explicit persistence unit mapping files listings
//		if ( persistenceUnit.getMappingFileNames() != null ) {
//			for ( String name : persistenceUnit.getMappingFileNames() ) {
//				metadataSources.addResource( name );
//			}
//		}
//
//		// explicit persistence unit managed class listings
//		//		IMPL NOTE : managed-classes can contain class or package names!!!
//		if ( persistenceUnit.getManagedClassNames() != null ) {
//			for ( String managedClassName : persistenceUnit.getManagedClassNames() ) {
//				// try it as a class name first...
//				final String classFileName = managedClassName.replace( '.', '/' ) + ".class";
//				final URL classFileUrl = classLoaderService.locateResource( classFileName );
//				if ( classFileUrl != null ) {
//					// it is a class
//					metadataSources.addAnnotatedClassName( managedClassName );
//					continue;
//				}
//
//				// otherwise, try it as a package name
//				final String packageInfoFileName = managedClassName.replace( '.', '/' ) + "/package-info.class";
//				final URL packageInfoFileUrl = classLoaderService.locateResource( packageInfoFileName );
//				if ( packageInfoFileUrl != null ) {
//					// it is a package
//					metadataSources.addPackage( managedClassName );
//					continue;
//				}
//
//				LOG.debugf(
//						"Unable to resolve class [%s] named in persistence unit [%s]",
//						managedClassName,
//						persistenceUnit.getName()
//				);
//			}
//		}

		List<AttributeConverterDefinition> attributeConverterDefinitions = null;

		// add any explicit Class references passed in
		final List<Class> loadedAnnotatedClasses = (List<Class>) configurationValues.remove( AvailableSettings.LOADED_CLASSES );
		if ( loadedAnnotatedClasses != null ) {
			for ( Class cls : loadedAnnotatedClasses ) {
				if ( AttributeConverter.class.isAssignableFrom( cls ) ) {
					if ( attributeConverterDefinitions == null ) {
						attributeConverterDefinitions = new ArrayList<>();
					}
					attributeConverterDefinitions.add( AttributeConverterDefinition.from( (Class<? extends AttributeConverter>) cls ) );
				}
				else {
					metadataSources.addAnnotatedClass( cls );
				}
			}
		}

		// add any explicit hbm.xml references passed in
		final String explicitHbmXmls = (String) configurationValues.remove( AvailableSettings.HBXML_FILES );
		if ( explicitHbmXmls != null ) {
			for ( String hbmXml : StringHelper.split( ", ", explicitHbmXmls ) ) {
				metadataSources.addResource( hbmXml );
			}
		}

		// add any explicit orm.xml references passed in
		final List<String> explicitOrmXmlList = (List<String>) configurationValues.remove( AvailableSettings.XML_FILE_NAMES );
		if ( explicitOrmXmlList != null ) {
			explicitOrmXmlList.forEach( metadataSources::addResource );
		}

		return attributeConverterDefinitions;
	}

	private void applyMetamodelBuilderSettings(
			MergedSettings mergedSettings,
			List<AttributeConverterDefinition> attributeConverterDefinitions) {
		metamodelBuilder.getBootstrapContext().markAsJpaBootstrap();

		if ( persistenceUnit.getTempClassLoader() != null ) {
			metamodelBuilder.applyTempClassLoader( persistenceUnit.getTempClassLoader() );
		}

		metamodelBuilder.applyScanEnvironment( new StandardJpaScanEnvironmentImpl( persistenceUnit ) );
		metamodelBuilder.applyScanOptions(
				new StandardScanOptions(
						(String) configurationValues.get( org.hibernate.cfg.AvailableSettings.SCANNER_DISCOVERY ),
						persistenceUnit.isExcludeUnlistedClasses()
				)
		);

		if ( mergedSettings.cacheRegionDefinitions != null ) {
			mergedSettings.cacheRegionDefinitions.forEach( metamodelBuilder::applyCacheRegionDefinition );
		}

		final TypeContributorList typeContributorList = (TypeContributorList) configurationValues.remove(
				TYPE_CONTRIBUTORS
		);
		if ( typeContributorList != null ) {
			typeContributorList.getTypeContributors().forEach( metamodelBuilder::applyTypes );
		}

		if ( attributeConverterDefinitions != null ) {
			attributeConverterDefinitions.forEach( metamodelBuilder::applyAttributeConverter );
		}
	}


	// Phase 2 concerns ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private Object validatorFactory;
	private Object cdiBeanManager;
	private DataSource dataSource;
	private MetadataImplementor metadata;

	/**
	 * Intended for internal testing only...
	 */
	public MetadataImplementor getMetadata() {
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
		// todo : close the bootstrap registry (not critical, but nice to do)
	}

	/**
	 * Used by extensions : Hibernate Reactive
	 */
	protected MetadataImplementor metadata() {
		if ( this.metadata == null ) {
			this.metadata = MetadataBuildingProcess.complete(
					managedResources,
					metamodelBuilder.getBootstrapContext(),
					metamodelBuilder.getMetadataBuildingOptions()
			);
		}
		return metadata;
	}

	@Override
	public void generateSchema() {
		// This seems overkill, but building the SF is necessary to get the Integrators to kick in.
		// Metamodel will clean this up...
		try {
			SessionFactoryBuilder sfBuilder = metadata().getSessionFactoryBuilder();
			populateSfBuilder( sfBuilder, standardServiceRegistry );

			SchemaManagementToolCoordinator.process(
					metadata, standardServiceRegistry, configurationValues, DelayedDropRegistryNotAvailableImpl.INSTANCE
			);
		}
		catch (Exception e) {
			throw persistenceException( "Error performing schema management", e );
		}

		// release this builder
		cancel();
	}

	@Override
	public EntityManagerFactory build() {
		final SessionFactoryBuilder sfBuilder = metadata().getSessionFactoryBuilder();
		populateSfBuilder( sfBuilder, standardServiceRegistry );

		try {
			return sfBuilder.build();
		}
		catch (Exception e) {
			throw persistenceException( "Unable to build Hibernate SessionFactory", e );
		}
	}

	protected void populateSfBuilder(SessionFactoryBuilder sfBuilder, StandardServiceRegistry ssr) {

		final StrategySelector strategySelector = ssr.getService( StrategySelector.class );

//		// Locate and apply the requested SessionFactory-level interceptor (if one)
//		final Object sessionFactoryInterceptorSetting = configurationValues.remove( org.hibernate.cfg.AvailableSettings.INTERCEPTOR );
//		if ( sessionFactoryInterceptorSetting != null ) {
//			final Interceptor sessionFactoryInterceptor =
//					strategySelector.resolveStrategy( Interceptor.class, sessionFactoryInterceptorSetting );
//			sfBuilder.applyInterceptor( sessionFactoryInterceptor );
//		}

		// will use user override value or default to false if not supplied to follow JPA spec.
		final boolean jtaTransactionAccessEnabled = readBooleanConfigurationValue( AvailableSettings.ALLOW_JTA_TRANSACTION_ACCESS );
		if ( !jtaTransactionAccessEnabled ) {
			( ( SessionFactoryBuilderImplementor ) sfBuilder ).disableJtaTransactionAccess();
		}

		final boolean allowRefreshDetachedEntity = readBooleanConfigurationValue( org.hibernate.cfg.AvailableSettings.ALLOW_REFRESH_DETACHED_ENTITY );
		if ( !allowRefreshDetachedEntity ) {
			( (SessionFactoryBuilderImplementor) sfBuilder ).disableRefreshDetachedEntity();
		}

		// Locate and apply any requested SessionFactoryObserver
		final Object sessionFactoryObserverSetting = configurationValues.remove( AvailableSettings.SESSION_FACTORY_OBSERVER );
		if ( sessionFactoryObserverSetting != null ) {
			final SessionFactoryObserver suppliedSessionFactoryObserver =
					strategySelector.resolveStrategy( SessionFactoryObserver.class, sessionFactoryObserverSetting );
			sfBuilder.addSessionFactoryObservers( suppliedSessionFactoryObserver );
		}

		sfBuilder.addSessionFactoryObservers( ServiceRegistryCloser.INSTANCE );

		sfBuilder.applyEntityNotFoundDelegate( JpaEntityNotFoundDelegate.INSTANCE );

		if ( this.validatorFactory != null ) {
			sfBuilder.applyValidatorFactory( validatorFactory );
		}
		if ( this.cdiBeanManager != null ) {
			sfBuilder.applyBeanManager( cdiBeanManager );
		}
	}


	private static class ServiceRegistryCloser implements SessionFactoryObserver {
		/**
		 * Singleton access
		 */
		public static final ServiceRegistryCloser INSTANCE = new ServiceRegistryCloser();

		@Override
		public void sessionFactoryCreated(SessionFactory sessionFactory) {
			// nothing to do
		}

		@Override
		public void sessionFactoryClosed(SessionFactory sessionFactory) {
			SessionFactoryImplementor sfi = ( (SessionFactoryImplementor) sessionFactory );
			sfi.getServiceRegistry().destroy();
			ServiceRegistry basicRegistry = sfi.getServiceRegistry().getParentServiceRegistry();
			( (ServiceRegistryImplementor) basicRegistry ).destroy();
		}
	}

	private PersistenceException persistenceException(String message) {
		return persistenceException( message, null );
	}

	protected PersistenceException persistenceException(String message, Exception cause) {
		return new PersistenceException(
				getExceptionHeader() + message,
				cause
		);
	}

	private String getExceptionHeader() {
		return "[PersistenceUnit: " + persistenceUnit.getName() + "] ";
	}

	private static class MergedSettings {
		private final Map configurationValues = new ConcurrentHashMap( 16, 0.75f, 1 );

		private Map<String, JaccPermissionDeclarations> jaccPermissionsByContextId;
		private List<CacheRegionDefinition> cacheRegionDefinitions;

		/**
		 * 	MergedSettings is initialized with hibernate.properties
		 */
		private MergedSettings() {
			configurationValues.putAll( Environment.getProperties() );
		}

		public void processPersistenceUnitDescriptorProperties(PersistenceUnitDescriptor persistenceUnit) {
			if ( persistenceUnit.getProperties() != null ) {
				configurationValues.putAll( persistenceUnit.getProperties() );
			}

			configurationValues.put( PERSISTENCE_UNIT_NAME, persistenceUnit.getName() );

		}

		public void processHibernateConfigXmlResources(LoadedConfig loadedConfig){
			if ( ! configurationValues.containsKey( SESSION_FACTORY_NAME ) ) {
				// there is not already a SF-name in the merged settings
				final String sfName = loadedConfig.getSessionFactoryName();
				if ( sfName != null ) {
					// but the cfg.xml file we are processing named one..
					configurationValues.put( SESSION_FACTORY_NAME, sfName );
				}
			}
			else {
				// make sure they match?
			}

			configurationValues.putAll( loadedConfig.getConfigurationValues() );
		}

		public Map getConfigurationValues() {
			return configurationValues;
		}

		private JaccPermissionDeclarations getJaccPermissions(String jaccContextId) {
			if ( jaccPermissionsByContextId == null ) {
				jaccPermissionsByContextId = new HashMap<>();
			}

			JaccPermissionDeclarations jaccPermissions = jaccPermissionsByContextId.get( jaccContextId );
			if ( jaccPermissions == null ) {
				jaccPermissions = new JaccPermissionDeclarations( jaccContextId );
				jaccPermissionsByContextId.put( jaccContextId, jaccPermissions );
			}
			return jaccPermissions;
		}

		private void addCacheRegionDefinition(CacheRegionDefinition cacheRegionDefinition) {
			if ( this.cacheRegionDefinitions == null ) {
				this.cacheRegionDefinitions = new ArrayList<>();
			}
			this.cacheRegionDefinitions.add( cacheRegionDefinition );
		}
	}

	private <T> T loadSettingInstance(String settingName, Object settingValue, Class<T> clazz) {
		T instance = null;
		Class<? extends T> instanceClass = null;

		if ( clazz.isAssignableFrom( settingValue.getClass() ) ) {
			instance = (T) settingValue;
		}
		else if ( settingValue instanceof Class ) {
			instanceClass = (Class<? extends T>) settingValue;
		}
		else if ( settingValue instanceof String ) {
			String settingStringValue = (String) settingValue;
			if ( standardServiceRegistry != null ) {
				final ClassLoaderService classLoaderService = standardServiceRegistry.getService( ClassLoaderService.class );

				instanceClass = classLoaderService.classForName( settingStringValue );
			}
			else {
				try {
					instanceClass = (Class<? extends T>) Class.forName( settingStringValue );
				}
				catch (ClassNotFoundException e) {
					throw new IllegalArgumentException( "Can't load class: " + settingStringValue, e );
				}
			}
		}
		else {
			throw new IllegalArgumentException(
				"The provided " + settingName + " setting value [" + settingValue + "] is not supported!"
			);
		}

		if ( instanceClass != null ) {
			try {
				instance = instanceClass.newInstance();
			}
			catch (InstantiationException | IllegalAccessException e) {
				throw new IllegalArgumentException(
						"The " + clazz.getSimpleName() +" class [" + instanceClass + "] could not be instantiated!",
						e
				);
			}
		}

		return instance;
	}

	/**
	 * Exposed to extensions: see Hibernate Reactive
	 * @return
	 */
	protected StandardServiceRegistry getStandardServiceRegistry() {
		return standardServiceRegistry;
	}
}
