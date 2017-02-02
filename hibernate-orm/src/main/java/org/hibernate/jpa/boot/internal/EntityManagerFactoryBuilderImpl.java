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
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.archive.scan.internal.StandardScanOptions;
import org.hibernate.boot.cfgxml.internal.ConfigLoader;
import org.hibernate.boot.cfgxml.spi.CfgXmlAccessService;
import org.hibernate.boot.cfgxml.spi.LoadedConfig;
import org.hibernate.boot.cfgxml.spi.MappingReference;
import org.hibernate.boot.model.TypeContributor;
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
import org.hibernate.internal.util.StringHelper;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.jpa.boot.spi.StrategyRegistrationProviderList;
import org.hibernate.jpa.boot.spi.TypeContributorList;
import org.hibernate.jpa.event.spi.JpaIntegrator;
import org.hibernate.jpa.internal.util.LogHelper;
import org.hibernate.jpa.internal.util.PersistenceUnitTransactionTypeHelper;
import org.hibernate.jpa.spi.IdentifierGeneratorStrategyProvider;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorBuilderImpl;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorBuilderImpl;
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
			integrationSettings = Collections.emptyMap();
		}

		// Build the boot-strap service registry, which mainly handles class loader interactions
		final BootstrapServiceRegistry bsr = buildBootstrapServiceRegistry( integrationSettings, providedClassLoader, providedClassLoaderService);

		// merge configuration sources and build the "standard" service registry
		final StandardServiceRegistryBuilder ssrBuilder = new StandardServiceRegistryBuilder( bsr );
		final MergedSettings mergedSettings = mergeSettings( persistenceUnit, integrationSettings, ssrBuilder );
		this.configurationValues = mergedSettings.getConfigurationValues();

		// Build the "standard" service registry
		ssrBuilder.applySettings( configurationValues );
		configure( ssrBuilder );
		this.standardServiceRegistry = ssrBuilder.build();
		configure( standardServiceRegistry, mergedSettings );

		final MetadataSources metadataSources = new MetadataSources( bsr );
		List<AttributeConverterDefinition> attributeConverterDefinitions = populate(
				metadataSources,
				mergedSettings,
				standardServiceRegistry
		);
		this.metamodelBuilder = (MetadataBuilderImplementor) metadataSources.getMetadataBuilder( standardServiceRegistry );
		populate( metamodelBuilder, mergedSettings, standardServiceRegistry, attributeConverterDefinitions );

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
				metamodelBuilder.getMetadataBuildingOptions()
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

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// temporary!
	@SuppressWarnings("unchecked")
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
	protected EnhancementContext getEnhancementContext(final boolean dirtyTrackingEnabled, final boolean lazyInitializationEnabled, final boolean associationManagementEnabled ) {
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

		bsrBuilder.applyIntegrator( new JpaIntegrator() );

		final IntegratorProvider integratorProvider = (IntegratorProvider) integrationSettings.get( INTEGRATOR_PROVIDER );
		if ( integratorProvider != null ) {
			for ( Integrator integrator : integratorProvider.getIntegrators() ) {
				bsrBuilder.applyIntegrator( integrator );
			}
		}
		
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

	@SuppressWarnings("unchecked")
	private MergedSettings mergeSettings(
			PersistenceUnitDescriptor persistenceUnit,
			Map<?,?> integrationSettings,
			StandardServiceRegistryBuilder ssrBuilder) {
		final MergedSettings mergedSettings = new MergedSettings();

		// first, apply persistence.xml-defined settings
		if ( persistenceUnit.getProperties() != null ) {
			mergedSettings.configurationValues.putAll( persistenceUnit.getProperties() );
		}

		mergedSettings.configurationValues.put( PERSISTENCE_UNIT_NAME, persistenceUnit.getName() );

		final ConfigLoader configLoader = new ConfigLoader( ssrBuilder.getBootstrapServiceRegistry() );

		// see if the persistence.xml settings named a Hibernate config file....
		final String cfgXmlResourceName1 = (String) mergedSettings.configurationValues.remove( CFG_FILE );
		if ( StringHelper.isNotEmpty( cfgXmlResourceName1 ) ) {
			final LoadedConfig loadedCfg = configLoader.loadConfigXmlResource( cfgXmlResourceName1 );
			processConfigXml( loadedCfg, mergedSettings, ssrBuilder );
		}

		// see if integration settings named a Hibernate config file....
		final String cfgXmlResourceName2 = (String) integrationSettings.get( CFG_FILE );
		if ( StringHelper.isNotEmpty( cfgXmlResourceName2 ) ) {
			integrationSettings.remove( CFG_FILE );
			final LoadedConfig loadedCfg = configLoader.loadConfigXmlResource( cfgXmlResourceName2 );
			processConfigXml( loadedCfg, mergedSettings, ssrBuilder );
		}

		// finally, apply integration-supplied settings (per JPA spec, integration settings should override other sources)
		for ( Map.Entry<?,?> entry : integrationSettings.entrySet() ) {
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

		if ( !mergedSettings.configurationValues.containsKey( JPA_VALIDATION_MODE ) ) {
			if ( persistenceUnit.getValidationMode() != null ) {
				mergedSettings.configurationValues.put( JPA_VALIDATION_MODE, persistenceUnit.getValidationMode() );
			}
		}

		if ( !mergedSettings.configurationValues.containsKey( JPA_SHARED_CACHE_MODE ) ) {
			if ( persistenceUnit.getSharedCacheMode() != null ) {
				mergedSettings.configurationValues.put( JPA_SHARED_CACHE_MODE, persistenceUnit.getSharedCacheMode() );
			}
		}

		final String jaccContextId = (String) mergedSettings.configurationValues.get( JACC_CONTEXT_ID );

		// here we are going to iterate the merged config settings looking for:
		//		1) additional JACC permissions
		//		2) additional cache region declarations
		//
		// we will also clean up an references with null entries
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

	@SuppressWarnings("unchecked")
	private void processConfigXml(
			LoadedConfig loadedConfig,
			MergedSettings mergedSettings,
			StandardServiceRegistryBuilder ssrBuilder) {
		if ( ! mergedSettings.configurationValues.containsKey( SESSION_FACTORY_NAME ) ) {
			// there is not already a SF-name in the merged settings
			final String sfName = loadedConfig.getSessionFactoryName();
			if ( sfName != null ) {
				// but the cfg.xml file we are processing named one..
				mergedSettings.configurationValues.put( SESSION_FACTORY_NAME, sfName );
			}
		}

		mergedSettings.configurationValues.putAll( loadedConfig.getConfigurationValues() );
		ssrBuilder.configure( loadedConfig );
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

	private void configure(StandardServiceRegistryBuilder ssrBuilder) {

		applyJdbcConnectionProperties( ssrBuilder );
		applyTransactionProperties( ssrBuilder );

		// flush beforeQuery completion validation
		if ( "true".equals( configurationValues.get( Environment.FLUSH_BEFORE_COMPLETION ) ) ) {
			ssrBuilder.applySetting( Environment.FLUSH_BEFORE_COMPLETION, "false" );
			LOG.definingFlushBeforeCompletionIgnoredInHem( Environment.FLUSH_BEFORE_COMPLETION );
		}

//		final StrategySelector strategySelector = ssrBuilder.getBootstrapServiceRegistry().getService( StrategySelector.class );
//		final Object interceptorSetting = configurationValues.remove( AvailableSettings.SESSION_INTERCEPTOR );
//		if ( interceptorSetting != null ) {
//			settings.setSessionInterceptorClass(
//					loadSessionInterceptorClass( interceptorSetting, strategySelector )
//			);
//		}
	}

	private void applyJdbcConnectionProperties(StandardServiceRegistryBuilder ssrBuilder) {
		if ( dataSource != null ) {
			ssrBuilder.applySetting( DATASOURCE, dataSource );
		}
		else if ( persistenceUnit.getJtaDataSource() != null ) {
			if ( ! ssrBuilder.getSettings().containsKey( DATASOURCE ) ) {
				ssrBuilder.applySetting( DATASOURCE, persistenceUnit.getJtaDataSource() );
				// HHH-8121 : make the PU-defined value available to EMF.getProperties()
				configurationValues.put( JPA_JTA_DATASOURCE, persistenceUnit.getJtaDataSource() );
			}
		}
		else if ( persistenceUnit.getNonJtaDataSource() != null ) {
			if ( ! ssrBuilder.getSettings().containsKey( DATASOURCE ) ) {
				ssrBuilder.applySetting( DATASOURCE, persistenceUnit.getNonJtaDataSource() );
				// HHH-8121 : make the PU-defined value available to EMF.getProperties()
				configurationValues.put( JPA_NON_JTA_DATASOURCE, persistenceUnit.getNonJtaDataSource() );
			}
		}
		else {
			final String driver = (String) configurationValues.get( JPA_JDBC_DRIVER );
			if ( StringHelper.isNotEmpty( driver ) ) {
				ssrBuilder.applySetting( DRIVER, driver );
			}
			final String url = (String) configurationValues.get( JPA_JDBC_URL );
			if ( StringHelper.isNotEmpty( url ) ) {
				ssrBuilder.applySetting( URL, url );
			}
			final String user = (String) configurationValues.get( JPA_JDBC_USER );
			if ( StringHelper.isNotEmpty( user ) ) {
				ssrBuilder.applySetting( USER, user );
			}
			final String pass = (String) configurationValues.get( JPA_JDBC_PASSWORD );
			if ( StringHelper.isNotEmpty( pass ) ) {
				ssrBuilder.applySetting( PASS, pass );
			}
		}
	}

	private void applyTransactionProperties(StandardServiceRegistryBuilder ssrBuilder) {
		PersistenceUnitTransactionType txnType = PersistenceUnitTransactionTypeHelper.interpretTransactionType(
				configurationValues.get( JPA_TRANSACTION_TYPE )
		);
		if ( txnType == null ) {
			txnType = persistenceUnit.getTransactionType();
		}
		if ( txnType == null ) {
			// is it more appropriate to have this be based on bootstrap entry point (EE vs SE)?
			txnType = PersistenceUnitTransactionType.RESOURCE_LOCAL;
		}
		boolean hasTxStrategy = configurationValues.containsKey( TRANSACTION_COORDINATOR_STRATEGY );
		if ( hasTxStrategy ) {
			LOG.overridingTransactionStrategyDangerous( TRANSACTION_COORDINATOR_STRATEGY );
		}
		else {
			if ( txnType == PersistenceUnitTransactionType.JTA ) {
				ssrBuilder.applySetting( TRANSACTION_COORDINATOR_STRATEGY, JtaTransactionCoordinatorBuilderImpl.class );
			}
			else if ( txnType == PersistenceUnitTransactionType.RESOURCE_LOCAL ) {
				ssrBuilder.applySetting( TRANSACTION_COORDINATOR_STRATEGY, JdbcResourceLocalTransactionCoordinatorBuilderImpl.class );
			}
		}
	}

	private void configure(StandardServiceRegistry ssr, MergedSettings mergedSettings) {
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
	protected List<AttributeConverterDefinition> populate(
			MetadataSources metadataSources,
			MergedSettings mergedSettings,
			StandardServiceRegistry ssr) {
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
			for ( String ormXml : explicitOrmXmlList ) {
				metadataSources.addResource( ormXml );
			}
		}

		return attributeConverterDefinitions;
	}

	protected void populate(
			MetadataBuilder metamodelBuilder,
			MergedSettings mergedSettings,
			StandardServiceRegistry ssr,
			List<AttributeConverterDefinition> attributeConverterDefinitions) {
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
			for ( CacheRegionDefinition localCacheRegionDefinition : mergedSettings.cacheRegionDefinitions ) {
				metamodelBuilder.applyCacheRegionDefinition( localCacheRegionDefinition );
			}
		}

		final TypeContributorList typeContributorList = (TypeContributorList) configurationValues.remove(
				TYPE_CONTRIBUTORS
		);
		if ( typeContributorList != null ) {
			for ( TypeContributor typeContributor : typeContributorList.getTypeContributors() ) {
				metamodelBuilder.applyTypes( typeContributor );
			}
		}

		if ( attributeConverterDefinitions != null ) {
			for ( AttributeConverterDefinition attributeConverterDefinition : attributeConverterDefinitions ) {
				metamodelBuilder.applyAttributeConverter( attributeConverterDefinition );
			}
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

	private MetadataImplementor metadata() {
		if ( this.metadata == null ) {
			this.metadata = MetadataBuildingProcess.complete( managedResources, metamodelBuilder.getMetadataBuildingOptions() );
		}
		return metadata;
	}

	@Override
	public void generateSchema() {
		// This seems overkill, but building the SF is necessary to get the Integrators to kick in.
		// Metamodel will clean this up...
		try {
			SessionFactoryBuilder sfBuilder = metadata().getSessionFactoryBuilder();
			populate( sfBuilder, standardServiceRegistry );

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

	@SuppressWarnings("unchecked")
	public EntityManagerFactory build() {
		SessionFactoryBuilder sfBuilder = metadata().getSessionFactoryBuilder();
		populate( sfBuilder, standardServiceRegistry );

		try {
			return sfBuilder.build();
		}
		catch (Exception e) {
			throw persistenceException( "Unable to build Hibernate SessionFactory", e );
		}
	}

	protected void populate(SessionFactoryBuilder sfBuilder, StandardServiceRegistry ssr) {
		( ( SessionFactoryBuilderImplementor) sfBuilder ).markAsJpaBootstrap();

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

	private PersistenceException persistenceException(String message, Exception cause) {
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

		private MergedSettings() {
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
}
