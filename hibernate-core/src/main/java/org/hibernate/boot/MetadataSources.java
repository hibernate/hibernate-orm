/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import javax.xml.transform.dom.DOMSource;

import org.hibernate.HibernateException;
import org.hibernate.boot.archive.spi.InputStreamAccess;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.internal.CacheableFileXmlSource;
import org.hibernate.boot.jaxb.internal.JarFileEntryXmlSource;
import org.hibernate.boot.jaxb.internal.JaxpSourceXmlSource;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.MetadataBuilderFactory;
import org.hibernate.boot.spi.XmlMappingBinderAccess;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.SerializationException;
import org.w3c.dom.Document;

/**
 * Entry point into working with sources of metadata information (mapping XML, annotations).   Tell Hibernate
 * about sources and then call {@link #buildMetadata()}, or use {@link #getMetadataBuilder()} to customize
 * how sources are processed (naming strategies, etc).
 *
 * @author Steve Ebersole
 *
 * @since 5.0
 */
public class MetadataSources implements Serializable {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( MetadataSources.class );

	private final ServiceRegistry serviceRegistry;

	private XmlMappingBinderAccess xmlMappingBinderAccess;

	private List<Binding> xmlBindings = new ArrayList<Binding>();
	private LinkedHashSet<Class<?>> annotatedClasses = new LinkedHashSet<Class<?>>();
	private LinkedHashSet<String> annotatedClassNames = new LinkedHashSet<String>();
	private LinkedHashSet<String> annotatedPackages = new LinkedHashSet<String>();

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
		this.xmlMappingBinderAccess = new XmlMappingBinderAccess( serviceRegistry );
	}

	protected static boolean isExpectedServiceRegistryType(ServiceRegistry serviceRegistry) {
		return BootstrapServiceRegistry.class.isInstance( serviceRegistry )
				|| StandardServiceRegistry.class.isInstance( serviceRegistry );
	}

	public XmlMappingBinderAccess getXmlMappingBinderAccess() {
		return xmlMappingBinderAccess;
	}

	public List<Binding> getXmlBindings() {
		return xmlBindings;
	}

	public Collection<String> getAnnotatedPackages() {
		return annotatedPackages;
	}

	public Collection<Class<?>> getAnnotatedClasses() {
		return annotatedClasses;
	}

	public Collection<String> getAnnotatedClassNames() {
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
		MetadataBuilderImpl defaultBuilder = new MetadataBuilderImpl( this );
		return getCustomBuilderOrDefault( defaultBuilder );
	}

	/**
	 * Get a builder for metadata where non-default options can be specified.
	 *
	 * @return The built metadata.
	 */
	public MetadataBuilder getMetadataBuilder(StandardServiceRegistry serviceRegistry) {
		MetadataBuilderImpl defaultBuilder = new MetadataBuilderImpl( this, serviceRegistry );
		return getCustomBuilderOrDefault( defaultBuilder );
	}

	/**
	 * In case a custom {@link MetadataBuilderFactory} creates a custom builder, return that one, otherwise the default
	 * builder.
	 */
	private MetadataBuilder getCustomBuilderOrDefault(MetadataBuilderImpl defaultBuilder) {
		final ClassLoaderService cls = serviceRegistry.getService( ClassLoaderService.class );
		final java.util.Collection<MetadataBuilderFactory> discoveredBuilderFactories = cls.loadJavaServices( MetadataBuilderFactory.class );

		MetadataBuilder builder = null;
		List<String> activeFactoryNames = null;

		for ( MetadataBuilderFactory discoveredBuilderFactory : discoveredBuilderFactories ) {
			final MetadataBuilder returnedBuilder = discoveredBuilderFactory.getMetadataBuilder( this, defaultBuilder );
			if ( returnedBuilder != null ) {
				if ( activeFactoryNames == null ) {
					activeFactoryNames = new ArrayList<String>();
				}
				activeFactoryNames.add( discoveredBuilderFactory.getClass().getName() );
				builder = returnedBuilder;
			}
		}

		if ( activeFactoryNames != null && activeFactoryNames.size() > 1 ) {
			throw new HibernateException(
					"Multiple active MetadataBuilder definitions were discovered : " +
							StringHelper.join( ", ", activeFactoryNames )
			);
		}

		return builder != null ? builder : defaultBuilder;
	}

	/**
	 * Short-hand form of calling {@link #getMetadataBuilder()} and using its
	 * {@link org.hibernate.boot.MetadataBuilder#build()} method in cases where the application wants
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
	 * Read metadata from the annotations attached to the given class.  The important
	 * distinction here is that the {@link Class} will not be accessed until later
	 * which is important for on-the-fly bytecode-enhancement
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
	 * Read package-level metadata.
	 *
	 * @param packageRef Java Package reference
	 *
	 * @return this (for method chaining)
	 */
	public MetadataSources addPackage(Package packageRef) {
		annotatedPackages.add( packageRef.getName() );
		return this;
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
	 * Read mappings as a application resourceName (i.e. classpath lookup).
	 *
	 * @param name The resource name
	 *
	 * @return this (for method chaining purposes)
	 */
	public MetadataSources addResource(String name) {
		xmlBindings.add( getXmlMappingBinderAccess().bind( name ) );
		return this;
	}

	/**
	 * Read mappings from a particular XML file
	 *
	 * @param path The path to a file.  Expected to be resolvable by {@link java.io.File#File(String)}
	 *
	 * @return this (for method chaining purposes)
	 *
	 * @see #addFile(java.io.File)
	 */
	public MetadataSources addFile(String path) {
		addFile( new File( path ) );
		return this;
	}

	/**
	 * Read mappings from a particular XML file
	 *
	 * @param file The reference to the XML file
	 *
	 * @return this (for method chaining purposes)
	 */
	public MetadataSources addFile(File file) {
		xmlBindings.add( getXmlMappingBinderAccess().bind( file ) );
		return this;
	}

	/**
	 * See {@link #addCacheableFile(java.io.File)} for description
	 *
	 * @param path The path to a file.  Expected to be resolvable by {@link java.io.File#File(String)}
	 *
	 * @return this (for method chaining purposes)
	 *
	 * @see #addCacheableFile(java.io.File)
	 */
	public MetadataSources addCacheableFile(String path) {
		final Origin origin = new Origin( SourceType.FILE, path );
		addCacheableFile( origin, new File( path ) );
		return this;
	}

	private void addCacheableFile(Origin origin, File file) {
		xmlBindings.add( new CacheableFileXmlSource( origin, file, false ).doBind( getXmlMappingBinderAccess().getMappingBinder() ) );
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
		final Origin origin = new Origin( SourceType.FILE, file.getName() );
		addCacheableFile( origin, file );
		return this;
	}

	/**
	 * <b>INTENDED FOR TESTSUITE USE ONLY!</b>
	 * <p/>
	 * Much like {@link #addCacheableFile(java.io.File)} except that here we will fail immediately if
	 * the cache version cannot be found or used for whatever reason
	 *
	 * @param file The xml file, not the bin!
	 *
	 * @return The dom "deserialized" from the cached file.
	 *
	 * @throws org.hibernate.type.SerializationException Indicates a problem deserializing the cached dom tree
	 * @throws java.io.FileNotFoundException Indicates that the cached file was not found or was not usable.
	 */
	public MetadataSources addCacheableFileStrictly(File file) throws SerializationException, FileNotFoundException {
		final Origin origin = new Origin( SourceType.FILE, file.getAbsolutePath() );
		xmlBindings.add( new CacheableFileXmlSource( origin, file, true ).doBind( getXmlMappingBinderAccess().getMappingBinder() ) );
		return this;
	}

	/**
	 * Read metadata from an {@link java.io.InputStream} access
	 *
	 * @param xmlInputStreamAccess Access to an input stream containing a DOM.
	 *
	 * @return this (for method chaining purposes)
	 */
	public MetadataSources addInputStream(InputStreamAccess xmlInputStreamAccess) {
		xmlBindings.add( getXmlMappingBinderAccess().bind( xmlInputStreamAccess ) );
		return this;
	}

	/**
	 * Read metadata from an {@link java.io.InputStream}.
	 *
	 * @param xmlInputStream The input stream containing a DOM.
	 *
	 * @return this (for method chaining purposes)
	 */
	public MetadataSources addInputStream(InputStream xmlInputStream) {
		xmlBindings.add( getXmlMappingBinderAccess().bind( xmlInputStream ) );
		return this;
	}

	/**
	 * Read mappings from a {@link java.net.URL}
	 *
	 * @param url The url for the mapping document to be read.
	 *
	 * @return this (for method chaining purposes)
	 */
	public MetadataSources addURL(URL url) {
		xmlBindings.add( getXmlMappingBinderAccess().bind( url ) );
		return this;
	}

	/**
	 * Read mappings from a DOM {@link org.w3c.dom.Document}
	 *
	 * @param document The DOM document
	 *
	 * @return this (for method chaining purposes)
	 *
	 * @deprecated since 5.0.  Use one of the other methods for passing mapping source(s).
	 */
	@Deprecated
	public MetadataSources addDocument(Document document) {
		final Origin origin = new Origin( SourceType.DOM, Origin.UNKNOWN_FILE_PATH );
		xmlBindings.add( new JaxpSourceXmlSource( origin, new DOMSource( document ) ).doBind( getXmlMappingBinderAccess().getMappingBinder() ) );
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
						xmlBindings.add(
								new JarFileEntryXmlSource( origin, jarFile, zipEntry ).doBind( getXmlMappingBinderAccess().getMappingBinder() )
						);
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

}
