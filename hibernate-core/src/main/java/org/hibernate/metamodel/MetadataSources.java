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
package org.hibernate.metamodel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import javax.persistence.AttributeConverter;
import javax.xml.transform.dom.DOMSource;

import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.internal.ClassLoaderAccessImpl;
import org.hibernate.metamodel.internal.JandexInitManager;
import org.hibernate.metamodel.internal.MetadataBuilderImpl;
import org.hibernate.metamodel.source.internal.jaxb.JaxbConverter;
import org.hibernate.metamodel.source.internal.jaxb.JaxbEmbeddable;
import org.hibernate.metamodel.source.internal.jaxb.JaxbEntity;
import org.hibernate.metamodel.source.internal.jaxb.JaxbEntityListener;
import org.hibernate.metamodel.source.internal.jaxb.JaxbEntityListeners;
import org.hibernate.metamodel.source.internal.jaxb.JaxbEntityMappings;
import org.hibernate.metamodel.source.internal.jaxb.JaxbMappedSuperclass;
import org.hibernate.metamodel.source.internal.jaxb.JaxbPersistenceUnitDefaults;
import org.hibernate.metamodel.source.internal.jaxb.JaxbPersistenceUnitMetadata;
import org.hibernate.metamodel.source.spi.MappingException;
import org.hibernate.metamodel.source.spi.MappingNotFoundException;
import org.hibernate.metamodel.spi.ClassLoaderAccess;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.SerializationException;
import org.hibernate.xml.internal.jaxb.UnifiedMappingBinder;
import org.hibernate.xml.spi.BindResult;
import org.hibernate.xml.spi.Origin;
import org.hibernate.xml.spi.SourceType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;
import org.w3c.dom.Document;

/**
 * Entry point into working with sources of metadata information ({@code hbm.xml}, annotations).   Tell Hibernate
 * about sources and then call {@link #buildMetadata()}.
 *
 * @author Steve Ebersole
 * @author Brett Meyer
 */
public class MetadataSources {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( MetadataSources.class );

	private final ServiceRegistry serviceRegistry;
	private List<BindResult> bindResultList = new ArrayList<BindResult>();
	private LinkedHashSet<Class<?>> annotatedClasses = new LinkedHashSet<Class<?>>();
	private LinkedHashSet<String> annotatedClassNames = new LinkedHashSet<String>();
	private LinkedHashSet<String> annotatedPackages = new LinkedHashSet<String>();

	private List<Class<? extends AttributeConverter>> converterClasses;

	public MetadataSources() {
		this( new BootstrapServiceRegistryBuilder().build() );
	}

	/**
	 * Create a metadata sources using the specified service registry.
	 *
	 * @param serviceRegistry The service registry to use.
	 */
	public MetadataSources(ServiceRegistry serviceRegistry) {
		// service registry really should be either BootstrapServiceRegistry or StandardServiceRegistry type...
		if ( ! isExpectedServiceRegistryType( serviceRegistry ) ) {
			LOG.debugf(
					"Unexpected ServiceRegistry type [%s] encountered during building of MetadataSources; may cause " +
							"problems later attempting to construct MetadataBuilder",
					serviceRegistry.getClass().getName()
			);
		}
		this.serviceRegistry = serviceRegistry;
	}

	protected static boolean isExpectedServiceRegistryType(ServiceRegistry serviceRegistry) {
		return BootstrapServiceRegistry.class.isInstance( serviceRegistry )
				|| StandardServiceRegistry.class.isInstance( serviceRegistry );
	}
	
	public void buildBindResults(ClassLoaderAccess classLoaderAccess) {
		final UnifiedMappingBinder jaxbProcessor = new UnifiedMappingBinder( classLoaderAccess );
		for ( BindResult bindResult : bindResultList ) {
			bindResult.bind( jaxbProcessor );
		}
	}

	public List<BindResult> getBindResultList() {
		return bindResultList;
	}

	public Iterable<String> getAnnotatedPackages() {
		return annotatedPackages;
	}

	public Iterable<Class<?>> getAnnotatedClasses() {
		return annotatedClasses;
	}

	public Iterable<String> getAnnotatedClassNames() {
		return annotatedClassNames;
	}

	public ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	/**
	 * Get a builder for metadata where non-default options can be specified.
	 *
	 * @return The built metadata.
	 */
	public MetadataBuilder getMetadataBuilder() {
		return new MetadataBuilderImpl( this );
	}

	/**
	 * Get a builder for metadata where non-default options can be specified.
	 *
	 * @return The built metadata.
	 */
	public MetadataBuilder getMetadataBuilder(StandardServiceRegistry serviceRegistry) {
		return new MetadataBuilderImpl( this, serviceRegistry );
	}

	/**
	 * Short-hand form of calling {@link #getMetadataBuilder()} and using its
	 * {@link org.hibernate.metamodel.MetadataBuilder#build()} method in cases where the application wants
	 * to accept the defaults.
	 *
	 * @return The built metadata.
	 */
	public Metadata buildMetadata() {
		return getMetadataBuilder().build();
	}

	public Metadata buildMetadata(StandardServiceRegistry serviceRegistry) {
		return getMetadataBuilder( serviceRegistry ).build();
	}

	/**
	 * Read metadata from the annotations attached to the given class.
	 *
	 * @param annotatedClass The class containing annotations
	 *
	 * @return this (for method chaining)
	 */
	public MetadataSources addAnnotatedClass(Class annotatedClass) {
		annotatedClasses.add( annotatedClass );
		return this;
	}

	/**
	 * Read metadata from the annotations attached to the given class.
	 *
	 * @param annotatedClassName The name of a class containing annotations
	 *
	 * @return this (for method chaining)
	 */
	public MetadataSources addAnnotatedClassName(String annotatedClassName) {
		annotatedClassNames.add( annotatedClassName );
		return this;
	}

	/**
	 * Read package-level metadata.
	 *
	 * @param packageName java package name without trailing '.', cannot be {@code null}
	 *
	 * @return this (for method chaining)
	 */
	public MetadataSources addPackage(String packageName) {
		if ( packageName == null ) {
			throw new IllegalArgumentException( "The specified package name cannot be null" );
		}
		if ( packageName.endsWith( "." ) ) {
			packageName = packageName.substring( 0, packageName.length() - 1 );
		}
		annotatedPackages.add( packageName );
		return this;
	}

	/**
	 * Read mappings as a application resourceName (i.e. classpath lookup).
	 *
	 * @param name The resource name
	 *
	 * @return this (for method chaining purposes)
	 */
	public MetadataSources addResource(String name) {
		LOG.tracef( "reading mappings from resource : %s", name );

		final Origin origin = new Origin( SourceType.RESOURCE, name );
		InputStream resourceInputStream = classLoaderService().locateResourceStream( name );
		if ( resourceInputStream == null ) {
			throw new MappingNotFoundException( origin );
		}
		add( resourceInputStream, origin, true );

		return this;
	}

	private ClassLoaderService classLoaderService() {
		return serviceRegistry.getService( ClassLoaderService.class );
	}

	private BindResult add(InputStream inputStream, Origin origin, boolean close) {
		BindResult bindResult = new BindResult( inputStream, origin, close );
		bindResultList.add( bindResult );
		return bindResult;
	}

	/**
	 * Read a mapping as an application resource using the convention that a class named {@code foo.bar.Foo} is
	 * mapped by a file named {@code foo/bar/Foo.hbm.xml} which can be resolved as a classpath resource.
	 *
	 * @param entityClass The mapped class. Cannot be {@code null} null.
	 *
	 * @return this (for method chaining purposes)
	 *
	 * @deprecated hbm.xml is a legacy mapping format now considered deprecated.
	 */
	@Deprecated
	public MetadataSources addClass(Class entityClass) {
		if ( entityClass == null ) {
			throw new IllegalArgumentException( "The specified class cannot be null" );
		}
		LOG.debugf( "adding resource mappings from class convention : %s", entityClass.getName() );
		final String mappingResourceName = entityClass.getName().replace( '.', '/' ) + ".hbm.xml";
		addResource( mappingResourceName );
		return this;
	}

	/**
	 * Adds an AttributeConverter by class.
	 *
	 * @param converterClass The AttributeConverter class.
	 *
	 * @return this (for method chaining purposes)
	 */
	public MetadataSources addAttributeConverter(Class<? extends AttributeConverter> converterClass) {
		if ( converterClasses == null ) {
			converterClasses = new ArrayList<Class<? extends AttributeConverter>>();
		}
		converterClasses.add( converterClass );
		return this;
	}

	/**
	 * Read mappings from a particular XML file
	 *
	 * @param path The path to a file.  Expected to be resolvable by {@link File#File(String)}
	 *
	 * @return this (for method chaining purposes)
	 *
	 * @see #addFile(java.io.File)
	 */
	public MetadataSources addFile(String path) {
		return addFile( new File( path ) );
	}

	/**
	 * Read mappings from a particular XML file
	 *
	 * @param file The reference to the XML file
	 *
	 * @return this (for method chaining purposes)
	 */
	public MetadataSources addFile(File file) {
		final String name = file.getAbsolutePath();
		LOG.tracef( "reading mappings from file : %s", name );
		final Origin origin = new Origin( SourceType.FILE, name );
		try {
			add( new FileInputStream( file ), origin, true );
		}
		catch ( FileNotFoundException e ) {
			throw new MappingNotFoundException( e, origin );
		}
		return this;
	}

	/**
	 * See {@link #addCacheableFile(java.io.File)} for description
	 *
	 * @param path The path to a file.  Expected to be resolvable by {@link File#File(String)}
	 *
	 * @return this (for method chaining purposes)
	 *
	 * @see #addCacheableFile(java.io.File)
	 */
	public MetadataSources addCacheableFile(String path) {
		return addCacheableFile( new File( path ) );
	}

	/**
	 * Add a cached mapping file.  A cached file is a serialized representation of the DOM structure of a
	 * particular mapping.  It is saved from a previous call as a file with the name {@code {xmlFile}.bin}
	 * where {@code {xmlFile}} is the name of the original mapping file.
	 * </p>
	 * If a cached {@code {xmlFile}.bin} exists and is newer than {@code {xmlFile}}, the {@code {xmlFile}.bin}
	 * file will be read directly. Otherwise {@code {xmlFile}} is read and then serialized to {@code {xmlFile}.bin} for
	 * use the next time.
	 *
	 * @param file The cacheable mapping file to be added, {@code {xmlFile}} in above discussion.
	 *
	 * @return this (for method chaining purposes)
	 */
	public MetadataSources addCacheableFile(File file) {
		Origin origin = new Origin( SourceType.FILE, file.getAbsolutePath() );
		File cachedFile = determineCachedDomFile( file );

		try {
			return addCacheableFileStrictly( file );
		}
		catch ( SerializationException e ) {
			LOG.unableToDeserializeCache( cachedFile.getName(), e );
		}
		catch ( FileNotFoundException e ) {
			LOG.cachedFileNotFound( cachedFile.getName(), e );
		}
		
		final FileInputStream inputStream;
		try {
			inputStream = new FileInputStream( file );
		}
		catch ( FileNotFoundException e ) {
			throw new MappingNotFoundException( origin );
		}

		LOG.readingMappingsFromFile( file.getPath() );
		BindResult metadataXml = add( inputStream, origin, true );

		try {
			LOG.debugf( "Writing cache file for: %s to: %s", file, cachedFile );
			SerializationHelper.serialize( ( Serializable ) metadataXml, new FileOutputStream( cachedFile ) );
		}
		catch ( Exception e ) {
			LOG.unableToWriteCachedFile( cachedFile.getName(), e.getMessage() );
		}

		return this;
	}

	/**
	 * <b>INTENDED FOR TESTSUITE USE ONLY!</b>
	 * <p/>
	 * Much like {@link #addCacheableFile(File)} except that here we will fail immediately if
	 * the cache version cannot be found or used for whatever reason
	 *
	 * @param file The xml file, not the bin!
	 *
	 * @return The dom "deserialized" from the cached file.
	 *
	 * @throws SerializationException Indicates a problem deserializing the cached dom tree
	 * @throws FileNotFoundException Indicates that the cached file was not found or was not usable.
	 */
	public MetadataSources addCacheableFileStrictly(File file) throws SerializationException, FileNotFoundException {
		File cachedFile = determineCachedDomFile( file );
		
		final boolean useCachedFile = file.exists()
				&& cachedFile.exists()
				&& file.lastModified() < cachedFile.lastModified();

		if ( ! useCachedFile ) {
			throw new FileNotFoundException( "Cached file could not be found or could not be used" );
		}

		LOG.readingCachedMappings( cachedFile );
		bindResultList.add( (BindResult) SerializationHelper.deserialize( new FileInputStream( cachedFile ) ) );
		return this;
	}

	private File determineCachedDomFile(File xmlFile) {
		return new File( xmlFile.getAbsolutePath() + ".bin" );
	}

	/**
	 * Read metadata from an {@link InputStream}.
	 *
	 * @param xmlInputStream The input stream containing a DOM.
	 *
	 * @return this (for method chaining purposes)
	 */
	public MetadataSources addInputStream(InputStream xmlInputStream) {
		add( xmlInputStream, new Origin( SourceType.INPUT_STREAM, Origin.UNKNOWN_FILE_PATH ), false );
		return this;
	}

	/**
	 * Read mappings from a {@link URL}
	 *
	 * @param url The url for the mapping document to be read.
	 *
	 * @return this (for method chaining purposes)
	 */
	public MetadataSources addURL(URL url) {
		final String urlExternalForm = url.toExternalForm();
		LOG.debugf( "Reading mapping document from URL : %s", urlExternalForm );

		final Origin origin = new Origin( SourceType.URL, urlExternalForm );
		try {
			add( url.openStream(), origin, true );
		}
		catch ( IOException e ) {
			throw new MappingNotFoundException( "Unable to open url stream [" + urlExternalForm + "]", e, origin );
		}
		return this;
	}

	/**
	 * Read mappings from a DOM {@link Document}
	 *
	 * @param document The DOM document
	 *
	 * @return this (for method chaining purposes)
	 */
	@Deprecated
	public MetadataSources addDocument(Document document) {
		final Origin origin = new Origin( SourceType.DOM, Origin.UNKNOWN_FILE_PATH );
		bindResultList.add( new BindResult( new DOMSource( document ), origin ) );
		return this;
	}

	/**
	 * Read all mappings from a jar file.
	 * <p/>
	 * Assumes that any file named <tt>*.hbm.xml</tt> is a mapping document.
	 *
	 * @param jar a jar file
	 *
	 * @return this (for method chaining purposes)
	 */
	public MetadataSources addJar(File jar) {
		LOG.debugf( "Seeking mapping documents in jar file : %s", jar.getName() );
		final Origin origin = new Origin( SourceType.JAR, jar.getAbsolutePath() );
		try {
			JarFile jarFile = new JarFile( jar );
			try {
				Enumeration jarEntries = jarFile.entries();
				while ( jarEntries.hasMoreElements() ) {
					final ZipEntry zipEntry = (ZipEntry) jarEntries.nextElement();
					if ( zipEntry.getName().endsWith( ".hbm.xml" ) ) {
						LOG.tracef( "found mapping document : %s", zipEntry.getName() );
						try {
							add( jarFile.getInputStream( zipEntry ), origin, true );
						}
						catch ( Exception e ) {
							throw new MappingException( "could not read mapping documents", e, origin );
						}
					}
				}
			}
			finally {
				try {
					jarFile.close();
				}
				catch ( Exception ignore ) {
				}
			}
		}
		catch ( IOException e ) {
			throw new MappingNotFoundException( e, origin );
		}
		return this;
	}

	/**
	 * Read all mapping documents from a directory tree.
	 * <p/>
	 * Assumes that any file named <tt>*.hbm.xml</tt> is a mapping document.
	 *
	 * @param dir The directory
	 *
	 * @return this (for method chaining purposes)
	 *
	 * @throws org.hibernate.MappingException Indicates problems reading the jar file or
	 * processing the contained mapping documents.
	 */
	public MetadataSources addDirectory(File dir) {
		File[] files = dir.listFiles();
		if ( files != null && files.length > 0 ) {
			for ( File file : files ) {
				if ( file.isDirectory() ) {
					addDirectory( file );
				}
				else if ( file.getName().endsWith( ".hbm.xml" ) ) {
					addFile( file );
				}
			}
		}
		return this;
	}

	/**
	 * @deprecated Use {@link #indexKnownClasses} instead
	 */
	@Deprecated
	public IndexView buildJandexView() {
		return buildJandexView( false );
	}

	/**
	 * Create a Jandex IndexView from scratch given the sources information contained here.
	 *
	 * @param autoIndexMemberTypes Should the types of class members automatically be added to the built index?
	 *
	 * @return The built Jandex Index
	 *
	 * @deprecated Use {@link #indexKnownClasses} instead
	 */
	@Deprecated
	public IndexView buildJandexView(boolean autoIndexMemberTypes) {
		JandexInitManager jandexInitManager = new JandexInitManager(
				null,
				new ClassLoaderAccessImpl(
						null,
						getServiceRegistry().getService( ClassLoaderService.class )
				),
				autoIndexMemberTypes
		);
		JandexIndexBuilder.process( jandexInitManager, this );
		return jandexInitManager.buildIndex();
	}

	public void indexKnownClasses(JandexInitManager jandexInitManager) {
		JandexIndexBuilder.process( jandexInitManager, this );
	}

	public static class JandexIndexBuilder {
		private static final Logger log = Logger.getLogger( JandexIndexBuilder.class );

		private final JandexInitManager jandexInitManager;

		private JandexIndexBuilder(JandexInitManager jandexInitManager) {
			this.jandexInitManager = jandexInitManager;
		}

		public static void process(JandexInitManager jandexInitManager, MetadataSources sources) {
			new JandexIndexBuilder( jandexInitManager ).process( sources );
		}

		private void process(MetadataSources sources) {
			// start off with any already-loaded Class references...
			for ( Class<?> clazz : sources.getAnnotatedClasses() ) {
				jandexInitManager.indexLoadedClass( clazz );
			}

			if ( sources.converterClasses != null ) {
				for ( Class<? extends AttributeConverter> converterClass : sources.converterClasses ) {
					jandexInitManager.indexLoadedClass( converterClass );
				}
			}

			for ( String className : sources.getAnnotatedClassNames() ) {
				jandexInitManager.indexClassName( DotName.createSimple( className ) );
			}

			// add package-info from the configured packages
			for ( String packageName : sources.getAnnotatedPackages() ) {
				// older code seemed to simply ignore packages that did not have package-info,
				// so do same
				try {
					jandexInitManager.indexResource( packageName.replace( '.', '/' ) + "/package-info.class" );
				}
				catch (Exception e) {
					log.debugf( "Skipping package [%s] which caused error indexing : %s", packageName, e.getMessage() );
				}
			}

			// the classes referenced in any orm.xml bindings (unless it is "metadata complete")
			for ( BindResult bindResult : sources.bindResultList ) {
				if ( JaxbEntityMappings.class.isInstance( bindResult.getRoot() ) ) {
					final JaxbEntityMappings ormXmlRoot = (JaxbEntityMappings) bindResult.getRoot();
					if ( !isMappingMetadataComplete( ormXmlRoot ) ) {
						indexOrmXmlReferences( ormXmlRoot );
					}
				}
			}
		}

		private DotName toDotName(String className, String packageName) {
			if ( StringHelper.isNotEmpty( packageName ) ) {
				if ( !className.contains( "." ) ) {
					return DotName.createSimple( packageName + '.' + className );
				}
			}

			return DotName.createSimple( className );
		}

		private boolean isMappingMetadataComplete(JaxbEntityMappings ormXmlRoot) {
			return ormXmlRoot.getPersistenceUnitMetadata() != null
					&& ormXmlRoot.getPersistenceUnitMetadata().getXmlMappingMetadataComplete() != null;
		}

		private void indexOrmXmlReferences(JaxbEntityMappings ormXmlRoot) {
			final String packageName = ormXmlRoot.getPackage();

			final JaxbPersistenceUnitMetadata puMetadata = ormXmlRoot.getPersistenceUnitMetadata();
			if ( puMetadata != null ) {
				final JaxbPersistenceUnitDefaults puDefaults = puMetadata.getPersistenceUnitDefaults();
				if ( puDefaults != null ) {
					indexEntityListeners( puDefaults.getEntityListeners(), packageName );
				}
			}

			for ( JaxbConverter jaxbConverter : ormXmlRoot.getConverter() ) {
				jandexInitManager.indexClassName( toDotName( jaxbConverter.getClazz(), packageName ) );
			}

			for ( JaxbEmbeddable jaxbEmbeddable : ormXmlRoot.getEmbeddable() ) {
				jandexInitManager.indexClassName( toDotName( jaxbEmbeddable.getClazz(), packageName ) );
			}

			for ( JaxbMappedSuperclass jaxbMappedSuperclass : ormXmlRoot.getMappedSuperclass() ) {
				jandexInitManager.indexClassName( toDotName( jaxbMappedSuperclass.getClazz(), packageName ) );
			}

			for ( JaxbEntity jaxbEntity : ormXmlRoot.getEntity() ) {
				jandexInitManager.indexClassName( toDotName( jaxbEntity.getClazz(), packageName ) );
				indexEntityListeners( jaxbEntity.getEntityListeners(), packageName );
			}
		}

		private void indexEntityListeners(JaxbEntityListeners listeners, String packageName) {
			if ( listeners == null ) {
				return;
			}

			for ( JaxbEntityListener listener : listeners.getEntityListener() ) {
				if ( StringHelper.isNotEmpty( listener.getClazz() ) ) {
					jandexInitManager.indexClassName( toDotName( listener.getClazz(), packageName ) );
				}
			}
		}
	}
}
