/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot;

import org.hibernate.HibernateException;
import org.hibernate.Internal;
import org.hibernate.boot.archive.spi.InputStreamAccess;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.internal.CacheableFileXmlSource;
import org.hibernate.boot.jaxb.internal.FileXmlSource;
import org.hibernate.boot.jaxb.internal.InputStreamAccessXmlSource;
import org.hibernate.boot.jaxb.internal.InputStreamXmlSource;
import org.hibernate.boot.jaxb.internal.JarFileEntryXmlSource;
import org.hibernate.boot.jaxb.internal.UrlXmlSource;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.jaxb.spi.JaxbBindableMappingDescriptor;
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

import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static java.util.Collections.addAll;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;

/**
 * Entry point for working with sources of O/R mapping metadata, either
 * in the form of annotated classes, or as XML mapping documents.
 * <p>
 * Note that XML mappings may be expressed using the JPA {@code orm.xml}
 * format, or in Hibernate's legacy {@code .hbm.xml} format.
 * <p>
 * An instance of {@code MetadataSources} may be obtained simply by
 * instantiation, using {@link #MetadataSources() new MetadataSources()}.
 * The client must register sources and then call {@link #buildMetadata()},
 * or use {@link #getMetadataBuilder()} to customize how the sources are
 * processed (by registering naming strategies, etc).
 * <p>
 * As an alternative to working directly with {@code MetadataSources}, and
 * {@link Metadata}, a program may use {@link org.hibernate.cfg.Configuration}.
 *
 * @author Steve Ebersole
 *
 * @since 5.0
 */
public class MetadataSources implements Serializable {

	private static final CoreMessageLogger log = CoreLogging.messageLogger( MetadataSources.class );

	private final ServiceRegistry serviceRegistry;
	private final ClassLoaderService classLoaderService;

	private XmlMappingBinderAccess xmlMappingBinderAccess;

	private List<Binding<JaxbEntityMappingsImpl>> mappingXmlBindings;
	private List<Binding<JaxbHbmHibernateMapping>> hbmXmlBindings;

	private LinkedHashSet<Class<?>> annotatedClasses;
	private LinkedHashSet<String> annotatedClassNames;
	private LinkedHashSet<String> annotatedPackages;

	private Map<String,Class<?>> extraQueryImports;

	/**
	 * Create a new instance, using a default {@link BootstrapServiceRegistry}.
	 */
	public MetadataSources() {
		this( new BootstrapServiceRegistryBuilder().build() );
	}

	/**
	 * Create a new instance using the given {@link ServiceRegistry}.
	 *
	 * @param serviceRegistry The service registry to use.
	 */
	public MetadataSources(ServiceRegistry serviceRegistry) {
		this( serviceRegistry, null );
	}

	/**
	 * Create a new instance using the given {@link ServiceRegistry}.
	 *
	 * @param serviceRegistry The service registry to use.
	 */
	public MetadataSources(ServiceRegistry serviceRegistry, XmlMappingBinderAccess xmlMappingBinderAccess) {
		// service registry really should be either BootstrapServiceRegistry or StandardServiceRegistry type...
		if ( !isExpectedServiceRegistryType( serviceRegistry ) ) {
			if ( log.isDebugEnabled() ) {
				log.debugf(
						"Unexpected ServiceRegistry type [%s] encountered during building of MetadataSources; may cause " +
								"problems later attempting to construct MetadataBuilder",
						serviceRegistry.getClass().getName()
				);
			}
		}
		this.serviceRegistry = serviceRegistry;
		this.classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
		this.xmlMappingBinderAccess = xmlMappingBinderAccess;
	}

	protected static boolean isExpectedServiceRegistryType(ServiceRegistry serviceRegistry) {
		return serviceRegistry instanceof BootstrapServiceRegistry
			|| serviceRegistry instanceof StandardServiceRegistry;
	}

	public XmlMappingBinderAccess getXmlMappingBinderAccess() {
		if ( xmlMappingBinderAccess == null ) {
			xmlMappingBinderAccess = new XmlMappingBinderAccess( serviceRegistry );
		}
		return xmlMappingBinderAccess;
	}

	/**
	 * @deprecated Prefer {@linkplain #getMappingXmlBindings()} and/or {@linkplain #getHbmXmlBindings()}
	 */
	@Deprecated(since = "7.0")
	public List<? extends Binding<? extends JaxbBindableMappingDescriptor>> getXmlBindings() {
		if ( mappingXmlBindings == null && hbmXmlBindings == null ) {
			return emptyList();
		}
		else if ( hbmXmlBindings == null ) {
			return mappingXmlBindings;
		}
		else if ( mappingXmlBindings == null ) {
			return hbmXmlBindings;
		}
		else {
			final ArrayList<Binding<? extends JaxbBindableMappingDescriptor>> combined =
					arrayList( mappingXmlBindings.size() + hbmXmlBindings.size() );
			combined.addAll( mappingXmlBindings );
			combined.addAll( hbmXmlBindings );
			return combined;
		}
	}

	public List<Binding<JaxbEntityMappingsImpl>> getMappingXmlBindings() {
		return mappingXmlBindings == null ? emptyList() : mappingXmlBindings;
	}

	public List<Binding<JaxbHbmHibernateMapping>> getHbmXmlBindings() {
		return hbmXmlBindings == null ? emptyList() : hbmXmlBindings;
	}

	public Collection<String> getAnnotatedPackages() {
		return annotatedPackages == null ? emptySet() : annotatedPackages;
	}

	public Collection<Class<?>> getAnnotatedClasses() {
		return annotatedClasses == null ? emptySet() : annotatedClasses;
	}

	public Collection<String> getAnnotatedClassNames() {
		return annotatedClassNames == null ? emptySet() : annotatedClassNames;
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
		return getCustomBuilderOrDefault( new MetadataBuilderImpl(this) );
	}

	/**
	 * Get a builder for metadata where non-default options can be specified.
	 *
	 * @return The built metadata.
	 */
	@Internal
	public MetadataBuilder getMetadataBuilder(StandardServiceRegistry serviceRegistry) {
		return getCustomBuilderOrDefault( new MetadataBuilderImpl(this, serviceRegistry ) );
	}

	/**
	 * In case a custom {@link MetadataBuilderFactory} creates a custom builder,
	 * return that one, otherwise return the default builder.
	 */
	private MetadataBuilder getCustomBuilderOrDefault(MetadataBuilderImpl defaultBuilder) {

		final Collection<MetadataBuilderFactory> discoveredBuilderFactories =
				serviceRegistry.requireService( ClassLoaderService.class )
						.loadJavaServices( MetadataBuilderFactory.class );

		MetadataBuilder builder = null;
		List<String> activeFactoryNames = null;

		for ( MetadataBuilderFactory discoveredBuilderFactory : discoveredBuilderFactories) {
			final MetadataBuilder returnedBuilder =
					discoveredBuilderFactory.getMetadataBuilder( this, defaultBuilder );
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
	 * Shorthand form of calling {@link #getMetadataBuilder()} and using its
	 * {@link MetadataBuilder#build()} method in cases where the application
	 * wants to accept the defaults.
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
	public MetadataSources addAnnotatedClass(Class<?> annotatedClass) {
		if ( annotatedClasses == null ) {
			annotatedClasses = new LinkedHashSet<>();
		}
		annotatedClasses.add( annotatedClass );
		return this;
	}

	/**
	 * Vararg form of {@link #addAnnotatedClass(Class)}.
	 */
	public MetadataSources addAnnotatedClasses(Class<?>... annotatedClasses) {
		if ( annotatedClasses != null && annotatedClasses.length > 0 ) {
			if ( this.annotatedClasses == null ) {
				this.annotatedClasses = new LinkedHashSet<>();
			}
			addAll( this.annotatedClasses, annotatedClasses );
		}
		return this;
	}

	/**
	 * Read metadata from the annotations attached to the given class. The
	 * important distinction here is that the {@link Class} will not be
	 * accessed until later, which is important for on-the-fly bytecode
	 * enhancement
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
	 * Vararg form of {@link #addAnnotatedClassName(String)}.
	 */
	public MetadataSources addAnnotatedClassNames(String... annotatedClassNames) {
		if ( annotatedClassNames != null && annotatedClassNames.length > 0 ) {
			addAll( this.annotatedClassNames, annotatedClassNames );
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
	 * Read mappings as an application resourceName (i.e. classpath lookup).
	 *
	 * @param name The resource name
	 *
	 * @return this (for method chaining purposes)
	 */
	public MetadataSources addResource(String name) {
		final XmlMappingBinderAccess binderAccess = getXmlMappingBinderAccess();
		addXmlBinding( UrlXmlSource.fromResource( name, classLoaderService, binderAccess.getMappingBinder() ) );
		return this;
	}

	/**
	 * Read mappings from a particular XML file.
	 * <p>
	 * The given path is resolved using {@link File#File(String)}.
	 *
	 * @param path The path to a file
	 *
	 * @return this (for method chaining purposes)
	 *
	 * @see #addFile(File)
	 */
	public MetadataSources addFile(String path) {
		addFile( new File( path ) );
		return this;
	}

	/**
	 * Read mappings from a particular XML file.
	 *
	 * @param file The reference to the XML file
	 *
	 * @return this (for method chaining purposes)
	 */
	public MetadataSources addFile(File file) {
		final XmlMappingBinderAccess binderAccess = getXmlMappingBinderAccess();
		addXmlBinding( FileXmlSource.fromFile( file, binderAccess.getMappingBinder() ) );
		return this;
	}

	/**
	 * Add XML mapping bindings created from an arbitrary source by the
	 * {@linkplain #getXmlMappingBinderAccess() binder}.
	 *
	 * @param binding The binding
	 *
	 * @return this (for method chaining purposes)
	 */
	public MetadataSources addXmlBinding(Binding<?> binding) {
		if ( binding.getRoot() instanceof JaxbEntityMappingsImpl ) {
			//noinspection unchecked
			return addMappingXmlBinding( (Binding<JaxbEntityMappingsImpl>) binding );
		}
		else if ( binding.getRoot() instanceof JaxbHbmHibernateMapping ) {
			//noinspection unchecked
			return addHbmXmlBinding( (Binding<JaxbHbmHibernateMapping>) binding );
		}

		throw new UnsupportedOperationException( "Unknown type of binding : " + binding.getRoot() );
	}

	/**
	 * Add a {@linkplain Binding binding} for {@linkplain JaxbEntityMappingsImpl mapping.xsd} document
	 *
	 * @param binding The binding
	 *
	 * @return this (for method chaining purposes)
	 */
	public MetadataSources addMappingXmlBinding(Binding<JaxbEntityMappingsImpl> binding) {
		if ( mappingXmlBindings == null ) {
			mappingXmlBindings = new ArrayList<>();
		}
		mappingXmlBindings.add( binding );
		return this;
	}

	/**
	 * Add a {@linkplain Binding binding} for {@linkplain JaxbHbmHibernateMapping hbm.xsd} document
	 *
	 * @param binding The binding
	 *
	 * @return this (for method chaining purposes)
	 */
	public MetadataSources addHbmXmlBinding(Binding<JaxbHbmHibernateMapping> binding) {
		if ( hbmXmlBindings == null ) {
			hbmXmlBindings = new ArrayList<>();
		}
		hbmXmlBindings.add( binding );
		return this;
	}

	/**
	 * Add a {@link #addCacheableFile(File) cached mapping file}.
	 * <p>
	 * The given path is resolved using {@link File#File(String)}.
	 *
	 * @param path The path to a file
	 *
	 * @return this (for method chaining purposes)
	 *
	 * @see #addCacheableFile(File)
	 */
	public MetadataSources addCacheableFile(String path) {
		addCacheableFile( new File( path ) );
		return this;
	}

	/**
	 * Add a {@link #addCacheableFile(File) cached mapping file}.
	 * <p>
	 * The given path is resolved using {@link File#File(String)}.
	 *
	 * @param path The path to a file
	 *
	 * @return this (for method chaining purposes)
	 *
	 * @see #addCacheableFile(File)
	 */
	public MetadataSources addCacheableFile(String path, File cacheDirectory) {
		addCacheableFile( new File( path ), cacheDirectory );
		return this;
	}

	/**
	 * Add a cached mapping file. A cached file is a serialized representation of
	 * the DOM structure of a particular mapping. It is saved from a previous call
	 * as a file with the name {@code {xmlFile}.bin} where {@code {xmlFile}} is the
	 * name of the original mapping file.
	 * <p>
	 * If a cached {@code {xmlFile}.bin} exists and is newer than {@code {xmlFile}},
	 * the {@code {xmlFile}.bin} file will be read directly. Otherwise {@code {xmlFile}}
	 * is read and then serialized to {@code {xmlFile}.bin} for use the next time.
	 *
	 * @param file The cacheable mapping file to be added, {@code {xmlFile}} in above discussion.
	 *
	 * @return this (for method chaining purposes)
	 */
	public MetadataSources addCacheableFile(File file) {
		return addCacheableFile( file, file.getParentFile() );
	}

	/**
	 * Add a cached mapping file.  A cached file is a serialized representation of
	 * the DOM structure of a particular mapping. It is saved from a previous call
	 * as a file with the name {@code {xmlFile}.bin} where {@code {xmlFile}} is the
	 * name of the original mapping file.
	 * <p>
	 * If a cached {@code {xmlFile}.bin} exists and is newer than {@code {xmlFile}},
	 * the {@code {xmlFile}.bin} file will be read directly. Otherwise {@code {xmlFile}}
	 * is read and then serialized to {@code {xmlFile}.bin} for use the next time.
	 *
	 * @param file The cacheable mapping file to be added, {@code {xmlFile}} in above discussion.
	 *
	 * @return this (for method chaining purposes)
	 */
	public MetadataSources addCacheableFile(File file, File cacheDirectory) {
		final XmlMappingBinderAccess binderAccess = getXmlMappingBinderAccess();
		addXmlBinding( CacheableFileXmlSource.fromCacheableFile(
				file,
				cacheDirectory,
				false,
				binderAccess.getMappingBinder()
		) );
		return this;
	}

	/**
	 * <b>INTENDED FOR TESTSUITE USE ONLY!</b>
	 * <p>
	 * Much like {@link #addCacheableFile(File)} except that here we will fail
	 * immediately if the cache version cannot be found or used for whatever reason.
	 *
	 * @param file The xml file, not the bin!
	 *
	 * @return The dom "deserialized" from the cached file.
	 *
	 * @throws SerializationException Indicates a problem deserializing the cached dom tree
	 * @throws MappingNotFoundException Indicates that the cached file was not found or was not usable.
	 */
	public MetadataSources addCacheableFileStrictly(File file) throws SerializationException {
		final XmlMappingBinderAccess binderAccess = getXmlMappingBinderAccess();
		addXmlBinding( CacheableFileXmlSource.fromCacheableFile(
				file,
				null,
				true,
				binderAccess.getMappingBinder()
		) );
		return this;
	}

	/**
	 * <b>INTENDED FOR TESTSUITE USE ONLY!</b>
	 * <p>
	 * Much like {@link #addCacheableFile(File)} except that here we will fail
	 * immediately if the cache version cannot be found or used for whatever reason.
	 *
	 * @param file The xml file, not the bin!
	 *
	 * @return The dom "deserialized" from the cached file.
	 *
	 * @throws SerializationException Indicates a problem deserializing the cached dom tree
	 * @throws MappingNotFoundException Indicates that the cached file was not found or was not usable.
	 */
	public MetadataSources addCacheableFileStrictly(File file, File cacheDir) throws SerializationException {
		final XmlMappingBinderAccess binderAccess = getXmlMappingBinderAccess();
		addXmlBinding( CacheableFileXmlSource.fromCacheableFile(
				file,
				cacheDir,
				true,
				binderAccess.getMappingBinder()
		) );
		return this;
	}

	/**
	 * Read metadata from an {@link InputStream} access
	 *
	 * @param xmlInputStreamAccess Access to an input stream containing a DOM.
	 *
	 * @return this (for method chaining purposes)
	 */
	public MetadataSources addInputStream(InputStreamAccess xmlInputStreamAccess) {
		final XmlMappingBinderAccess binderAccess = getXmlMappingBinderAccess();
		addXmlBinding( InputStreamAccessXmlSource.fromStreamAccess( xmlInputStreamAccess, binderAccess.getMappingBinder() ) );
		return this;
	}

	/**
	 * Read metadata from an {@link InputStream}.
	 *
	 * @param xmlInputStream The input stream containing a DOM.
	 *
	 * @return this (for method chaining purposes)
	 */
	public MetadataSources addInputStream(InputStream xmlInputStream) {
		final XmlMappingBinderAccess binderAccess = getXmlMappingBinderAccess();
		addXmlBinding( InputStreamXmlSource.fromStream( xmlInputStream, binderAccess.getMappingBinder() ) );
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
		final XmlMappingBinderAccess binderAccess = getXmlMappingBinderAccess();
		addXmlBinding( UrlXmlSource.fromUrl( url, binderAccess.getMappingBinder() ) );
		return this;
	}

	/**
	 * Read all {@code .hbm.xml} mappings from a jar file.
	 * <p>
	 * Assumes that any file named {@code *.hbm.xml} is a mapping document.
	 * This method does not support {@code orm.xml} files!
	 *
	 * @param jar a jar file
	 *
	 * @return this (for method chaining purposes)
	 */
	public MetadataSources addJar(File jar) {
		final XmlMappingBinderAccess binderAccess = getXmlMappingBinderAccess();
		JarFileEntryXmlSource.fromJar( jar, binderAccess.getMappingBinder(), this::addXmlBinding );
		return this;
	}

	/**
	 * Read all {@code .hbm.xml} mapping documents from a directory tree.
	 * <p>
	 * Assumes that any file named {@code *.hbm.xml} is a mapping document.
	 * This method does not support {@code orm.xml} files!
	 *
	 * @param dir The directory
	 *
	 * @return this (for method chaining purposes)
	 *
	 * @throws org.hibernate.MappingException Indicates problems reading the jar file or
	 * processing the contained mapping documents.
	 */
	public MetadataSources addDirectory(File dir) {
		final File[] files = dir.listFiles();
		if ( files != null ) {
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
