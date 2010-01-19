// $Id$
/*
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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

import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.EntityResolver;
import org.xml.sax.SAXException;

import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.MappingException;
import org.hibernate.MappingNotFoundException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.cfg.Settings;
import org.hibernate.cfg.SettingsFactory;
import org.hibernate.cfg.annotations.reflection.XMLContext;
import org.hibernate.ejb.connection.InjectedDataSourceConnectionProvider;
import org.hibernate.ejb.instrument.InterceptFieldClassFileTransformer;
import org.hibernate.ejb.packaging.JarVisitorFactory;
import org.hibernate.ejb.packaging.NamedInputStream;
import org.hibernate.ejb.packaging.NativeScanner;
import org.hibernate.ejb.packaging.PersistenceMetadata;
import org.hibernate.ejb.packaging.PersistenceXmlLoader;
import org.hibernate.ejb.packaging.Scanner;
import org.hibernate.ejb.transaction.JoinableCMTTransactionFactory;
import org.hibernate.ejb.util.ConfigurationHelper;
import org.hibernate.ejb.util.LogHelper;
import org.hibernate.ejb.util.NamingHelper;
import org.hibernate.engine.FilterDefinition;
import org.hibernate.event.EventListeners;
import org.hibernate.mapping.AuxiliaryDatabaseObject;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.secure.JACCConfiguration;
import org.hibernate.transaction.JDBCTransactionFactory;
import org.hibernate.util.CollectionHelper;
import org.hibernate.util.ReflectHelper;
import org.hibernate.util.StringHelper;
import org.hibernate.util.XMLHelper;

/**
 * Allow a fine tuned configuration of an EJB 3.0 EntityManagerFactory
 *
 * A Ejb3Configuration object is only guaranteed to create one EntityManagerFactory.
 * Multiple usage of #buildEntityManagerFactory() is not guaranteed.
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
 */
public class Ejb3Configuration implements Serializable, Referenceable {
	private static final String IMPLEMENTATION_NAME = HibernatePersistence.class.getName();
	private static final String META_INF_ORM_XML = "META-INF/orm.xml";
	private final Logger log = LoggerFactory.getLogger( Ejb3Configuration.class );
	private static EntityNotFoundDelegate ejb3EntityNotFoundDelegate = new Ejb3EntityNotFoundDelegate();
	private static Configuration DEFAULT_CONFIGURATION = new AnnotationConfiguration();
	private String persistenceUnitName;
	private String cfgXmlResource;

	private static class Ejb3EntityNotFoundDelegate implements EntityNotFoundDelegate, Serializable {
		public void handleEntityNotFound(String entityName, Serializable id) {
			throw new EntityNotFoundException("Unable to find " + entityName  + " with id " + id);
		}
	}

	static {
		Version.touch();
	}

	private AnnotationConfiguration cfg;
	private SettingsFactory settingsFactory;
	//made transient and not restored in deserialization on purpose, should no longer be called after restoration
	private transient EventListenerConfigurator listenerConfigurator;
	private PersistenceUnitTransactionType transactionType;
	private boolean discardOnClose;
	//made transient and not restored in deserialization on purpose, should no longer be called after restoration
	private transient ClassLoader overridenClassLoader;
	private boolean isConfigurationProcessed = false;


	public Ejb3Configuration() {
		settingsFactory = new InjectionSettingsFactory();
		cfg = new AnnotationConfiguration( settingsFactory );
		cfg.setEntityNotFoundDelegate( ejb3EntityNotFoundDelegate );
		listenerConfigurator = new EventListenerConfigurator( this );
	}

	/**
	 * Used to inject a datasource object as the connection provider.
	 * If used, be sure to <b>not override</b> the hibernate.connection.provider_class
	 * property
	 */
	public void setDataSource(DataSource ds) {
		if ( ds != null ) {
			Map cpInjection = new HashMap();
			cpInjection.put( "dataSource", ds );
			( (InjectionSettingsFactory) settingsFactory ).setConnectionProviderInjectionData( cpInjection );
			this.setProperty( Environment.CONNECTION_PROVIDER, InjectedDataSourceConnectionProvider.class.getName() );
		}
	}

	/**
	 * create a factory from a parsed persistence.xml
	 * Especially the scanning of classes and additional jars is done already at this point.
	 */
	private Ejb3Configuration configure(PersistenceMetadata metadata, Map overrides) {
		log.debug( "Creating Factory: {}", metadata.getName() );

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
		if ( metadata.getValidationMode() != null) {
			workingVars.put( AvailableSettings.VALIDATION_MODE, metadata.getValidationMode() );
		}
		if ( metadata.getSharedCacheMode() != null) {
			workingVars.put( AvailableSettings.SHARED_CACHE_MODE, metadata.getSharedCacheMode() );
		}
		Properties props = new Properties();
		props.putAll( metadata.getProps() );
		if ( overrides != null ) {
			for ( Map.Entry entry : (Set<Map.Entry>) overrides.entrySet() ) {
				Object value = entry.getValue();
				props.put( entry.getKey(), value == null ? "" :  value ); //alter null, not allowed in properties
			}
		}
		configure( props, workingVars );
		return this;
	}

	/**
	 * Build the configuration from an entity manager name and given the
	 * appropriate extra properties. Those properties override the one get through
	 * the peristence.xml file.
	 * If the persistence unit name is not found or does not match the Persistence Provider, null is returned
	 *
	 * This method is used in a non managed environment
	 *
	 * @param persistenceUnitName persistence unit name
	 * @param integration properties passed to the persistence provider
	 * @return configured Ejb3Configuration or null if no persistence unit match
	 */
	public Ejb3Configuration configure(String persistenceUnitName, Map integration) {
		try {
			log.debug( "Look up for persistence unit: {}", persistenceUnitName );
			integration = integration == null ?
					CollectionHelper.EMPTY_MAP :
					Collections.unmodifiableMap( integration );
			Enumeration<URL> xmls = Thread.currentThread()
					.getContextClassLoader()
					.getResources( "META-INF/persistence.xml" );
			if ( ! xmls.hasMoreElements() ) {
				log.info( "Could not find any META-INF/persistence.xml file in the classpath");
			}
			while ( xmls.hasMoreElements() ) {
				URL url = xmls.nextElement();
				log.trace( "Analysing persistence.xml: {}", url );
				List<PersistenceMetadata> metadataFiles = PersistenceXmlLoader.deploy(
						url,
						integration,
						cfg.getEntityResolver(),
						PersistenceUnitTransactionType.RESOURCE_LOCAL );
				for ( PersistenceMetadata metadata : metadataFiles ) {
					log.trace( "{}", metadata );

					if ( metadata.getProvider() == null || IMPLEMENTATION_NAME.equalsIgnoreCase(
							metadata.getProvider()
					) ) {
						//correct provider

						//lazy load the scanner to avoid unnecessary IOExceptions
						Scanner scanner = null;
						URL jarURL = null;
						if ( metadata.getName() == null ) {
							scanner = buildScanner();
							jarURL = JarVisitorFactory.getJarURLFromURLEntry( url, "/META-INF/persistence.xml" );
							metadata.setName( scanner.getUnqualifiedJarName(jarURL) );
						}
						if ( persistenceUnitName == null && xmls.hasMoreElements() ) {
							throw new PersistenceException( "No name provided and several persistence units found" );
						}
						else if ( persistenceUnitName == null || metadata.getName().equals( persistenceUnitName ) ) {
							if (scanner == null) {
								scanner = buildScanner();
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

	private NativeScanner buildScanner() {
		return new NativeScanner();
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
	 * Process configuration from a PersistenceUnitInfo object
	 * Typically called by the container
	 */
	public Ejb3Configuration configure(PersistenceUnitInfo info, Map integration) {
		if ( log.isDebugEnabled() ) {
			log.debug( "Processing {}", LogHelper.logPersistenceUnitInfo( info ) );
		}
		else {
			log.info( "Processing PersistenceUnitInfo [\n\tname: {}\n\t...]", info.getPersistenceUnitName() );
		}

		integration = integration != null ? Collections.unmodifiableMap( integration ) : CollectionHelper.EMPTY_MAP;
		String provider = (String) integration.get( AvailableSettings.PROVIDER );
		if ( provider == null ) provider = info.getPersistenceProviderClassName();
		if ( provider != null && ! provider.trim().startsWith( IMPLEMENTATION_NAME ) ) {
			log.info( "Required a different provider: {}", provider );
			return null;
		}
		if ( info.getClassLoader() == null ) {
			throw new IllegalStateException(
					"[PersistenceUnit: " + info.getPersistenceUnitName() == null ? "" : info.getPersistenceUnitName()
							+ "] " + "PersistenceUnitInfo.getClassLoader() id null" );
		}
		//set the classloader
		Thread thread = Thread.currentThread();
		ClassLoader contextClassLoader = thread.getContextClassLoader();
		boolean sameClassLoader = info.getClassLoader().equals( contextClassLoader );
		if ( ! sameClassLoader ) {
			overridenClassLoader = info.getClassLoader();
			thread.setContextClassLoader( overridenClassLoader );
		}
		else {
			overridenClassLoader = null;
		}

		try {
			Map workingVars = new HashMap();
			workingVars.put( AvailableSettings.PERSISTENCE_UNIT_NAME, info.getPersistenceUnitName() );
			this.persistenceUnitName = info.getPersistenceUnitName();
			List<String> entities = new ArrayList<String>( 50 );
			if ( info.getManagedClassNames() != null ) entities.addAll( info.getManagedClassNames() );
			List<NamedInputStream> hbmFiles = new ArrayList<NamedInputStream>();
			List<String> packages = new ArrayList<String>();
			List<String> xmlFiles = new ArrayList<String>( 50 );
			if ( info.getMappingFileNames() != null ) xmlFiles.addAll( info.getMappingFileNames() );
			//Should always be true if the container is not dump
			boolean searchForORMFiles = ! xmlFiles.contains( META_INF_ORM_XML );

			ScanningContext context = new ScanningContext();
			context.scanner( buildScanner() )
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

			Properties properties = info.getProperties() != null ?
					info.getProperties() :
					new Properties();
			ConfigurationHelper.overrideProperties( properties, integration );

			//FIXME entities is used to enhance classes and to collect annotated entities this should not be mixed
			//fill up entities with the on found in xml files
			addXMLEntities( xmlFiles, info, entities );

			//FIXME send the appropriate entites.
			if ( "true".equalsIgnoreCase( properties.getProperty( AvailableSettings.USE_CLASS_ENHANCER ) ) ) {
				info.addTransformer( new InterceptFieldClassFileTransformer( entities ) );
			}

			workingVars.put( AvailableSettings.CLASS_NAMES, entities );
			workingVars.put( AvailableSettings.PACKAGE_NAMES, packages );
			workingVars.put( AvailableSettings.XML_FILE_NAMES, xmlFiles );
			if ( hbmFiles.size() > 0 ) workingVars.put( AvailableSettings.HBXML_FILES, hbmFiles );

			//validation-mode
			final Object validationMode = info.getValidationMode();
			if ( validationMode != null) {
				workingVars.put( AvailableSettings.VALIDATION_MODE, validationMode );
			}

			//shared-cache-mode
			final Object sharedCacheMode = info.getSharedCacheMode();
			if ( sharedCacheMode != null) {
				workingVars.put( AvailableSettings.SHARED_CACHE_MODE, sharedCacheMode );
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
				isJTA = info.getJtaDataSource() != null ? Boolean.TRUE : Boolean.FALSE;
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

	private void addXMLEntities(List<String> xmlFiles, PersistenceUnitInfo info, List<String> entities) {
		//TODO handle inputstream related hbm files
		ClassLoader newTempClassLoader = info.getNewTempClassLoader();
		if (newTempClassLoader == null) {
			log.warn( "Persistence provider caller does not implement the EJB3 spec correctly. PersistenceUnitInfo.getNewTempClassLoader() is null." );
			return;
		}
		XMLHelper xmlHelper = new XMLHelper();
		List errors = new ArrayList();
		SAXReader saxReader = xmlHelper.createSAXReader( "XML InputStream", errors, cfg.getEntityResolver() );
		try {
			saxReader.setFeature( "http://apache.org/xml/features/validation/schema", true );
			//saxReader.setFeature( "http://apache.org/xml/features/validation/dynamic", true );
			//set the default schema locators
			saxReader.setProperty( "http://apache.org/xml/properties/schema/external-schemaLocation",
					"http://java.sun.com/xml/ns/persistence/orm orm_1_0.xsd");
		}
		catch (SAXException e) {
			saxReader.setValidation( false );
		}

		for ( String xmlFile : xmlFiles ) {

			InputStream resourceAsStream = newTempClassLoader.getResourceAsStream( xmlFile );
			if (resourceAsStream == null) continue;
			BufferedInputStream is = new BufferedInputStream( resourceAsStream );
			try {
				errors.clear();
				org.dom4j.Document doc = saxReader.read( is );
				if ( errors.size() != 0 ) {
					throw new MappingException( "invalid mapping: " + xmlFile, (Throwable) errors.get( 0 ) );
				}
				Element rootElement = doc.getRootElement();
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
			catch (DocumentException e) {
				throw new MappingException( "Could not parse mapping document in input stream", e );
			}
			finally {
				try {
					is.close();
				}
				catch (IOException ioe) {
					log.warn( "Could not close input stream", ioe );
				}
			}
		}
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
		log.debug( "Detect class: {}; detect hbm: {}", detectClasses, detectHbm );
		context.detectClasses( detectClasses ).detectHbmFiles( detectHbm );
	}

	private void scanForClasses(ScanningContext scanningContext, List<String> packages, List<String> entities, List<NamedInputStream> hbmFiles) {
		if (scanningContext.url == null) {
			log.error( "Container is providing a null PersistenceUnitRootUrl: discovery impossible");
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
	public EntityManagerFactory createEntityManagerFactory(Map workingVars) {
		Properties props = new Properties();
		if ( workingVars != null ) {
			props.putAll( workingVars );
			//remove huge non String elements for a clean props
			props.remove( AvailableSettings.CLASS_NAMES );
			props.remove( AvailableSettings.PACKAGE_NAMES );
			props.remove( AvailableSettings.HBXML_FILES );
			props.remove( AvailableSettings.LOADED_CLASSES );
		}
		configure( props, workingVars );
		return buildEntityManagerFactory();
	}

	/**
	 * Process configuration and build an EntityManagerFactory <b>when</b> the configuration is ready
	 * @deprecated
	 */
	public EntityManagerFactory createEntityManagerFactory() {
		configure( cfg.getProperties(), new HashMap() );
		return buildEntityManagerFactory();
	}

	public EntityManagerFactory buildEntityManagerFactory() {
		Thread thread = null;
		ClassLoader contextClassLoader = null;
		if (overridenClassLoader != null) {
			thread = Thread.currentThread();
			contextClassLoader = thread.getContextClassLoader();
			thread.setContextClassLoader( overridenClassLoader );
		}
		try {
			configure( (Properties)null, null );
			NamingHelper.bind(this);
			return new EntityManagerFactoryImpl(
					cfg.buildSessionFactory(),
					transactionType,
					discardOnClose,
					getSessionInterceptorClass( cfg.getProperties() ),
					cfg
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

	private Class getSessionInterceptorClass(Properties properties) {
		String sessionInterceptorClassname = (String) properties.get( AvailableSettings.SESSION_INTERCEPTOR );
		if ( StringHelper.isNotEmpty( sessionInterceptorClassname ) ) {
			try {
				Class interceptorClass = ReflectHelper.classForName( sessionInterceptorClassname, Ejb3Configuration.class );
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
		else {
			return null;
		}
	}

	public Reference getReference() throws NamingException {
		log.debug("Returning a Reference to the Ejb3Configuration");
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

	/**
	 * create a factory from a canonical workingVars map and the overriden properties
	 *
	 */
	private Ejb3Configuration configure(
			Properties properties, Map workingVars
	) {
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
		if ( preparedProperties.containsKey( AvailableSettings.INTERCEPTOR )
				&& ( cfg.getInterceptor() == null
				|| cfg.getInterceptor().equals( defaultInterceptor ) ) ) {
			//cfg.setInterceptor has precedence over configuration file
			String interceptorName = preparedProperties.getProperty( AvailableSettings.INTERCEPTOR );
			try {
				Class interceptor = classForName( interceptorName );
				cfg.setInterceptor( (Interceptor) interceptor.newInstance() );
			}
			catch (ClassNotFoundException e) {
				throw new PersistenceException(
						getExceptionHeader() + "Unable to find interceptor class: " + interceptorName, e
				);
			}
			catch (IllegalAccessException e) {
				throw new PersistenceException(
						getExceptionHeader() + "Unable to access interceptor class: " + interceptorName, e
				);
			}
			catch (InstantiationException e) {
				throw new PersistenceException(
						getExceptionHeader() + "Unable to instanciate interceptor class: " + interceptorName, e
				);
			}
			catch (ClassCastException e) {
				throw new PersistenceException(
						getExceptionHeader() + "Interceptor class does not implement Interceptor interface: " + interceptorName, e
				);
			}
		}
		if ( preparedProperties.containsKey( AvailableSettings.NAMING_STRATEGY )
				&& ( cfg.getNamingStrategy() == null
				|| cfg.getNamingStrategy().equals( defaultNamingStrategy ) ) ) {
			//cfg.setNamingStrategy has precedence over configuration file
			String namingStrategyName = preparedProperties.getProperty( AvailableSettings.NAMING_STRATEGY );
			try {
				Class namingStragegy = classForName( namingStrategyName );
				cfg.setNamingStrategy( (NamingStrategy) namingStragegy.newInstance() );
			}
			catch (ClassNotFoundException e) {
				throw new PersistenceException(
						getExceptionHeader() + "Unable to find naming strategy class: " + namingStrategyName, e
				);
			}
			catch (IllegalAccessException e) {
				throw new PersistenceException(
						getExceptionHeader() + "Unable to access naming strategy class: " + namingStrategyName, e
				);
			}
			catch (InstantiationException e) {
				throw new PersistenceException(
						getExceptionHeader() + "Unable to instanciate naming strategy class: " + namingStrategyName, e
				);
			}
			catch (ClassCastException e) {
				throw new PersistenceException(
						getExceptionHeader() + "Naming strategyy class does not implement NmaingStrategy interface: " + namingStrategyName,
						e
				);
			}
		}

		if ( jaccKeys.size() > 0 ) {
			addSecurity( jaccKeys, preparedProperties, workingVars );
		}

		//initialize listeners
		listenerConfigurator.setProperties( preparedProperties );
		listenerConfigurator.configure();

		//some spec compliance checking
		//TODO centralize that?
		if ( ! "true".equalsIgnoreCase( cfg.getProperty( Environment.AUTOCOMMIT ) ) ) {
			log.warn( "{} = false break the EJB3 specification", Environment.AUTOCOMMIT );
		}
		discardOnClose = preparedProperties.getProperty( AvailableSettings.DISCARD_PC_ON_CLOSE )
				.equals( "true" );
		return this;
	}

	private void addClassesToSessionFactory(Map workingVars) {
		if ( workingVars.containsKey( AvailableSettings.CLASS_NAMES ) ) {
			Collection<String> classNames = (Collection<String>) workingVars.get(
					AvailableSettings.CLASS_NAMES
			);
			addNamedAnnotatedClasses( this, classNames, workingVars );
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
			Collection<String> xmlFiles = (Collection<String>) workingVars.get(
					AvailableSettings.XML_FILE_NAMES
			);
			for ( String xmlFile : xmlFiles ) {
				Boolean useMetaInf = null;
				try {
					if ( xmlFile.endsWith( META_INF_ORM_XML ) ) useMetaInf = true;
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
				if ( log.isInfoEnabled() ) {
					if ( Boolean.TRUE.equals( useMetaInf ) ) {
						log.info( "{} {} found", getExceptionHeader(), META_INF_ORM_XML);
					}
					else if (Boolean.FALSE.equals( useMetaInf ) ) {
						log.info( "{} No {} found", getExceptionHeader(), META_INF_ORM_XML);
					}
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
		if ( StringHelper.isNotEmpty( persistenceUnitName ) ) {
			return "[PersistenceUnit: " + persistenceUnitName + "] ";
		}
		else {
			return "";
		}
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
					Environment.TRANSACTION_STRATEGY, JoinableCMTTransactionFactory.class.getName()
			);
		}
		else if ( ! hasTxStrategy && transactionType == PersistenceUnitTransactionType.RESOURCE_LOCAL ) {
			preparedProperties.setProperty( Environment.TRANSACTION_STRATEGY, JDBCTransactionFactory.class.getName() );
		}
		if ( hasTxStrategy ) {
			log.warn(
					"Overriding {} is dangerous, this might break the EJB3 specification implementation",
					Environment.TRANSACTION_STRATEGY
			);
		}
		if ( preparedProperties.getProperty( Environment.FLUSH_BEFORE_COMPLETION ).equals( "true" ) ) {
			preparedProperties.setProperty( Environment.FLUSH_BEFORE_COMPLETION, "false" );
			log.warn( "Defining {}=true ignored in HEM", Environment.FLUSH_BEFORE_COMPLETION );
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
		log.debug( "Adding security" );
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
				if ( pkg == null ) {
					throw new PersistenceException( getExceptionHeader() +  "class or package not found", cnfe );
				}
				else {
					cfg.addPackage( name );
				}
			}
		}
	}


	public Settings buildSettings() throws HibernateException {
		Thread thread = null;
		ClassLoader contextClassLoader = null;
		if (overridenClassLoader != null) {
			thread = Thread.currentThread();
			contextClassLoader = thread.getContextClassLoader();
			thread.setContextClassLoader( overridenClassLoader );
		}
		try {
			return settingsFactory.buildSettings( cfg.getProperties() );
		}
		finally {
			if (thread != null) thread.setContextClassLoader( contextClassLoader );
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

	public EventListeners getEventListeners() {
		return cfg.getEventListeners();
	}

	SessionFactory buildSessionFactory() throws HibernateException {
		return cfg.buildSessionFactory();
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

	public void setListeners(String type, String[] listenerClasses) {
		cfg.setListeners( type, listenerClasses );
	}

	public void setListeners(String type, Object[] listeners) {
		cfg.setListeners( type, listeners );
	}

	/**
	 * This API is intended to give a read-only configuration.
	 * It is sueful when working with SchemaExport or any Configuration based
	 * tool.
	 * DO NOT update configuration through it.
	 */
	public AnnotationConfiguration getHibernateConfiguration() {
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
