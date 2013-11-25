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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
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
import javax.persistence.AttributeConverter;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceException;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import org.hibernate.Interceptor;
import org.hibernate.InvalidMappingException;
import org.hibernate.MappingException;
import org.hibernate.MappingNotFoundException;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.selector.StrategyRegistrationProvider;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.cfg.beanvalidation.BeanValidationIntegrator;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.transaction.internal.jdbc.JdbcTransactionFactory;
import org.hibernate.engine.transaction.internal.jta.CMTTransactionFactory;
import org.hibernate.id.factory.spi.MutableIdentifierGeneratorFactory;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.internal.jaxb.cfg.JaxbHibernateConfiguration;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.ValueHolder;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.boot.scan.internal.StandardScanOptions;
import org.hibernate.jpa.boot.scan.internal.StandardScanner;
import org.hibernate.jpa.boot.scan.spi.ScanOptions;
import org.hibernate.jpa.boot.scan.spi.ScanResult;
import org.hibernate.jpa.boot.scan.spi.Scanner;
import org.hibernate.jpa.boot.spi.ClassDescriptor;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.hibernate.jpa.boot.spi.InputStreamAccess;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.hibernate.jpa.boot.spi.MappingFileDescriptor;
import org.hibernate.jpa.boot.spi.NamedInputStream;
import org.hibernate.jpa.boot.spi.PackageDescriptor;
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
import org.hibernate.metamodel.source.annotations.JPADotNames;
import org.hibernate.metamodel.source.annotations.JandexHelper;
import org.hibernate.metamodel.spi.TypeContributor;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.secure.spi.GrantedPermission;
import org.hibernate.secure.spi.JaccService;
import org.hibernate.service.ConfigLoader;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.logging.Logger;

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
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Explicit "injectables"
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private Object validatorFactory;
	private DataSource dataSource;
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private final PersistenceUnitDescriptor persistenceUnit;
	private final SettingsImpl settings = new SettingsImpl();
	private final StandardServiceRegistryBuilder serviceRegistryBuilder;
	private final Map configurationValues;

	private final List<GrantedPermission> grantedJaccPermissions = new ArrayList<GrantedPermission>();
	private final List<CacheRegionDefinition> cacheRegionDefinitions = new ArrayList<CacheRegionDefinition>();
	// todo : would much prefer this as a local variable...
	private final List<JaxbHibernateConfiguration.JaxbSessionFactory.JaxbMapping> cfgXmlNamedMappings = new ArrayList<JaxbHibernateConfiguration.JaxbSessionFactory.JaxbMapping>();
	private Interceptor sessionFactoryInterceptor;
	private NamingStrategy namingStrategy;
	private SessionFactoryObserver suppliedSessionFactoryObserver;

	private MetadataSources metadataSources;
	private Configuration hibernateConfiguration;

	private static EntityNotFoundDelegate jpaEntityNotFoundDelegate = new JpaEntityNotFoundDelegate();
	
	private ClassLoader providedClassLoader;

	private static class JpaEntityNotFoundDelegate implements EntityNotFoundDelegate, Serializable {
		public void handleEntityNotFound(String entityName, Serializable id) {
			throw new EntityNotFoundException( "Unable to find " + entityName  + " with id " + id );
		}
	}

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
			integrationSettings = Collections.emptyMap();
		}
		
		this.providedClassLoader = providedClassLoader;

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

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Next we do a preliminary pass at metadata processing, which involves:
		//		1) scanning
		final ScanResult scanResult = scan( bootstrapServiceRegistry );
		final DeploymentResources deploymentResources = buildDeploymentResources( scanResult, bootstrapServiceRegistry );
		//		2) building a Jandex index
		final IndexView jandexIndex = locateOrBuildJandexIndex( deploymentResources );
		//		3) building "metadata sources" to keep for later to use in building the SessionFactory
		metadataSources = prepareMetadataSources( jandexIndex, deploymentResources, bootstrapServiceRegistry );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		withValidatorFactory( configurationValues.get( AvailableSettings.VALIDATION_FACTORY ) );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// push back class transformation to the environment; for the time being this only has any effect in EE
		// container situations, calling back into PersistenceUnitInfo#addClassTransformer
		final boolean useClassTransformer = "true".equals( configurationValues.remove( AvailableSettings.USE_CLASS_ENHANCER ) );
		if ( useClassTransformer ) {
			persistenceUnit.pushClassTransformer( metadataSources.collectMappingClassNames() );
		}
	}

	private static interface DeploymentResources {
		public Iterable<ClassDescriptor> getClassDescriptors();
		public Iterable<PackageDescriptor> getPackageDescriptors();
		public Iterable<MappingFileDescriptor> getMappingFileDescriptors();
	}

	private DeploymentResources buildDeploymentResources(
			ScanResult scanResult,
			BootstrapServiceRegistry bootstrapServiceRegistry) {

		// mapping files ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		final ArrayList<MappingFileDescriptor> mappingFileDescriptors = new ArrayList<MappingFileDescriptor>();

		final Set<String> nonLocatedMappingFileNames = new HashSet<String>();
		final List<String> explicitMappingFileNames = persistenceUnit.getMappingFileNames();
		if ( explicitMappingFileNames != null ) {
			nonLocatedMappingFileNames.addAll( explicitMappingFileNames );
		}

		for ( MappingFileDescriptor mappingFileDescriptor : scanResult.getLocatedMappingFiles() ) {
			mappingFileDescriptors.add( mappingFileDescriptor );
			nonLocatedMappingFileNames.remove( mappingFileDescriptor.getName() );
		}

		for ( String name : nonLocatedMappingFileNames ) {
			MappingFileDescriptor descriptor = buildMappingFileDescriptor( name, bootstrapServiceRegistry );
			mappingFileDescriptors.add( descriptor );
		}


		// classes and packages ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		final HashMap<String, ClassDescriptor> classDescriptorMap = new HashMap<String, ClassDescriptor>();
		final HashMap<String, PackageDescriptor> packageDescriptorMap = new HashMap<String, PackageDescriptor>();

		for ( ClassDescriptor classDescriptor : scanResult.getLocatedClasses() ) {
			classDescriptorMap.put( classDescriptor.getName(), classDescriptor );
		}

		for ( PackageDescriptor packageDescriptor : scanResult.getLocatedPackages() ) {
			packageDescriptorMap.put( packageDescriptor.getName(), packageDescriptor );
		}

		final List<String> explicitClassNames = persistenceUnit.getManagedClassNames();
		if ( explicitClassNames != null ) {
			for ( String explicitClassName : explicitClassNames ) {
				// IMPL NOTE : explicitClassNames can contain class or package names!!!
				if ( classDescriptorMap.containsKey( explicitClassName ) ) {
					continue;
				}
				if ( packageDescriptorMap.containsKey( explicitClassName ) ) {
					continue;
				}

				// try it as a class name first...
				final String classFileName = explicitClassName.replace( '.', '/' ) + ".class";
				final URL classFileUrl = bootstrapServiceRegistry.getService( ClassLoaderService.class )
						.locateResource( classFileName );
				if ( classFileUrl != null ) {
					classDescriptorMap.put(
							explicitClassName,
							new ClassDescriptorImpl( explicitClassName, new UrlInputStreamAccess( classFileUrl ) )
					);
					continue;
				}

				// otherwise, try it as a package name
				final String packageInfoFileName = explicitClassName.replace( '.', '/' ) + "/package-info.class";
				final URL packageInfoFileUrl = bootstrapServiceRegistry.getService( ClassLoaderService.class )
						.locateResource( packageInfoFileName );
				if ( packageInfoFileUrl != null ) {
					packageDescriptorMap.put(
							explicitClassName,
							new PackageDescriptorImpl( explicitClassName, new UrlInputStreamAccess( packageInfoFileUrl ) )
					);
					continue;
				}

				LOG.debugf(
						"Unable to resolve class [%s] named in persistence unit [%s]",
						explicitClassName,
						persistenceUnit.getName()
				);
			}
		}

		return new DeploymentResources() {
			@Override
			public Iterable<ClassDescriptor> getClassDescriptors() {
				return classDescriptorMap.values();
			}

			@Override
			public Iterable<PackageDescriptor> getPackageDescriptors() {
				return packageDescriptorMap.values();
			}

			@Override
			public Iterable<MappingFileDescriptor> getMappingFileDescriptors() {
				return mappingFileDescriptors;
			}
		};
	}

	private MappingFileDescriptor buildMappingFileDescriptor(
			String name,
			BootstrapServiceRegistry bootstrapServiceRegistry) {
		final URL url = bootstrapServiceRegistry.getService( ClassLoaderService.class ).locateResource( name );
		if ( url == null ) {
			throw persistenceException( "Unable to resolve named mapping-file [" + name + "]" );
		}

		return new MappingFileDescriptorImpl( name, new UrlInputStreamAccess( url ) );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// temporary!
	@SuppressWarnings("unchecked")
	public Map getConfigurationValues() {
		return Collections.unmodifiableMap( configurationValues );
	}

	public Configuration getHibernateConfiguration() {
		return hibernateConfiguration;
	}
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


	@SuppressWarnings("unchecked")
	private MetadataSources prepareMetadataSources(
			IndexView jandexIndex,
			DeploymentResources deploymentResources,
			BootstrapServiceRegistry bootstrapServiceRegistry) {
		// todo : this needs to tie into the metamodel branch...
		MetadataSources metadataSources = new MetadataSources();

		for ( ClassDescriptor classDescriptor : deploymentResources.getClassDescriptors() ) {
			final String className = classDescriptor.getName();
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
								JandexHelper.getValue( converterAnnotation, "autoApply", boolean.class,
										bootstrapServiceRegistry.getService( ClassLoaderService.class ) )
						)
				);
			}
			else {
				metadataSources.annotatedMappingClassNames.add( className );
			}
		}

		for ( PackageDescriptor packageDescriptor : deploymentResources.getPackageDescriptors() ) {
			metadataSources.packageNames.add( packageDescriptor.getName() );
		}

		for ( MappingFileDescriptor mappingFileDescriptor : deploymentResources.getMappingFileDescriptors() ) {
			metadataSources.namedMappingFileInputStreams.add( mappingFileDescriptor.getStreamAccess().asNamedInputStream() );
		}

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

	private IndexView locateOrBuildJandexIndex(DeploymentResources deploymentResources) {
		// for now create a whole new Index to work with, eventually we need to:
		//		1) accept an Index as an incoming config value
		//		2) pass that Index along to the metamodel code...
		IndexView jandexIndex = (IndexView) configurationValues.get( JANDEX_INDEX );
		if ( jandexIndex == null ) {
			jandexIndex = buildJandexIndex( deploymentResources );
		}
		return jandexIndex;
	}

	private IndexView buildJandexIndex(DeploymentResources deploymentResources) {
		Indexer indexer = new Indexer();

		for ( ClassDescriptor classDescriptor : deploymentResources.getClassDescriptors() ) {
			indexStream( indexer, classDescriptor.getStreamAccess() );
		}

		for ( PackageDescriptor packageDescriptor : deploymentResources.getPackageDescriptors() ) {
			indexStream( indexer, packageDescriptor.getStreamAccess() );
		}

		// for now we just skip entities defined in (1) orm.xml files and (2) hbm.xml files.  this part really needs
		// metamodel branch...

		// for now, we also need to wrap this in a CompositeIndex until Jandex is updated to use a common interface
		// between the 2...
		return indexer.complete();
	}

	private void indexStream(Indexer indexer, InputStreamAccess streamAccess) {
		try {
			InputStream stream = streamAccess.accessInputStream();
			try {
				indexer.index( stream );
			}
			finally {
				try {
					stream.close();
				}
				catch (Exception ignore) {
				}
			}
		}
		catch ( IOException e ) {
			throw persistenceException( "Unable to index from stream " + streamAccess.getStreamName(), e );
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
			for ( Integrator integrator : integratorProvider.getIntegrators() ) {
				bootstrapServiceRegistryBuilder.with( integrator );
			}
		}
		
		final StrategyRegistrationProviderList strategyRegistrationProviderList
				= (StrategyRegistrationProviderList) integrationSettings.get( STRATEGY_REGISTRATION_PROVIDERS );
		if ( strategyRegistrationProviderList != null ) {
			for ( StrategyRegistrationProvider strategyRegistrationProvider : strategyRegistrationProviderList
					.getStrategyRegistrationProviders() ) {
				bootstrapServiceRegistryBuilder.withStrategySelectors( strategyRegistrationProvider );
			}
		}

		// TODO: If providedClassLoader is present (OSGi, etc.) *and*
		// an APP_CLASSLOADER is provided, should throw an exception or
		// warn?
		ClassLoader classLoader;
		ClassLoader appClassLoader = (ClassLoader) integrationSettings.get( org.hibernate.cfg.AvailableSettings.APP_CLASSLOADER );
		if ( providedClassLoader != null ) {
			classLoader = providedClassLoader;
		}
		else if ( appClassLoader != null ) {
			classLoader = appClassLoader;
		}
		else {
			classLoader = persistenceUnit.getClassLoader();
		}
		bootstrapServiceRegistryBuilder.with( classLoader );

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

		final String cfgXmlResourceName1 = (String) merged.remove( AvailableSettings.CFG_FILE );
		if ( StringHelper.isNotEmpty( cfgXmlResourceName1 ) ) {
			// it does, so load those properties
			JaxbHibernateConfiguration configurationElement = configLoaderHolder.getValue()
					.loadConfigXmlResource( cfgXmlResourceName1 );
			processHibernateConfigurationElement( configurationElement, merged );
		}

		// see if integration settings named a Hibernate config file....
		final String cfgXmlResourceName2 = (String) integrationSettings.get( AvailableSettings.CFG_FILE );
		if ( StringHelper.isNotEmpty( cfgXmlResourceName2 ) ) {
			integrationSettings.remove( AvailableSettings.CFG_FILE );
			// it does, so load those properties
			JaxbHibernateConfiguration configurationElement = configLoaderHolder.getValue().loadConfigXmlResource(
					cfgXmlResourceName2
			);
			processHibernateConfigurationElement( configurationElement, merged );
		}

		// finally, apply integration-supplied settings (per JPA spec, integration settings should override other sources)
		merged.putAll( integrationSettings );

		if ( !merged.containsKey( AvailableSettings.VALIDATION_MODE ) ) {
			if ( persistenceUnit.getValidationMode() != null ) {
				merged.put( AvailableSettings.VALIDATION_MODE, persistenceUnit.getValidationMode() );
			}
		}

		if ( !merged.containsKey( AvailableSettings.SHARED_CACHE_MODE ) ) {
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
			for ( JaxbHibernateConfiguration.JaxbSecurity.JaxbGrant grant : configurationElement.getSecurity().getGrant() ) {
				grantedJaccPermissions.add(
						new GrantedPermission(
								grant.getRole(),
								grant.getEntityName(),
								grant.getActions()
						)
				);
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

			grantedJaccPermissions.add( new GrantedPermission( role, clazz, (String) value ) );
		}
		catch ( IndexOutOfBoundsException e ) {
			throw persistenceException( "Illegal usage of " + AvailableSettings.JACC_PREFIX + ": " + key );
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
	private ScanResult scan(BootstrapServiceRegistry bootstrapServiceRegistry) {
		final Scanner scanner = locateOrBuildScanner( bootstrapServiceRegistry );
		final ScanOptions scanOptions = determineScanOptions();

		return scanner.scan( persistenceUnit, scanOptions );
	}

	private ScanOptions determineScanOptions() {
		return new StandardScanOptions(
				(String) configurationValues.get( AvailableSettings.AUTODETECTION ),
				persistenceUnit.isExcludeUnlistedClasses()
		);
	}

	@SuppressWarnings("unchecked")
	private Scanner locateOrBuildScanner(BootstrapServiceRegistry bootstrapServiceRegistry) {
		final Object value = configurationValues.remove( AvailableSettings.SCANNER );
		if ( value == null ) {
			return new StandardScanner();
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

	@Override
	public void generateSchema() {
		processProperties();

		final ServiceRegistry serviceRegistry = buildServiceRegistry();
		final ClassLoaderService classLoaderService = serviceRegistry.getService( ClassLoaderService.class );

		// IMPL NOTE : TCCL handling here is temporary.
		//		It is needed because this code still uses Hibernate Configuration and Hibernate commons-annotations
		// 		in turn which relies on TCCL being set.

		( (ClassLoaderServiceImpl) classLoaderService ).withTccl(
				new ClassLoaderServiceImpl.Work() {
					@Override
					public Object perform() {
						final Configuration hibernateConfiguration = buildHibernateConfiguration( serviceRegistry );
						
						// This seems overkill, but building the SF is necessary to get the Integrators to kick in.
						// Metamodel will clean this up...
						try {
							hibernateConfiguration.buildSessionFactory( serviceRegistry );
						}
						catch (MappingException e) {
							throw persistenceException( "Unable to build Hibernate SessionFactory", e );
						}
						
						JpaSchemaGenerator.performGeneration( hibernateConfiguration, serviceRegistry );
						
						return null;
					}
				}
		);

		// release this builder
		cancel();
	}

	@SuppressWarnings("unchecked")
	public EntityManagerFactory build() {
		processProperties();

		final ServiceRegistry serviceRegistry = buildServiceRegistry();
		final ClassLoaderService classLoaderService = serviceRegistry.getService( ClassLoaderService.class );

		// IMPL NOTE : TCCL handling here is temporary.
		//		It is needed because this code still uses Hibernate Configuration and Hibernate commons-annotations
		// 		in turn which relies on TCCL being set.

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
						
						// must do after buildSessionFactory to let the Integrators kick in
						JpaSchemaGenerator.performGeneration( hibernateConfiguration, serviceRegistry );

						if ( suppliedSessionFactoryObserver != null ) {
							sessionFactory.addObserver( suppliedSessionFactoryObserver );
						}
						sessionFactory.addObserver( new ServiceRegistryCloser() );

						// NOTE : passing cfg is temporary until
						return new EntityManagerFactoryImpl(
								persistenceUnit.getName(),
								sessionFactory,
								settings,
								configurationValues,
								hibernateConfiguration
						);
					}
				}
		);
	}

	private void processProperties() {
		applyJdbcConnectionProperties();
		applyTransactionProperties();

		Object validationFactory = this.validatorFactory;
		if ( validationFactory == null ) {
			validationFactory = configurationValues.get( AvailableSettings.VALIDATION_FACTORY );
		}
		if ( validationFactory != null ) {
			BeanValidationIntegrator.validateFactory( validationFactory );
			serviceRegistryBuilder.applySetting( AvailableSettings.VALIDATION_FACTORY, validationFactory );
			configurationValues.put( AvailableSettings.VALIDATION_FACTORY, this.validatorFactory );
		}

		// flush before completion validation
		if ( "true".equals( configurationValues.get( Environment.FLUSH_BEFORE_COMPLETION ) ) ) {
			serviceRegistryBuilder.applySetting( Environment.FLUSH_BEFORE_COMPLETION, "false" );
			LOG.definingFlushBeforeCompletionIgnoredInHem( Environment.FLUSH_BEFORE_COMPLETION );
		}

		final StrategySelector strategySelector = serviceRegistryBuilder.getBootstrapServiceRegistry().getService( StrategySelector.class );

		for ( Object oEntry : configurationValues.entrySet() ) {
			Map.Entry entry = (Map.Entry) oEntry;
			if ( entry.getKey() instanceof String ) {
				final String keyString = (String) entry.getKey();

				if ( AvailableSettings.INTERCEPTOR.equals( keyString ) ) {
					sessionFactoryInterceptor = strategySelector.resolveStrategy( Interceptor.class, entry.getValue() );
				}
				else if ( AvailableSettings.SESSION_INTERCEPTOR.equals( keyString ) ) {
					settings.setSessionInterceptorClass(
							loadSessionInterceptorClass( entry.getValue(), strategySelector )
					);
				}
				else if ( AvailableSettings.NAMING_STRATEGY.equals( keyString ) ) {
					namingStrategy = strategySelector.resolveStrategy( NamingStrategy.class, entry.getValue() );
				}
				else if ( AvailableSettings.SESSION_FACTORY_OBSERVER.equals( keyString ) ) {
					suppliedSessionFactoryObserver = strategySelector.resolveStrategy( SessionFactoryObserver.class, entry.getValue() );
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
		if ( dataSource != null ) {
			serviceRegistryBuilder.applySetting( org.hibernate.cfg.AvailableSettings.DATASOURCE, dataSource );
		}
		else if ( persistenceUnit.getJtaDataSource() != null ) {
			if ( ! serviceRegistryBuilder.getSettings().containsKey( org.hibernate.cfg.AvailableSettings.DATASOURCE ) ) {
				serviceRegistryBuilder.applySetting( org.hibernate.cfg.AvailableSettings.DATASOURCE, persistenceUnit.getJtaDataSource() );
				// HHH-8121 : make the PU-defined value available to EMF.getProperties()
				configurationValues.put( AvailableSettings.JTA_DATASOURCE, persistenceUnit.getJtaDataSource() );
			}
		}
		else if ( persistenceUnit.getNonJtaDataSource() != null ) {
			if ( ! serviceRegistryBuilder.getSettings().containsKey( org.hibernate.cfg.AvailableSettings.DATASOURCE ) ) {
				serviceRegistryBuilder.applySetting( org.hibernate.cfg.AvailableSettings.DATASOURCE, persistenceUnit.getNonJtaDataSource() );
				// HHH-8121 : make the PU-defined value available to EMF.getProperties()
				configurationValues.put( AvailableSettings.NON_JTA_DATASOURCE, persistenceUnit.getNonJtaDataSource() );
			}
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

	@SuppressWarnings("unchecked")
	private Class<? extends Interceptor> loadSessionInterceptorClass(Object value, StrategySelector strategySelector) {
		if ( value == null ) {
			return null;
		}

		return Class.class.isInstance( value )
				? (Class<? extends Interceptor>) value
				: strategySelector.selectStrategyImplementor( Interceptor.class, value.toString() );
	}

	public ServiceRegistry buildServiceRegistry() {
		return serviceRegistryBuilder.build();
	}

	public Configuration buildHibernateConfiguration(ServiceRegistry serviceRegistry) {
		Properties props = new Properties();
		props.putAll( configurationValues );
		Configuration cfg = new Configuration();
		cfg.getProperties().putAll( props );

		cfg.setEntityNotFoundDelegate( jpaEntityNotFoundDelegate );

		if ( namingStrategy != null ) {
			cfg.setNamingStrategy( namingStrategy );
		}

		if ( sessionFactoryInterceptor != null ) {
			cfg.setInterceptor( sessionFactoryInterceptor );
		}

		final Object strategyProviderValue = props.get( AvailableSettings.IDENTIFIER_GENERATOR_STRATEGY_PROVIDER );
		final IdentifierGeneratorStrategyProvider strategyProvider = strategyProviderValue == null
				? null
				: serviceRegistry.getService( StrategySelector.class )
						.resolveStrategy( IdentifierGeneratorStrategyProvider.class, strategyProviderValue );

		if ( strategyProvider != null ) {
			final MutableIdentifierGeneratorFactory identifierGeneratorFactory = cfg.getIdentifierGeneratorFactory();
			for ( Map.Entry<String,Class<?>> entry : strategyProvider.getStrategies().entrySet() ) {
				identifierGeneratorFactory.register( entry.getKey(), entry.getValue() );
			}
		}

		if ( grantedJaccPermissions != null ) {
			final JaccService jaccService = serviceRegistry.getService( JaccService.class );
			for ( GrantedPermission grantedPermission : grantedJaccPermissions ) {
				jaccService.addPermission( grantedPermission );
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
				if ( AttributeConverter.class.isAssignableFrom( cls ) ) {
					cfg.addAttributeConverter( (Class<? extends AttributeConverter>) cls );
				}
				else {
					cfg.addAnnotatedClass( cls );
				}
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
			catch ( InvalidMappingException e ) {
				// try our best to give the file name
				if ( StringHelper.isNotEmpty( namedInputStream.getName() ) ) {
					throw new InvalidMappingException(
							"Error while parsing file: " + namedInputStream.getName(),
							e.getType(),
							e.getPath(),
							e
					);
				}
				else {
					throw e;
				}
			}
			catch (MappingException me) {
				// try our best to give the file name
				if ( StringHelper.isNotEmpty( namedInputStream.getName() ) ) {
					throw new MappingException("Error while parsing file: " + namedInputStream.getName(), me );
				}
				else {
					throw me;
				}
			}
		}
		for ( String packageName : metadataSources.packageNames ) {
			cfg.addPackage( packageName );
		}
		
		final TypeContributorList typeContributorList
				= (TypeContributorList) configurationValues.get( TYPE_CONTRIBUTORS );
		if ( typeContributorList != null ) {
			configurationValues.remove( TYPE_CONTRIBUTORS );
			for ( TypeContributor typeContributor : typeContributorList.getTypeContributors() ) {
				cfg.registerTypeContributor( typeContributor );
			}
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
