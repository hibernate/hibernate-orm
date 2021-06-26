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
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.hibernate.boot.jaxb.internal.XmlSources;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.jaxb.spi.XmlSource;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.MetadataBuilderFactory;
import org.hibernate.boot.spi.XmlMappingBinderAccess;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
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
	private final ClassLoaderService classLoaderService;
	private final boolean disableXmlMappingBinders;

	private XmlMappingBinderAccess xmlMappingBinderAccess;

	private List<Binding<?>> xmlBindings;
	private LinkedHashSet<Class<?>> annotatedClasses;
	private LinkedHashSet<String> annotatedClassNames;
	private LinkedHashSet<String> annotatedPackages;

	private Map<String,Class<?>> extraQueryImports;

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
			if ( LOG.isDebugEnabled() ) {
				LOG.debugf(
						"Unexpected ServiceRegistry type [%s] encountered during building of MetadataSources; may cause " +
								"problems later attempting to construct MetadataBuilder",
						serviceRegistry.getClass().getName()
				);
			}
		}
		this.serviceRegistry = serviceRegistry;
		this.classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
		this.disableXmlMappingBinders = false;
	}

	/**
	 * Consider this an SPI, used by Quarkus
	 * @param serviceRegistry
	 * @param disableXmlMappingBinders
	 */
	public MetadataSources(ServiceRegistry serviceRegistry, boolean disableXmlMappingBinders) {
		Objects.requireNonNull( serviceRegistry );
		this.serviceRegistry = serviceRegistry;
		this.classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
		this.disableXmlMappingBinders = disableXmlMappingBinders;
	}

	protected static boolean isExpectedServiceRegistryType(ServiceRegistry serviceRegistry) {
		return serviceRegistry instanceof BootstrapServiceRegistry
				|| serviceRegistry instanceof StandardServiceRegistry;
	}

	public XmlMappingBinderAccess getXmlMappingBinderAccess() {
		if ( disableXmlMappingBinders ) {
			return null;
		}
		if ( xmlMappingBinderAccess == null ) {
			xmlMappingBinderAccess = new XmlMappingBinderAccess( serviceRegistry );
		}
		return xmlMappingBinderAccess;
	}

	public List<Binding<?>> getXmlBindings() {
		return xmlBindings == null ? Collections.emptyList() : xmlBindings;
	}

	public Collection<String> getAnnotatedPackages() {
		return annotatedPackages == null ? Collections.emptySet() : annotatedPackages;
	}

	public Collection<Class<?>> getAnnotatedClasses() {
		return annotatedClasses == null ? Collections.emptySet() : annotatedClasses;
	}

	public Collection<String> getAnnotatedClassNames() {
		return annotatedClassNames == null ? Collections.emptySet() : annotatedClassNames;
	}

	public Map<String,Class<?>> getExtraQueryImports() {
		return extraQueryImports;
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
	 * @deprecated Use {@link #getMetadataBuilder()} instead
	 */
	@Deprecated
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
					activeFactoryNames = new ArrayList<>();
				}
				activeFactoryNames.add( discoveredBuilderFactory.getClass().getName() );
				builder = returnedBuilder;
			}
		}

		if ( activeFactoryNames != null && activeFactoryNames.size() > 1 ) {
			throw new HibernateException(
					"Multiple active MetadataBuilder definitions were discovered : " +
							String.join(", ", activeFactoryNames)
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
		if ( annotatedClasses == null ) {
			annotatedClasses = new LinkedHashSet<>();
		}
		annotatedClasses.add( annotatedClass );
		return this;
	}

	/**
	 * Var-arg form of {@link #addAnnotatedClass}
	 */
	public MetadataSources addAnnotatedClasses(Class<?>... annotatedClasses) {
		if ( annotatedClasses != null && annotatedClasses.length > 0 ) {
			if ( this.annotatedClasses == null ) {
				this.annotatedClasses = new LinkedHashSet<>();
			}
			Collections.addAll( this.annotatedClasses, annotatedClasses );
		}
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
		if ( annotatedClassNames == null ) {
			annotatedClassNames = new LinkedHashSet<>();
		}
		annotatedClassNames.add( annotatedClassName );
		return this;
	}

	/**
	 * Var-arg form of {@link #addAnnotatedClassName}
	 */
	public MetadataSources addAnnotatedClassNames(String... annotatedClassNames) {
		if ( annotatedClassNames != null && annotatedClassNames.length > 0 ) {
			Collections.addAll( this.annotatedClassNames, annotatedClassNames );
		}
		return this;
	}

	public MetadataSources addQueryImport(String importedName, Class<?> target) {
		if ( extraQueryImports == null ) {
			extraQueryImports = new HashMap<>();
		}

		extraQueryImports.put( importedName, target );

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

		addPackageInternal( packageName );
		return this;
	}

	private void addPackageInternal(String packageName) {
		if ( annotatedPackages == null ) {
			annotatedPackages = new LinkedHashSet<>();
		}
		annotatedPackages.add( packageName );
	}

	/**
	 * Read package-level metadata.
	 *
	 * @param packageRef Java Package reference
	 *
	 * @return this (for method chaining)
	 */
	public MetadataSources addPackage(Package packageRef) {
		addPackageInternal( packageRef.getName() );
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
		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "adding resource mappings from class convention : %s", entityClass.getName() );
		}
		final String mappingResourceName = entityClass.getName().replace( '.', '/' ) + ".hbm.xml";
		addResource( mappingResourceName );
		return this;
	}

	/**
	 * Read mappings as an application resourceName (i.e. classpath lookup).
	 *
	 * @param name The resource name
	 *
	 * @return this (for method chaining purposes)
	 */
	public MetadataSources addResource(String name) {
		final XmlSource xmlSource = XmlSources.fromResource( name, classLoaderService );
		final XmlMappingBinderAccess binderAccess = getXmlMappingBinderAccess();
		getXmlBindingsForWrite().add( xmlSource.doBind( binderAccess.getMappingBinder()  ) );
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
		final XmlSource xmlSource = XmlSources.fromFile( file );
		final XmlMappingBinderAccess binderAccess = getXmlMappingBinderAccess();
		getXmlBindingsForWrite().add( xmlSource.doBind( binderAccess.getMappingBinder()  ) );
		return this;
	}

	/**
	 * Add XML mapping bindings created from an arbitrary source by the {@link #getXmlMappingBinderAccess() binder}.
	 *
	 * @param binding The binding.
	 *
	 * @return this (for method chaining purposes)
	 */
	public MetadataSources addXmlBinding(Binding<?> binding) {
		getXmlBindingsForWrite().add( binding );
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
		addCacheableFile( new File( path ) );
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
	public MetadataSources addCacheableFile(String path, File cacheDirectory) {
		addCacheableFile( new File( path ), cacheDirectory );
		return this;
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
		return addCacheableFile( file, file.getParentFile() );
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
	public MetadataSources addCacheableFile(File file, File cacheDirectory) {
		final XmlSource xmlSource = XmlSources.fromCacheableFile( file, cacheDirectory );
		final XmlMappingBinderAccess binderAccess = getXmlMappingBinderAccess();
		getXmlBindingsForWrite().add( xmlSource.doBind( binderAccess.getMappingBinder()  ) );
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
	public MetadataSources addCacheableFileStrictly(File file) throws SerializationException {
		final XmlSource xmlSource = XmlSources.fromCacheableFile( file, true );
		final XmlMappingBinderAccess binderAccess = getXmlMappingBinderAccess();
		getXmlBindingsForWrite().add( xmlSource.doBind( binderAccess.getMappingBinder()  ) );
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
	public MetadataSources addCacheableFileStrictly(File file, File cacheDir) throws SerializationException {
		final XmlSource xmlSource = XmlSources.fromCacheableFile( file, cacheDir, true );
		final XmlMappingBinderAccess binderAccess = getXmlMappingBinderAccess();
		getXmlBindingsForWrite().add( xmlSource.doBind( binderAccess.getMappingBinder()  ) );
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
		final XmlSource xmlSource = XmlSources.fromStream( xmlInputStreamAccess );
		final XmlMappingBinderAccess binderAccess = getXmlMappingBinderAccess();
		getXmlBindingsForWrite().add( xmlSource.doBind( binderAccess.getMappingBinder()  ) );
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
		final XmlSource xmlSource = XmlSources.fromStream( xmlInputStream );
		final XmlMappingBinderAccess binderAccess = getXmlMappingBinderAccess();
		getXmlBindingsForWrite().add( xmlSource.doBind( binderAccess.getMappingBinder()  ) );
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
		final XmlSource xmlSource = XmlSources.fromUrl( url );
		final XmlMappingBinderAccess binderAccess = getXmlMappingBinderAccess();
		getXmlBindingsForWrite().add( xmlSource.doBind( binderAccess.getMappingBinder()  ) );
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
		final XmlSource xmlSource = XmlSources.fromDocument( document );
		final XmlMappingBinderAccess binderAccess = getXmlMappingBinderAccess();
		getXmlBindingsForWrite().add( xmlSource.doBind( binderAccess.getMappingBinder()  ) );
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
		final XmlMappingBinderAccess binderAccess = getXmlMappingBinderAccess();
		XmlSources.fromJar(
				jar,
				xmlSource -> getXmlBindingsForWrite().add( xmlSource.doBind( binderAccess.getMappingBinder() ) )
		);
		return this;
	}

	private List<Binding<?>> getXmlBindingsForWrite() {
		if ( xmlBindings == null ) {
			xmlBindings = new ArrayList<>();
		}
		return xmlBindings;
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
