/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.jboss.logging.Logger;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;

import org.hibernate.DuplicateMappingException;
import org.hibernate.cfg.EJB3DTDEntityResolver;
import org.hibernate.cfg.EJB3NamingStrategy;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.mapping.FetchProfile;
import org.hibernate.mapping.MetadataSource;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.PluralAttributeBinding;
import org.hibernate.metamodel.relational.Database;
import org.hibernate.metamodel.source.MappingException;
import org.hibernate.metamodel.source.MappingNotFoundException;
import org.hibernate.metamodel.source.Metadata;
import org.hibernate.metamodel.source.Origin;
import org.hibernate.metamodel.source.SourceType;
import org.hibernate.metamodel.source.annotations.AnnotationBinder;
import org.hibernate.metamodel.source.hbm.HibernateXmlBinder;
import org.hibernate.metamodel.source.spi.MetadataImplementor;
import org.hibernate.service.BasicServiceRegistry;
import org.hibernate.service.classloading.spi.ClassLoaderService;

/**
 * Container for configuration data while building and binding the metamodel
 *
 * @author Steve Ebersole
 */
public class MetadataImpl implements Metadata, MetadataImplementor, Serializable {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger( CoreMessageLogger.class, MetadataImpl.class.getName() );

	private final BasicServiceRegistry serviceRegistry;

	private final Database database = new Database();

//	private final MetadataSourceQueue metadataSourceQueue;

	private final JaxbHelper jaxbHelper;
	private final AnnotationBinder annotationBinder;
	private final HibernateXmlBinder hibernateXmlBinder;

	private final EntityResolver entityResolver;
	private final NamingStrategy namingStrategy;
	private Map<String, EntityBinding> entityBindingMap = new HashMap<String, EntityBinding>();
	private Map<String, PluralAttributeBinding> collectionBindingMap = new HashMap<String, PluralAttributeBinding>();
	private Map<String, FetchProfile> fetchProfiles = new HashMap<String, FetchProfile>();
	private Map<String, String> imports;

	public MetadataImpl(BasicServiceRegistry serviceRegistry) {
		this( serviceRegistry, EJB3NamingStrategy.INSTANCE, EJB3DTDEntityResolver.INSTANCE );
	}

	public MetadataImpl(BasicServiceRegistry serviceRegistry, NamingStrategy namingStrategy, EntityResolver entityResolver) {
		this.serviceRegistry = serviceRegistry;
		this.namingStrategy = namingStrategy;
		this.entityResolver = entityResolver;
		this.jaxbHelper = new JaxbHelper( this );
		this.annotationBinder = new AnnotationBinder( this );
		this.hibernateXmlBinder = new HibernateXmlBinder( this );
//		this.metadataSourceQueue = new MetadataSourceQueue( this );
	}

	public BasicServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	public EntityResolver getEntityResolver() {
		return entityResolver;
	}

	public HibernateXmlBinder getHibernateXmlBinder() {
		return hibernateXmlBinder;
	}

	public AnnotationBinder getAnnotationBinder() {
		return annotationBinder;
	}

//	public MetadataSourceQueue getMetadataSourceQueue() {
//		return metadataSourceQueue;
//	}

	public Database getDatabase() {
		return database;
	}

	public NamingStrategy getNamingStrategy() {
		return namingStrategy;
	}

	public EntityBinding getEntityBinding(String entityName) {
		return entityBindingMap.get( entityName );
	}

	public Iterable<EntityBinding> getEntityBindings() {
		return entityBindingMap.values();
	}

	public void addEntity(EntityBinding entityBinding) {
		final String entityName = entityBinding.getEntity().getName();
		if ( entityBindingMap.containsKey( entityName ) ) {
			throw new DuplicateMappingException( DuplicateMappingException.Type.ENTITY, entityName );
		}
		entityBindingMap.put( entityName, entityBinding );
	}

	public PluralAttributeBinding getCollection(String collectionRole) {
		return collectionBindingMap.get( collectionRole );
	}

	public Iterable<PluralAttributeBinding> getCollections() {
		return collectionBindingMap.values();
	}

	public void addCollection(PluralAttributeBinding pluralAttributeBinding) {
		final String owningEntityName = pluralAttributeBinding.getEntityBinding().getEntity().getName();
		final String attributeName = pluralAttributeBinding.getAttribute().getName();
		final String collectionRole = owningEntityName + '.' + attributeName;
		if ( collectionBindingMap.containsKey( collectionRole ) ) {
			throw new DuplicateMappingException( DuplicateMappingException.Type.ENTITY, collectionRole );
		}
		collectionBindingMap.put( collectionRole, pluralAttributeBinding );
	}

	public void addImport(String importName, String entityName) {
		if ( imports == null ) {
			imports = new HashMap<String, String>();
		}
		LOG.trace( "Import: " + importName + " -> " + entityName );
		String old = imports.put( importName, entityName );
		if ( old != null ) {
			LOG.debug( "import name [" + importName + "] overrode previous [{" + old + "}]" );
		}
	}

	public Iterable<FetchProfile> getFetchProfiles() {
		return fetchProfiles.values();
	}

	public FetchProfile findOrCreateFetchProfile(String profileName, MetadataSource source) {
		FetchProfile profile = fetchProfiles.get( profileName );
		if ( profile == null ) {
			profile = new FetchProfile( profileName, source );
			fetchProfiles.put( profileName, profile );
		}
		return profile;
	}

	// Metadata contract ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private List<JaxbRoot> jaxbRootList = new ArrayList<JaxbRoot>();

	/**
	 * TODO : STRICTLY FOR TESTING PURPOSES, REMOVE AFTER JAXB AND BINDING STUFF HAS BEEN VALIDATED!!!!!!!
	 */
	public List<JaxbRoot> getJaxbRootList() {
		return jaxbRootList;
	}

	@Override
	public Metadata addAnnotatedClass(Class annotatedClass) {
		return null; // todo : implement method body
	}

	@Override
	public Metadata addPackage(String packageName) {
		return null; // todo : implement method body
	}

	@Override
	public Metadata addResource(String name) {
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

	@Override
	public Metadata addClass(Class entityClass) {
		LOG.debugf( "adding resource mappings from class convention : %s", entityClass.getName() );
		final String mappingResourceName = entityClass.getName().replace( '.', '/' ) + ".hbm.xml";
		addResource( mappingResourceName );
		return this;
	}

	@Override
	public Metadata addFile(String path) {
		return addFile( new File( path ) );
	}

	@Override
	public Metadata addFile(File file) {
		final String name =  file.getAbsolutePath();
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

	@Override
	public Metadata addCacheableFile(String path) {
		return null; // todo : implement method body
	}

	@Override
	public Metadata addCacheableFile(File file) {
		return null; // todo : implement method body
	}

	@Override
	public Metadata addInputStream(InputStream xmlInputStream) {
		add( xmlInputStream, new Origin( SourceType.INPUT_STREAM, "<unknown>" ), false );
		return this;
	}

	@Override
	public Metadata addURL(URL url) {
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

	@Override
	public Metadata addDocument(Document document) {
		final Origin origin = new Origin( SourceType.DOM, "<unknown>" );
		JaxbRoot jaxbRoot = jaxbHelper.unmarshal( document, origin );
		jaxbRootList.add( jaxbRoot );
		return this;
	}

	@Override
	public Metadata addJar(File jar) {
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
						catch (Exception e) {
							throw new MappingException( "could not read mapping documents", e, origin );
						}
					}
				}
			}
			finally {
				try {
					jarFile.close();
				}
				catch (Exception ignore) {
				}
			}
		}
		catch (IOException e) {
			throw new MappingNotFoundException( e, origin );
		}
		return this;
	}

	@Override
	public Metadata addDirectory(File dir) {
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
