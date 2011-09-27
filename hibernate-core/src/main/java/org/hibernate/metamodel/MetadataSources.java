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
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.jboss.logging.Logger;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;

import org.hibernate.cfg.EJB3DTDEntityResolver;
import org.hibernate.cfg.EJB3NamingStrategy;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.internal.jaxb.JaxbRoot;
import org.hibernate.internal.jaxb.Origin;
import org.hibernate.internal.jaxb.SourceType;
import org.hibernate.metamodel.source.MappingException;
import org.hibernate.metamodel.source.MappingNotFoundException;
import org.hibernate.metamodel.source.internal.JaxbHelper;
import org.hibernate.metamodel.source.internal.MetadataBuilderImpl;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.classloading.spi.ClassLoaderService;

/**
 * @author Steve Ebersole
 */
public class MetadataSources {
	private static final Logger LOG = Logger.getLogger( MetadataSources.class );

	private List<JaxbRoot> jaxbRootList = new ArrayList<JaxbRoot>();
	private LinkedHashSet<Class<?>> annotatedClasses = new LinkedHashSet<Class<?>>();
	private LinkedHashSet<String> annotatedPackages = new LinkedHashSet<String>();

	private final JaxbHelper jaxbHelper;

	private final ServiceRegistry serviceRegistry;
	private final EntityResolver entityResolver;
	private final NamingStrategy namingStrategy;

	private final MetadataBuilderImpl metadataBuilder;

	public MetadataSources(ServiceRegistry serviceRegistry) {
		this( serviceRegistry, EJB3DTDEntityResolver.INSTANCE, EJB3NamingStrategy.INSTANCE );
	}

	public MetadataSources(ServiceRegistry serviceRegistry, EntityResolver entityResolver, NamingStrategy namingStrategy) {
		this.serviceRegistry = serviceRegistry;
		this.entityResolver = entityResolver;
		this.namingStrategy = namingStrategy;

		this.jaxbHelper = new JaxbHelper( this );
		this.metadataBuilder = new MetadataBuilderImpl( this );
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

	public ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	public NamingStrategy getNamingStrategy() {
		return namingStrategy;
	}

	public MetadataBuilder getMetadataBuilder() {
		return metadataBuilder;
	}

	public Metadata buildMetadata() {
		return getMetadataBuilder().buildMetadata();
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
			JaxbRoot jaxbRoot = jaxbHelper.unmarshal( inputStream, origin );
			jaxbRootList.add( jaxbRoot );
			return jaxbRoot;
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
		return this; // todo : implement method body
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
		return this; // todo : implement method body
	}

	/**
	 * Read metadata from an {@link InputStream}.
	 *
	 * @param xmlInputStream The input stream containing a DOM.
	 *
	 * @return this (for method chaining purposes)
	 */
	public MetadataSources addInputStream(InputStream xmlInputStream) {
		add( xmlInputStream, new Origin( SourceType.INPUT_STREAM, "<unknown>" ), false );
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
		final Origin origin = new Origin( SourceType.DOM, "<unknown>" );
		JaxbRoot jaxbRoot = jaxbHelper.unmarshal( document, origin );
		jaxbRootList.add( jaxbRoot );
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
		for ( File file : files ) {
			if ( file.isDirectory() ) {
				addDirectory( file );
			}
			else if ( file.getName().endsWith( ".hbm.xml" ) ) {
				addFile( file );
			}
		}
		return this;
	}
}
