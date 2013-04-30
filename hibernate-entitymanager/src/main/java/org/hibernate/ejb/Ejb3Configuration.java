/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.ejb;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import javax.naming.BinaryRefAddr;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityNotFoundException;
import javax.persistence.MappedSuperclass;
import javax.persistence.PersistenceException;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import org.dom4j.Element;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.MappingException;
import org.hibernate.MappingNotFoundException;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.cfg.annotations.reflection.XMLContext;
import org.hibernate.cfg.beanvalidation.BeanValidationIntegrator;
import org.hibernate.ejb.cfg.spi.IdentifierGeneratorStrategyProvider;
import org.hibernate.ejb.connection.InjectedDataSourceConnectionProvider;
import org.hibernate.ejb.event.JpaIntegrator;
import org.hibernate.ejb.instrument.InterceptFieldClassFileTransformer;
import org.hibernate.ejb.internal.EntityManagerMessageLogger;
import org.hibernate.ejb.packaging.JarVisitorFactory;
import org.hibernate.ejb.packaging.NamedInputStream;
import org.hibernate.ejb.packaging.NativeScanner;
import org.hibernate.ejb.packaging.PersistenceMetadata;
import org.hibernate.ejb.packaging.PersistenceXmlLoader;
import org.hibernate.ejb.packaging.Scanner;
import org.hibernate.ejb.util.ConfigurationHelper;
import org.hibernate.ejb.util.LogHelper;
import org.hibernate.ejb.util.NamingHelper;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.engine.transaction.internal.jdbc.JdbcTransactionFactory;
import org.hibernate.engine.transaction.internal.jta.CMTTransactionFactory;
import org.hibernate.id.factory.spi.MutableIdentifierGeneratorFactory;
import org.hibernate.internal.SessionFactoryObserverChain;
import org.hibernate.internal.util.ClassLoaderHelper;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.internal.util.xml.MappingReader;
import org.hibernate.internal.util.xml.OriginImpl;
import org.hibernate.internal.util.xml.XmlDocument;
import org.hibernate.mapping.AuxiliaryDatabaseObject;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.spi.TypeContributor;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.secure.internal.JACCConfiguration;
import org.hibernate.service.BootstrapServiceRegistryBuilder;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;
import org.hibernate.service.internal.StandardServiceRegistryImpl;
import org.hibernate.service.jdbc.connections.internal.DatasourceConnectionProviderImpl;
import org.jboss.logging.Logger;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

/**
 * Allow a fine tuned configuration of an EJB 3.0 EntityManagerFactory
 *
 * A Ejb3Configuration object is only guaranteed to create one EntityManagerFactory.
 * Multiple usage of {@link #buildEntityManagerFactory()} is not guaranteed.
 *
 * After #buildEntityManagerFactory() has been called, you no longer can change the configuration
 * state (no class adding, no property change etc)
 *
 * When serialized / deserialized or retrieved from the JNDI, you no longer can change the
 * configuration state (no class adding, no property change etc)
 *
 * Putting the configuration in the JNDI is an expensive operation that requires a partial
 * serialization
 *
 * @author Emmanuel Bernard
 *
 * @deprecated Direct usage of this class has never been supported.  Instead, the application should obtain reference
 * to the {@link EntityManagerFactory} as outlined in the JPA specification, section <i>7.3 Obtaining an Entity
 * Manager Factory</i> based on runtime environment.  Additionally this class will be removed in Hibernate release
 * 5.0 for the same reasoning outlined on {@link Configuration} due to move towards new
 * {@link org.hibernate.SessionFactory} building methodology.  See
 * <a href="http://opensource.atlassian.com/projects/hibernate/browse/HHH-6181">HHH-6181</a> and
 * <a href="http://opensource.atlassian.com/projects/hibernate/browse/HHH-6159">HHH-6159</a> for details
 */
@Deprecated
@SuppressWarnings( {"JavaDoc"})
public class Ejb3Configuration implements Serializable, Referenceable {

    private static final EntityManagerMessageLogger LOG = Logger.getMessageLogger(
			EntityManagerMessageLogger.class,
			Ejb3Configuration.class.getName()
	);
	private static final String IMPLEMENTATION_NAME = HibernatePersistence.class.getName();
	private static final String META_INF_ORM_XML = "META-INF/orm.xml";
	private static final String PARSED_MAPPING_DOMS = "hibernate.internal.mapping_doms";

	private static EntityNotFoundDelegate ejb3EntityNotFoundDelegate = new Ejb3EntityNotFoundDelegate();
	private static Configuration DEFAULT_CONFIGURATION = new Configuration();

	private static class Ejb3EntityNotFoundDelegate implements EntityNotFoundDelegate, Serializable {
		public void handleEntityNotFound(String entityName, Serializable id) {
			throw new EntityNotFoundException("Unable to find " + entityName  + " with id " + id);
		}
	}

	private String persistenceUnitName;
	private String cfgXmlResource;

	private Configuration cfg;
	//made transient and not restored in deserialization on purpose, should no longer be called after restoration
	private PersistenceUnitTransactionType transactionType;
	private boolean discardOnClose;
	//made transient and not restored in deserialization on purpose, should no longer be called after restoration
	private transient ClassLoader overridenClassLoader;
	private boolean isConfigurationProcessed = false;


	public Ejb3Configuration() {
		overridenClassLoader = ClassLoaderHelper.overridenClassLoader;
		cfg = new Configuration();
		cfg.setEntityNotFoundDelegate( ejb3EntityNotFoundDelegate );
	}

	/**
	 * Used to inject a datasource object as the connection provider.
	 * If used, be sure to <b>not override</b> the hibernate.connection.provider_class
	 * property
	 */
	@SuppressWarnings({ "JavaDoc", "unchecked" })
	public void setDataSource(DataSource ds) {
		if ( ds != null ) {
			cfg.getProperties().put( Environment.DATASOURCE, ds );
			this.setProperty( Environment.CONNECTION_PROVIDER, DatasourceConnectionProviderImpl.class.getName() );
		}
	}

	/**
	 * create a factory from a parsed persistence.xml
	 * Especially the scanning of classes and additional jars is done already at this point.
	 * <p/>
	 * NOTE: public only for unit testing purposes; not a public API!
	 *
	 * @param metadata The information parsed from the persistence.xml
	 * @param overridesIn Any explicitly passed config settings
	 *
	 * @return this
	 */
	@SuppressWarnings({ "unchecked" })
	public Ejb3Configuration configure(PersistenceMetadata metadata, Map overridesIn) {
        LOG.debugf("Creating Factory: %s", metadata.getName());

		Map overrides = new HashMap();
		if ( overridesIn != null ) {
			overrides.putAll( overridesIn );
		}

		Map workingVars = new HashMap();
		workingVars.put( AvailableSettings.PERSISTENCE_UNIT_NAME, metadata.getName() );
		this.persistenceUnitName = metadata.getName();

		if ( StringHelper.isNotEmpty( metadata.getJtaDatasource() ) ) {
			this.setProperty( Environment.DATASOURCE, metadata.getJtaDatasource() );
		}
		else if ( StringHelper.isNotEmpty( metadata.getNonJtaDatasource() ) ) {
			this.setProperty( Environment.DATASOURCE, metadata.getNonJtaDatasource() );
		}
		else {
			final String driver = (String) metadata.getProps().get( AvailableSettings.JDBC_DRIVER );
			if ( StringHelper.isNotEmpty( driver ) ) {
				this.setProperty( Environment.DRIVER, driver );
			}
			final String url = (String) metadata.getProps().get( AvailableSettings.JDBC_URL );
			if ( StringHelper.isNotEmpty( url ) ) {
				this.setProperty( Environment.URL, url );
			}
			final String user = (String) metadata.getProps().get( AvailableSettings.JDBC_USER );
			if ( StringHelper.isNotEmpty( user ) ) {
				this.setProperty( Environment.USER, user );
			}
			final String pass = (String) metadata.getProps().get( AvailableSettings.JDBC_PASSWORD );
			if ( StringHelper.isNotEmpty( pass ) ) {
				this.setProperty( Environment.PASS, pass );
			}
		}
		defineTransactionType( metadata.getTransactionType(), workingVars );
		if ( metadata.getClasses().size() > 0 ) {
			workingVars.put( AvailableSettings.CLASS_NAMES, metadata.getClasses() );
		}
		if ( metadata.getPackages().size() > 0 ) {
			workingVars.put( AvailableSettings.PACKAGE_NAMES, metadata.getPackages() );
		}
		if ( metadata.getMappingFiles().size() > 0 ) {
			workingVars.put( AvailableSettings.XML_FILE_NAMES, metadata.getMappingFiles() );
		}
		if ( metadata.getHbmfiles().size() > 0 ) {
			workingVars.put( AvailableSettings.HBXML_FILES, metadata.getHbmfiles() );
		}

		Properties props = new Properties();
		props.putAll( metadata.getProps() );

		// validation factory
		final Object validationFactory = overrides.get( AvailableSettings.VALIDATION_FACTORY );
		if ( validationFactory != null ) {
			BeanValidationIntegrator.validateFactory( validationFactory );
			props.put( AvailableSettings.VALIDATION_FACTORY, validationFactory );
		}
		overrides.remove( AvailableSettings.VALIDATION_FACTORY );

		// validation-mode (overrides has precedence)
		{
			final Object integrationValue = overrides.get( AvailableSettings.VALIDATION_MODE );
			if ( integrationValue != null ) {
				props.put( AvailableSettings.VALIDATION_MODE, integrationValue.toString() );
			}
			else if ( metadata.getValidationMode() != null ) {
				props.put( AvailableSettings.VALIDATION_MODE, metadata.getValidationMode() );
			}
			overrides.remove( AvailableSettings.VALIDATION_MODE );
		}

		// shared-cache-mode (overrides has precedence)
		{
			final Object integrationValue = overrides.get( AvailableSettings.SHARED_CACHE_MODE );
			if ( integrationValue != null ) {
				props.put( AvailableSettings.SHARED_CACHE_MODE, integrationValue.toString() );
			}
			else if ( metadata.getSharedCacheMode() != null ) {
				props.put( AvailableSettings.SHARED_CACHE_MODE, metadata.getSharedCacheMode() );
			}
			overrides.remove( AvailableSettings.SHARED_CACHE_MODE );
		}

		for ( Map.Entry entry : (Set<Map.Entry>) overrides.entrySet() ) {
			Object value = entry.getValue();
			props.put( entry.getKey(), value == null ? "" :  value ); //alter null, not allowed in properties
		}

		configure( props, workingVars );
		return this;
	}

	/**
	 * Build the configuration from an entity manager name and given the
	 * appropriate extra properties. Those properties override the one get through
	 * the persistence.xml file.
	 * If the persistence unit name is not found or does not match the Persistence Provider, null is returned
	 *
	 * This method is used in a non managed environment
	 *
	 * @param persistenceUnitName persistence unit name
	 * @param integration properties passed to the persistence provider
	 *
	 * @return configured Ejb3Configuration or null if no persistence unit match
	 *
	 * @see HibernatePersistence#createEntityManagerFactory(String, java.util.Map)
	 */
	@SuppressWarnings({ "unchecked" })
	public Ejb3Configuration configure(String persistenceUnitName, Map integration) {
		try {
            LOG.debugf("Look up for persistence unit: %s", persistenceUnitName);
			integration = integration == null ?
					CollectionHelper.EMPTY_MAP :
					Collections.unmodifiableMap( integration );
			Enumeration<URL> xmls = ClassLoaderHelper.getContextClassLoader()
					.getResources( "META-INF/persistence.xml" );
            if (!xmls.hasMoreElements()) LOG.unableToFindPersistenceXmlInClasspath();
			while ( xmls.hasMoreElements() ) {
				URL url = xmls.nextElement();
                LOG.trace("Analyzing persistence.xml: " + url);
				List<PersistenceMetadata> metadataFiles = PersistenceXmlLoader.deploy(
						url,
						integration,
						cfg.getEntityResolver(),
						PersistenceUnitTransactionType.RESOURCE_LOCAL );
				for ( PersistenceMetadata metadata : metadataFiles ) {
                    LOG.trace(metadata);

					if ( metadata.getProvider() == null || IMPLEMENTATION_NAME.equalsIgnoreCase(
							metadata.getProvider()
					) ) {
						//correct provider

						//lazy load the scanner to avoid unnecessary IOExceptions
						Scanner scanner = null;
						URL jarURL = null;
						if ( metadata.getName() == null ) {
							scanner = buildScanner( metadata.getProps(), integration );
							jarURL = JarVisitorFactory.getJarURLFromURLEntry( url, "/META-INF/persistence.xml" );
							metadata.setName( scanner.getUnqualifiedJarName(jarURL) );
						}
						if ( persistenceUnitName == null && xmls.hasMoreElements() ) {
							throw new PersistenceException( "No name provided and several persistence units found" );
						}
						else if ( persistenceUnitName == null || metadata.getName().equals( persistenceUnitName ) ) {
							if (scanner == null) {
								scanner = buildScanner( metadata.getProps(), integration );
								jarURL = JarVisitorFactory.getJarURLFromURLEntry( url, "/META-INF/persistence.xml" );
							}
							//scan main JAR
							ScanningContext mainJarScanCtx = new ScanningContext()
									.scanner( scanner )
									.url( jarURL )
									.explicitMappingFiles( metadata.getMappingFiles() )
									.searchOrm( true );
							setDetectedArtifactsOnScanningContext( mainJarScanCtx, metadata.getProps(), integration,
																				metadata.getExcludeUnlistedClasses() );
							addMetadataFromScan( mainJarScanCtx, metadata );

							ScanningContext otherJarScanCtx = new ScanningContext()
									.scanner( scanner )
									.explicitMappingFiles( metadata.getMappingFiles() )
									.searchOrm( true );
							setDetectedArtifactsOnScanningContext( otherJarScanCtx, metadata.getProps(), integration,
																				false );
							for ( String jarFile : metadata.getJarFiles() ) {
								otherJarScanCtx.url( JarVisitorFactory.getURLFromPath( jarFile ) );
								addMetadataFromScan( otherJarScanCtx, metadata );
							}
							return configure( metadata, integration );
						}
					}
				}
			}
			return null;
		}
		catch (Exception e) {
			if ( e instanceof PersistenceException) {
				throw (PersistenceException) e;
			}
			else {
				throw new PersistenceException( getExceptionHeader() + "Unable to configure EntityManagerFactory", e );
			}
		}
	}

	private Scanner buildScanner(Properties properties, Map<?,?> integration) {
		//read the String or Instance from the integration map first and use the properties as a backup.
		Object scanner = integration.get( AvailableSettings.SCANNER );
		if (scanner == null) {
			scanner = properties.getProperty( AvailableSettings.SCANNER );
		}
		if (scanner != null) {
			Class<?> scannerClass;
			if ( scanner instanceof String ) {
				try {
					scannerClass = ReflectHelper.classForName( (String) scanner, this.getClass() );
				}
				catch ( ClassNotFoundException e ) {
					throw new PersistenceException(  "Cannot find scanner class. " + AvailableSettings.SCANNER + "=" + scanner, e );
				}
			}
			else if (scanner instanceof Class) {
				scannerClass = (Class<? extends Scanner>) scanner;
			}
			else if (scanner instanceof Scanner) {
				return (Scanner) scanner;
			}
			else {
				throw new PersistenceException(  "Scanner class configuration error: unknown type on the property. " + AvailableSettings.SCANNER );
			}
			try {
				return (Scanner) scannerClass.newInstance();
			}
			catch ( InstantiationException e ) {
				throw new PersistenceException(  "Unable to load Scanner class: " + scannerClass, e );
			}
			catch ( IllegalAccessException e ) {
				throw new PersistenceException(  "Unable to load Scanner class: " + scannerClass, e );
			}
		}
		else {
			return new NativeScanner();
		}
	}

	private static class ScanningContext {
		//boolean excludeUnlistedClasses;
		private Scanner scanner;
		private URL url;
		private List<String> explicitMappingFiles;
		private boolean detectClasses;
		private boolean detectHbmFiles;
		private boolean searchOrm;

		public ScanningContext scanner(Scanner scanner) {
			this.scanner = scanner;
			return this;
		}

		public ScanningContext url(URL url) {
			this.url = url;
			return this;
		}

		public ScanningContext explicitMappingFiles(List<String> explicitMappingFiles) {
			this.explicitMappingFiles = explicitMappingFiles;
			return this;
		}

		public ScanningContext detectClasses(boolean detectClasses) {
			this.detectClasses = detectClasses;
			return this;
		}

		public ScanningContext detectHbmFiles(boolean detectHbmFiles) {
			this.detectHbmFiles = detectHbmFiles;
			return this;
		}

		public ScanningContext searchOrm(boolean searchOrm) {
			this.searchOrm = searchOrm;
			return this;
		}
	}

	private static void addMetadataFromScan(ScanningContext scanningContext, PersistenceMetadata metadata) throws IOException {
		List<String> classes = metadata.getClasses();
		List<String> packages = metadata.getPackages();
		List<NamedInputStream> hbmFiles = metadata.getHbmfiles();
		List<String> mappingFiles = metadata.getMappingFiles();
		addScannedEntries( scanningContext, classes, packages, hbmFiles, mappingFiles );
	}

	private static void addScannedEntries(ScanningContext scanningContext, List<String> classes, List<String> packages, List<NamedInputStream> hbmFiles, List<String> mappingFiles) throws IOException {
		Scanner scanner = scanningContext.scanner;
		if (scanningContext.detectClasses) {
			Set<Class<? extends Annotation>> annotationsToExclude = new HashSet<Class<? extends Annotation>>(3);
			annotationsToExclude.add( Entity.class );
			annotationsToExclude.add( MappedSuperclass.class );
			annotationsToExclude.add( Embeddable.class );
			Set<Class<?>> matchingClasses = scanner.getClassesInJar( scanningContext.url, annotationsToExclude );
			for (Class<?> clazz : matchingClasses) {
				classes.add( clazz.getName() );
			}

			Set<Package> matchingPackages = scanner.getPackagesInJar( scanningContext.url, new HashSet<Class<? extends Annotation>>(0) );
			for (Package pkg : matchingPackages) {
				packages.add( pkg.getName() );
			}
		}
		Set<String> patterns = new HashSet<String>();
		if (scanningContext.searchOrm) {
			patterns.add( META_INF_ORM_XML );
		}
		if (scanningContext.detectHbmFiles) {
			patterns.add( "**/*.hbm.xml" );
		}
		if ( mappingFiles != null) patterns.addAll( mappingFiles );
		if (patterns.size() !=0) {
			Set<NamedInputStream> files = scanner.getFilesInJar( scanningContext.url, patterns );
			for (NamedInputStream file : files) {
				hbmFiles.add( file );
				if (mappingFiles != null) mappingFiles.remove( file.getName() );
			}
		}
	}

	/**
	 * Process configuration from a PersistenceUnitInfo object; typically called by the container
	 * via {@link javax.persistence.spi.PersistenceProvider#createContainerEntityManagerFactory}.
	 * In Hibernate EM, this correlates to {@link HibernatePersistence#createContainerEntityManagerFactory}
	 *
	 * @param info The persistence unit info passed in by the container (usually from processing a persistence.xml).
	 * @param integration The map of integration properties from the container to configure the provider.
	 *
	 * @return this
	 *
	 * @see HibernatePersistence#createContainerEntityManagerFactory
	 */
	@SuppressWarnings({ "unchecked" })
	public Ejb3Configuration configure(PersistenceUnitInfo info, Map integration) {
        if (LOG.isDebugEnabled()) LOG.debugf("Processing %s", LogHelper.logPersistenceUnitInfo(info));
        else LOG.processingPersistenceUnitInfoName(info.getPersistenceUnitName());

		// Spec says the passed map may be null, so handle that to make further processing easier...
		integration = integration != null ? Collections.unmodifiableMap( integration ) : CollectionHelper.EMPTY_MAP;

		// See if we (Hibernate) are the persistence provider
		String provider = (String) integration.get( AvailableSettings.PROVIDER );
		if ( provider == null ) {
			provider = info.getPersistenceProviderClassName();
		}
		if ( provider != null && ! provider.trim().startsWith( IMPLEMENTATION_NAME ) ) {
            LOG.requiredDifferentProvider(provider);
			return null;
		}

		// set the classloader, passed in by the container in info, to set as the TCCL so that
		// Hibernate uses it to properly resolve class references.
		Thread thread = Thread.currentThread();
		ClassLoader contextClassLoader = thread.getContextClassLoader();
		boolean sameClassLoader = true;
		if (overridenClassLoader != null) {
			thread.setContextClassLoader( overridenClassLoader );
			sameClassLoader = false;
		}
		else {
			if ( info.getClassLoader() == null ) {
				throw new IllegalStateException(
						"[PersistenceUnit: " + info.getPersistenceUnitName() == null ? "" : info.getPersistenceUnitName()
								+ "] " + "PersistenceUnitInfo.getClassLoader() id null" );
			}
			sameClassLoader = info.getClassLoader().equals( contextClassLoader );
			if ( ! sameClassLoader ) {
				overridenClassLoader = info.getClassLoader();
				thread.setContextClassLoader( overridenClassLoader );
			}
			else {
				overridenClassLoader = null;
			}
		}

		// Best I can tell, 'workingVars' is some form of additional configuration contract.
		// But it does not correlate 1-1 to EMF/SF settings.  It really is like a set of de-typed
		// additional configuration info.  I think it makes better sense to define this as an actual
		// contract if that was in fact the intent; the code here is pretty confusing.
		try {
			Map workingVars = new HashMap();
			workingVars.put( AvailableSettings.PERSISTENCE_UNIT_NAME, info.getPersistenceUnitName() );
			this.persistenceUnitName = info.getPersistenceUnitName();
			List<String> entities = new ArrayList<String>( 50 );
			if ( info.getManagedClassNames() != null ) entities.addAll( info.getManagedClassNames() );
			List<NamedInputStream> hbmFiles = new ArrayList<NamedInputStream>();
			List<String> packages = new ArrayList<String>();
			List<String> xmlFiles = new ArrayList<String>( 50 );
			List<XmlDocument> xmlDocuments = new ArrayList<XmlDocument>( 50 );
			if ( info.getMappingFileNames() != null ) {
				xmlFiles.addAll( info.getMappingFileNames() );
			}
			//Should always be true if the container is not dump
			boolean searchForORMFiles = ! xmlFiles.contains( META_INF_ORM_XML );

			ScanningContext context = new ScanningContext();
			final Properties copyOfProperties = (Properties) info.getProperties().clone();
			ConfigurationHelper.overrideProperties( copyOfProperties, integration );
			context.scanner( buildScanner( copyOfProperties, integration ) )
					.searchOrm( searchForORMFiles )
					.explicitMappingFiles( null ); //URLs provided by the container already

			//context for other JARs
			setDetectedArtifactsOnScanningContext(context, info.getProperties(), null, false );
			for ( URL jar : info.getJarFileUrls() ) {
				context.url(jar);
				scanForClasses( context, packages, entities, hbmFiles );
			}

			//main jar
			context.url( info.getPersistenceUnitRootUrl() );
			setDetectedArtifactsOnScanningContext( context, info.getProperties(), null, info.excludeUnlistedClasses() );
			scanForClasses( context, packages, entities, hbmFiles );

			Properties properties = info.getProperties() != null ? info.getProperties() : new Properties();
			ConfigurationHelper.overrideProperties( properties, integration );

			//FIXME entities is used to enhance classes and to collect annotated entities this should not be mixed
			//fill up entities with the on found in xml files
			addXMLEntities( xmlFiles, info, entities, xmlDocuments );

			//FIXME send the appropriate entites.
			if ( "true".equalsIgnoreCase( properties.getProperty( AvailableSettings.USE_CLASS_ENHANCER ) ) ) {
				info.addTransformer( new InterceptFieldClassFileTransformer( entities ) );
			}

			workingVars.put( AvailableSettings.CLASS_NAMES, entities );
			workingVars.put( AvailableSettings.PACKAGE_NAMES, packages );
			workingVars.put( AvailableSettings.XML_FILE_NAMES, xmlFiles );
			workingVars.put( PARSED_MAPPING_DOMS, xmlDocuments );

			if ( hbmFiles.size() > 0 ) {
				workingVars.put( AvailableSettings.HBXML_FILES, hbmFiles );
			}

			// validation factory
			final Object validationFactory = integration.get( AvailableSettings.VALIDATION_FACTORY );
			if ( validationFactory != null ) {
				BeanValidationIntegrator.validateFactory( validationFactory );
				properties.put( AvailableSettings.VALIDATION_FACTORY, validationFactory );
			}

			// validation-mode (integration has precedence)
			{
				final Object integrationValue = integration.get( AvailableSettings.VALIDATION_MODE );
				if ( integrationValue != null ) {
					properties.put( AvailableSettings.VALIDATION_MODE, integrationValue.toString() );
				}
				else if ( info.getValidationMode() != null ) {
					properties.put( AvailableSettings.VALIDATION_MODE, info.getValidationMode().name() );
				}
			}

			// shared-cache-mode (integration has precedence)
			{
				final Object integrationValue = integration.get( AvailableSettings.SHARED_CACHE_MODE );
				if ( integrationValue != null ) {
					properties.put( AvailableSettings.SHARED_CACHE_MODE, integrationValue.toString() );
				}
				else if ( info.getSharedCacheMode() != null ) {
					properties.put( AvailableSettings.SHARED_CACHE_MODE, info.getSharedCacheMode().name() );
				}
			}

			//datasources
			Boolean isJTA = null;
			boolean overridenDatasource = false;
			if ( integration.containsKey( AvailableSettings.JTA_DATASOURCE ) ) {
				String dataSource = (String) integration.get( AvailableSettings.JTA_DATASOURCE );
				overridenDatasource = true;
				properties.setProperty( Environment.DATASOURCE, dataSource );
				isJTA = Boolean.TRUE;
			}
			if ( integration.containsKey( AvailableSettings.NON_JTA_DATASOURCE ) ) {
				String dataSource = (String) integration.get( AvailableSettings.NON_JTA_DATASOURCE );
				overridenDatasource = true;
				properties.setProperty( Environment.DATASOURCE, dataSource );
				if (isJTA == null) isJTA = Boolean.FALSE;
			}

			if ( ! overridenDatasource && ( info.getJtaDataSource() != null || info.getNonJtaDataSource() != null ) ) {
				isJTA = info.getJtaDataSource() != null;
				this.setDataSource(
						isJTA ? info.getJtaDataSource() : info.getNonJtaDataSource()
				);
				this.setProperty(
						Environment.CONNECTION_PROVIDER, InjectedDataSourceConnectionProvider.class.getName()
				);
			}
			/*
			 * If explicit type => use it
			 * If a JTA DS is used => JTA transaction,
			 * if a non JTA DS is used => RESOURCe_LOCAL
			 * if none, set to JavaEE default => JTA transaction
			 */
			PersistenceUnitTransactionType transactionType = info.getTransactionType();
			if (transactionType == null) {
				if (isJTA == Boolean.TRUE) {
					transactionType = PersistenceUnitTransactionType.JTA;
				}
				else if ( isJTA == Boolean.FALSE ) {
					transactionType = PersistenceUnitTransactionType.RESOURCE_LOCAL;
				}
				else {
					transactionType = PersistenceUnitTransactionType.JTA;
				}
			}
			defineTransactionType( transactionType, workingVars );
			configure( properties, workingVars );
		}
		finally {
			//After EMF, set the CCL back
			if ( ! sameClassLoader ) {
				thread.setContextClassLoader( contextClassLoader );
			}
		}
		return this;
	}

	/**
	 * Processes {@code xmlFiles} argument and populates:<ul>
	 * <li>the {@code entities} list with encountered classnames</li>
	 * <li>the {@code xmlDocuments} list with parsed/validated {@link XmlDocument} corrolary to each xml file</li>
	 * </ul>
	 *
	 * @param xmlFiles The XML resource names; these will be resolved by classpath lookup and parsed/validated.
	 * @param info The PUI
	 * @param entities (output) The names of all encountered "mapped" classes
	 * @param xmlDocuments (output) The list of {@link XmlDocument} instances of each entry in {@code xmlFiles}
	 */
	@SuppressWarnings({ "unchecked" })
	private void addXMLEntities(
			List<String> xmlFiles,
			PersistenceUnitInfo info,
			List<String> entities,
			List<XmlDocument> xmlDocuments) {
		//TODO handle inputstream related hbm files
		ClassLoader classLoaderToUse = info.getNewTempClassLoader();
		if ( classLoaderToUse == null ) {
            LOG.persistenceProviderCallerDoesNotImplementEjb3SpecCorrectly();
			return;
		}
		for ( final String xmlFile : xmlFiles ) {
			final InputStream fileInputStream = classLoaderToUse.getResourceAsStream( xmlFile );
			if ( fileInputStream == null ) {
                LOG.unableToResolveMappingFile(xmlFile);
				continue;
			}
			final InputSource inputSource = new InputSource( fileInputStream );

			XmlDocument metadataXml = MappingReader.INSTANCE.readMappingDocument(
					cfg.getEntityResolver(),
					inputSource,
					new OriginImpl( "persistence-unit-info", xmlFile )
			);
			xmlDocuments.add( metadataXml );
			try {
				final Element rootElement = metadataXml.getDocumentTree().getRootElement();
				if ( rootElement != null && "entity-mappings".equals( rootElement.getName() ) ) {
					Element element = rootElement.element( "package" );
					String defaultPackage = element != null ? element.getTextTrim() : null;
					List<Element> elements = rootElement.elements( "entity" );
					for (Element subelement : elements ) {
						String classname = XMLContext.buildSafeClassName( subelement.attributeValue( "class" ), defaultPackage );
						if ( ! entities.contains( classname ) ) {
							entities.add( classname );
						}
					}
					elements = rootElement.elements( "mapped-superclass" );
					for (Element subelement : elements ) {
						String classname = XMLContext.buildSafeClassName( subelement.attributeValue( "class" ), defaultPackage );
						if ( ! entities.contains( classname ) ) {
							entities.add( classname );
						}
					}
					elements = rootElement.elements( "embeddable" );
					for (Element subelement : elements ) {
						String classname = XMLContext.buildSafeClassName( subelement.attributeValue( "class" ), defaultPackage );
						if ( ! entities.contains( classname ) ) {
							entities.add( classname );
						}
					}
				}
				else if ( rootElement != null && "hibernate-mappings".equals( rootElement.getName() ) ) {
					//FIXME include hbm xml entities to enhance them but entities is also used to collect annotated entities
				}
			}
			finally {
				try {
					fileInputStream.close();
				}
				catch (IOException ioe) {
                    LOG.unableToCloseInputStream(ioe);
				}
			}
		}
		xmlFiles.clear();
	}

	private void defineTransactionType(Object overridenTxType, Map workingVars) {
		if ( overridenTxType == null ) {
//			if ( transactionType == null ) {
//				transactionType = PersistenceUnitTransactionType.JTA; //this is the default value
//			}
			//nothing to override
		}
		else if ( overridenTxType instanceof String ) {
			transactionType = PersistenceXmlLoader.getTransactionType( (String) overridenTxType );
		}
		else if ( overridenTxType instanceof PersistenceUnitTransactionType ) {
			transactionType = (PersistenceUnitTransactionType) overridenTxType;
		}
		else {
			throw new PersistenceException( getExceptionHeader() +
					AvailableSettings.TRANSACTION_TYPE + " of the wrong class type"
							+ ": " + overridenTxType.getClass()
			);
		}

	}

	public Ejb3Configuration setProperty(String key, String value) {
		cfg.setProperty( key, value );
		return this;
	}

	/**
	 * Set ScanningContext detectClasses and detectHbmFiles according to context
	 */
	private void setDetectedArtifactsOnScanningContext(ScanningContext context,
													   Properties properties,
													   Map overridenProperties,
													   boolean excludeIfNotOverriden) {

		boolean detectClasses = false;
		boolean detectHbm = false;
		String detectSetting = overridenProperties != null ?
				(String) overridenProperties.get( AvailableSettings.AUTODETECTION ) :
				null;
		detectSetting = detectSetting == null ?
				properties.getProperty( AvailableSettings.AUTODETECTION) :
				detectSetting;
		if ( detectSetting == null && excludeIfNotOverriden) {
			//not overriden through HibernatePersistence.AUTODETECTION so we comply with the spec excludeUnlistedClasses
			context.detectClasses( false ).detectHbmFiles( false );
			return;
		}

		if ( detectSetting == null){
			detectSetting = "class,hbm";
		}
		StringTokenizer st = new StringTokenizer( detectSetting, ", ", false );
		while ( st.hasMoreElements() ) {
			String element = (String) st.nextElement();
			if ( "class".equalsIgnoreCase( element ) ) detectClasses = true;
			if ( "hbm".equalsIgnoreCase( element ) ) detectHbm = true;
		}
        LOG.debugf("Detect class: %s; detect hbm: %s", detectClasses, detectHbm);
		context.detectClasses( detectClasses ).detectHbmFiles( detectHbm );
	}

	private void scanForClasses(ScanningContext scanningContext, List<String> packages, List<String> entities, List<NamedInputStream> hbmFiles) {
		if (scanningContext.url == null) {
            LOG.containerProvidingNullPersistenceUnitRootUrl();
			return;
		}
		try {
			addScannedEntries( scanningContext, entities, packages, hbmFiles, null );
		}
		catch (RuntimeException e) {
			throw new RuntimeException( "error trying to scan <jar-file>: " + scanningContext.url.toString(), e );
		}
		catch( IOException e ) {
			throw new RuntimeException( "Error while reading " + scanningContext.url.toString(), e );
		}
	}

	/**
	 * create a factory from a list of properties and
	 * HibernatePersistence.CLASS_NAMES -> Collection<String> (use to list the classes from config files
	 * HibernatePersistence.PACKAGE_NAMES -> Collection<String> (use to list the mappings from config files
	 * HibernatePersistence.HBXML_FILES -> Collection<InputStream> (input streams of hbm files)
	 * HibernatePersistence.LOADED_CLASSES -> Collection<Class> (list of loaded classes)
	 * <p/>
	 * <b>Used by JBoss AS only</b>
	 * @deprecated use the Java Persistence API
	 */
	// This is used directly by JBoss so don't remove until further notice.  bill@jboss.org
	@Deprecated
    public EntityManagerFactory createEntityManagerFactory(Map workingVars) {
		configure( workingVars );
		return buildEntityManagerFactory();
	}

	/**
	 * Process configuration and build an EntityManagerFactory <b>when</b> the configuration is ready
	 * @deprecated
	 */
	@Deprecated
	public EntityManagerFactory createEntityManagerFactory() {
		configure( cfg.getProperties(), new HashMap() );
		return buildEntityManagerFactory();
	}

	public EntityManagerFactory buildEntityManagerFactory() {
		return buildEntityManagerFactory( new BootstrapServiceRegistryBuilder() );
	}

	public EntityManagerFactory buildEntityManagerFactory(BootstrapServiceRegistryBuilder builder) {
		Thread thread = null;
		ClassLoader contextClassLoader = null;

		if ( overridenClassLoader != null ) {
			thread = Thread.currentThread();
			contextClassLoader = thread.getContextClassLoader();
			thread.setContextClassLoader( overridenClassLoader );
		}

		try {
			final ServiceRegistry serviceRegistry = buildLifecycleControledServiceRegistry( builder );
			return  new EntityManagerFactoryImpl(
					transactionType,
					discardOnClose,
					getSessionInterceptorClass( cfg.getProperties() ),
					cfg,
					serviceRegistry,
					persistenceUnitName
			);
		}
		catch (HibernateException e) {
			throw new PersistenceException( getExceptionHeader() + "Unable to build EntityManagerFactory", e );
		}
		finally {
			if (thread != null) {
				thread.setContextClassLoader( contextClassLoader );
			}
		}
	}

	private ServiceRegistry buildLifecycleControledServiceRegistry(BootstrapServiceRegistryBuilder builder) {
		final ServiceRegistryBuilder serviceRegistryBuilder = new ServiceRegistryBuilder(
				builder.with( new JpaIntegrator() ).build()
		);
		serviceRegistryBuilder.applySettings( cfg.getProperties() );
		configure( (Properties ) null, null );
		NamingHelper.bind( this );
		final ServiceRegistry serviceRegistry = serviceRegistryBuilder.buildServiceRegistry();
		SessionFactoryObserver serviceRegistryCloser = new SessionFactoryObserver() {
			@Override
			public void sessionFactoryCreated(SessionFactory factory) {
			}

			@Override
			public void sessionFactoryClosed(SessionFactory factory) {
				( ( StandardServiceRegistryImpl ) serviceRegistry ).destroy();
			}
		};
		if ( cfg.getSessionFactoryObserver() != null ) {
			SessionFactoryObserverChain aggregator = new SessionFactoryObserverChain();
			aggregator.addObserver( cfg.getSessionFactoryObserver() );
			aggregator.addObserver( serviceRegistryCloser );
			cfg.setSessionFactoryObserver( aggregator );
		}
		else {
			cfg.setSessionFactoryObserver( serviceRegistryCloser );
		}
		return serviceRegistry;
	}

	private Class getSessionInterceptorClass(Properties properties) {
		String sessionInterceptorClassname = (String) properties.get( AvailableSettings.SESSION_INTERCEPTOR );
		if ( StringHelper.isNotEmpty( sessionInterceptorClassname ) ) {
			try {
				Class interceptorClass = ReflectHelper.classForName(
						sessionInterceptorClassname, Ejb3Configuration.class
				);
				interceptorClass.newInstance();
				return interceptorClass;
			}
			catch (ClassNotFoundException e) {
				throw new PersistenceException( getExceptionHeader() + "Unable to load "
						+ AvailableSettings.SESSION_INTERCEPTOR + ": " + sessionInterceptorClassname, e);
			}
			catch (IllegalAccessException e) {
				throw new PersistenceException( getExceptionHeader() + "Unable to instanciate "
						+ AvailableSettings.SESSION_INTERCEPTOR + ": " + sessionInterceptorClassname, e);
			}
			catch (InstantiationException e) {
				throw new PersistenceException( getExceptionHeader() + "Unable to instanciate "
						+ AvailableSettings.SESSION_INTERCEPTOR + ": " + sessionInterceptorClassname, e);
			}
        }
        return null;
	}

	public Reference getReference() throws NamingException {
        LOG.debugf( "Returning a Reference to the Ejb3Configuration" );
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		ObjectOutput out = null;
		byte[] serialized;
		try {
			out = new ObjectOutputStream( stream );
			out.writeObject( this );
			out.close();
			serialized = stream.toByteArray();
			stream.close();
		}
		catch (IOException e) {
			NamingException namingException = new NamingException( "Unable to serialize Ejb3Configuration" );
			namingException.setRootCause( e );
			throw namingException;
		}

		return new Reference(
				Ejb3Configuration.class.getName(),
				new BinaryRefAddr("object", serialized ),
				Ejb3ConfigurationObjectFactory.class.getName(),
				null
		);
	}

	@SuppressWarnings( {"unchecked"})
	public Ejb3Configuration configure(Map configValues) {
		Properties props = new Properties();
		if ( configValues != null ) {
			props.putAll( configValues );
			//remove huge non String elements for a clean props
			props.remove( AvailableSettings.CLASS_NAMES );
			props.remove( AvailableSettings.PACKAGE_NAMES );
			props.remove( AvailableSettings.HBXML_FILES );
			props.remove( AvailableSettings.LOADED_CLASSES );
		}
		return configure( props, configValues );
	}

	/**
	 * Configures this configuration object from 2 distinctly different sources.
	 *
	 * @param properties These are the properties that came from the user, either via
	 * a persistence.xml or explicitly passed in to one of our
	 * {@link javax.persistence.spi.PersistenceProvider}/{@link HibernatePersistence} contracts.
	 * @param workingVars Is collection of settings which need to be handled similarly
	 * between the 2 main bootstrap methods, but where the values are determine very differently
	 * by each bootstrap method.  todo eventually make this a contract (class/interface)
	 *
	 * @return The configured configuration
	 *
	 * @see HibernatePersistence
	 */
	private Ejb3Configuration configure(Properties properties, Map workingVars) {
		//TODO check for people calling more than once this method (except buildEMF)
		if (isConfigurationProcessed) return this;
		isConfigurationProcessed = true;
		Properties preparedProperties = prepareProperties( properties, workingVars );
		if ( workingVars == null ) workingVars = CollectionHelper.EMPTY_MAP;

		if ( preparedProperties.containsKey( AvailableSettings.CFG_FILE ) ) {
			String cfgFileName = preparedProperties.getProperty( AvailableSettings.CFG_FILE );
			cfg.configure( cfgFileName );
		}

		cfg.addProperties( preparedProperties ); //persistence.xml has priority over hibernate.cfg.xml

		addClassesToSessionFactory( workingVars );

		//processes specific properties
		List<String> jaccKeys = new ArrayList<String>();


		Interceptor defaultInterceptor = DEFAULT_CONFIGURATION.getInterceptor();
		NamingStrategy defaultNamingStrategy = DEFAULT_CONFIGURATION.getNamingStrategy();

		Iterator propertyIt = preparedProperties.keySet().iterator();
		while ( propertyIt.hasNext() ) {
			Object uncastObject = propertyIt.next();
			//had to be safe
			if ( uncastObject != null && uncastObject instanceof String ) {
				String propertyKey = (String) uncastObject;
				if ( propertyKey.startsWith( AvailableSettings.CLASS_CACHE_PREFIX ) ) {
					setCacheStrategy( propertyKey, preparedProperties, true, workingVars );
				}
				else if ( propertyKey.startsWith( AvailableSettings.COLLECTION_CACHE_PREFIX ) ) {
					setCacheStrategy( propertyKey, preparedProperties, false, workingVars );
				}
				else if ( propertyKey.startsWith( AvailableSettings.JACC_PREFIX )
						&& ! ( propertyKey.equals( AvailableSettings.JACC_CONTEXT_ID )
						|| propertyKey.equals( AvailableSettings.JACC_ENABLED ) ) ) {
					jaccKeys.add( propertyKey );
				}
			}
		}
		final Interceptor interceptor = instantiateCustomClassFromConfiguration(
				preparedProperties,
				defaultInterceptor,
				cfg.getInterceptor(),
				AvailableSettings.INTERCEPTOR,
				"interceptor",
				Interceptor.class
		);
		if ( interceptor != null ) {
			cfg.setInterceptor( interceptor );
		}
		final NamingStrategy namingStrategy = instantiateCustomClassFromConfiguration(
				preparedProperties,
				defaultNamingStrategy,
				cfg.getNamingStrategy(),
				AvailableSettings.NAMING_STRATEGY,
				"naming strategy",
				NamingStrategy.class
		);
		if ( namingStrategy != null ) {
			cfg.setNamingStrategy( namingStrategy );
		}

		final SessionFactoryObserver observer = instantiateCustomClassFromConfiguration(
				preparedProperties,
				null,
				cfg.getSessionFactoryObserver(),
				AvailableSettings.SESSION_FACTORY_OBSERVER,
				"SessionFactory observer",
				SessionFactoryObserver.class
		);
		if ( observer != null ) {
			cfg.setSessionFactoryObserver( observer );
		}

		final IdentifierGeneratorStrategyProvider strategyProvider = instantiateCustomClassFromConfiguration(
				preparedProperties,
				null,
				null,
				AvailableSettings.IDENTIFIER_GENERATOR_STRATEGY_PROVIDER,
				"Identifier generator strategy provider",
				IdentifierGeneratorStrategyProvider.class
		);
		if ( strategyProvider != null ) {
			final MutableIdentifierGeneratorFactory identifierGeneratorFactory = cfg.getIdentifierGeneratorFactory();
			for ( Map.Entry<String,Class<?>> entry : strategyProvider.getStrategies().entrySet() ) {
				identifierGeneratorFactory.register( entry.getKey(), entry.getValue() );
			}
		}

		if ( jaccKeys.size() > 0 ) {
			addSecurity( jaccKeys, preparedProperties, workingVars );
		}

		//some spec compliance checking
		//TODO centralize that?
        if (!"true".equalsIgnoreCase(cfg.getProperty(Environment.AUTOCOMMIT))) LOG.jdbcAutoCommitFalseBreaksEjb3Spec(Environment.AUTOCOMMIT);
        discardOnClose = preparedProperties.getProperty(AvailableSettings.DISCARD_PC_ON_CLOSE).equals("true");
		return this;
	}

	private <T> T instantiateCustomClassFromConfiguration(
			Properties preparedProperties,
			T defaultObject,
			T cfgObject,
			String propertyName,
			String classDescription,
			Class<T> objectClass) {
		if ( preparedProperties.containsKey( propertyName )
				&& ( cfgObject == null || cfgObject.equals( defaultObject ) ) ) {
			//cfg.setXxx has precedence over configuration file
			String className = preparedProperties.getProperty( propertyName );
			try {
				Class<T> clazz = classForName( className );
				return clazz.newInstance();
				//cfg.setInterceptor( (Interceptor) instance.newInstance() );
			}
			catch (ClassNotFoundException e) {
				throw new PersistenceException(
						getExceptionHeader() + "Unable to find " + classDescription + " class: " + className, e
				);
			}
			catch (IllegalAccessException e) {
				throw new PersistenceException(
						getExceptionHeader() + "Unable to access " + classDescription + " class: " + className, e
				);
			}
			catch (InstantiationException e) {
				throw new PersistenceException(
						getExceptionHeader() + "Unable to instantiate " + classDescription + " class: " + className, e
				);
			}
			catch (ClassCastException e) {
				throw new PersistenceException(
						getExceptionHeader() + classDescription + " class does not implement " + objectClass + " interface: "
								+ className, e
				);
			}
		}
		return null;
	}

	@SuppressWarnings({ "unchecked" })
	private void addClassesToSessionFactory(Map workingVars) {
		if ( workingVars.containsKey( AvailableSettings.CLASS_NAMES ) ) {
			Collection<String> classNames = (Collection<String>) workingVars.get(
					AvailableSettings.CLASS_NAMES
			);
			addNamedAnnotatedClasses( this, classNames, workingVars );
		}

		if ( workingVars.containsKey( PARSED_MAPPING_DOMS ) ) {
			Collection<XmlDocument> xmlDocuments = (Collection<XmlDocument>) workingVars.get( PARSED_MAPPING_DOMS );
			for ( XmlDocument xmlDocument : xmlDocuments ) {
				cfg.add( xmlDocument );
			}
		}

		//TODO apparently only used for Tests, get rid of it?
		if ( workingVars.containsKey( AvailableSettings.LOADED_CLASSES ) ) {
			Collection<Class> classes = (Collection<Class>) workingVars.get( AvailableSettings.LOADED_CLASSES );
			for ( Class clazz : classes ) {
				cfg.addAnnotatedClass( clazz );
			}
		}
		if ( workingVars.containsKey( AvailableSettings.PACKAGE_NAMES ) ) {
			Collection<String> packages = (Collection<String>) workingVars.get(
					AvailableSettings.PACKAGE_NAMES
			);
			for ( String pkg : packages ) {
				cfg.addPackage( pkg );
			}
		}
		if ( workingVars.containsKey( AvailableSettings.XML_FILE_NAMES ) ) {
			Collection<String> xmlFiles = (Collection<String>) workingVars.get( AvailableSettings.XML_FILE_NAMES );
			for ( String xmlFile : xmlFiles ) {
				Boolean useMetaInf = null;
				try {
					if ( xmlFile.endsWith( META_INF_ORM_XML ) ) {
						useMetaInf = true;
					}
					cfg.addResource( xmlFile );
				}
				catch( MappingNotFoundException e ) {
					if ( ! xmlFile.endsWith( META_INF_ORM_XML ) ) {
						throw new PersistenceException( getExceptionHeader()
								+ "Unable to find XML mapping file in classpath: " + xmlFile);
					}
					else {
						useMetaInf = false;
						//swallow it, the META-INF/orm.xml is optional
					}
				}
				catch( MappingException me ) {
					throw new PersistenceException( getExceptionHeader()
								+ "Error while reading JPA XML file: " + xmlFile, me);
				}
                if (Boolean.TRUE.equals(useMetaInf)) {
					LOG.exceptionHeaderFound(getExceptionHeader(), META_INF_ORM_XML);
				}
                else if (Boolean.FALSE.equals(useMetaInf)) {
					LOG.exceptionHeaderNotFound(getExceptionHeader(), META_INF_ORM_XML);
				}
			}
		}
		if ( workingVars.containsKey( AvailableSettings.HBXML_FILES ) ) {
			Collection<NamedInputStream> hbmXmlFiles = (Collection<NamedInputStream>) workingVars.get(
					AvailableSettings.HBXML_FILES
			);
			for ( NamedInputStream is : hbmXmlFiles ) {
				try {
					//addInputStream has the responsibility to close the stream
					cfg.addInputStream( new BufferedInputStream( is.getStream() ) );
				}
				catch (MappingException me) {
					//try our best to give the file name
					if ( StringHelper.isEmpty( is.getName() ) ) {
						throw me;
					}
					else {
						throw new MappingException("Error while parsing file: " + is.getName(), me );
					}
				}
			}
		}
	}

	private String getExceptionHeader() {
        return (StringHelper.isNotEmpty(persistenceUnitName)) ? "[PersistenceUnit: " + persistenceUnitName + "] " : "";
	}

	private Properties prepareProperties(Properties properties, Map workingVars) {
		Properties preparedProperties = new Properties();

		//defaults different from Hibernate
		preparedProperties.setProperty( Environment.RELEASE_CONNECTIONS, "auto" );
		preparedProperties.setProperty( Environment.JPAQL_STRICT_COMPLIANCE, "true" );
		//settings that always apply to a compliant EJB3
		preparedProperties.setProperty( Environment.AUTOCOMMIT, "true" );
		preparedProperties.setProperty( Environment.USE_IDENTIFIER_ROLLBACK, "false" );
		preparedProperties.setProperty( Environment.FLUSH_BEFORE_COMPLETION, "false" );
		preparedProperties.setProperty( AvailableSettings.DISCARD_PC_ON_CLOSE, "false" );
		if (cfgXmlResource != null) {
			preparedProperties.setProperty( AvailableSettings.CFG_FILE, cfgXmlResource );
			cfgXmlResource = null;
		}

		//override the new defaults with the user defined ones
		//copy programmatically defined properties
		if ( cfg.getProperties() != null ) preparedProperties.putAll( cfg.getProperties() );
		//copy them coping from configuration
		if ( properties != null ) preparedProperties.putAll( properties );
		//note we don't copy cfg.xml properties, since they have to be overriden

		if (transactionType == null) {
			//if it has not been set, the user use a programmatic way
			transactionType = PersistenceUnitTransactionType.RESOURCE_LOCAL;
		}
		defineTransactionType(
				preparedProperties.getProperty( AvailableSettings.TRANSACTION_TYPE ),
				workingVars
		);
		boolean hasTxStrategy = StringHelper.isNotEmpty(
				preparedProperties.getProperty( Environment.TRANSACTION_STRATEGY )
		);
		if ( ! hasTxStrategy && transactionType == PersistenceUnitTransactionType.JTA ) {
			preparedProperties.setProperty(
					Environment.TRANSACTION_STRATEGY, CMTTransactionFactory.class.getName()
			);
		}
		else if ( ! hasTxStrategy && transactionType == PersistenceUnitTransactionType.RESOURCE_LOCAL ) {
			preparedProperties.setProperty( Environment.TRANSACTION_STRATEGY, JdbcTransactionFactory.class.getName() );
		}
        if (hasTxStrategy) LOG.overridingTransactionStrategyDangerous(Environment.TRANSACTION_STRATEGY);
		if ( preparedProperties.getProperty( Environment.FLUSH_BEFORE_COMPLETION ).equals( "true" ) ) {
			preparedProperties.setProperty( Environment.FLUSH_BEFORE_COMPLETION, "false" );
            LOG.definingFlushBeforeCompletionIgnoredInHem(Environment.FLUSH_BEFORE_COMPLETION);
		}
		return preparedProperties;
	}

	private Class classForName(String className) throws ClassNotFoundException {
		return ReflectHelper.classForName( className, this.getClass() );
	}

	private void setCacheStrategy(String propertyKey, Map properties, boolean isClass, Map workingVars) {
		String role = propertyKey.substring(
				( isClass ? AvailableSettings.CLASS_CACHE_PREFIX
						.length() : AvailableSettings.COLLECTION_CACHE_PREFIX.length() )
						+ 1
		);
		//dot size added
		String value = (String) properties.get( propertyKey );
		StringTokenizer params = new StringTokenizer( value, ";, " );
		if ( !params.hasMoreTokens() ) {
			StringBuilder error = new StringBuilder( "Illegal usage of " );
			error.append(
					isClass ? AvailableSettings.CLASS_CACHE_PREFIX : AvailableSettings.COLLECTION_CACHE_PREFIX
			);
			error.append( ": " ).append( propertyKey ).append( " " ).append( value );
			throw new PersistenceException( getExceptionHeader() + error.toString() );
		}
		String usage = params.nextToken();
		String region = null;
		if ( params.hasMoreTokens() ) {
			region = params.nextToken();
		}
		if ( isClass ) {
			boolean lazyProperty = true;
			if ( params.hasMoreTokens() ) {
				lazyProperty = "all".equalsIgnoreCase( params.nextToken() );
			}
			cfg.setCacheConcurrencyStrategy( role, usage, region, lazyProperty );
		}
		else {
			cfg.setCollectionCacheConcurrencyStrategy( role, usage, region );
		}
	}

	private void addSecurity(List<String> keys, Map properties, Map workingVars) {
        LOG.debugf("Adding security");
		if ( !properties.containsKey( AvailableSettings.JACC_CONTEXT_ID ) ) {
			throw new PersistenceException( getExceptionHeader() +
					"Entities have been configured for JACC, but "
							+ AvailableSettings.JACC_CONTEXT_ID
							+ " has not been set"
			);
		}
		String contextId = (String) properties.get( AvailableSettings.JACC_CONTEXT_ID );
		setProperty( Environment.JACC_CONTEXTID, contextId );

		int roleStart = AvailableSettings.JACC_PREFIX.length() + 1;

		for ( String key : keys ) {
			JACCConfiguration jaccCfg = new JACCConfiguration( contextId );
			try {
				String role = key.substring( roleStart, key.indexOf( '.', roleStart ) );
				int classStart = roleStart + role.length() + 1;
				String clazz = key.substring( classStart, key.length() );
				String actions = (String) properties.get( key );
				jaccCfg.addPermission( role, clazz, actions );
			}
			catch (IndexOutOfBoundsException e) {
				throw new PersistenceException( getExceptionHeader() +
						"Illegal usage of " + AvailableSettings.JACC_PREFIX + ": " + key );
			}
		}
	}

	private void addNamedAnnotatedClasses(
			Ejb3Configuration cfg, Collection<String> classNames, Map workingVars
	) {
		for ( String name : classNames ) {
			try {
				Class clazz = classForName( name );
				cfg.addAnnotatedClass( clazz );
			}
			catch (ClassNotFoundException cnfe) {
				Package pkg;
				try {
					pkg = classForName( name + ".package-info" ).getPackage();
				}
				catch (ClassNotFoundException e) {
					pkg = null;
				}
                if (pkg == null) throw new PersistenceException(getExceptionHeader() + "class or package not found", cnfe);
                else cfg.addPackage(name);
			}
		}
	}

	public Ejb3Configuration addProperties(Properties props) {
		cfg.addProperties( props );
		return this;
	}

	public Ejb3Configuration addAnnotatedClass(Class persistentClass) throws MappingException {
		Thread thread = null;
		ClassLoader contextClassLoader = null;
		if (overridenClassLoader != null) {
			thread = Thread.currentThread();
			contextClassLoader = thread.getContextClassLoader();
			thread.setContextClassLoader( overridenClassLoader );
		}
		try {
			cfg.addAnnotatedClass( persistentClass );
			return this;
		}
		finally {
			if (thread != null) thread.setContextClassLoader( contextClassLoader );
		}
	}

	public Ejb3Configuration configure(String resource) throws HibernateException {
		//delay the call to configure to allow proper addition of all annotated classes (EJB-330)
		if (cfgXmlResource != null)
			throw new PersistenceException("configure(String) method already called for " + cfgXmlResource);
		this.cfgXmlResource = resource;
		return this;
	}

	public Ejb3Configuration addPackage(String packageName) throws MappingException {
		Thread thread = null;
		ClassLoader contextClassLoader = null;
		if (overridenClassLoader != null) {
			thread = Thread.currentThread();
			contextClassLoader = thread.getContextClassLoader();
			thread.setContextClassLoader( overridenClassLoader );
		}
		try {
			cfg.addPackage( packageName );
			return this;
		}
		finally {
			if (thread != null) thread.setContextClassLoader( contextClassLoader );
		}
	}

	public Ejb3Configuration addFile(String xmlFile) throws MappingException {
		Thread thread = null;
		ClassLoader contextClassLoader = null;
		if (overridenClassLoader != null) {
			thread = Thread.currentThread();
			contextClassLoader = thread.getContextClassLoader();
			thread.setContextClassLoader( overridenClassLoader );
		}
		try {
			cfg.addFile( xmlFile );
			return this;
		}
		finally {
			if (thread != null) thread.setContextClassLoader( contextClassLoader );
		}
	}

	public Ejb3Configuration addClass(Class persistentClass) throws MappingException {
		Thread thread = null;
		ClassLoader contextClassLoader = null;
		if (overridenClassLoader != null) {
			thread = Thread.currentThread();
			contextClassLoader = thread.getContextClassLoader();
			thread.setContextClassLoader( overridenClassLoader );
		}
		try {
			cfg.addClass( persistentClass );
			return this;
		}
		finally {
			if (thread != null) thread.setContextClassLoader( contextClassLoader );
		}
	}

	public Ejb3Configuration addFile(File xmlFile) throws MappingException {
		Thread thread = null;
		ClassLoader contextClassLoader = null;
		if (overridenClassLoader != null) {
			thread = Thread.currentThread();
			contextClassLoader = thread.getContextClassLoader();
			thread.setContextClassLoader( overridenClassLoader );
		}
		try {
			cfg.addFile( xmlFile );
			return this;
		}
		finally {
			if (thread != null) thread.setContextClassLoader( contextClassLoader );
		}
	}

	public void buildMappings() {
		Thread thread = null;
		ClassLoader contextClassLoader = null;
		if (overridenClassLoader != null) {
			thread = Thread.currentThread();
			contextClassLoader = thread.getContextClassLoader();
			thread.setContextClassLoader( overridenClassLoader );
		}
		try {
			cfg.buildMappings();
		}
		finally {
			if (thread != null) thread.setContextClassLoader( contextClassLoader );
		}
	}

	public Iterator getClassMappings() {
		Thread thread = null;
		ClassLoader contextClassLoader = null;
		if (overridenClassLoader != null) {
			thread = Thread.currentThread();
			contextClassLoader = thread.getContextClassLoader();
			thread.setContextClassLoader( overridenClassLoader );
		}
		try {
			return cfg.getClassMappings();
		}
		finally {
			if (thread != null) thread.setContextClassLoader( contextClassLoader );
		}
	}

	public Iterator getTableMappings() {
		return cfg.getTableMappings();
	}

	public PersistentClass getClassMapping(String persistentClass) {
		return cfg.getClassMapping( persistentClass );
	}

	public org.hibernate.mapping.Collection getCollectionMapping(String role) {
		return cfg.getCollectionMapping( role );
	}

	public void setEntityResolver(EntityResolver entityResolver) {
		cfg.setEntityResolver( entityResolver );
	}

	public Map getNamedQueries() {
		return cfg.getNamedQueries();
	}

	public Interceptor getInterceptor() {
		return cfg.getInterceptor();
	}

	public Properties getProperties() {
		return cfg.getProperties();
	}

	public Ejb3Configuration setInterceptor(Interceptor interceptor) {
		cfg.setInterceptor( interceptor );
		return this;
	}

	public Ejb3Configuration setProperties(Properties properties) {
		cfg.setProperties( properties );
		return this;
	}

	public Map getFilterDefinitions() {
		return cfg.getFilterDefinitions();
	}

	public void addFilterDefinition(FilterDefinition definition) {
		cfg.addFilterDefinition( definition );
	}

	public void addAuxiliaryDatabaseObject(AuxiliaryDatabaseObject object) {
		cfg.addAuxiliaryDatabaseObject( object );
	}

	public NamingStrategy getNamingStrategy() {
		return cfg.getNamingStrategy();
	}

	public Ejb3Configuration setNamingStrategy(NamingStrategy namingStrategy) {
		cfg.setNamingStrategy( namingStrategy );
		return this;
	}

	public Ejb3Configuration setSessionFactoryObserver(SessionFactoryObserver observer) {
		cfg.setSessionFactoryObserver( observer );
		return this;
	}

	/**
	 * This API is intended to give a read-only configuration.
	 * It is sueful when working with SchemaExport or any Configuration based
	 * tool.
	 * DO NOT update configuration through it.
	 */
	public Configuration getHibernateConfiguration() {
		//TODO make it really read only (maybe through proxying)
		return cfg;
	}

	public Ejb3Configuration addInputStream(InputStream xmlInputStream) throws MappingException {
		Thread thread = null;
		ClassLoader contextClassLoader = null;
		if (overridenClassLoader != null) {
			thread = Thread.currentThread();
			contextClassLoader = thread.getContextClassLoader();
			thread.setContextClassLoader( overridenClassLoader );
		}
		try {
			cfg.addInputStream( xmlInputStream );
			return this;
		}
		finally {
			if (thread != null) thread.setContextClassLoader( contextClassLoader );
		}
	}

	public Ejb3Configuration addResource(String path) throws MappingException {
		Thread thread = null;
		ClassLoader contextClassLoader = null;
		if (overridenClassLoader != null) {
			thread = Thread.currentThread();
			contextClassLoader = thread.getContextClassLoader();
			thread.setContextClassLoader( overridenClassLoader );
		}
		try {
			cfg.addResource( path );
			return this;
		}
		finally {
			if (thread != null) thread.setContextClassLoader( contextClassLoader );
		}
	}

	public Ejb3Configuration addResource(String path, ClassLoader classLoader) throws MappingException {
		cfg.addResource( path, classLoader );
		return this;
	}
	
	public Ejb3Configuration addTypeContributor(TypeContributor typeContributor) {
		cfg.registerTypeContributor( typeContributor );
		return this;
	}

	private enum XML_SEARCH {
		HBM,
		ORM_XML,
		BOTH,
		NONE;

		public static XML_SEARCH getType(boolean searchHbm, boolean searchOrm) {
			return searchHbm ?
					searchOrm ? XML_SEARCH.BOTH : XML_SEARCH.HBM :
					searchOrm ? XML_SEARCH.ORM_XML : XML_SEARCH.NONE;
		}
	}
}
