/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpa.boot.internal;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import javax.persistence.AttributeConverter;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import org.hibernate.Interceptor;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.selector.StrategyRegistrationProvider;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.boot.spi.CacheRegionDefinition;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.cfg.beanvalidation.BeanValidationIntegrator;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.transaction.internal.jdbc.JdbcTransactionFactory;
import org.hibernate.engine.transaction.internal.jta.CMTTransactionFactory;
import org.hibernate.id.factory.spi.MutableIdentifierGeneratorFactory;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.ValueHolder;
import org.hibernate.jaxb.spi.cfg.JaxbHibernateConfiguration;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.jpa.boot.spi.StrategyRegistrationProviderList;
import org.hibernate.jpa.boot.spi.TypeContributorList;
import org.hibernate.jpa.event.spi.JpaIntegrator;
import org.hibernate.jpa.internal.EntityManagerFactoryImpl;
import org.hibernate.jpa.internal.EntityManagerMessageLogger;
import org.hibernate.jpa.internal.schemagen.JpaSchemaGenerator;
import org.hibernate.jpa.internal.util.LogHelper;
import org.hibernate.jpa.internal.util.PersistenceUnitTransactionTypeHelper;
import org.hibernate.jpa.spi.IdentifierGeneratorStrategyProvider;
import org.hibernate.metamodel.MetadataBuilder;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.SessionFactoryBuilder;
import org.hibernate.metamodel.archive.scan.internal.StandardScanOptions;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.metamodel.spi.TypeContributor;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.secure.spi.GrantedPermission;
import org.hibernate.secure.spi.JaccPermissionDeclarations;
import org.hibernate.secure.spi.JaccService;
import org.hibernate.service.ConfigLoader;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import org.jboss.logging.Logger;

import static org.hibernate.cfg.AvailableSettings.JACC_CONTEXT_ID;
import static org.hibernate.cfg.AvailableSettings.JACC_PREFIX;
import static org.hibernate.cfg.AvailableSettings.SESSION_FACTORY_NAME;
import static org.hibernate.jaxb.spi.cfg.JaxbHibernateConfiguration.JaxbSecurity.JaxbGrant;
import static org.hibernate.jaxb.spi.cfg.JaxbHibernateConfiguration.JaxbSessionFactory.JaxbClassCache;
import static org.hibernate.jaxb.spi.cfg.JaxbHibernateConfiguration.JaxbSessionFactory.JaxbCollectionCache;
import static org.hibernate.jaxb.spi.cfg.JaxbHibernateConfiguration.JaxbSessionFactory.JaxbMapping;
import static org.hibernate.jpa.AvailableSettings.CFG_FILE;
import static org.hibernate.jpa.AvailableSettings.CLASS_CACHE_PREFIX;
import static org.hibernate.jpa.AvailableSettings.COLLECTION_CACHE_PREFIX;
import static org.hibernate.jpa.AvailableSettings.DISCARD_PC_ON_CLOSE;
import static org.hibernate.jpa.AvailableSettings.PERSISTENCE_UNIT_NAME;
import static org.hibernate.jpa.AvailableSettings.SHARED_CACHE_MODE;
import static org.hibernate.jpa.AvailableSettings.VALIDATION_MODE;

/**
 * @author Steve Ebersole
 */
public class EntityManagerFactoryBuilderImpl implements EntityManagerFactoryBuilder {
    private static final EntityManagerMessageLogger LOG = Logger.getMessageLogger(
			EntityManagerMessageLogger.class,
			EntityManagerFactoryBuilderImpl.class.getName()
	);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// New settings
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
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
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Explicit "injectables"
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private Object validatorFactory;
	private Object cdiBeanManager;
	private DataSource dataSource;
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private final PersistenceUnitDescriptor persistenceUnit;

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// things built in first phase, needed for second phase..
	private final Map configurationValues;
	private final StandardServiceRegistry standardServiceRegistry;
	private final MetadataImplementor metadata;
	private final SettingsImpl settings;


	public EntityManagerFactoryBuilderImpl(PersistenceUnitDescriptor persistenceUnit, Map integrationSettings) {
		this( persistenceUnit, integrationSettings, null );
	}

	public EntityManagerFactoryBuilderImpl(
			PersistenceUnitDescriptor persistenceUnit,
			Map integrationSettings,
			ClassLoader providedClassLoader ) {
		LogHelper.logPersistenceUnitInformation( persistenceUnit );
		this.persistenceUnit = persistenceUnit;

		if ( integrationSettings == null ) {
			integrationSettings = new HashMap();
		}

		try {
			// Build the boot-strap service registry, which mainly handles class loader interactions
			final BootstrapServiceRegistry bsr = buildBootstrapServiceRegistry( integrationSettings, providedClassLoader );

			// merge configuration sources
			final MergedSettings mergedSettings = mergeSettings( persistenceUnit, integrationSettings, bsr );
			this.configurationValues = mergedSettings.getConfigurationValues();

			// Build the "standard" service registry
			final StandardServiceRegistryBuilder ssrBuilder = new StandardServiceRegistryBuilder( bsr );
			ssrBuilder.applySettings( configurationValues );
			this.settings = configure( ssrBuilder );
			this.standardServiceRegistry = ssrBuilder.build();
			configure( standardServiceRegistry, mergedSettings );

			// Build the Metadata object
			final MetadataSources metadataSources = new MetadataSources( bsr );
			populate( metadataSources, mergedSettings, standardServiceRegistry );
			final MetadataBuilder metamodelBuilder = metadataSources.getMetadataBuilder( standardServiceRegistry );
			populate( metamodelBuilder, mergedSettings, standardServiceRegistry );
			this.metadata = (MetadataImplementor) metamodelBuilder.build();

			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// push back class transformation to the environment; for the time being this only has any effect in EE
			// container situations, calling back into PersistenceUnitInfo#addClassTransformer
			final boolean useClassTransformer = "true".equals( configurationValues.remove( AvailableSettings.USE_CLASS_ENHANCER ) );
			if ( useClassTransformer ) {
				persistenceUnit.pushClassTransformer( collectNamesOfClassesToEnhance( metadata ) );
			}
		}
		catch (PersistenceException pe) {
			throw pe;
		}
		catch (Exception e) {
			throw persistenceException( "Unable to build Hibernate SessionFactory", e );
		}
	}

	/**
	 * Gets the metadata.
	 * @return the metadata.
	 * @deprecated This should only be needed for testing and should ultimately be removed.
	 */
	public MetadataImplementor getMetadata() {
		return metadata;
	}

	private SettingsImpl configure(StandardServiceRegistryBuilder ssrBuilder) {
		final SettingsImpl settings = new SettingsImpl();

		applyJdbcConnectionProperties( ssrBuilder );
		applyTransactionProperties( ssrBuilder, settings );

		// flush before completion validation
		if ( "true".equals( configurationValues.get( Environment.FLUSH_BEFORE_COMPLETION ) ) ) {
			ssrBuilder.applySetting( Environment.FLUSH_BEFORE_COMPLETION, "false" );
			LOG.definingFlushBeforeCompletionIgnoredInHem( Environment.FLUSH_BEFORE_COMPLETION );
		}

		final Object value = configurationValues.get( DISCARD_PC_ON_CLOSE );
		if ( value != null ) {
			settings.setReleaseResourcesOnCloseEnabled( "true".equals( value ) );
		}

		final StrategySelector strategySelector = ssrBuilder.getBootstrapServiceRegistry().getService( StrategySelector.class );
		final Object interceptorSetting = configurationValues.remove( AvailableSettings.SESSION_INTERCEPTOR );
		if ( interceptorSetting != null ) {
			settings.setSessionInterceptorClass(
					loadSessionInterceptorClass( interceptorSetting, strategySelector )
			);
		}

		return settings;
	}

	private void applyJdbcConnectionProperties(StandardServiceRegistryBuilder ssrBuilder) {
		if ( dataSource != null ) {
			ssrBuilder.applySetting( org.hibernate.cfg.AvailableSettings.DATASOURCE, dataSource );
		}
		else if ( persistenceUnit.getJtaDataSource() != null ) {
			if ( ! ssrBuilder.getSettings().containsKey( org.hibernate.cfg.AvailableSettings.DATASOURCE ) ) {
				ssrBuilder.applySetting( org.hibernate.cfg.AvailableSettings.DATASOURCE, persistenceUnit.getJtaDataSource() );
				// HHH-8121 : make the PU-defined value available to EMF.getProperties()
				configurationValues.put( AvailableSettings.JTA_DATASOURCE, persistenceUnit.getJtaDataSource() );
			}
		}
		else if ( persistenceUnit.getNonJtaDataSource() != null ) {
			if ( ! ssrBuilder.getSettings().containsKey( org.hibernate.cfg.AvailableSettings.DATASOURCE ) ) {
				ssrBuilder.applySetting( org.hibernate.cfg.AvailableSettings.DATASOURCE, persistenceUnit.getNonJtaDataSource() );
				// HHH-8121 : make the PU-defined value available to EMF.getProperties()
				configurationValues.put( AvailableSettings.NON_JTA_DATASOURCE, persistenceUnit.getNonJtaDataSource() );
			}
		}
		else {
			final String driver = (String) configurationValues.get( AvailableSettings.JDBC_DRIVER );
			if ( StringHelper.isNotEmpty( driver ) ) {
				ssrBuilder.applySetting( org.hibernate.cfg.AvailableSettings.DRIVER, driver );
			}
			final String url = (String) configurationValues.get( AvailableSettings.JDBC_URL );
			if ( StringHelper.isNotEmpty( url ) ) {
				ssrBuilder.applySetting( org.hibernate.cfg.AvailableSettings.URL, url );
			}
			final String user = (String) configurationValues.get( AvailableSettings.JDBC_USER );
			if ( StringHelper.isNotEmpty( user ) ) {
				ssrBuilder.applySetting( org.hibernate.cfg.AvailableSettings.USER, user );
			}
			final String pass = (String) configurationValues.get( AvailableSettings.JDBC_PASSWORD );
			if ( StringHelper.isNotEmpty( pass ) ) {
				ssrBuilder.applySetting( org.hibernate.cfg.AvailableSettings.PASS, pass );
			}
		}
	}

	private void applyTransactionProperties(StandardServiceRegistryBuilder ssrBuilder, SettingsImpl settings) {
		PersistenceUnitTransactionType txnType = PersistenceUnitTransactionTypeHelper.interpretTransactionType(
				configurationValues.get( AvailableSettings.TRANSACTION_TYPE )
		);
		if ( txnType == null ) {
			txnType = persistenceUnit.getTransactionType();
		}
		if ( txnType == null ) {
			// is it more appropriate to have this be based on bootstrap entry point (EE vs SE)?
			txnType = PersistenceUnitTransactionType.RESOURCE_LOCAL;
		}
		settings.setTransactionType( txnType );
		boolean hasTxStrategy = configurationValues.containsKey( Environment.TRANSACTION_STRATEGY );
		if ( hasTxStrategy ) {
			LOG.overridingTransactionStrategyDangerous( Environment.TRANSACTION_STRATEGY );
		}
		else {
			if ( txnType == PersistenceUnitTransactionType.JTA ) {
				ssrBuilder.applySetting( Environment.TRANSACTION_STRATEGY, CMTTransactionFactory.class );
			}
			else if ( txnType == PersistenceUnitTransactionType.RESOURCE_LOCAL ) {
				ssrBuilder.applySetting( Environment.TRANSACTION_STRATEGY, JdbcTransactionFactory.class );
			}
		}
	}

	@SuppressWarnings("unchecked")
	private Class<? extends Interceptor> loadSessionInterceptorClass(Object value, StrategySelector strategySelector) {
		if ( value == null ) {
			return null;
		}

		return Class.class.isInstance( value )
				? (Class<? extends Interceptor>) value
				: strategySelector.selectStrategyImplementor( Interceptor.class, value.toString() );
	}

	private List<String> collectNamesOfClassesToEnhance(MetadataImplementor metadata) {
		final List<String> entityClassNames = new ArrayList<String>();
		for ( EntityBinding eb : metadata.getEntityBindings() ) {
			entityClassNames.add( eb.getEntity().getDescriptor().getName().toString() );
		}
		return entityClassNames;
	}

	/**
	 * Builds the {@link BootstrapServiceRegistry} used to eventually build the {@link org.hibernate.boot.registry.StandardServiceRegistryBuilder}; mainly
	 * used here during instantiation to define class-loading behavior.
	 *
	 *
	 * @param integrationSettings Any integration settings passed by the EE container or SE application
	 * @param providedClassLoader Typically (if non-null) a osgi bundle classloader.
	 *
	 * @return The built BootstrapServiceRegistry
	 */
	private BootstrapServiceRegistry buildBootstrapServiceRegistry(
			Map integrationSettings,
			ClassLoader providedClassLoader) {
		final BootstrapServiceRegistryBuilder bsrBuilder = new BootstrapServiceRegistryBuilder();
		bsrBuilder.with( new JpaIntegrator() );

		final IntegratorProvider integratorProvider = (IntegratorProvider) integrationSettings.get( INTEGRATOR_PROVIDER );
		if ( integratorProvider != null ) {
			for ( Integrator integrator : integratorProvider.getIntegrators() ) {
				bsrBuilder.with( integrator );
			}
		}

		final StrategyRegistrationProviderList strategyRegistrationProviderList
				= (StrategyRegistrationProviderList) integrationSettings.get( STRATEGY_REGISTRATION_PROVIDERS );
		if ( strategyRegistrationProviderList != null ) {
			for ( StrategyRegistrationProvider strategyRegistrationProvider : strategyRegistrationProviderList
					.getStrategyRegistrationProviders() ) {
				bsrBuilder.withStrategySelectors( strategyRegistrationProvider );
			}
		}


		// ClassLoaders ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		if ( persistenceUnit.getClassLoader() != null ) {
			bsrBuilder.with( persistenceUnit.getClassLoader() );
		}

		if ( providedClassLoader != null ) {
			bsrBuilder.with( providedClassLoader );
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
					bsrBuilder.with( classLoader );
				}
			}
			else if ( classLoadersSetting.getClass().isArray() ) {
				for ( ClassLoader classLoader : (ClassLoader[]) classLoadersSetting ) {
					bsrBuilder.with( classLoader );
				}
			}
			else if ( ClassLoader.class.isInstance( classLoadersSetting ) ) {
				bsrBuilder.with( (ClassLoader) classLoadersSetting );
			}
		}

		return bsrBuilder.build();
	}

	@SuppressWarnings("unchecked")
	private MergedSettings mergeSettings(
			PersistenceUnitDescriptor persistenceUnit,
			Map<?,?> integrationSettings,
			final BootstrapServiceRegistry bsr) {
		final MergedSettings mergedSettings = new MergedSettings(
				determineJaccContext( integrationSettings, persistenceUnit.getProperties() )
		);

		// first, apply persistence.xml-defined settings
		if ( persistenceUnit.getProperties() != null ) {
			mergedSettings.configurationValues.putAll( persistenceUnit.getProperties() );
		}

		mergedSettings.configurationValues.put( PERSISTENCE_UNIT_NAME, persistenceUnit.getName() );

		final ValueHolder<ConfigLoader> configLoaderHolder = new ValueHolder<ConfigLoader>(
				new ValueHolder.DeferredInitializer<ConfigLoader>() {
					@Override
					public ConfigLoader initialize() {
						return new ConfigLoader( bsr );
					}
				}
		);

		// see if the persistence.xml settings named a Hibernate config file....
		final String cfgXmlResourceName1 = (String) mergedSettings.configurationValues.remove( CFG_FILE );
		if ( StringHelper.isNotEmpty( cfgXmlResourceName1 ) ) {
			processConfigXml( configLoaderHolder.getValue(), cfgXmlResourceName1, mergedSettings );
		}

		// see if integration settings named a Hibernate config file....
		final String cfgXmlResourceName2 = (String) integrationSettings.get( CFG_FILE );
		if ( StringHelper.isNotEmpty( cfgXmlResourceName2 ) ) {
			integrationSettings.remove( CFG_FILE );
			processConfigXml( configLoaderHolder.getValue(), cfgXmlResourceName2, mergedSettings );
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

		if ( !mergedSettings.configurationValues.containsKey( VALIDATION_MODE ) ) {
			if ( persistenceUnit.getValidationMode() != null ) {
				mergedSettings.configurationValues.put( VALIDATION_MODE, persistenceUnit.getValidationMode() );
			}
		}

		if ( !mergedSettings.configurationValues.containsKey( SHARED_CACHE_MODE ) ) {
			if ( persistenceUnit.getSharedCacheMode() != null ) {
				mergedSettings.configurationValues.put( SHARED_CACHE_MODE, persistenceUnit.getSharedCacheMode() );
			}
		}

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
					mergedSettings.getJaccPermissions().addPermissionDeclaration(
							parseJaccConfigEntry( keyString, valueString )
					);
				}
				else if ( keyString.startsWith( CLASS_CACHE_PREFIX ) ) {
					mergedSettings.add(
							parseCacheRegionDefinitionEntry(
									keyString.substring( CLASS_CACHE_PREFIX.length() + 1 ),
									valueString,
									CacheRegionDefinition.CacheRegionType.ENTITY
							)
					);
				}
				else if ( keyString.startsWith( COLLECTION_CACHE_PREFIX ) ) {
					mergedSettings.add(
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

	private JaccPermissionDeclarations determineJaccContext(Map integrationSettings, Properties properties) {
		String jaccContextId = (String) integrationSettings.get( JACC_CONTEXT_ID );
		if ( jaccContextId == null && properties != null ) {
			jaccContextId = properties.getProperty( JACC_CONTEXT_ID );
		}

 		return new JaccPermissionDeclarations( jaccContextId );
	}

	@SuppressWarnings("unchecked")
	private void processConfigXml(ConfigLoader configLoader, String cfgXmlResourceName, MergedSettings mergedSettings) {
		JaxbHibernateConfiguration configurationElement = configLoader.loadConfigXmlResource( cfgXmlResourceName );

		if ( ! mergedSettings.configurationValues.containsKey( SESSION_FACTORY_NAME ) ) {
			// there is not already a SF-name in the merged settings
			final String sfName = configurationElement.getSessionFactory().getName();
			if ( sfName != null ) {
				// but the cfg.xml file we are processing named one..
				mergedSettings.configurationValues.put( SESSION_FACTORY_NAME, sfName );
			}
		}

		for ( JaxbHibernateConfiguration.JaxbSessionFactory.JaxbProperty jaxbProperty : configurationElement.getSessionFactory().getProperty() ) {
			mergedSettings.configurationValues.put( jaxbProperty.getName(), jaxbProperty.getValue() );
		}

		for ( JaxbMapping jaxbMapping : configurationElement.getSessionFactory().getMapping() ) {
			mergedSettings.add( jaxbMapping );
		}

		for ( Object cacheDeclaration : configurationElement.getSessionFactory().getClassCacheOrCollectionCache() ) {
			if ( JaxbClassCache.class.isInstance( cacheDeclaration ) ) {
				final JaxbClassCache jaxbClassCache = (JaxbClassCache) cacheDeclaration;
				mergedSettings.add(
						new CacheRegionDefinition(
								CacheRegionDefinition.CacheRegionType.ENTITY,
								jaxbClassCache.getClazz(),
								jaxbClassCache.getUsage().value(),
								jaxbClassCache.getRegion(),
								"all".equals( jaxbClassCache.getInclude() )
						)
				);
			}
			else {
				final JaxbCollectionCache jaxbCollectionCache = (JaxbCollectionCache) cacheDeclaration;
				mergedSettings.add(
						new CacheRegionDefinition(
								CacheRegionDefinition.CacheRegionType.COLLECTION,
								jaxbCollectionCache.getCollection(),
								jaxbCollectionCache.getUsage().value(),
								jaxbCollectionCache.getRegion(),
								false
						)
				);
			}
		}

		if ( configurationElement.getSecurity() != null ) {
			for ( JaxbGrant grant : configurationElement.getSecurity().getGrant() ) {
				mergedSettings.getJaccPermissions().addPermissionDeclaration(
						new GrantedPermission(
								grant.getRole(),
								grant.getEntityName(),
								grant.getActions()
						)
				);
			}
		}
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

	private void configure(StandardServiceRegistry ssr, MergedSettings mergedSettings) {
		final StrategySelector strategySelector = ssr.getService( StrategySelector.class );

		// apply any JACC permissions
		final JaccService jaccService = ssr.getService( JaccService.class );
		if ( jaccService == null ) {
			// JACC not enabled
			if ( !mergedSettings.getJaccPermissions().getPermissionDeclarations().isEmpty() ) {
				LOG.debug( "JACC Service not enabled, but JACC permissions specified" );
			}
		}
		else {
			for ( GrantedPermission grantedPermission : mergedSettings.getJaccPermissions().getPermissionDeclarations() ) {
				jaccService.addPermission( grantedPermission );
			}
		}


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
	private void populate(
			MetadataSources metadataSources,
			MergedSettings mergedSettings,
			StandardServiceRegistry ssr) {
		final ClassLoaderService classLoaderService = ssr.getService( ClassLoaderService.class );

		// todo : make sure MetadataSources/Metadata are capable of handling duplicate sources

		// explicit persistence unit mapping files listings
		if ( persistenceUnit.getMappingFileNames() != null ) {
			for ( String name : persistenceUnit.getMappingFileNames() ) {
				metadataSources.addResource( name );
			}
		}

		// explicit persistence unit managed class listings
		//		IMPL NOTE : managed-classes can contain class or package names!!!
		if ( persistenceUnit.getManagedClassNames() != null ) {
			for ( String managedClassName : persistenceUnit.getManagedClassNames() ) {
				// try it as a class name first...
				final String classFileName = managedClassName.replace( '.', '/' ) + ".class";
				final URL classFileUrl = classLoaderService.locateResource( classFileName );
				if ( classFileUrl != null ) {
					// it is a class
					metadataSources.addAnnotatedClassName( managedClassName );
					continue;
				}

				// otherwise, try it as a package name
				final String packageInfoFileName = managedClassName.replace( '.', '/' ) + "/package-info.class";
				final URL packageInfoFileUrl = classLoaderService.locateResource( packageInfoFileName );
				if ( packageInfoFileUrl != null ) {
					// it is a package
					metadataSources.addPackage( managedClassName );
					continue;
				}

				LOG.debugf(
						"Unable to resolve class [%s] named in persistence unit [%s]",
						managedClassName,
						persistenceUnit.getName()
				);
			}
		}

		// add metadata sources explicitly listed in a cfg.xml file referenced by the application
		if ( mergedSettings.cfgXmlNamedMappings != null ) {
			for ( JaxbMapping jaxbMapping : mergedSettings.cfgXmlNamedMappings ) {
				if ( jaxbMapping.getClazz() != null ) {
					metadataSources.addAnnotatedClassName( jaxbMapping.getClazz() );
				}
				else if ( jaxbMapping.getResource() != null ) {
					metadataSources.addResource( jaxbMapping.getResource() );
				}
				else if ( jaxbMapping.getJar() != null ) {
					metadataSources.addJar( new File( jaxbMapping.getJar() ) );
				}
				else if ( jaxbMapping.getPackage() != null ) {
					metadataSources.addPackage( jaxbMapping.getPackage() );
				}
			}
		}

		// add any explicit Class references passed in
		final List<Class> loadedAnnotatedClasses = (List<Class>) configurationValues.remove( AvailableSettings.LOADED_CLASSES );
		if ( loadedAnnotatedClasses != null ) {
			for ( Class cls : loadedAnnotatedClasses ) {
				if ( AttributeConverter.class.isAssignableFrom( cls ) ) {
					metadataSources.addAttributeConverter( (Class<? extends AttributeConverter>) cls );
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
	}

	private void populate(MetadataBuilder metamodelBuilder, MergedSettings mergedSettings, StandardServiceRegistry ssr) {
		metamodelBuilder.with( new StandardJpaScanEnvironmentImpl( persistenceUnit ) );
		metamodelBuilder.with(
				new StandardScanOptions(
						(String) configurationValues.get( AvailableSettings.AUTODETECTION ),
						persistenceUnit.isExcludeUnlistedClasses()
				)
		);

		if ( mergedSettings.cacheRegionDefinitions != null ) {
			for ( CacheRegionDefinition localCacheRegionDefinition : mergedSettings.cacheRegionDefinitions ) {
				metamodelBuilder.with( localCacheRegionDefinition );
			}
		}

		final Object namingStrategySetting = configurationValues.remove( AvailableSettings.NAMING_STRATEGY );
		if ( namingStrategySetting != null ) {
			final StrategySelector strategySelector = ssr.getService( StrategySelector.class );
			metamodelBuilder.with( strategySelector.resolveStrategy( NamingStrategy.class, namingStrategySetting ) );
		}

		final TypeContributorList typeContributorList = (TypeContributorList) configurationValues.remove(
				TYPE_CONTRIBUTORS
		);
		if ( typeContributorList != null ) {
			for ( TypeContributor typeContributor : typeContributorList.getTypeContributors() ) {
				metamodelBuilder.with( typeContributor );
			}
		}
	}

	/**
	 * Completely and utterly not supported :)  Here for use by tests
	 */
	public Map getConfigurationValues() {
		return configurationValues;
	}



	// Phase 2 concerns ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

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
		if ( standardServiceRegistry != null ) {
			StandardServiceRegistryBuilder.destroy( standardServiceRegistry );
		}

	}

	@Override
	public void generateSchema() {
		// This seems overkill, but building the SF is necessary to get the Integrators to kick in.
		// Metamodel will clean this up...
		try {
			SessionFactoryBuilder sfBuilder = metadata.getSessionFactoryBuilder();
			populate( sfBuilder, standardServiceRegistry );
			sfBuilder.build();

			JpaSchemaGenerator.performGeneration( metadata, configurationValues, standardServiceRegistry );
		}
		catch (Exception e) {
			throw persistenceException( "Unable to build Hibernate SessionFactory", e );
		}


		// release this builder
		cancel();
	}

	@SuppressWarnings("unchecked")
	public EntityManagerFactory build() {
		SessionFactoryBuilder sfBuilder = metadata.getSessionFactoryBuilder();
		populate( sfBuilder, standardServiceRegistry );

		SessionFactoryImplementor sessionFactory;
		try {
			sessionFactory = (SessionFactoryImplementor) sfBuilder.build();
		}
		catch (Exception e) {
			throw persistenceException( "Unable to build Hibernate SessionFactory", e );
		}

		return new EntityManagerFactoryImpl(
				persistenceUnit.getName(),
				sessionFactory,
				metadata,
				settings,
				configurationValues
		);
	}

	private void populate(SessionFactoryBuilder sfBuilder, StandardServiceRegistry ssr) {
		final StrategySelector strategySelector = ssr.getService( StrategySelector.class );

		// Locate and apply the requested SessionFactory-level interceptor (if one)
		final Object sessionFactoryInterceptorSetting = configurationValues.remove( AvailableSettings.INTERCEPTOR );
		if ( sessionFactoryInterceptorSetting != null ) {
			final Interceptor sessionFactoryInterceptor =
					strategySelector.resolveStrategy( Interceptor.class, sessionFactoryInterceptorSetting );
			sfBuilder.with( sessionFactoryInterceptor );
		}

		// Locate and apply any requested SessionFactoryObserver
		final Object sessionFactoryObserverSetting = configurationValues.remove( AvailableSettings.SESSION_FACTORY_OBSERVER );
		if ( sessionFactoryObserverSetting != null ) {
			final SessionFactoryObserver suppliedSessionFactoryObserver =
					strategySelector.resolveStrategy( SessionFactoryObserver.class, sessionFactoryObserverSetting );
			sfBuilder.add( suppliedSessionFactoryObserver );
		}

		sfBuilder.add( new ServiceRegistryCloser() );

		sfBuilder.with( JpaEntityNotFoundDelegate.INSTANCE );

		if ( this.validatorFactory != null ) {
			sfBuilder.withValidatorFactory( validatorFactory );
		}
		if ( this.cdiBeanManager != null ) {
			sfBuilder.withBeanManager( cdiBeanManager );
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



	public static class ServiceRegistryCloser implements SessionFactoryObserver {
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

	public static class MergedSettings {
		private final JaccPermissionDeclarations jaccPermissions;

		private final Map configurationValues = new ConcurrentHashMap( 16, 0.75f, 1 );

		private List<CacheRegionDefinition> cacheRegionDefinitions;
		private List<JaxbMapping> cfgXmlNamedMappings;

		public MergedSettings(JaccPermissionDeclarations jaccPermissions) {
			this.jaccPermissions = jaccPermissions;
		}

		public Map getConfigurationValues() {
			return configurationValues;
		}

		public JaccPermissionDeclarations getJaccPermissions() {
			return jaccPermissions;
		}

		public void add(CacheRegionDefinition cacheRegionDefinition) {
			if ( cacheRegionDefinitions == null ) {
				cacheRegionDefinitions = new ArrayList<CacheRegionDefinition>();
			}
			cacheRegionDefinitions.add( cacheRegionDefinition );
		}

		public void add(JaxbMapping jaxbMapping) {
			if ( cfgXmlNamedMappings == null ) {
				cfgXmlNamedMappings =  new ArrayList<JaxbMapping>();
			}
			cfgXmlNamedMappings.add( jaxbMapping );
		}
	}

}
