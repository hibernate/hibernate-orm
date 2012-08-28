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

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityNotFoundException;
import javax.persistence.MappedSuperclass;
import javax.persistence.PersistenceException;
import javax.persistence.spi.PersistenceUnitTransactionType;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;

import org.jboss.logging.Logger;

import org.hibernate.Interceptor;
import org.hibernate.MappingException;
import org.hibernate.MappingNotFoundException;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.cfg.beanvalidation.BeanValidationIntegrator;
import org.hibernate.id.factory.spi.MutableIdentifierGeneratorFactory;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.transaction.internal.jdbc.JdbcTransactionFactory;
import org.hibernate.engine.transaction.internal.jta.CMTTransactionFactory;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.internal.jaxb.cfg.JaxbHibernateConfiguration;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.ValueHolder;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.jpa.internal.EntityManagerFactoryImpl;
import org.hibernate.jpa.internal.EntityManagerMessageLogger;
import org.hibernate.jpa.event.spi.JpaIntegrator;
import org.hibernate.jpa.internal.util.LogHelper;
import org.hibernate.jpa.internal.util.PersistenceUnitTransactionTypeHelper;
import org.hibernate.jpa.packaging.internal.NativeScanner;
import org.hibernate.jpa.packaging.spi.NamedInputStream;
import org.hibernate.jpa.packaging.spi.Scanner;
import org.hibernate.jpa.spi.IdentifierGeneratorStrategyProvider;
import org.hibernate.metamodel.source.annotations.JPADotNames;
import org.hibernate.metamodel.source.annotations.JandexHelper;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.secure.internal.JACCConfiguration;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.service.ConfigLoader;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * @author Steve Ebersole
 */
public class EntityManagerFactoryBuilderImpl implements EntityManagerFactoryBuilder {
    private static final EntityManagerMessageLogger LOG = Logger.getMessageLogger(
			EntityManagerMessageLogger.class,
			EntityManagerFactoryBuilderImpl.class.getName()
	);

	private static final String META_INF_ORM_XML = "META-INF/orm.xml";


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// New settings
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	/**
	 * Names a {@link IntegratorProvider}
	 */
	public static final String INTEGRATOR_PROVIDER = "hibernate.integrator_provider";

	/**
	 * Names a Jandex {@link Index} instance to use.
	 */
	public static final String JANDEX_INDEX = "hibernate.jandex_index";
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private final PersistenceUnitDescriptor persistenceUnit;
	private final SettingsImpl settings = new SettingsImpl();
	private final StandardServiceRegistryBuilder serviceRegistryBuilder;
	private final Map<?,?> configurationValues;

	private final List<JaccDefinition> jaccDefinitions = new ArrayList<JaccDefinition>();
	private final List<CacheRegionDefinition> cacheRegionDefinitions = new ArrayList<CacheRegionDefinition>();
	// todo : would much prefer this as a local variable...
	private final List<JaxbHibernateConfiguration.JaxbSessionFactory.JaxbMapping> cfgXmlNamedMappings = new ArrayList<JaxbHibernateConfiguration.JaxbSessionFactory.JaxbMapping>();
	private Interceptor sessionFactoryInterceptor;
	private NamingStrategy namingStrategy;
	private SessionFactoryObserver suppliedSessionFactoryObserver;

	private MetadataSources metadataSources;
	private Configuration hibernateConfiguration;

	private static EntityNotFoundDelegate jpaEntityNotFoundDelegate = new JpaEntityNotFoundDelegate();

	private static class JpaEntityNotFoundDelegate implements EntityNotFoundDelegate, Serializable {
		public void handleEntityNotFound(String entityName, Serializable id) {
			throw new EntityNotFoundException( "Unable to find " + entityName  + " with id " + id );
		}
	}

	public EntityManagerFactoryBuilderImpl(PersistenceUnitDescriptor persistenceUnit, Map integrationSettings) {
		LogHelper.logPersistenceUnitInformation( persistenceUnit );

		this.persistenceUnit = persistenceUnit;

		if ( integrationSettings == null ) {
			integrationSettings = Collections.emptyMap();
		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// First we build the boot-strap service registry, which mainly handles class loader interactions
		final BootstrapServiceRegistry bootstrapServiceRegistry = buildBootstrapServiceRegistry( integrationSettings );
		// And the main service registry.  This is needed to start adding configuration values, etc
		this.serviceRegistryBuilder = new StandardServiceRegistryBuilder( bootstrapServiceRegistry );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Next we build a merged map of all the configuration values
		this.configurationValues = mergePropertySources( persistenceUnit, integrationSettings, bootstrapServiceRegistry );
		// add all merged configuration values into the service registry builder
		this.serviceRegistryBuilder.applySettings( configurationValues );
		// And being processing those configuration values
		processProperties( bootstrapServiceRegistry );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Next we do a preliminary pass at metadata processing, which involves:
		//		1) scanning
		ScanResult scanResult = scan( bootstrapServiceRegistry );
		//		2) building a Jandex index
		Set<String> collectedManagedClassNames = collectManagedClassNames( scanResult );
		IndexView jandexIndex = locateOrBuildJandexIndex( collectedManagedClassNames, scanResult.getPackageNames(), bootstrapServiceRegistry );
		//		3) building "metadata sources" to keep for later to use in building the SessionFactory
		metadataSources = prepareMetadataSources( jandexIndex, collectedManagedClassNames, scanResult, bootstrapServiceRegistry );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// push back class transformation to the environment; for the time being this only has any effect in EE
		// container situations, calling back into PersistenceUnitInfo#addClassTransformer
		final boolean useClassTransformer = "true".equals( configurationValues.remove( AvailableSettings.USE_CLASS_ENHANCER ) );
		if ( useClassTransformer ) {
			persistenceUnit.pushClassTransformer( metadataSources.collectMappingClassNames() );
		}
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// temporary!
	public Map<?, ?> getConfigurationValues() {
		return Collections.unmodifiableMap( configurationValues );
	}

	public Configuration getHibernateConfiguration() {
		return hibernateConfiguration;
	}
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


	@SuppressWarnings("unchecked")
	private MetadataSources prepareMetadataSources(
			IndexView jandexIndex,
			Set<String> collectedManagedClassNames,
			ScanResult scanResult,
			BootstrapServiceRegistry bootstrapServiceRegistry) {
		// todo : this needs to tie into the metamodel branch...
		MetadataSources metadataSources = new MetadataSources();

		for ( String className : collectedManagedClassNames ) {
			final ClassInfo classInfo = jandexIndex.getClassByName( DotName.createSimple( className ) );
			if ( classInfo == null ) {
				// Not really sure what this means.  Most likely it is explicitly listed in the persistence unit,
				// but mapped via mapping file.  Anyway assume its a mapping class...
				metadataSources.annotatedMappingClassNames.add( className );
				continue;
			}

			// logic here assumes an entity is not also a converter...
			AnnotationInstance converterAnnotation = JandexHelper.getSingleAnnotation(
					classInfo.annotations(),
					JPADotNames.CONVERTER
			);
			if ( converterAnnotation != null ) {
				metadataSources.converterDescriptors.add(
						new MetadataSources.ConverterDescriptor(
								className,
								JandexHelper.getValue( converterAnnotation, "autoApply", boolean.class )
						)
				);
			}
			else {
				metadataSources.annotatedMappingClassNames.add( className );
			}
		}

		metadataSources.packageNames.addAll( scanResult.getPackageNames() );

		metadataSources.namedMappingFileInputStreams.addAll( scanResult.getHbmFiles() );

		metadataSources.mappingFileResources.addAll( scanResult.getMappingFiles() );
		final String explicitHbmXmls = (String) configurationValues.remove( AvailableSettings.HBXML_FILES );
		if ( explicitHbmXmls != null ) {
			metadataSources.mappingFileResources.addAll( Arrays.asList( StringHelper.split( ", ", explicitHbmXmls ) ) );
		}
		final List<String> explicitOrmXml = (List<String>) configurationValues.remove( AvailableSettings.XML_FILE_NAMES );
		if ( explicitOrmXml != null ) {
			metadataSources.mappingFileResources.addAll( explicitOrmXml );
		}

		return metadataSources;
	}

	private Set<String> collectManagedClassNames(ScanResult scanResult) {
		Set<String> collectedNames = new HashSet<String>();
		if ( persistenceUnit.getManagedClassNames() != null ) {
			collectedNames.addAll( persistenceUnit.getManagedClassNames() );
		}
		collectedNames.addAll( scanResult.getManagedClassNames() );
		return collectedNames;
	}

	private IndexView locateOrBuildJandexIndex(
			Set<String> collectedManagedClassNames,
			List<String> packageNames,
			BootstrapServiceRegistry bootstrapServiceRegistry) {
		// for now create a whole new Index to work with, eventually we need to:
		//		1) accept an Index as an incoming config value
		//		2) pass that Index along to the metamodel code...
		//
		// (1) is mocked up here, but JBoss AS does not currently pass in any Index to use...
		IndexView jandexIndex = (IndexView) configurationValues.get( JANDEX_INDEX );
		if ( jandexIndex == null ) {
			jandexIndex = buildJandexIndex( collectedManagedClassNames, packageNames, bootstrapServiceRegistry );
		}
		return jandexIndex;
	}

	private IndexView buildJandexIndex(Set<String> classNamesSource, List<String> packageNames, BootstrapServiceRegistry bootstrapServiceRegistry) {
		Indexer indexer = new Indexer();

		for ( String className : classNamesSource ) {
			indexResource( className.replace( '.', '/' ) + ".class", indexer, bootstrapServiceRegistry );
		}

		// add package-info from the configured packages
		for ( String packageName : packageNames ) {
			indexResource( packageName.replace( '.', '/' ) + "/package-info.class", indexer, bootstrapServiceRegistry );
		}

		// for now we just skip entities defined in (1) orm.xml files and (2) hbm.xml files.  this part really needs
		// metamodel branch...

		// for now, we also need to wrap this in a CompositeIndex until Jandex is updated to use a common interface
		// between the 2...
		return CompositeIndex.create( indexer.complete() );
	}

	private void indexResource(String resourceName, Indexer indexer, BootstrapServiceRegistry bootstrapServiceRegistry) {
		InputStream stream = bootstrapServiceRegistry.getService( ClassLoaderService.class ).locateResourceStream( resourceName );
		try {
			indexer.index( stream );
		}
		catch ( IOException e ) {
			throw persistenceException( "Unable to open input stream for resource " + resourceName, e );
		}
	}

	/**
	 * Builds the {@link BootstrapServiceRegistry} used to eventually build the {@link org.hibernate.boot.registry.StandardServiceRegistryBuilder}; mainly
	 * used here during instantiation to define class-loading behavior.
	 *
	 * @param integrationSettings Any integration settings passed by the EE container or SE application
	 *
	 * @return The built BootstrapServiceRegistry
	 */
	private BootstrapServiceRegistry buildBootstrapServiceRegistry(Map integrationSettings) {
		final BootstrapServiceRegistryBuilder bootstrapServiceRegistryBuilder = new BootstrapServiceRegistryBuilder();
		bootstrapServiceRegistryBuilder.with( new JpaIntegrator() );

		final IntegratorProvider integratorProvider = (IntegratorProvider) integrationSettings.get( INTEGRATOR_PROVIDER );
		if ( integratorProvider != null ) {
			integrationSettings.remove( INTEGRATOR_PROVIDER );
			for ( Integrator integrator : integratorProvider.getIntegrators() ) {
				bootstrapServiceRegistryBuilder.with( integrator );
			}
		}

		ClassLoader classLoader = (ClassLoader) integrationSettings.get( org.hibernate.cfg.AvailableSettings.APP_CLASSLOADER );
		if ( classLoader != null ) {
			integrationSettings.remove( org.hibernate.cfg.AvailableSettings.APP_CLASSLOADER );
		}
		else {
			classLoader = persistenceUnit.getClassLoader();
		}
		bootstrapServiceRegistryBuilder.withApplicationClassLoader( classLoader );

		return bootstrapServiceRegistryBuilder.build();
	}

	@SuppressWarnings("unchecked")
	private Map mergePropertySources(
			PersistenceUnitDescriptor persistenceUnit,
			Map integrationSettings,
			final BootstrapServiceRegistry bootstrapServiceRegistry) {
		final Map merged = new HashMap();
		// first, apply persistence.xml-defined settings
		if ( persistenceUnit.getProperties() != null ) {
			merged.putAll( persistenceUnit.getProperties() );
		}

		merged.put( AvailableSettings.PERSISTENCE_UNIT_NAME, persistenceUnit.getName() );

		// see if the persistence.xml settings named a Hibernate config file....
		final ValueHolder<ConfigLoader> configLoaderHolder = new ValueHolder<ConfigLoader>(
				new ValueHolder.DeferredInitializer<ConfigLoader>() {
					@Override
					public ConfigLoader initialize() {
						return new ConfigLoader( bootstrapServiceRegistry );
					}
				}
		);

		{
			final String cfgXmlResourceName = (String) merged.remove( AvailableSettings.CFG_FILE );
			if ( StringHelper.isNotEmpty( cfgXmlResourceName ) ) {
				// it does, so load those properties
				JaxbHibernateConfiguration configurationElement = configLoaderHolder.getValue()
						.loadConfigXmlResource( cfgXmlResourceName );
				processHibernateConfigurationElement( configurationElement, merged );
			}
		}

		// see if integration settings named a Hibernate config file....
		{
			final String cfgXmlResourceName = (String) integrationSettings.get( AvailableSettings.CFG_FILE );
			if ( StringHelper.isNotEmpty( cfgXmlResourceName ) ) {
				integrationSettings.remove( AvailableSettings.CFG_FILE );
				// it does, so load those properties
				JaxbHibernateConfiguration configurationElement = configLoaderHolder.getValue().loadConfigXmlResource(
						cfgXmlResourceName
				);
				processHibernateConfigurationElement( configurationElement, merged );
			}
		}

		// finally, apply integration-supplied settings (per JPA spec, integration settings should override other sources)
		merged.putAll( integrationSettings );

		if ( ! merged.containsKey( AvailableSettings.VALIDATION_MODE ) ) {
			if ( persistenceUnit.getValidationMode() != null ) {
				merged.put( AvailableSettings.VALIDATION_MODE, persistenceUnit.getValidationMode() );
			}
		}

		if ( ! merged.containsKey( AvailableSettings.SHARED_CACHE_MODE ) ) {
			if ( persistenceUnit.getSharedCacheMode() != null ) {
				merged.put( AvailableSettings.SHARED_CACHE_MODE, persistenceUnit.getSharedCacheMode() );
			}
		}

		// was getting NPE exceptions from the underlying map when just using #putAll, so going this safer route...
		Iterator itr = merged.entrySet().iterator();
		while ( itr.hasNext() ) {
			final Map.Entry entry = (Map.Entry) itr.next();
			if ( entry.getValue() == null ) {
				itr.remove();
			}
		}

		return merged;
	}

	@SuppressWarnings("unchecked")
	private void processHibernateConfigurationElement(
			JaxbHibernateConfiguration configurationElement,
			Map mergeMap) {
		if ( ! mergeMap.containsKey( org.hibernate.cfg.AvailableSettings.SESSION_FACTORY_NAME ) ) {
			String cfgName = configurationElement.getSessionFactory().getName();
			if ( cfgName != null ) {
				mergeMap.put( org.hibernate.cfg.AvailableSettings.SESSION_FACTORY_NAME, cfgName );
			}
		}

		for ( JaxbHibernateConfiguration.JaxbSessionFactory.JaxbProperty jaxbProperty : configurationElement.getSessionFactory().getProperty() ) {
			mergeMap.put( jaxbProperty.getName(), jaxbProperty.getValue() );
		}

		for ( JaxbHibernateConfiguration.JaxbSessionFactory.JaxbMapping jaxbMapping : configurationElement.getSessionFactory().getMapping() ) {
			cfgXmlNamedMappings.add( jaxbMapping );
		}

		for ( Object cacheDeclaration : configurationElement.getSessionFactory().getClassCacheOrCollectionCache() ) {
			if ( JaxbHibernateConfiguration.JaxbSessionFactory.JaxbClassCache.class.isInstance( cacheDeclaration ) ) {
				final JaxbHibernateConfiguration.JaxbSessionFactory.JaxbClassCache jaxbClassCache
						= (JaxbHibernateConfiguration.JaxbSessionFactory.JaxbClassCache) cacheDeclaration;
				cacheRegionDefinitions.add(
						new CacheRegionDefinition(
								CacheRegionDefinition.CacheType.ENTITY,
								jaxbClassCache.getClazz(),
								jaxbClassCache.getUsage().value(),
								jaxbClassCache.getRegion(),
								"all".equals( jaxbClassCache.getInclude() )
						)
				);
			}
			else {
				final JaxbHibernateConfiguration.JaxbSessionFactory.JaxbCollectionCache jaxbCollectionCache
						= (JaxbHibernateConfiguration.JaxbSessionFactory.JaxbCollectionCache) cacheDeclaration;
				cacheRegionDefinitions.add(
						new CacheRegionDefinition(
								CacheRegionDefinition.CacheType.COLLECTION,
								jaxbCollectionCache.getCollection(),
								jaxbCollectionCache.getUsage().value(),
								jaxbCollectionCache.getRegion(),
								false
						)
				);
			}
		}

		if ( configurationElement.getSecurity() != null ) {
			final String contextId = configurationElement.getSecurity().getContext();
			for ( JaxbHibernateConfiguration.JaxbSecurity.JaxbGrant grant : configurationElement.getSecurity().getGrant() ) {
				jaccDefinitions.add(
						new JaccDefinition(
								contextId,
								grant.getRole(),
								grant.getEntityName(),
								grant.getActions()
						)
				);
			}
		}
	}

	private void processProperties(BootstrapServiceRegistry bootstrapServiceRegistry) {
		applyJdbcConnectionProperties();
		applyTransactionProperties();

		final Object validationFactory = configurationValues.get( AvailableSettings.VALIDATION_FACTORY );
		if ( validationFactory != null ) {
			BeanValidationIntegrator.validateFactory( validationFactory );
		}

		// flush before completion validation
		if ( "true".equals( configurationValues.get( Environment.FLUSH_BEFORE_COMPLETION ) ) ) {
			serviceRegistryBuilder.applySetting( Environment.FLUSH_BEFORE_COMPLETION, "false" );
			LOG.definingFlushBeforeCompletionIgnoredInHem( Environment.FLUSH_BEFORE_COMPLETION );
		}

		for ( Map.Entry entry : configurationValues.entrySet() ) {
			if ( entry.getKey() instanceof String ) {
				final String keyString = (String) entry.getKey();

				if ( AvailableSettings.INTERCEPTOR.equals( keyString ) ) {
					sessionFactoryInterceptor = instantiateCustomClassFromConfiguration(
							entry.getValue(),
							Interceptor.class,
							bootstrapServiceRegistry
					);
				}
				else if ( AvailableSettings.SESSION_INTERCEPTOR.equals( keyString ) ) {
					settings.setSessionInterceptorClass(
							loadSessionInterceptorClass( entry.getValue(), bootstrapServiceRegistry )
					);
				}
				else if ( AvailableSettings.NAMING_STRATEGY.equals( keyString ) ) {
					namingStrategy = instantiateCustomClassFromConfiguration(
							entry.getValue(),
							NamingStrategy.class,
							bootstrapServiceRegistry
					);
				}
				else if ( AvailableSettings.SESSION_FACTORY_OBSERVER.equals( keyString ) ) {
					suppliedSessionFactoryObserver = instantiateCustomClassFromConfiguration(
							entry.getValue(),
							SessionFactoryObserver.class,
							bootstrapServiceRegistry
					);
				}
				else if ( AvailableSettings.DISCARD_PC_ON_CLOSE.equals( keyString ) ) {
					settings.setReleaseResourcesOnCloseEnabled( "true".equals( entry.getValue() ) );
				}
				else if ( keyString.startsWith( AvailableSettings.CLASS_CACHE_PREFIX ) ) {
					addCacheRegionDefinition(
							keyString.substring( AvailableSettings.CLASS_CACHE_PREFIX.length() + 1 ),
							(String) entry.getValue(),
							CacheRegionDefinition.CacheType.ENTITY
					);
				}
				else if ( keyString.startsWith( AvailableSettings.COLLECTION_CACHE_PREFIX ) ) {
					addCacheRegionDefinition(
							keyString.substring( AvailableSettings.COLLECTION_CACHE_PREFIX.length() + 1 ),
							(String) entry.getValue(),
							CacheRegionDefinition.CacheType.COLLECTION
					);
				}
				else if ( keyString.startsWith( AvailableSettings.JACC_PREFIX )
						&& ! ( keyString.equals( AvailableSettings.JACC_CONTEXT_ID )
						|| keyString.equals( AvailableSettings.JACC_ENABLED ) ) ) {
					addJaccDefinition( (String) entry.getKey(), entry.getValue() );
				}
			}
		}
	}

	private void applyJdbcConnectionProperties() {
		if ( persistenceUnit.getJtaDataSource() != null ) {
			serviceRegistryBuilder.applySetting( Environment.DATASOURCE, persistenceUnit.getJtaDataSource() );
		}
		else if ( persistenceUnit.getNonJtaDataSource() != null ) {
			serviceRegistryBuilder.applySetting( Environment.DATASOURCE, persistenceUnit.getNonJtaDataSource() );
		}
		else {
			final String driver = (String) configurationValues.get( AvailableSettings.JDBC_DRIVER );
			if ( StringHelper.isNotEmpty( driver ) ) {
				serviceRegistryBuilder.applySetting( org.hibernate.cfg.AvailableSettings.DRIVER, driver );
			}
			final String url = (String) configurationValues.get( AvailableSettings.JDBC_URL );
			if ( StringHelper.isNotEmpty( url ) ) {
				serviceRegistryBuilder.applySetting( org.hibernate.cfg.AvailableSettings.URL, url );
			}
			final String user = (String) configurationValues.get( AvailableSettings.JDBC_USER );
			if ( StringHelper.isNotEmpty( user ) ) {
				serviceRegistryBuilder.applySetting( org.hibernate.cfg.AvailableSettings.USER, user );
			}
			final String pass = (String) configurationValues.get( AvailableSettings.JDBC_PASSWORD );
			if ( StringHelper.isNotEmpty( pass ) ) {
				serviceRegistryBuilder.applySetting( org.hibernate.cfg.AvailableSettings.PASS, pass );
			}
		}
	}

	private void applyTransactionProperties() {
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
				serviceRegistryBuilder.applySetting( Environment.TRANSACTION_STRATEGY, CMTTransactionFactory.class );
			}
			else if ( txnType == PersistenceUnitTransactionType.RESOURCE_LOCAL ) {
				serviceRegistryBuilder.applySetting( Environment.TRANSACTION_STRATEGY, JdbcTransactionFactory.class );
			}
		}
	}

	private String jaccContextId;

	private void addJaccDefinition(String key, Object value) {
		if ( jaccContextId == null ) {
			jaccContextId = (String) configurationValues.get( AvailableSettings.JACC_CONTEXT_ID );
			if ( jaccContextId == null ) {
				throw persistenceException(
						"Entities have been configured for JACC, but "
								+ AvailableSettings.JACC_CONTEXT_ID + " has not been set"
				);
			}
		}

		try {
			final int roleStart = AvailableSettings.JACC_PREFIX.length() + 1;
			final String role = key.substring( roleStart, key.indexOf( '.', roleStart ) );
			final int classStart = roleStart + role.length() + 1;
			final String clazz = key.substring( classStart, key.length() );

			final JaccDefinition def = new JaccDefinition( jaccContextId, role, clazz, (String) value );

			jaccDefinitions.add( def );

		}
		catch ( IndexOutOfBoundsException e ) {
			throw persistenceException( "Illegal usage of " + AvailableSettings.JACC_PREFIX + ": " + key );
		}
	}

	@SuppressWarnings("unchecked")
	private Class<? extends Interceptor> loadSessionInterceptorClass(
			Object value,
			BootstrapServiceRegistry bootstrapServiceRegistry) {
		if ( value == null ) {
			return null;
		}

		Class theClass;
		if ( Class.class.isInstance( value ) ) {
			theClass = (Class) value;
		}
		else {
			theClass = bootstrapServiceRegistry.getService( ClassLoaderService.class ).classForName( value.toString() );
		}

		try {
			return (Class<? extends Interceptor>) theClass;
		}
		catch (ClassCastException e) {
			throw persistenceException(
					String.format(
							"Specified Interceptor implementation class [%s] was not castable to Interceptor",
							theClass.getName()
					)
			);
		}
	}

	private void addCacheRegionDefinition(String role, String value, CacheRegionDefinition.CacheType cacheType) {
		final StringTokenizer params = new StringTokenizer( value, ";, " );
		if ( !params.hasMoreTokens() ) {
			StringBuilder error = new StringBuilder( "Illegal usage of " );
			if ( cacheType == CacheRegionDefinition.CacheType.ENTITY ) {
				error.append( AvailableSettings.CLASS_CACHE_PREFIX )
						.append( ": " )
						.append( AvailableSettings.CLASS_CACHE_PREFIX );
			}
			else {
				error.append( AvailableSettings.COLLECTION_CACHE_PREFIX )
						.append( ": " )
						.append( AvailableSettings.COLLECTION_CACHE_PREFIX );
			}
			error.append( '.' )
					.append( role )
					.append( ' ' )
					.append( value )
					.append( ".  Was expecting configuration, but found none" );
			throw persistenceException( error.toString() );
		}

		String usage = params.nextToken();
		String region = null;
		if ( params.hasMoreTokens() ) {
			region = params.nextToken();
		}
		boolean lazyProperty = true;
		if ( cacheType == CacheRegionDefinition.CacheType.ENTITY ) {
			if ( params.hasMoreTokens() ) {
				lazyProperty = "all".equalsIgnoreCase( params.nextToken() );
			}
		}
		else {
			lazyProperty = false;
		}

		final CacheRegionDefinition def = new CacheRegionDefinition( cacheType, role, usage, region, lazyProperty );
		cacheRegionDefinitions.add( def );
	}

	@SuppressWarnings("unchecked")
	private <T> T instantiateCustomClassFromConfiguration(
			Object value,
			Class<T> type,
			ServiceRegistry bootstrapServiceRegistry) {
		if ( value == null ) {
			return null;
		}

		if ( type.isInstance( value ) ) {
			return (T) value;
		}

		final Class<? extends T> implementationClass;

		if ( Class.class.isInstance( value ) ) {
			try {
				implementationClass = (Class<? extends T>) value;
			}
			catch (ClassCastException e) {
				throw persistenceException(
						String.format(
								"Specified implementation class [%s] was not of expected type [%s]",
								((Class) value).getName(),
								type.getName()
						)
				);
			}
		}
		else {
			final String implementationClassName = value.toString();
			try {
				implementationClass = bootstrapServiceRegistry.getService( ClassLoaderService.class )
						.classForName( implementationClassName );
			}
			catch (ClassCastException e) {
				throw persistenceException(
						String.format(
								"Specified implementation class [%s] was not of expected type [%s]",
								implementationClassName,
								type.getName()
						)
				);
			}
		}

		try {
			return implementationClass.newInstance();
		}
		catch (Exception e) {
			throw persistenceException(
					String.format(
							"Unable to instantiate specified implementation class [%s]",
							implementationClass.getName()
					),
					e
			);
		}
	}

	@SuppressWarnings("unchecked")
	private ScanResult scan(BootstrapServiceRegistry bootstrapServiceRegistry) {
		Scanner scanner = locateOrBuildScanner( bootstrapServiceRegistry );
		ScanningContext scanningContext = new ScanningContext();

		final ScanResult scanResult = new ScanResult();
		if ( persistenceUnit.getMappingFileNames() != null ) {
			scanResult.getMappingFiles().addAll( persistenceUnit.getMappingFileNames() );
		}

		// dunno, but the old code did it...
		scanningContext.setSearchOrm( ! scanResult.getMappingFiles().contains( META_INF_ORM_XML ) );

		if ( persistenceUnit.getJarFileUrls() != null ) {
			prepareAutoDetectionSettings( scanningContext, false );
			for ( URL jar : persistenceUnit.getJarFileUrls() ) {
				scanningContext.setUrl( jar );
				scanInContext( scanner, scanningContext, scanResult );
			}
		}

		prepareAutoDetectionSettings( scanningContext, persistenceUnit.isExcludeUnlistedClasses() );
		scanningContext.setUrl( persistenceUnit.getPersistenceUnitRootUrl() );
		scanInContext( scanner, scanningContext, scanResult );

		return scanResult;
	}

	@SuppressWarnings("unchecked")
	private Scanner locateOrBuildScanner(BootstrapServiceRegistry bootstrapServiceRegistry) {
		final Object value = configurationValues.remove( AvailableSettings.SCANNER );
		if ( value == null ) {
			return new NativeScanner();
		}

		if ( Scanner.class.isInstance( value ) ) {
			return (Scanner) value;
		}

		Class<? extends Scanner> scannerClass;
		if ( Class.class.isInstance( value ) ) {
			try {
				scannerClass = (Class<? extends Scanner>) value;
			}
			catch ( ClassCastException e ) {
				throw persistenceException( "Expecting Scanner implementation, but found " + ((Class) value).getName() );
			}
		}
		else {
			final String scannerClassName = value.toString();
			try {
				scannerClass = bootstrapServiceRegistry.getService( ClassLoaderService.class ).classForName( scannerClassName );
			}
			catch ( ClassCastException e ) {
				throw persistenceException( "Expecting Scanner implementation, but found " + scannerClassName );
			}
		}

		try {
			return scannerClass.newInstance();
		}
		catch ( Exception e ) {
			throw persistenceException( "Unable to instantiate Scanner class: " + scannerClass, e );
		}
	}

	private void prepareAutoDetectionSettings(ScanningContext context, boolean excludeUnlistedClasses) {
		final String detectionSetting = (String) configurationValues.get( AvailableSettings.AUTODETECTION );

		if ( detectionSetting == null ) {
			if ( excludeUnlistedClasses ) {
				context.setDetectClasses( false );
				context.setDetectHbmFiles( false );
			}
			else {
				context.setDetectClasses( true );
				context.setDetectHbmFiles( true );
			}
		}
		else {
			for ( String token : StringHelper.split( ", ", detectionSetting ) ) {
				if ( "class".equalsIgnoreCase( token ) ) {
					context.setDetectClasses( true );
				}
				if ( "hbm".equalsIgnoreCase( token ) ) {
					context.setDetectClasses( true );
				}
			}
		}
	}

	private void scanInContext(
			Scanner scanner,
			ScanningContext scanningContext,
			ScanResult scanResult) {
		if ( scanningContext.getUrl() == null ) {
			// not sure i like just ignoring this being null, but this is exactly what the old code does...
			LOG.containerProvidingNullPersistenceUnitRootUrl();
			return;
		}

		try {
			if ( scanningContext.isDetectClasses() ) {
				Set<Package> matchingPackages = scanner.getPackagesInJar( scanningContext.url, new HashSet<Class<? extends Annotation>>(0) );
				for ( Package pkg : matchingPackages ) {
					scanResult.getPackageNames().add( pkg.getName() );
				}

				Set<Class<? extends Annotation>> annotationsToLookFor = new HashSet<Class<? extends Annotation>>();
				annotationsToLookFor.add( Entity.class );
				annotationsToLookFor.add( MappedSuperclass.class );
				annotationsToLookFor.add( Embeddable.class );
				annotationsToLookFor.add( Converter.class );
				Set<Class<?>> matchingClasses = scanner.getClassesInJar( scanningContext.url, annotationsToLookFor );
				for ( Class<?> clazz : matchingClasses ) {
					scanResult.getManagedClassNames().add( clazz.getName() );
				}
			}

			Set<String> patterns = new HashSet<String>();
			if ( scanningContext.isSearchOrm() ) {
				patterns.add( META_INF_ORM_XML );
			}
			if ( scanningContext.isDetectHbmFiles() ) {
				patterns.add( "**/*.hbm.xml" );
			}
			if ( ! scanResult.getMappingFiles().isEmpty() ) {
				patterns.addAll( scanResult.getMappingFiles() );
			}
			if ( patterns.size() != 0 ) {
				Set<NamedInputStream> files = scanner.getFilesInJar( scanningContext.getUrl(), patterns );
				for ( NamedInputStream file : files ) {
					scanResult.getHbmFiles().add( file );
					scanResult.getMappingFiles().remove( file.getName() );
				}
			}
		}
		catch (PersistenceException e ) {
			throw e;
		}
		catch ( RuntimeException e ) {
			throw persistenceException( "error trying to scan url: " + scanningContext.getUrl().toString(), e );
		}
	}

	@Override
	public void cancel() {
		// todo : close the bootstrap registry (not critical, but nice to do)

	}

	@SuppressWarnings("unchecked")
	public EntityManagerFactory buildEntityManagerFactory() {
		// IMPL NOTE : TCCL handling here is temporary.
		//		It is needed because this code still uses Hibernate Configuration and Hibernate commons-annotations
		// 		in turn which relies on TCCL being set.

		final ServiceRegistry serviceRegistry = buildServiceRegistry();
		final ClassLoaderService classLoaderService = serviceRegistry.getService( ClassLoaderService.class );

		return ( (ClassLoaderServiceImpl) classLoaderService ).withTccl(
				new ClassLoaderServiceImpl.Work<EntityManagerFactoryImpl>() {
					@Override
					public EntityManagerFactoryImpl perform() {
						hibernateConfiguration = buildHibernateConfiguration( serviceRegistry );

						SessionFactoryImplementor sessionFactory;
						try {
							sessionFactory = (SessionFactoryImplementor) hibernateConfiguration.buildSessionFactory( serviceRegistry );
						}
						catch (MappingException e) {
							throw persistenceException( "Unable to build Hibernate SessionFactory", e );
						}

						if ( suppliedSessionFactoryObserver != null ) {
							sessionFactory.addObserver( suppliedSessionFactoryObserver );
						}
						sessionFactory.addObserver( new ServiceRegistryCloser() );

						// NOTE : passing cfg is temporary until
						return new EntityManagerFactoryImpl( persistenceUnit.getName(), sessionFactory, settings, configurationValues, hibernateConfiguration );
					}
				}
		);
	}

	public ServiceRegistry buildServiceRegistry() {
		return serviceRegistryBuilder.buildServiceRegistry();
	}

	public Configuration buildHibernateConfiguration(ServiceRegistry serviceRegistry) {
		Properties props = new Properties();
		props.putAll( configurationValues );
		Configuration cfg = new Configuration().setProperties( props );

		cfg.setEntityNotFoundDelegate( jpaEntityNotFoundDelegate );

		if ( namingStrategy != null ) {
			cfg.setNamingStrategy( namingStrategy );
		}

		if ( sessionFactoryInterceptor != null ) {
			cfg.setInterceptor( sessionFactoryInterceptor );
		}

		final IdentifierGeneratorStrategyProvider strategyProvider = instantiateCustomClassFromConfiguration(
				props.get( AvailableSettings.IDENTIFIER_GENERATOR_STRATEGY_PROVIDER ),
				IdentifierGeneratorStrategyProvider.class,
				serviceRegistry
		);
		if ( strategyProvider != null ) {
			final MutableIdentifierGeneratorFactory identifierGeneratorFactory = cfg.getIdentifierGeneratorFactory();
			for ( Map.Entry<String,Class<?>> entry : strategyProvider.getStrategies().entrySet() ) {
				identifierGeneratorFactory.register( entry.getKey(), entry.getValue() );
			}
		}

		if ( jaccDefinitions != null ) {
			for ( JaccDefinition jaccDefinition : jaccDefinitions ) {
				JACCConfiguration jaccCfg = new JACCConfiguration( jaccDefinition.contextId );
				jaccCfg.addPermission( jaccDefinition.role, jaccDefinition.clazz, jaccDefinition.actions );
			}
		}

		if ( cacheRegionDefinitions != null ) {
			for ( CacheRegionDefinition cacheRegionDefinition : cacheRegionDefinitions ) {
				if ( cacheRegionDefinition.cacheType == CacheRegionDefinition.CacheType.ENTITY ) {
					cfg.setCacheConcurrencyStrategy(
							cacheRegionDefinition.role,
							cacheRegionDefinition.usage,
							cacheRegionDefinition.region,
							cacheRegionDefinition.cacheLazy
					);
				}
				else {
					cfg.setCollectionCacheConcurrencyStrategy(
							cacheRegionDefinition.role,
							cacheRegionDefinition.usage,
							cacheRegionDefinition.region
					);
				}
			}
		}


		// todo : need to have this use the metamodel codebase eventually...

		for ( JaxbHibernateConfiguration.JaxbSessionFactory.JaxbMapping jaxbMapping : cfgXmlNamedMappings ) {
			if ( jaxbMapping.getClazz() != null ) {
				cfg.addAnnotatedClass(
						serviceRegistry.getService( ClassLoaderService.class ).classForName( jaxbMapping.getClazz() )
				);
			}
			else if ( jaxbMapping.getResource() != null ) {
				cfg.addResource( jaxbMapping.getResource() );
			}
			else if ( jaxbMapping.getJar() != null ) {
				cfg.addJar( new File( jaxbMapping.getJar() ) );
			}
			else if ( jaxbMapping.getPackage() != null ) {
				cfg.addPackage( jaxbMapping.getPackage() );
			}
		}

		List<Class> loadedAnnotatedClasses = (List<Class>) configurationValues.remove( AvailableSettings.LOADED_CLASSES );
		if ( loadedAnnotatedClasses != null ) {
			for ( Class cls : loadedAnnotatedClasses ) {
				cfg.addAnnotatedClass( cls );
			}
		}

		for ( String className : metadataSources.getAnnotatedMappingClassNames() ) {
			cfg.addAnnotatedClass( serviceRegistry.getService( ClassLoaderService.class ).classForName( className ) );
		}

		for ( MetadataSources.ConverterDescriptor converterDescriptor : metadataSources.getConverterDescriptors() ) {
			final Class<? extends AttributeConverter> converterClass;
			try {
				Class theClass = serviceRegistry.getService( ClassLoaderService.class ).classForName( converterDescriptor.converterClassName );
				converterClass = (Class<? extends AttributeConverter>) theClass;
			}
			catch (ClassCastException e) {
				throw persistenceException(
						String.format(
								"AttributeConverter implementation [%s] does not implement AttributeConverter interface",
								converterDescriptor.converterClassName
						)
				);
			}
			cfg.addAttributeConverter( converterClass, converterDescriptor.autoApply );
		}

		for ( String resourceName : metadataSources.mappingFileResources ) {
			Boolean useMetaInf = null;
			try {
				if ( resourceName.endsWith( META_INF_ORM_XML ) ) {
					useMetaInf = true;
				}
				cfg.addResource( resourceName );
			}
			catch( MappingNotFoundException e ) {
				if ( ! resourceName.endsWith( META_INF_ORM_XML ) ) {
					throw persistenceException( "Unable to find XML mapping file in classpath: " + resourceName );
				}
				else {
					useMetaInf = false;
					//swallow it, the META-INF/orm.xml is optional
				}
			}
			catch( MappingException me ) {
				throw persistenceException( "Error while reading JPA XML file: " + resourceName, me );
			}

			if ( Boolean.TRUE.equals( useMetaInf ) ) {
				LOG.exceptionHeaderFound( getExceptionHeader(), META_INF_ORM_XML );
			}
			else if (Boolean.FALSE.equals(useMetaInf)) {
				LOG.exceptionHeaderNotFound( getExceptionHeader(), META_INF_ORM_XML );
			}
		}
		for ( NamedInputStream namedInputStream : metadataSources.namedMappingFileInputStreams ) {
			try {
				//addInputStream has the responsibility to close the stream
				cfg.addInputStream( new BufferedInputStream( namedInputStream.getStream() ) );
			}
			catch (MappingException me) {
				//try our best to give the file name
				if ( StringHelper.isEmpty( namedInputStream.getName() ) ) {
					throw me;
				}
				else {
					throw new MappingException("Error while parsing file: " + namedInputStream.getName(), me );
				}
			}
		}
		for ( String packageName : metadataSources.packageNames ) {
			cfg.addPackage( packageName );
		}
		return cfg;
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

	public static class CacheRegionDefinition {
		public static enum CacheType { ENTITY, COLLECTION }

		public final CacheType cacheType;
		public final String role;
		public final String usage;
		public final String region;
		public final boolean cacheLazy;

		public CacheRegionDefinition(
				CacheType cacheType,
				String role,
				String usage,
				String region, boolean cacheLazy) {
			this.cacheType = cacheType;
			this.role = role;
			this.usage = usage;
			this.region = region;
			this.cacheLazy = cacheLazy;
		}
	}

	public static class JaccDefinition {
		public final String contextId;
		public final String role;
		public final String clazz;
		public final String actions;

		public JaccDefinition(String contextId, String role, String clazz, String actions) {
			this.contextId = contextId;
			this.role = role;
			this.clazz = clazz;
			this.actions = actions;
		}
	}

	public static class ScanningContext {
		private URL url;
		private boolean detectClasses;
		private boolean detectHbmFiles;
		private boolean searchOrm;

		public URL getUrl() {
			return url;
		}

		public void setUrl(URL url) {
			this.url = url;
		}

		public boolean isDetectClasses() {
			return detectClasses;
		}

		public void setDetectClasses(boolean detectClasses) {
			this.detectClasses = detectClasses;
		}

		public boolean isDetectHbmFiles() {
			return detectHbmFiles;
		}

		public void setDetectHbmFiles(boolean detectHbmFiles) {
			this.detectHbmFiles = detectHbmFiles;
		}

		public boolean isSearchOrm() {
			return searchOrm;
		}

		public void setSearchOrm(boolean searchOrm) {
			this.searchOrm = searchOrm;
		}
	}

	private static class ScanResult {
		private final List<String> managedClassNames = new ArrayList<String>();
		private final List<String> packageNames = new ArrayList<String>();
		private final List<NamedInputStream> hbmFiles = new ArrayList<NamedInputStream>();
		private final List<String> mappingFiles = new ArrayList<String>();

		public List<String> getManagedClassNames() {
			return managedClassNames;
		}

		public List<String> getPackageNames() {
			return packageNames;
		}

		public List<NamedInputStream> getHbmFiles() {
			return hbmFiles;
		}

		public List<String> getMappingFiles() {
			return mappingFiles;
		}
	}

	public static class MetadataSources {
		private final List<String> annotatedMappingClassNames = new ArrayList<String>();
		private final List<ConverterDescriptor> converterDescriptors = new ArrayList<ConverterDescriptor>();
		private final List<NamedInputStream> namedMappingFileInputStreams = new ArrayList<NamedInputStream>();
		private final List<String> mappingFileResources = new ArrayList<String>();
		private final List<String> packageNames = new ArrayList<String>();

		public List<String> getAnnotatedMappingClassNames() {
			return annotatedMappingClassNames;
		}

		public List<ConverterDescriptor> getConverterDescriptors() {
			return converterDescriptors;
		}

		public List<NamedInputStream> getNamedMappingFileInputStreams() {
			return namedMappingFileInputStreams;
		}

		public List<String> getPackageNames() {
			return packageNames;
		}

		public List<String> collectMappingClassNames() {
			// todo : the complete answer to this involves looking through the mapping files as well.
			// 		Really need the metamodel branch code to do that properly
			return annotatedMappingClassNames;
		}

		public static class ConverterDescriptor {
			private final String converterClassName;
			private final boolean autoApply;

			public ConverterDescriptor(String converterClassName, boolean autoApply) {
				this.converterClassName = converterClassName;
				this.autoApply = autoApply;
			}
		}
	}
}
