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
import java.util.Set;
import java.util.StringTokenizer;
import javax.persistence.AttributeConverter;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityNotFoundException;
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
import org.hibernate.metamodel.Metadata;
import org.hibernate.metamodel.MetadataBuilder;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.SessionFactoryBuilder;
import org.hibernate.metamodel.internal.source.annotations.util.JPADotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.metamodel.spi.TypeContributor;
import org.hibernate.metamodel.spi.source.InvalidMappingException;
import org.hibernate.metamodel.spi.source.MappingException;
import org.hibernate.metamodel.spi.source.MappingNotFoundException;
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
	private final List<CacheRegionDefinition> localCacheRegionDefinitions = new ArrayList<CacheRegionDefinition>();
	// todo : would much prefer this as a local variable...
	private final List<JaxbHibernateConfiguration.JaxbSessionFactory.JaxbMapping> cfgXmlNamedMappings = new ArrayList<JaxbHibernateConfiguration.JaxbSessionFactory.JaxbMapping>();

	private IndexView jandexIndex;

	private LocalMetadataSources localMetadataSources;

	private static EntityNotFoundDelegate jpaEntityNotFoundDelegate = new JpaEntityNotFoundDelegate();
	
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

		// Build the boot-strap service registry, which mainly handles class loader interactions
		final BootstrapServiceRegistry bsr = buildBootstrapServiceRegistry( integrationSettings, providedClassLoader );

		// Create the builder for the "standard" service registry (so we can start adding configuration values, etc)
		this.serviceRegistryBuilder = new StandardServiceRegistryBuilder( bsr );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Next we build a merged map of all the configuration values
		this.configurationValues = mergePropertySources( persistenceUnit, integrationSettings, bsr );
		// add all merged configuration values into the service registry builder
		this.serviceRegistryBuilder.applySettings( configurationValues );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Next we do a preliminary pass at metadata processing, which involves:
		//		1) scanning
		final ScanResult scanResult = scan( bsr );
		final DeploymentResources deploymentResources = buildDeploymentResources( scanResult, bsr );
		//		2) building a Jandex index
		jandexIndex = locateOrBuildJandexIndex( deploymentResources );
		//		3) building "metadata sources" to keep for later to use in building the SessionFactory
		localMetadataSources = prepareMetadataSources( jandexIndex, deploymentResources, bsr );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		withValidatorFactory( configurationValues.get( AvailableSettings.VALIDATION_FACTORY ) );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// push back class transformation to the environment; for the time being this only has any effect in EE
		// container situations, calling back into PersistenceUnitInfo#addClassTransformer
		final boolean useClassTransformer = "true".equals( configurationValues.remove( AvailableSettings.USE_CLASS_ENHANCER ) );
		if ( useClassTransformer ) {
			persistenceUnit.pushClassTransformer( localMetadataSources.collectMappingClassNames() );
		}
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
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


	@SuppressWarnings("unchecked")
	private LocalMetadataSources prepareMetadataSources(
			IndexView jandexIndex,
			DeploymentResources deploymentResources,
			BootstrapServiceRegistry bsr) {
		LocalMetadataSources localMetadataSources = new LocalMetadataSources();

		for ( ClassDescriptor classDescriptor : deploymentResources.getClassDescriptors() ) {
			final String className = classDescriptor.getName();
			final ClassInfo classInfo = jandexIndex.getClassByName( DotName.createSimple( className ) );
			if ( classInfo == null ) {
				// Not really sure what this means.  Most likely it is explicitly listed in the persistence unit,
				// but mapped via mapping file.  Anyway assume its a mapping class...
				localMetadataSources.annotatedMappingClassNames.add( className );
				continue;
			}

			// logic here assumes an entity is not also a converter...
			AnnotationInstance converterAnnotation = JandexHelper.getSingleAnnotation(
					classInfo.annotations(),
					JPADotNames.CONVERTER
			);
			if ( converterAnnotation != null ) {
				localMetadataSources.converterDescriptors.add(
						new LocalMetadataSources.ConverterDescriptor(
								className,
								JandexHelper.getValue(
										converterAnnotation, "autoApply", boolean.class,
										bsr.getService( ClassLoaderService.class )
								)
						)
				);
			}
			else {
				localMetadataSources.annotatedMappingClassNames.add( className );
			}
		}

		for ( PackageDescriptor packageDescriptor : deploymentResources.getPackageDescriptors() ) {
			localMetadataSources.packageNames.add( packageDescriptor.getName() );
		}

		for ( MappingFileDescriptor mappingFileDescriptor : deploymentResources.getMappingFileDescriptors() ) {
			localMetadataSources.inputStreamAccessList.add( mappingFileDescriptor.getStreamAccess() );
		}

		final String explicitHbmXmls = (String) configurationValues.remove( AvailableSettings.HBXML_FILES );
		if ( explicitHbmXmls != null ) {
			localMetadataSources.mappingFileResources.addAll( Arrays.asList( StringHelper.split( ", ", explicitHbmXmls ) ) );
		}

		final List<String> explicitOrmXml = (List<String>) configurationValues.remove( AvailableSettings.XML_FILE_NAMES );
		if ( explicitOrmXml != null ) {
			localMetadataSources.mappingFileResources.addAll( explicitOrmXml );
		}

		return localMetadataSources;
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

		boolean addedAny = false;

		for ( ClassDescriptor classDescriptor : deploymentResources.getClassDescriptors() ) {
			addedAny = true;
			indexStream( indexer, classDescriptor.getStreamAccess() );
		}

		for ( PackageDescriptor packageDescriptor : deploymentResources.getPackageDescriptors() ) {
			addedAny = true;
			indexStream( indexer, packageDescriptor.getStreamAccess() );
		}

		// for now we just skip entities defined in (1) orm.xml files and (2) hbm.xml files.  this part really needs
		// metamodel branch...

		// for now, we also need to wrap this in a CompositeIndex until Jandex is updated to use a common interface
		// between the 2...

		return addedAny ? indexer.complete() : null;
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
				localCacheRegionDefinitions.add(
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
				final JaxbHibernateConfiguration.JaxbSessionFactory.JaxbCollectionCache jaxbCollectionCache
						= (JaxbHibernateConfiguration.JaxbSessionFactory.JaxbCollectionCache) cacheDeclaration;
				localCacheRegionDefinitions.add(
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

	private void addCacheRegionDefinition(String role, String value, CacheRegionDefinition.CacheRegionType cacheType) {
		final StringTokenizer params = new StringTokenizer( value, ";, " );
		if ( !params.hasMoreTokens() ) {
			StringBuilder error = new StringBuilder( "Illegal usage of " );
			if ( cacheType == CacheRegionDefinition.CacheRegionType.ENTITY ) {
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
		if ( cacheType == CacheRegionDefinition.CacheRegionType.ENTITY ) {
			if ( params.hasMoreTokens() ) {
				lazyProperty = "all".equalsIgnoreCase( params.nextToken() );
			}
		}
		else {
			lazyProperty = false;
		}

		final CacheRegionDefinition def = new CacheRegionDefinition( cacheType, role, usage, region, lazyProperty );
		localCacheRegionDefinitions.add( def );
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

		final StandardServiceRegistry ssr = serviceRegistryBuilder.build();

		final MetadataSources metadataSources = new MetadataSources( ssr );
		populate( metadataSources, ssr );

		final MetadataBuilder metamodelBuilder = metadataSources.getMetadataBuilder( ssr );
		populate( metamodelBuilder, ssr );

		final MetadataImplementor metadata = (MetadataImplementor) metamodelBuilder.build();

		// This seems overkill, but building the SF is necessary to get the Integrators to kick in.
		// Metamodel will clean this up...
		try {
			SessionFactoryBuilder sfBuilder = metadata.getSessionFactoryBuilder();
			populate( sfBuilder, ssr );
			sfBuilder.build();
		}
		catch (MappingException e) {
			throw persistenceException( "Unable to build Hibernate SessionFactory", e );
		}

		JpaSchemaGenerator.performGeneration( metadata, configurationValues, ssr );

		// release this builder
		cancel();
	}

	@SuppressWarnings("unchecked")
	public EntityManagerFactory build() {
		processProperties();

		final StandardServiceRegistry ssr = serviceRegistryBuilder.build();
		configure( ssr );

		final MetadataSources metadataSources = new MetadataSources( ssr );
		populate( metadataSources, ssr );

		final MetadataBuilder metamodelBuilder = metadataSources.getMetadataBuilder( ssr );
		populate( metamodelBuilder, ssr );

		final Metadata metadata = metamodelBuilder.build();

		SessionFactoryBuilder sfBuilder = metadata.getSessionFactoryBuilder();
		populate( sfBuilder, ssr );

		SessionFactoryImplementor sessionFactory;
		try {
			sessionFactory = (SessionFactoryImplementor) metadata.buildSessionFactory();
		}
		catch (MappingException e) {
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

	private void configure(StandardServiceRegistry ssr) {
		final StrategySelector strategySelector = ssr.getService( StrategySelector.class );

		// apply any JACC permissions
		final JaccService jaccService = ssr.getService( JaccService.class );
		if ( jaccService == null ) {
			// JACC not enabled
			if ( !grantedJaccPermissions.isEmpty() ) {
				// todo : warn?
				LOG.debugf( "JACC Service not enabled, but JACC permissions specified" );
			}
		}
		else {
			for ( GrantedPermission grantedPermission : grantedJaccPermissions ) {
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

	private void populate(MetadataSources metadataSources, StandardServiceRegistry ssr) {
		final ClassLoaderService classLoaderService = ssr.getService( ClassLoaderService.class );

		for ( JaxbHibernateConfiguration.JaxbSessionFactory.JaxbMapping jaxbMapping : cfgXmlNamedMappings ) {
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

		List<Class> loadedAnnotatedClasses = (List<Class>) configurationValues.remove( AvailableSettings.LOADED_CLASSES );
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

		for ( String className : localMetadataSources.getAnnotatedMappingClassNames() ) {
			metadataSources.addAnnotatedClassName( className );
		}

		for ( LocalMetadataSources.ConverterDescriptor converterDescriptor : localMetadataSources.getConverterDescriptors() ) {
			try {
				Class theClass = classLoaderService.classForName( converterDescriptor.converterClassName );
				metadataSources.addAttributeConverter(
						(Class<? extends AttributeConverter>) theClass,
						converterDescriptor.autoApply
				);
			}
			catch (ClassCastException e) {
				throw persistenceException(
						String.format(
								"AttributeConverter implementation [%s] does not implement AttributeConverter interface",
								converterDescriptor.converterClassName
						)
				);
			}
		}

		for ( String resourceName : localMetadataSources.mappingFileResources ) {
			Boolean useMetaInf = null;
			try {
				if ( resourceName.endsWith( META_INF_ORM_XML ) ) {
					useMetaInf = true;
				}
				metadataSources.addResource( resourceName );
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

		for ( InputStreamAccess inputStreamAccess : localMetadataSources.inputStreamAccessList ) {
			try {
				//addInputStream has the responsibility to close the stream
				metadataSources.addInputStream( new BufferedInputStream( inputStreamAccess.accessInputStream() ) );
			}
			catch ( InvalidMappingException e ) {
				throw persistenceException(
						String.format(
								"Error parsing mapping from stream : %s - %s ",
								inputStreamAccess.getStreamName(),
								e.getOrigin().getName()
						)
				);
			}
			catch (MappingException e) {
				throw persistenceException(
						String.format(
								"Error parsing mapping from stream : %s - %s ",
								inputStreamAccess.getStreamName(),
								e.getOrigin().getName()
						)
				);
			}
		}

		for ( String packageName : localMetadataSources.packageNames ) {
			metadataSources.addPackage( packageName );
		}
	}

	private void populate(MetadataBuilder metamodelBuilder, StandardServiceRegistry ssr) {
		for ( CacheRegionDefinition localCacheRegionDefinition : localCacheRegionDefinitions ) {
			metamodelBuilder.with( localCacheRegionDefinition );
		}

		final Object namingStrategySetting = configurationValues.remove( AvailableSettings.NAMING_STRATEGY );
		if ( namingStrategySetting != null ) {
			final StrategySelector strategySelector = serviceRegistryBuilder.getBootstrapServiceRegistry().getService( StrategySelector.class );
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

		metamodelBuilder.with( jandexIndex );
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

		sfBuilder.with( jpaEntityNotFoundDelegate );
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

				if ( AvailableSettings.SESSION_INTERCEPTOR.equals( keyString ) ) {
					settings.setSessionInterceptorClass(
							loadSessionInterceptorClass( entry.getValue(), strategySelector )
					);
				}
				else if ( AvailableSettings.DISCARD_PC_ON_CLOSE.equals( keyString ) ) {
					settings.setReleaseResourcesOnCloseEnabled( "true".equals( entry.getValue() ) );
				}
				else if ( keyString.startsWith( AvailableSettings.CLASS_CACHE_PREFIX ) ) {
					addCacheRegionDefinition(
							keyString.substring( AvailableSettings.CLASS_CACHE_PREFIX.length() + 1 ),
							(String) entry.getValue(),
							CacheRegionDefinition.CacheRegionType.ENTITY
					);
				}
				else if ( keyString.startsWith( AvailableSettings.COLLECTION_CACHE_PREFIX ) ) {
					addCacheRegionDefinition(
							keyString.substring( AvailableSettings.COLLECTION_CACHE_PREFIX.length() + 1 ),
							(String) entry.getValue(),
							CacheRegionDefinition.CacheRegionType.COLLECTION
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

	public static class LocalMetadataSources {
		private final List<String> annotatedMappingClassNames = new ArrayList<String>();
		private final List<ConverterDescriptor> converterDescriptors = new ArrayList<ConverterDescriptor>();
		private final List<InputStreamAccess> inputStreamAccessList = new ArrayList<InputStreamAccess>();
		private final List<String> mappingFileResources = new ArrayList<String>();
		private final List<String> packageNames = new ArrayList<String>();

		public List<String> getAnnotatedMappingClassNames() {
			return annotatedMappingClassNames;
		}

		public List<ConverterDescriptor> getConverterDescriptors() {
			return converterDescriptors;
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
