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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.CacheRegionDefinition;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.jaxb.internal.JaxbMappingProcessor;
import org.hibernate.jaxb.spi.JaxbRoot;
import org.hibernate.jaxb.spi.Origin;
import org.hibernate.jaxb.spi.SourceType;
import org.hibernate.jaxb.spi.orm.JaxbEntityMappings;
import org.hibernate.metamodel.internal.MetadataBuilderImpl;
import org.hibernate.metamodel.internal.source.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JPADotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.internal.source.annotations.xml.mocker.EntityMappingsMocker;
import org.hibernate.metamodel.spi.source.InvalidMappingException;
import org.hibernate.metamodel.spi.source.MappingException;
import org.hibernate.metamodel.spi.source.MappingNotFoundException;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.SerializationException;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
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
	public static final String UNKNOWN_FILE_PATH = "<unknown>";
	
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class, MetadataSources.class.getName());
	
	/**
	 * temporary option
	 */
	public static final String USE_NEW_METADATA_MAPPINGS = "hibernate.test.new_metadata_mappings";

	private final ServiceRegistry serviceRegistry;
	private final JaxbMappingProcessor jaxbProcessor;
	private final List<CacheRegionDefinition> externalCacheRegionDefinitions = new ArrayList<CacheRegionDefinition>();
	private List<JaxbRoot> jaxbRootList = new ArrayList<JaxbRoot>();
	private LinkedHashSet<Class<?>> annotatedClasses = new LinkedHashSet<Class<?>>();
	private LinkedHashSet<String> annotatedClassNames = new LinkedHashSet<String>();
	private LinkedHashSet<String> annotatedPackages = new LinkedHashSet<String>();

	private boolean hasOrmXmlJaxbRoots;

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
		this.jaxbProcessor = new JaxbMappingProcessor( serviceRegistry );
	}

	protected static boolean isExpectedServiceRegistryType(ServiceRegistry serviceRegistry) {
		return BootstrapServiceRegistry.class.isInstance( serviceRegistry )
				|| StandardServiceRegistry.class.isInstance( serviceRegistry );
	}

	public List<JaxbRoot> getJaxbRootList() {
		return jaxbRootList;
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

	public List<CacheRegionDefinition> getExternalCacheRegionDefinitions() {
		return externalCacheRegionDefinitions;
	}

	public ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	public boolean hasOrmXmlJaxbRoots() {
		return hasOrmXmlJaxbRoots;
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

	private JaxbRoot add(InputStream inputStream, Origin origin, boolean close) {
		try {
			JaxbRoot jaxbRoot = jaxbProcessor.unmarshal( inputStream, origin );
			addJaxbRoot( jaxbRoot );
			return jaxbRoot;
		}
		catch ( Exception e ) {
			throw new InvalidMappingException( origin, e );
		}
		finally {
			if ( close ) {
				try {
					inputStream.close();
				}
				catch ( IOException ignore ) {
					LOG.trace( "Was unable to close input stream" );
				}
			}
		}
	}

	/**
	 * Read a mapping as an application resource using the convention that a class named {@code foo.bar.Foo} is
	 * mapped by a file named {@code foo/bar/Foo.hbm.xml} which can be resolved as a classpath resource.
	 *
	 * @param entityClass The mapped class. Cannot be {@code null} null.
	 *
	 * @return this (for method chaining purposes)
	 */
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
		JaxbRoot metadataXml = add( inputStream, origin, true );

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
	 * @param xmlFile The xml file, not the bin!
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
		addJaxbRoot( ( JaxbRoot ) SerializationHelper.deserialize( new FileInputStream( cachedFile ) ) );
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
		add( xmlInputStream, new Origin( SourceType.INPUT_STREAM, UNKNOWN_FILE_PATH ), false );
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
	public MetadataSources addDocument(Document document) {
		final Origin origin = new Origin( SourceType.DOM, UNKNOWN_FILE_PATH );
		JaxbRoot jaxbRoot = jaxbProcessor.unmarshal( document, origin );
		addJaxbRoot( jaxbRoot );
		return this;
	}

	private void addJaxbRoot(JaxbRoot jaxbRoot) {
		hasOrmXmlJaxbRoots = hasOrmXmlJaxbRoots || JaxbEntityMappings.class.isInstance( jaxbRoot.getRoot() );
		jaxbRootList.add( jaxbRoot );
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

	public MetadataSources addCacheRegionDefinitions(List<CacheRegionDefinition> cacheRegionDefinitions) {
		externalCacheRegionDefinitions.addAll( cacheRegionDefinitions );
		return this;
	}

	@SuppressWarnings("unchecked")
	public IndexView wrapJandexView(IndexView jandexView) {
		if ( ! hasOrmXmlJaxbRoots ) {
			// no need to wrap
			return jandexView;
		}

		final List<JaxbEntityMappings> collectedOrmXmlMappings = new ArrayList<JaxbEntityMappings>();
		for ( JaxbRoot jaxbRoot : getJaxbRootList() ) {
			if ( JaxbEntityMappings.class.isInstance( jaxbRoot.getRoot() ) ) {
				collectedOrmXmlMappings.add( ( (JaxbRoot<JaxbEntityMappings>) jaxbRoot ).getRoot() );
			}
		}

		if ( collectedOrmXmlMappings.isEmpty() ) {
			// log a warning or something
		}

		return new EntityMappingsMocker( collectedOrmXmlMappings, jandexView, serviceRegistry ).mockNewIndex();
	}
	
	public IndexView buildJandexView() {
		return buildJandexView( false );
	}

	public IndexView buildJandexView(boolean autoIndexMemberTypes) {
		// create a jandex index from the annotated classes

		Indexer indexer = new Indexer();
		
		Set<Class<?>> processedClasses = new HashSet<Class<?>>();
		for ( Class<?> clazz : getAnnotatedClasses() ) {
			indexClass( clazz, indexer, processedClasses, autoIndexMemberTypes );
		}

		for ( String className : getAnnotatedClassNames() ) {
			indexResource( className.replace( '.', '/' ) + ".class", indexer );
		}

		// add package-info from the configured packages
		for ( String packageName : getAnnotatedPackages() ) {
			indexResource( packageName.replace( '.', '/' ) + "/package-info.class", indexer );
		}

		return wrapJandexView( indexer.complete() );
	}

	private void indexClass(Class clazz, Indexer indexer, Set<Class<?>> processedClasses, boolean autoIndexMemberTypes) {
		if ( clazz == null || clazz == Object.class
				|| processedClasses.contains( clazz ) ) {
			return;
		}
		
		processedClasses.add( clazz );

		ClassInfo classInfo = indexResource(
				clazz.getName().replace( '.', '/' ) + ".class", indexer );

		// index all super classes of the specified class. Using org.hibernate.cfg.Configuration it was not
		// necessary to add all annotated classes. Entities would be enough. Mapped superclasses would be
		// discovered while processing the annotations. To keep this behavior we index all classes in the
		// hierarchy (see also HHH-7484)
		indexClass( clazz.getSuperclass(), indexer, processedClasses, autoIndexMemberTypes );
		
		// Similarly, index any inner class.
		for ( Class innerClass : clazz.getDeclaredClasses() ) {
			indexClass( innerClass, indexer, processedClasses, autoIndexMemberTypes );
		}
		
		if ( autoIndexMemberTypes ) {
			// If autoIndexMemberTypes, don't require @Embeddable
			// classes to be explicitly identified.
			// Automatically find them by checking the fields' types.
			for ( Class<?> fieldType : ReflectHelper.getMemberTypes( clazz ) ) {		
				if ( !fieldType.isPrimitive() && fieldType != Object.class ) {
					try {
						IndexView fieldIndex = JandexHelper.indexForClass(
								serviceRegistry.getService( ClassLoaderService.class ),
								fieldType );
						if ( !fieldIndex.getAnnotations(
								JPADotNames.EMBEDDABLE ).isEmpty() ) {
							indexClass( fieldType, indexer, processedClasses, autoIndexMemberTypes );
						}
					} catch ( Exception e ) {
						// do nothing
					}
				}
			}
			
			// Also check for classes within a @Target annotation.
			for ( AnnotationInstance targetAnnotation : JandexHelper.getAnnotations(
					classInfo, HibernateDotNames.TARGET ) ) {
				String targetClassName = targetAnnotation.value().asClass().name()
						.toString();
				Class<?> targetClass = serviceRegistry.getService(
						ClassLoaderService.class ).classForName( targetClassName );
				indexClass(targetClass, indexer, processedClasses, autoIndexMemberTypes );
			}
		}
	}

	private ClassInfo indexResource(String resourceName, Indexer indexer) {
		ClassLoaderService classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
		
		if ( classLoaderService.locateResource( resourceName ) != null ) {
			InputStream stream = classLoaderService.locateResourceStream( resourceName );
			try {
				return indexer.index( stream );
			}
			catch ( IOException e ) {
				throw new HibernateException( "Unable to open input stream for resource " + resourceName, e );
			}
		}
		
		return null;
	}
}
