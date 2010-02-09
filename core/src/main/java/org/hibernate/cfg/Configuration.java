/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
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
 *
 */
package org.hibernate.cfg;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.dom4j.Attribute;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import org.hibernate.EmptyInterceptor;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.InvalidMappingException;
import org.hibernate.MappingException;
import org.hibernate.MappingNotFoundException;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.DuplicateMappingException;
import org.hibernate.id.IdentifierGeneratorAggregator;
import org.hibernate.tuple.entity.EntityTuplizerFactory;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.engine.FilterDefinition;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.NamedQueryDefinition;
import org.hibernate.engine.NamedSQLQueryDefinition;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.event.AutoFlushEventListener;
import org.hibernate.event.DeleteEventListener;
import org.hibernate.event.DirtyCheckEventListener;
import org.hibernate.event.EventListeners;
import org.hibernate.event.EvictEventListener;
import org.hibernate.event.FlushEntityEventListener;
import org.hibernate.event.FlushEventListener;
import org.hibernate.event.InitializeCollectionEventListener;
import org.hibernate.event.LoadEventListener;
import org.hibernate.event.LockEventListener;
import org.hibernate.event.MergeEventListener;
import org.hibernate.event.PersistEventListener;
import org.hibernate.event.PostCollectionRecreateEventListener;
import org.hibernate.event.PostCollectionRemoveEventListener;
import org.hibernate.event.PostCollectionUpdateEventListener;
import org.hibernate.event.PostDeleteEventListener;
import org.hibernate.event.PostInsertEventListener;
import org.hibernate.event.PostLoadEventListener;
import org.hibernate.event.PostUpdateEventListener;
import org.hibernate.event.PreCollectionRecreateEventListener;
import org.hibernate.event.PreCollectionRemoveEventListener;
import org.hibernate.event.PreCollectionUpdateEventListener;
import org.hibernate.event.PreDeleteEventListener;
import org.hibernate.event.PreInsertEventListener;
import org.hibernate.event.PreLoadEventListener;
import org.hibernate.event.PreUpdateEventListener;
import org.hibernate.event.RefreshEventListener;
import org.hibernate.event.ReplicateEventListener;
import org.hibernate.event.SaveOrUpdateEventListener;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.id.factory.DefaultIdentifierGeneratorFactory;
import org.hibernate.impl.SessionFactoryImpl;
import org.hibernate.mapping.AuxiliaryDatabaseObject;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.IdentifierCollection;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.mapping.FetchProfile;
import org.hibernate.mapping.DenormalizedTable;
import org.hibernate.mapping.TypeDef;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.secure.JACCConfiguration;
import org.hibernate.tool.hbm2ddl.DatabaseMetadata;
import org.hibernate.tool.hbm2ddl.TableMetadata;
import org.hibernate.tool.hbm2ddl.IndexMetadata;
import org.hibernate.type.SerializationException;
import org.hibernate.type.Type;
import org.hibernate.util.ArrayHelper;
import org.hibernate.util.CollectionHelper;
import org.hibernate.util.ConfigHelper;
import org.hibernate.util.PropertiesHelper;
import org.hibernate.util.ReflectHelper;
import org.hibernate.util.SerializationHelper;
import org.hibernate.util.StringHelper;
import org.hibernate.util.XMLHelper;

/**
 * An instance of <tt>Configuration</tt> allows the application
 * to specify properties and mapping documents to be used when
 * creating a <tt>SessionFactory</tt>. Usually an application will create
 * a single <tt>Configuration</tt>, build a single instance of
 * <tt>SessionFactory</tt> and then instantiate <tt>Session</tt>s in
 * threads servicing client requests. The <tt>Configuration</tt> is meant
 * only as an initialization-time object. <tt>SessionFactory</tt>s are
 * immutable and do not retain any association back to the
 * <tt>Configuration</tt>.<br>
 * <br>
 * A new <tt>Configuration</tt> will use the properties specified in
 * <tt>hibernate.properties</tt> by default.
 *
 * @author Gavin King
 * @see org.hibernate.SessionFactory
 */
public class Configuration implements Serializable {

	private static Logger log = LoggerFactory.getLogger( Configuration.class );

	protected Map classes;
	protected Map imports;
	protected Map collections;
	protected Map tables;
	protected List auxiliaryDatabaseObjects;

	protected Map namedQueries;
	protected Map namedSqlQueries;
	protected Map/*<String, SqlResultSetMapping>*/ sqlResultSetMappings;

	protected Map typeDefs;
	protected Map filterDefinitions;
	protected Map fetchProfiles;

	protected Map tableNameBinding;
	protected Map columnNameBindingPerTable;

	protected List secondPasses;
	protected List propertyReferences;
//	protected List extendsQueue;
	protected Map extendsQueue;

	protected Map sqlFunctions;

	private EntityTuplizerFactory entityTuplizerFactory;
//	private ComponentTuplizerFactory componentTuplizerFactory; todo : HHH-3517 and HHH-1907

	private Interceptor interceptor;
	private Properties properties;
	private EntityResolver entityResolver;
	private EntityNotFoundDelegate entityNotFoundDelegate;

	protected transient XMLHelper xmlHelper;
	protected NamingStrategy namingStrategy;
	private SessionFactoryObserver sessionFactoryObserver;

	private EventListeners eventListeners;

	protected final SettingsFactory settingsFactory;

	private transient Mapping mapping = buildMapping();

	private DefaultIdentifierGeneratorFactory identifierGeneratorFactory;

	//Map<Class<?>, org.hibernate.mapping.MappedSuperclass>
	private Map mappedSuperclasses;

	protected Configuration(SettingsFactory settingsFactory) {
		this.settingsFactory = settingsFactory;
		reset();
	}

	public Configuration() {
		this( new SettingsFactory() );
	}

	protected void reset() {
		classes = new HashMap();
		imports = new HashMap();
		collections = new HashMap();
		tables = new TreeMap();

		namedQueries = new HashMap();
		namedSqlQueries = new HashMap();
		sqlResultSetMappings = new HashMap();

		typeDefs = new HashMap();
		filterDefinitions = new HashMap();
		fetchProfiles = new HashMap();
		auxiliaryDatabaseObjects = new ArrayList();

		tableNameBinding = new HashMap();
		columnNameBindingPerTable = new HashMap();

		propertyReferences = new ArrayList();
		secondPasses = new ArrayList();
//		extendsQueue = new ArrayList();
		extendsQueue = new HashMap();

		namingStrategy = DefaultNamingStrategy.INSTANCE;
		xmlHelper = new XMLHelper();
		interceptor = EmptyInterceptor.INSTANCE;
		properties = Environment.getProperties();
		entityResolver = XMLHelper.DEFAULT_DTD_RESOLVER;
		eventListeners = new EventListeners();

		sqlFunctions = new HashMap();

		entityTuplizerFactory = new EntityTuplizerFactory();
//		componentTuplizerFactory = new ComponentTuplizerFactory();

		identifierGeneratorFactory = new DefaultIdentifierGeneratorFactory();

		mappedSuperclasses = new HashMap();
	}

	public EntityTuplizerFactory getEntityTuplizerFactory() {
		return entityTuplizerFactory;
	}

//	public ComponentTuplizerFactory getComponentTuplizerFactory() {
//		return componentTuplizerFactory;
//	}

	/**
	 * Iterate the entity mappings
	 *
	 * @return Iterator of the entity mappings currently contained in the configuration.
	 */
	public Iterator getClassMappings() {
		return classes.values().iterator();
	}

	/**
	 * Iterate the collection mappings
	 *
	 * @return Iterator of the collection mappings currently contained in the configuration.
	 */
	public Iterator getCollectionMappings() {
		return collections.values().iterator();
	}

	/**
	 * Iterate the table mappings
	 *
	 * @return Iterator of the table mappings currently contained in the configuration.
	 */
	public Iterator getTableMappings() {
		return tables.values().iterator();
	}

	/**
	 * Iterate the mapped superclasses mappings
	 * EXPERIMENTAL Consider this API as PRIVATE
	 *
	 * @return Iterator<MappedSuperclass> over the MappedSuperclass mapping currently contained in the configuration.
	 */
	public Iterator getMappedSuperclassMappings() {
		return mappedSuperclasses.values().iterator();
	}

	/**
	 * Get the mapping for a particular entity
	 *
	 * @param entityName An entity name.
	 * @return the entity mapping information
	 */
	public PersistentClass getClassMapping(String entityName) {
		return (PersistentClass) classes.get( entityName );
	}

	/**
	 * Get the mapping for a particular collection role
	 *
	 * @param role a collection role
	 * @return The collection mapping information
	 */
	public Collection getCollectionMapping(String role) {
		return (Collection) collections.get( role );
	}

	/**
	 * Set a custom entity resolver. This entity resolver must be
	 * set before addXXX(misc) call.
	 * Default value is {@link org.hibernate.util.DTDEntityResolver}
	 *
	 * @param entityResolver entity resolver to use
	 */
	public void setEntityResolver(EntityResolver entityResolver) {
		this.entityResolver = entityResolver;
	}

	public EntityResolver getEntityResolver() {
		return entityResolver;
	}

	/**
	 * Retrieve the user-supplied delegate to handle non-existent entity
	 * scenarios.  May be null.
	 *
	 * @return The user-supplied delegate
	 */
	public EntityNotFoundDelegate getEntityNotFoundDelegate() {
		return entityNotFoundDelegate;
	}

	/**
	 * Specify a user-supplied delegate to be used to handle scenarios where an entity could not be
	 * located by specified id.  This is mainly intended for EJB3 implementations to be able to
	 * control how proxy initialization errors should be handled...
	 *
	 * @param entityNotFoundDelegate The delegate to use
	 */
	public void setEntityNotFoundDelegate(EntityNotFoundDelegate entityNotFoundDelegate) {
		this.entityNotFoundDelegate = entityNotFoundDelegate;
	}

	/**
	 * Read mappings from a particular XML file
	 *
	 * @param xmlFile a path to a file
	 * @return this (for method chaining purposes)
	 * @throws org.hibernate.MappingException Indicates inability to locate or parse
	 * the specified mapping file.
	 * @see #addFile(java.io.File)
	 */
	public Configuration addFile(String xmlFile) throws MappingException {
		return addFile( new File( xmlFile ) );
	}

	/**
	 * Read mappings from a particular XML file
	 *
	 * @param xmlFile a path to a file
	 * @return this (for method chaining purposes)
	 * @throws org.hibernate.MappingException Indicates inability to locate or parse
	 * the specified mapping file.
	 */
	public Configuration addFile(File xmlFile) throws MappingException {
		log.info( "Reading mappings from file: " + xmlFile.getPath() );
		if ( !xmlFile.exists() ) {
			throw new MappingNotFoundException( "file", xmlFile.toString() );
		}
		try {
			List errors = new ArrayList();
			org.dom4j.Document doc = xmlHelper.createSAXReader( xmlFile.toString(), errors, entityResolver ).read( xmlFile );
			if ( errors.size() != 0 ) {
				throw new InvalidMappingException( "file", xmlFile.toString(), ( Throwable ) errors.get( 0 ) );
			}
			add( doc );
			return this;
		}
		catch ( InvalidMappingException e ) {
			throw e;
		}
		catch  ( MappingNotFoundException e ) {
			throw e;
		}
		catch ( Exception e ) {
			throw new InvalidMappingException( "file", xmlFile.toString(), e );
		}
	}

	/**
	 * Add a cached mapping file.  A cached file is a serialized representation
	 * of the DOM structure of a particular mapping.  It is saved from a previous
	 * call as a file with the name <tt>xmlFile + ".bin"</tt> where xmlFile is
	 * the name of the original mapping file.
	 * </p>
	 * If a cached <tt>xmlFile + ".bin"</tt> exists and is newer than
	 * <tt>xmlFile</tt> the <tt>".bin"</tt> file will be read directly. Otherwise
	 * xmlFile is read and then serialized to <tt>xmlFile + ".bin"</tt> for use
	 * the next time.
	 *
	 * @param xmlFile The cacheable mapping file to be added.
	 * @return this (for method chaining purposes)
	 * @throws MappingException Indicates problems reading the cached file or processing
	 * the non-cached file.
	 */
	public Configuration addCacheableFile(File xmlFile) throws MappingException {
		File cachedFile = determineCachedDomFile( xmlFile );

		try {
			return addCacheableFileStrictly( xmlFile );
		}
		catch ( SerializationException e ) {
			log.warn( "Could not deserialize cache file: " + cachedFile.getPath() + " : " + e );
		}
		catch ( FileNotFoundException e ) {
			log.warn( "I/O reported cached file could not be found : " + cachedFile.getPath() + " : " + e );
		}

		if ( !xmlFile.exists() ) {
			throw new MappingNotFoundException( "file", xmlFile.toString() );
		}

		log.info( "Reading mappings from file: " + xmlFile );
		List errors = new ArrayList();
		try {
			org.dom4j.Document doc = xmlHelper.createSAXReader( xmlFile.getAbsolutePath(), errors, entityResolver ).read( xmlFile );
			if ( errors.size() != 0 ) {
				throw new InvalidMappingException( "file", xmlFile.toString(), (Throwable) errors.get(0) );
			}

			try {
				log.debug( "Writing cache file for: " + xmlFile + " to: " + cachedFile );
				SerializationHelper.serialize( ( Serializable ) doc, new FileOutputStream( cachedFile ) );
			}
			catch ( SerializationException e ) {
				log.warn( "Could not write cached file: " + cachedFile, e );
			}
			catch ( FileNotFoundException e ) {
				log.warn( "I/O reported error writing cached file : " + cachedFile.getPath(), e );
			}

			add( doc );
		}
		catch (DocumentException e) {
			throw new InvalidMappingException( "file", xmlFile.toString(), e );
		}

		return this;
	}

	private File determineCachedDomFile(File xmlFile) {
		return new File( xmlFile.getAbsolutePath() + ".bin" );
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
	 * @throws MappingException Indicates a problem in the underlyiong call to {@link #add(org.dom4j.Document)}
	 * @throws SerializationException Indicates a problem deserializing the cached dom tree
	 * @throws FileNotFoundException Indicates that the cached file was not found or was not usable.
	 */
	public Configuration addCacheableFileStrictly(File xmlFile)
			throws MappingException, SerializationException, FileNotFoundException {
		final File cachedFile = determineCachedDomFile( xmlFile );

		final boolean useCachedFile = xmlFile.exists()
				&& cachedFile.exists()
				&& xmlFile.lastModified() < cachedFile.lastModified();

		if ( ! useCachedFile ) {
			throw new FileNotFoundException( "Cached file could not be found or could not be used" );
		}

		log.info( "Reading mappings from cache file: " + cachedFile );
		org.dom4j.Document document =
				( org.dom4j.Document ) SerializationHelper.deserialize( new FileInputStream( cachedFile ) );
		add( document );
		return this;
	}

	/**
	 * Add a cacheable mapping file.
	 *
	 * @param xmlFile The name of the file to be added.  This must be in a form
	 * useable to simply construct a {@link java.io.File} instance.
	 * @return this (for method chaining purposes)
	 * @throws MappingException Indicates problems reading the cached file or processing
	 * the non-cached file.
	 * @see #addCacheableFile(java.io.File)
	 */
	public Configuration addCacheableFile(String xmlFile) throws MappingException {
		return addCacheableFile( new File( xmlFile ) );
	}


	/**
	 * Read mappings from a <tt>String</tt>
	 *
	 * @param xml an XML string
	 * @return this (for method chaining purposes)
	 * @throws org.hibernate.MappingException Indicates problems parsing the
	 * given XML string
	 */
	public Configuration addXML(String xml) throws MappingException {
		if ( log.isDebugEnabled() ) {
			log.debug( "Mapping XML:\n" + xml );
		}
		try {
			List errors = new ArrayList();
			org.dom4j.Document doc = xmlHelper.createSAXReader( "XML String", errors, entityResolver )
					.read( new StringReader( xml ) );
			if ( errors.size() != 0 ) {
				throw new MappingException( "invalid mapping", (Throwable) errors.get( 0 ) );
			}
			add( doc );
		}
		catch (DocumentException e) {
			throw new MappingException( "Could not parse mapping document in XML string", e );
		}
		return this;
	}

	/**
	 * Read mappings from a <tt>URL</tt>
	 *
	 * @param url The url for the mapping document to be read.
	 * @return this (for method chaining purposes)
	 * @throws MappingException Indicates problems reading the URL or processing
	 * the mapping document.
	 */
	public Configuration addURL(URL url) throws MappingException {
		if ( log.isDebugEnabled() ) {
			log.debug( "Reading mapping document from URL:" + url.toExternalForm() );
		}
		try {
			addInputStream( url.openStream() );
		}
		catch ( InvalidMappingException e ) {
			throw new InvalidMappingException( "URL", url.toExternalForm(), e.getCause() );
		}
		catch (Exception e) {
			throw new InvalidMappingException( "URL", url.toExternalForm(), e );
		}
		return this;
	}

	/**
	 * Read mappings from a DOM <tt>Document</tt>
	 *
	 * @param doc The DOM document
	 * @return this (for method chaining purposes)
	 * @throws MappingException Indicates problems reading the DOM or processing
	 * the mapping document.
	 */
	public Configuration addDocument(Document doc) throws MappingException {
		if ( log.isDebugEnabled() ) {
			log.debug( "Mapping document:\n" + doc );
		}
		add( xmlHelper.createDOMReader().read( doc ) );
		return this;
	}

	/**
	 * Read mappings from an {@link java.io.InputStream}.
	 *
	 * @param xmlInputStream The input stream containing a DOM.
	 * @return this (for method chaining purposes)
	 * @throws MappingException Indicates problems reading the stream, or
	 * processing the contained mapping document.
	 */
	public Configuration addInputStream(InputStream xmlInputStream) throws MappingException {
		try {
			List errors = new ArrayList();
			org.dom4j.Document doc = xmlHelper.createSAXReader( "XML InputStream", errors, entityResolver )
					.read( new InputSource( xmlInputStream ) );
			if ( errors.size() != 0 ) {
				throw new InvalidMappingException( "invalid mapping", null, (Throwable) errors.get( 0 ) );
			}
			add( doc );
			return this;
		}
		catch (DocumentException e) {
			throw new InvalidMappingException( "input stream", null, e );
		}
		finally {
			try {
				xmlInputStream.close();
			}
			catch (IOException ioe) {
				log.warn( "Could not close input stream", ioe );
			}
		}
	}

	/**
	 * Read mappings as a application resource (i.e. classpath lookup).
	 *
	 * @param resourceName The resource name
	 * @param classLoader The class loader to use.
	 * @return this (for method chaining purposes)
	 * @throws MappingException Indicates problems locating the resource or
	 * processing the contained mapping document.
	 */
	public Configuration addResource(String resourceName, ClassLoader classLoader) throws MappingException {
		log.info( "Reading mappings from resource: " + resourceName );
		InputStream rsrc = classLoader.getResourceAsStream( resourceName );
		if ( rsrc == null ) {
			throw new MappingNotFoundException( "resource", resourceName );
		}
		try {
			return addInputStream( rsrc );
		}
		catch (MappingException me) {
			throw new InvalidMappingException( "resource", resourceName, me );
		}
	}

	/**
	 * Read mappings as a application resourceName (i.e. classpath lookup)
	 * trying different classloaders.
	 *
	 * @param resourceName The resource name
	 * @return this (for method chaining purposes)
	 * @throws MappingException Indicates problems locating the resource or
	 * processing the contained mapping document.
	 */
	public Configuration addResource(String resourceName) throws MappingException {
		log.info( "Reading mappings from resource : " + resourceName );
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		InputStream rsrc = null;
		if (contextClassLoader!=null) {
			rsrc = contextClassLoader.getResourceAsStream( resourceName );
		}
		if ( rsrc == null ) {
			rsrc = Environment.class.getClassLoader().getResourceAsStream( resourceName );
		}
		if ( rsrc == null ) {
			throw new MappingNotFoundException( "resource", resourceName );
		}
		try {
			return addInputStream( rsrc );
		}
		catch (MappingException me) {
			throw new InvalidMappingException( "resource", resourceName, me );
		}
	}

	/**
	 * Read a mapping as an application resouurce using the convention that a class
	 * named <tt>foo.bar.Foo</tt> is mapped by a file <tt>foo/bar/Foo.hbm.xml</tt>
	 * which can be resolved as a classpath resource.
	 *
	 * @param persistentClass The mapped class
	 * @return this (for method chaining purposes)
	 * @throws MappingException Indicates problems locating the resource or
	 * processing the contained mapping document.
	 */
	public Configuration addClass(Class persistentClass) throws MappingException {
		String mappingResourceName = persistentClass.getName().replace( '.', '/' ) + ".hbm.xml";
		log.info( "Reading mappings from resource: " + mappingResourceName );
		return addResource( mappingResourceName, persistentClass.getClassLoader() );
	}

	/**
	 * Read all mappings from a jar file
	 * <p/>
	 * Assumes that any file named <tt>*.hbm.xml</tt> is a mapping document.
	 *
	 * @param jar a jar file
	 * @return this (for method chaining purposes)
	 * @throws MappingException Indicates problems reading the jar file or
	 * processing the contained mapping documents.
	 */
	public Configuration addJar(File jar) throws MappingException {
		log.info( "Searching for mapping documents in jar: " + jar.getName() );
		JarFile jarFile = null;
		try {
			try {
				jarFile = new JarFile( jar );
			}
			catch (IOException ioe) {
				throw new InvalidMappingException(
						"Could not read mapping documents from jar: " + jar.getName(), "jar", jar.getName(),
						ioe
				);
			}
			Enumeration jarEntries = jarFile.entries();
			while ( jarEntries.hasMoreElements() ) {
				ZipEntry ze = (ZipEntry) jarEntries.nextElement();
				if ( ze.getName().endsWith( ".hbm.xml" ) ) {
					log.info( "Found mapping document in jar: " + ze.getName() );
					try {
						addInputStream( jarFile.getInputStream( ze ) );
					}
					catch (Exception e) {
						throw new InvalidMappingException(
								"Could not read mapping documents from jar: " + jar.getName(),
								"jar",
								jar.getName(),
								e
						);
					}
				}
			}
		}
		finally {
			try {
				if ( jarFile != null ) {
					jarFile.close();
				}
			}
			catch (IOException ioe) {
				log.error("could not close jar", ioe);
			}
		}

		return this;
	}

	/**
	 * Read all mapping documents from a directory tree.
	 * <p/>
	 * Assumes that any file named <tt>*.hbm.xml</tt> is a mapping document.
	 *
	 * @param dir The directory
	 * @return this (for method chaining purposes)
	 * @throws MappingException Indicates problems reading the jar file or
	 * processing the contained mapping documents.
	 */
	public Configuration addDirectory(File dir) throws MappingException {
		File[] files = dir.listFiles();
		for ( int i = 0; i < files.length ; i++ ) {
			if ( files[i].isDirectory() ) {
				addDirectory( files[i] );
			}
			else if ( files[i].getName().endsWith( ".hbm.xml" ) ) {
				addFile( files[i] );
			}
		}
		return this;
	}

	protected void add(org.dom4j.Document doc) throws MappingException {
		HbmBinder.bindRoot( doc, createMappings(), CollectionHelper.EMPTY_MAP );
	}

	/**
	 * Create a new <tt>Mappings</tt> to add class and collection
	 * mappings to.
	 */
	public Mappings createMappings() {
		return new MappingsImpl();
	}


	private Iterator iterateGenerators(Dialect dialect) throws MappingException {

		TreeMap generators = new TreeMap();
		String defaultCatalog = properties.getProperty( Environment.DEFAULT_CATALOG );
		String defaultSchema = properties.getProperty( Environment.DEFAULT_SCHEMA );

		Iterator iter = classes.values().iterator();
		while ( iter.hasNext() ) {
			PersistentClass pc = ( PersistentClass ) iter.next();
			if ( !pc.isInherited() ) {
				IdentifierGenerator ig = pc.getIdentifier().createIdentifierGenerator(
						getIdentifierGeneratorFactory(),
						dialect,
						defaultCatalog,
						defaultSchema,
						(RootClass) pc
				);

				if ( ig instanceof PersistentIdentifierGenerator ) {
					generators.put( ( (PersistentIdentifierGenerator) ig ).generatorKey(), ig );
				}
				else if ( ig instanceof IdentifierGeneratorAggregator ) {
					( (IdentifierGeneratorAggregator) ig ).registerPersistentGenerators( generators );
				}
			}
		}

		iter = collections.values().iterator();
		while ( iter.hasNext() ) {
			Collection collection = ( Collection ) iter.next();
			if ( collection.isIdentified() ) {
				IdentifierGenerator ig = ( ( IdentifierCollection ) collection ).getIdentifier().createIdentifierGenerator(
						getIdentifierGeneratorFactory(),
						dialect,
						defaultCatalog,
						defaultSchema,
						null
				);

				if ( ig instanceof PersistentIdentifierGenerator ) {
					generators.put( ( (PersistentIdentifierGenerator) ig ).generatorKey(), ig );
				}
			}
		}

		return generators.values().iterator();
	}

	/**
	 * Generate DDL for dropping tables
	 *
	 * @see org.hibernate.tool.hbm2ddl.SchemaExport
	 */
	public String[] generateDropSchemaScript(Dialect dialect) throws HibernateException {

		secondPassCompile();

		String defaultCatalog = properties.getProperty( Environment.DEFAULT_CATALOG );
		String defaultSchema = properties.getProperty( Environment.DEFAULT_SCHEMA );

		ArrayList script = new ArrayList( 50 );

		// drop them in reverse order in case db needs it done that way...
		ListIterator itr = auxiliaryDatabaseObjects.listIterator( auxiliaryDatabaseObjects.size() );
		while ( itr.hasPrevious() ) {
			AuxiliaryDatabaseObject object = (AuxiliaryDatabaseObject) itr.previous();
			if ( object.appliesToDialect( dialect ) ) {
				script.add( object.sqlDropString( dialect, defaultCatalog, defaultSchema ) );
			}
		}

		if ( dialect.dropConstraints() ) {
			Iterator iter = getTableMappings();
			while ( iter.hasNext() ) {
				Table table = (Table) iter.next();
				if ( table.isPhysicalTable() ) {
					Iterator subIter = table.getForeignKeyIterator();
					while ( subIter.hasNext() ) {
						ForeignKey fk = (ForeignKey) subIter.next();
						if ( fk.isPhysicalConstraint() ) {
							script.add(
									fk.sqlDropString(
											dialect,
											defaultCatalog,
											defaultSchema
										)
								);
						}
					}
				}
			}
		}


		Iterator iter = getTableMappings();
		while ( iter.hasNext() ) {

			Table table = (Table) iter.next();
			if ( table.isPhysicalTable() ) {

				/*Iterator subIter = table.getIndexIterator();
				while ( subIter.hasNext() ) {
					Index index = (Index) subIter.next();
					if ( !index.isForeignKey() || !dialect.hasImplicitIndexForForeignKey() ) {
						script.add( index.sqlDropString(dialect) );
					}
				}*/

				script.add(
						table.sqlDropString(
								dialect,
								defaultCatalog,
								defaultSchema
							)
					);

			}

		}

		iter = iterateGenerators( dialect );
		while ( iter.hasNext() ) {
			String[] lines = ( (PersistentIdentifierGenerator) iter.next() ).sqlDropStrings( dialect );
			for ( int i = 0; i < lines.length ; i++ ) {
				script.add( lines[i] );
			}
		}

		return ArrayHelper.toStringArray( script );
	}

	/**
	 * Generate DDL for creating tables
	 *
	 * @see org.hibernate.tool.hbm2ddl.SchemaExport
	 */
	public String[] generateSchemaCreationScript(Dialect dialect) throws HibernateException {
		secondPassCompile();

		ArrayList script = new ArrayList( 50 );
		String defaultCatalog = properties.getProperty( Environment.DEFAULT_CATALOG );
		String defaultSchema = properties.getProperty( Environment.DEFAULT_SCHEMA );

		Iterator iter = getTableMappings();
		while ( iter.hasNext() ) {
			Table table = (Table) iter.next();
			if ( table.isPhysicalTable() ) {
				script.add(
						table.sqlCreateString(
								dialect,
								mapping,
								defaultCatalog,
								defaultSchema
							)
					);
				Iterator comments = table.sqlCommentStrings( dialect, defaultCatalog, defaultSchema );
				while ( comments.hasNext() ) {
					script.add( comments.next() );
				}
			}
		}

		iter = getTableMappings();
		while ( iter.hasNext() ) {
			Table table = (Table) iter.next();
			if ( table.isPhysicalTable() ) {

				if ( !dialect.supportsUniqueConstraintInCreateAlterTable() ) {
					Iterator subIter = table.getUniqueKeyIterator();
					while ( subIter.hasNext() ) {
						UniqueKey uk = (UniqueKey) subIter.next();
						String constraintString = uk.sqlCreateString( dialect, mapping, defaultCatalog, defaultSchema );
						if (constraintString != null) script.add( constraintString );
					}
				}


				Iterator subIter = table.getIndexIterator();
				while ( subIter.hasNext() ) {
					Index index = (Index) subIter.next();
					script.add(
							index.sqlCreateString(
									dialect,
									mapping,
									defaultCatalog,
									defaultSchema
								)
						);
				}

				if ( dialect.hasAlterTable() ) {
					subIter = table.getForeignKeyIterator();
					while ( subIter.hasNext() ) {
						ForeignKey fk = (ForeignKey) subIter.next();
						if ( fk.isPhysicalConstraint() ) {
							script.add(
									fk.sqlCreateString(
											dialect, mapping,
											defaultCatalog,
											defaultSchema
										)
								);
						}
					}
				}

			}
		}

		iter = iterateGenerators( dialect );
		while ( iter.hasNext() ) {
			String[] lines = ( (PersistentIdentifierGenerator) iter.next() ).sqlCreateStrings( dialect );
			for ( int i = 0; i < lines.length ; i++ ) {
				script.add( lines[i] );
			}
		}

		Iterator itr = auxiliaryDatabaseObjects.iterator();
		while ( itr.hasNext() ) {
			AuxiliaryDatabaseObject object = (AuxiliaryDatabaseObject) itr.next();
			if ( object.appliesToDialect( dialect ) ) {
				script.add( object.sqlCreateString( dialect, mapping, defaultCatalog, defaultSchema ) );
			}
		}

		return ArrayHelper.toStringArray( script );
	}

	/**
	 * Generate DDL for altering tables
	 *
	 * @see org.hibernate.tool.hbm2ddl.SchemaUpdate
	 */
	public String[] generateSchemaUpdateScript(Dialect dialect, DatabaseMetadata databaseMetadata)
			throws HibernateException {
		secondPassCompile();

		String defaultCatalog = properties.getProperty( Environment.DEFAULT_CATALOG );
		String defaultSchema = properties.getProperty( Environment.DEFAULT_SCHEMA );

		ArrayList script = new ArrayList( 50 );

		Iterator iter = getTableMappings();
		while ( iter.hasNext() ) {
			Table table = (Table) iter.next();
			if ( table.isPhysicalTable() ) {
				
				TableMetadata tableInfo = databaseMetadata.getTableMetadata(
						table.getName(),
						( table.getSchema() == null ) ? defaultSchema : table.getSchema(),
						( table.getCatalog() == null ) ? defaultCatalog : table.getCatalog(),
								table.isQuoted()

					);
				if ( tableInfo == null ) {
					script.add(
							table.sqlCreateString(
									dialect,
									mapping,
									defaultCatalog,
									defaultSchema
								)
						);
				}
				else {
					Iterator subiter = table.sqlAlterStrings(
							dialect,
							mapping,
							tableInfo,
							defaultCatalog,
							defaultSchema
						);
					while ( subiter.hasNext() ) {
						script.add( subiter.next() );
					}
				}

				Iterator comments = table.sqlCommentStrings( dialect, defaultCatalog, defaultSchema );
				while ( comments.hasNext() ) {
					script.add( comments.next() );
				}

			}
		}

		iter = getTableMappings();
		while ( iter.hasNext() ) {
			Table table = (Table) iter.next();
			if ( table.isPhysicalTable() ) {

				TableMetadata tableInfo = databaseMetadata.getTableMetadata(
						table.getName(),
						table.getSchema(),
						table.getCatalog(),
						table.isQuoted()
					);

				if ( dialect.hasAlterTable() ) {
					Iterator subIter = table.getForeignKeyIterator();
					while ( subIter.hasNext() ) {
						ForeignKey fk = (ForeignKey) subIter.next();
						if ( fk.isPhysicalConstraint() ) {
							boolean create = tableInfo == null || (
									tableInfo.getForeignKeyMetadata( fk ) == null && (
											//Icky workaround for MySQL bug:
											!( dialect instanceof MySQLDialect ) ||
													tableInfo.getIndexMetadata( fk.getName() ) == null
										)
								);
							if ( create ) {
								script.add(
										fk.sqlCreateString(
												dialect,
												mapping,
												defaultCatalog,
												defaultSchema
											)
									);
							}
						}
					}
				}

				Iterator subIter = table.getIndexIterator();
				while ( subIter.hasNext() ) {
					final Index index = (Index) subIter.next();
					// Skip if index already exists
					if ( tableInfo != null && StringHelper.isNotEmpty( index.getName() ) ) {
						final IndexMetadata meta = tableInfo.getIndexMetadata( index.getName() );
						if ( meta != null ) {
							continue;
						}
					}
					script.add(
							index.sqlCreateString(
									dialect,
									mapping,
									defaultCatalog,
									defaultSchema
							)
					);
				}

//broken, 'cos we don't generate these with names in SchemaExport
//				subIter = table.getUniqueKeyIterator();
//				while ( subIter.hasNext() ) {
//					UniqueKey uk = (UniqueKey) subIter.next();
//					if ( tableInfo==null || tableInfo.getIndexMetadata( uk.getFilterName() ) == null ) {
//						script.add( uk.sqlCreateString(dialect, mapping) );
//					}
//				}
			}
		}

		iter = iterateGenerators( dialect );
		while ( iter.hasNext() ) {
			PersistentIdentifierGenerator generator = (PersistentIdentifierGenerator) iter.next();
			Object key = generator.generatorKey();
			if ( !databaseMetadata.isSequence( key ) && !databaseMetadata.isTable( key ) ) {
				String[] lines = generator.sqlCreateStrings( dialect );
				for ( int i = 0; i < lines.length ; i++ ) {
					script.add( lines[i] );
				}
			}
		}

		return ArrayHelper.toStringArray( script );
	}

	public void validateSchema(Dialect dialect, DatabaseMetadata databaseMetadata)
			throws HibernateException {
		secondPassCompile();

		String defaultCatalog = properties.getProperty( Environment.DEFAULT_CATALOG );
		String defaultSchema = properties.getProperty( Environment.DEFAULT_SCHEMA );
		
		Iterator iter = getTableMappings();
		while ( iter.hasNext() ) {
			Table table = (Table) iter.next();
			if ( table.isPhysicalTable() ) {
				

				TableMetadata tableInfo = databaseMetadata.getTableMetadata(
						table.getName(),
						( table.getSchema() == null ) ? defaultSchema : table.getSchema(),
						( table.getCatalog() == null ) ? defaultCatalog : table.getCatalog(),
								table.isQuoted());
				if ( tableInfo == null ) {
					throw new HibernateException( "Missing table: " + table.getName() );
				}
				else {
					table.validateColumns( dialect, mapping, tableInfo );
				}

			}
		}

		iter = iterateGenerators( dialect );
		while ( iter.hasNext() ) {
			PersistentIdentifierGenerator generator = (PersistentIdentifierGenerator) iter.next();
			Object key = generator.generatorKey();
			if ( !databaseMetadata.isSequence( key ) && !databaseMetadata.isTable( key ) ) {
				throw new HibernateException( "Missing sequence or table: " + key );
			}
		}
	}

	private void validate() throws MappingException {
		Iterator iter = classes.values().iterator();
		while ( iter.hasNext() ) {
			( (PersistentClass) iter.next() ).validate( mapping );
		}
		iter = collections.values().iterator();
		while ( iter.hasNext() ) {
			( (Collection) iter.next() ).validate( mapping );
		}
	}

	/**
	 * Call this to ensure the mappings are fully compiled/built. Usefull to ensure getting
	 * access to all information in the metamodel when calling e.g. getClassMappings().
	 */
	public void buildMappings() {
		secondPassCompile();
	}

	// This method may be called many times!!
	protected void secondPassCompile() throws MappingException {
		log.debug( "processing extends queue" );

		processExtendsQueue();

		log.debug( "processing collection mappings" );

		Iterator iter = secondPasses.iterator();
		while ( iter.hasNext() ) {
			SecondPass sp = (SecondPass) iter.next();
			if ( ! (sp instanceof QuerySecondPass) ) {
				sp.doSecondPass( classes );
				iter.remove();
			}
		}

		log.debug( "processing native query and ResultSetMapping mappings" );
		iter = secondPasses.iterator();
		while ( iter.hasNext() ) {
			SecondPass sp = (SecondPass) iter.next();
			sp.doSecondPass( classes );
			iter.remove();
		}

		log.debug( "processing association property references" );

		iter = propertyReferences.iterator();
		while ( iter.hasNext() ) {
			Mappings.PropertyReference upr = (Mappings.PropertyReference) iter.next();

			PersistentClass clazz = getClassMapping( upr.referencedClass );
			if ( clazz == null ) {
				throw new MappingException(
						"property-ref to unmapped class: " +
						upr.referencedClass
					);
			}

			Property prop = clazz.getReferencedProperty( upr.propertyName );
			if ( upr.unique ) {
				( (SimpleValue) prop.getValue() ).setAlternateUniqueKey( true );
			}
		}

		//TODO: Somehow add the newly created foreign keys to the internal collection

		log.debug( "processing foreign key constraints" );

		iter = getTableMappings();
		Set done = new HashSet();
		while ( iter.hasNext() ) {
			secondPassCompileForeignKeys( (Table) iter.next(), done );
		}

	}

	/**
	 * Try to empty the extends queue.
	 */
	private void processExtendsQueue() {
		// todo : would love to have this work on a notification basis
		//    where the successful binding of an entity/subclass would
		//    emit a notification which the extendsQueue entries could
		//    react to...
		org.dom4j.Document document = findPossibleExtends();
		while ( document != null ) {
			add( document );
			document = findPossibleExtends();
		}

		if ( extendsQueue.size() > 0 ) {
//			Iterator iterator = extendsQueue.iterator();
			Iterator iterator = extendsQueue.keySet().iterator();
			StringBuffer buf = new StringBuffer( "Following superclasses referenced in extends not found: " );
			while ( iterator.hasNext() ) {
				final ExtendsQueueEntry entry = ( ExtendsQueueEntry ) iterator.next();
				buf.append( entry.getExplicitName() );
				if ( entry.getMappingPackage() != null ) {
					buf.append( "[" ).append( entry.getMappingPackage() ).append( "]" );
				}
				if ( iterator.hasNext() ) {
					buf.append( "," );
				}
			}
			throw new MappingException( buf.toString() );
		}
	}

	/**
	 * Find the first possible element in the queue of extends.
	 */
	protected org.dom4j.Document findPossibleExtends() {
//		Iterator iter = extendsQueue.iterator();
		Iterator iter = extendsQueue.keySet().iterator();
		while ( iter.hasNext() ) {
			final ExtendsQueueEntry entry = ( ExtendsQueueEntry ) iter.next();
			if ( getClassMapping( entry.getExplicitName() ) != null ) {
				// found
				iter.remove();
				return entry.getDocument();
			}
			else if ( getClassMapping( HbmBinder.getClassName( entry.getExplicitName(), entry.getMappingPackage() ) ) != null ) {
				// found
				iter.remove();
				return entry.getDocument();
			}
		}
		return null;
	}

	protected void secondPassCompileForeignKeys(Table table, Set done) throws MappingException {

		table.createForeignKeys();

		Iterator iter = table.getForeignKeyIterator();
		while ( iter.hasNext() ) {

			ForeignKey fk = (ForeignKey) iter.next();
			if ( !done.contains( fk ) ) {
				done.add( fk );
				final String referencedEntityName = fk.getReferencedEntityName();
				if ( referencedEntityName == null ) {
					throw new MappingException(
							"An association from the table " +
							fk.getTable().getName() +
							" does not specify the referenced entity"
						);
				}
				if ( log.isDebugEnabled() ) {
					log.debug( "resolving reference to class: " + referencedEntityName );
				}
				PersistentClass referencedClass = (PersistentClass) classes.get( referencedEntityName );
				if ( referencedClass == null ) {
					throw new MappingException(
							"An association from the table " +
							fk.getTable().getName() +
							" refers to an unmapped class: " +
							referencedEntityName
						);
				}
				if ( referencedClass.isJoinedSubclass() ) {
					secondPassCompileForeignKeys( referencedClass.getSuperclass().getTable(), done );
				}
				fk.setReferencedTable( referencedClass.getTable() );
				fk.alignColumns();
			}
		}
	}

	/**
	 * Get the named queries
	 */
	public Map getNamedQueries() {
		return namedQueries;
	}

	/**
	 * Instantiate a new <tt>SessionFactory</tt>, using the properties and
	 * mappings in this configuration. The <tt>SessionFactory</tt> will be
	 * immutable, so changes made to the <tt>Configuration</tt> after
	 * building the <tt>SessionFactory</tt> will not affect it.
	 *
	 * @return a new factory for <tt>Session</tt>s
	 * @see org.hibernate.SessionFactory
	 */
	public SessionFactory buildSessionFactory() throws HibernateException {
		log.debug( "Preparing to build session factory with filters : " + filterDefinitions );
		secondPassCompile();
		validate();
		Environment.verifyProperties( properties );
		Properties copy = new Properties();
		copy.putAll( properties );
		PropertiesHelper.resolvePlaceHolders( copy );
		Settings settings = buildSettings( copy );

		return new SessionFactoryImpl(
				this,
				mapping,
				settings,
				getInitializedEventListeners(),
				sessionFactoryObserver
			);
	}

	private EventListeners getInitializedEventListeners() {
		EventListeners result = (EventListeners) eventListeners.shallowCopy();
		result.initializeListeners( this );
		return result;
	}

	/**
	 * Return the configured <tt>Interceptor</tt>
	 */
	public Interceptor getInterceptor() {
		return interceptor;
	}

	/**
	 * Get all properties
	 */
	public Properties getProperties() {
		return properties;
	}

	/**
	 * Configure an <tt>Interceptor</tt>
	 */
	public Configuration setInterceptor(Interceptor interceptor) {
		this.interceptor = interceptor;
		return this;
	}

	/**
	 * Specify a completely new set of properties
	 */
	public Configuration setProperties(Properties properties) {
		this.properties = properties;
		return this;
	}

	/**
	 * Set the given properties
	 */
	public Configuration addProperties(Properties extraProperties) {
		this.properties.putAll( extraProperties );
		return this;
	}

	/**
	 * Adds the incoming properties to the internap properties structure,
	 * as long as the internal structure does not already contain an
	 * entry for the given key.
	 *
	 * @param properties
	 * @return this
	 */
	public Configuration mergeProperties(Properties properties) {
		Iterator itr = properties.entrySet().iterator();
		while ( itr.hasNext() ) {
			final Map.Entry entry = ( Map.Entry ) itr.next();
			if ( this.properties.containsKey( entry.getKey() ) ) {
				continue;
			}
			this.properties.setProperty( ( String ) entry.getKey(), ( String ) entry.getValue() );
		}
		return this;
	}

	/**
	 * Set a property
	 */
	public Configuration setProperty(String propertyName, String value) {
		properties.setProperty( propertyName, value );
		return this;
	}

	/**
	 * Get a property
	 */
	public String getProperty(String propertyName) {
		return properties.getProperty( propertyName );
	}

	private void addProperties(Element parent) {
		Iterator iter = parent.elementIterator( "property" );
		while ( iter.hasNext() ) {
			Element node = (Element) iter.next();
			String name = node.attributeValue( "name" );
			String value = node.getText().trim();
			log.debug( name + "=" + value );
			properties.setProperty( name, value );
			if ( !name.startsWith( "hibernate" ) ) {
				properties.setProperty( "hibernate." + name, value );
			}
		}
		Environment.verifyProperties( properties );
	}

	/**
	 * Get the configuration file as an <tt>InputStream</tt>. Might be overridden
	 * by subclasses to allow the configuration to be located by some arbitrary
	 * mechanism.
	 */
	protected InputStream getConfigurationInputStream(String resource) throws HibernateException {

		log.info( "Configuration resource: " + resource );

		return ConfigHelper.getResourceAsStream( resource );

	}

	/**
	 * Use the mappings and properties specified in an application
	 * resource named <tt>hibernate.cfg.xml</tt>.
	 */
	public Configuration configure() throws HibernateException {
		configure( "/hibernate.cfg.xml" );
		return this;
	}

	/**
	 * Use the mappings and properties specified in the given application
	 * resource. The format of the resource is defined in
	 * <tt>hibernate-configuration-3.0.dtd</tt>.
	 * <p/>
	 * The resource is found via <tt>getConfigurationInputStream(resource)</tt>.
	 */
	public Configuration configure(String resource) throws HibernateException {
		log.info( "configuring from resource: " + resource );
		InputStream stream = getConfigurationInputStream( resource );
		return doConfigure( stream, resource );
	}

	/**
	 * Use the mappings and properties specified in the given document.
	 * The format of the document is defined in
	 * <tt>hibernate-configuration-3.0.dtd</tt>.
	 *
	 * @param url URL from which you wish to load the configuration
	 * @return A configuration configured via the file
	 * @throws HibernateException
	 */
	public Configuration configure(URL url) throws HibernateException {
		log.info( "configuring from url: " + url.toString() );
		try {
			return doConfigure( url.openStream(), url.toString() );
		}
		catch (IOException ioe) {
			throw new HibernateException( "could not configure from URL: " + url, ioe );
		}
	}

	/**
	 * Use the mappings and properties specified in the given application
	 * file. The format of the file is defined in
	 * <tt>hibernate-configuration-3.0.dtd</tt>.
	 *
	 * @param configFile <tt>File</tt> from which you wish to load the configuration
	 * @return A configuration configured via the file
	 * @throws HibernateException
	 */
	public Configuration configure(File configFile) throws HibernateException {
		log.info( "configuring from file: " + configFile.getName() );
		try {
			return doConfigure( new FileInputStream( configFile ), configFile.toString() );
		}
		catch (FileNotFoundException fnfe) {
			throw new HibernateException( "could not find file: " + configFile, fnfe );
		}
	}

	/**
	 * Use the mappings and properties specified in the given application
	 * resource. The format of the resource is defined in
	 * <tt>hibernate-configuration-3.0.dtd</tt>.
	 *
	 * @param stream	   Inputstream to be read from
	 * @param resourceName The name to use in warning/error messages
	 * @return A configuration configured via the stream
	 * @throws HibernateException
	 */
	protected Configuration doConfigure(InputStream stream, String resourceName) throws HibernateException {

		org.dom4j.Document doc;
		try {
			List errors = new ArrayList();
			doc = xmlHelper.createSAXReader( resourceName, errors, entityResolver )
					.read( new InputSource( stream ) );
			if ( errors.size() != 0 ) {
				throw new MappingException(
						"invalid configuration",
						(Throwable) errors.get( 0 )
					);
			}
		}
		catch (DocumentException e) {
			throw new HibernateException(
					"Could not parse configuration: " + resourceName,
					e
				);
		}
		finally {
			try {
				stream.close();
			}
			catch (IOException ioe) {
				log.warn( "could not close input stream for: " + resourceName, ioe );
			}
		}

		return doConfigure( doc );

	}

	/**
	 * Use the mappings and properties specified in the given XML document.
	 * The format of the file is defined in
	 * <tt>hibernate-configuration-3.0.dtd</tt>.
	 *
	 * @param document an XML document from which you wish to load the configuration
	 * @return A configuration configured via the <tt>Document</tt>
	 * @throws HibernateException if there is problem in accessing the file.
	 */
	public Configuration configure(Document document) throws HibernateException {
		log.info( "configuring from XML document" );
		return doConfigure( xmlHelper.createDOMReader().read( document ) );
	}

	protected Configuration doConfigure(org.dom4j.Document doc) throws HibernateException {

		Element sfNode = doc.getRootElement().element( "session-factory" );
		String name = sfNode.attributeValue( "name" );
		if ( name != null ) {
			properties.setProperty( Environment.SESSION_FACTORY_NAME, name );
		}
		addProperties( sfNode );
		parseSessionFactory( sfNode, name );

		Element secNode = doc.getRootElement().element( "security" );
		if ( secNode != null ) {
			parseSecurity( secNode );
		}

		log.info( "Configured SessionFactory: " + name );
		log.debug( "properties: " + properties );

		return this;

	}


	private void parseSessionFactory(Element sfNode, String name) {
		Iterator elements = sfNode.elementIterator();
		while ( elements.hasNext() ) {
			Element subelement = (Element) elements.next();
			String subelementName = subelement.getName();
			if ( "mapping".equals( subelementName ) ) {
				parseMappingElement( subelement, name );
			}
			else if ( "class-cache".equals( subelementName ) ) {
				String className = subelement.attributeValue( "class" );
				Attribute regionNode = subelement.attribute( "region" );
				final String region = ( regionNode == null ) ? className : regionNode.getValue();
				boolean includeLazy = !"non-lazy".equals( subelement.attributeValue( "include" ) );
				setCacheConcurrencyStrategy( className, subelement.attributeValue( "usage" ), region, includeLazy );
			}
			else if ( "collection-cache".equals( subelementName ) ) {
				String role = subelement.attributeValue( "collection" );
				Attribute regionNode = subelement.attribute( "region" );
				final String region = ( regionNode == null ) ? role : regionNode.getValue();
				setCollectionCacheConcurrencyStrategy( role, subelement.attributeValue( "usage" ), region );
			}
			else if ( "listener".equals( subelementName ) ) {
				parseListener( subelement );
			}
			else if ( "event".equals( subelementName ) ) {
				parseEvent( subelement );
			}
		}
	}

	protected void parseMappingElement(Element subelement, String name) {
		Attribute rsrc = subelement.attribute( "resource" );
		Attribute file = subelement.attribute( "file" );
		Attribute jar = subelement.attribute( "jar" );
		Attribute pkg = subelement.attribute( "package" );
		Attribute clazz = subelement.attribute( "class" );
		if ( rsrc != null ) {
			log.debug( name + "<-" + rsrc );
			addResource( rsrc.getValue() );
		}
		else if ( jar != null ) {
			log.debug( name + "<-" + jar );
			addJar( new File( jar.getValue() ) );
		}
		else if ( pkg != null ) {
			throw new MappingException(
					"An AnnotationConfiguration instance is required to use <mapping package=\"" +
					pkg.getValue() + "\"/>"
				);
		}
		else if ( clazz != null ) {
			throw new MappingException(
					"An AnnotationConfiguration instance is required to use <mapping class=\"" +
					clazz.getValue() + "\"/>"
				);
		}
		else {
			if ( file == null ) {
				throw new MappingException(
						"<mapping> element in configuration specifies no attributes"
					);
			}
			log.debug( name + "<-" + file );
			addFile( file.getValue() );
		}
	}

	private void parseSecurity(Element secNode) {
		String contextId = secNode.attributeValue( "context" );
      setProperty(Environment.JACC_CONTEXTID, contextId);
		log.info( "JACC contextID: " + contextId );
		JACCConfiguration jcfg = new JACCConfiguration( contextId );
		Iterator grantElements = secNode.elementIterator();
		while ( grantElements.hasNext() ) {
			Element grantElement = (Element) grantElements.next();
			String elementName = grantElement.getName();
			if ( "grant".equals( elementName ) ) {
				jcfg.addPermission(
						grantElement.attributeValue( "role" ),
						grantElement.attributeValue( "entity-name" ),
						grantElement.attributeValue( "actions" )
					);
			}
		}
	}

	private void parseEvent(Element element) {
		String type = element.attributeValue( "type" );
		List listeners = element.elements();
		String[] listenerClasses = new String[ listeners.size() ];
		for ( int i = 0; i < listeners.size() ; i++ ) {
			listenerClasses[i] = ( (Element) listeners.get( i ) ).attributeValue( "class" );
		}
		log.debug( "Event listeners: " + type + "=" + StringHelper.toString( listenerClasses ) );
		setListeners( type, listenerClasses );
	}

	private void parseListener(Element element) {
		String type = element.attributeValue( "type" );
		if ( type == null ) {
			throw new MappingException( "No type specified for listener" );
		}
		String impl = element.attributeValue( "class" );
		log.debug( "Event listener: " + type + "=" + impl );
		setListeners( type, new String[]{impl} );
	}

	public void setListener(String type, String listener) {
		String[] listeners = null;
		if ( listener != null ) {
			listeners = (String[]) Array.newInstance( String.class, 1 );
			listeners[0] = listener;
		}
		setListeners( type, listeners );
	}

	public void setListeners(String type, String[] listenerClasses) {
		Object[] listeners = null;
		if ( listenerClasses != null ) {
			listeners = (Object[]) Array.newInstance( eventListeners.getListenerClassFor(type), listenerClasses.length );
			for ( int i = 0; i < listeners.length ; i++ ) {
				try {
					listeners[i] = ReflectHelper.classForName( listenerClasses[i] ).newInstance();
				}
				catch (Exception e) {
					throw new MappingException(
							"Unable to instantiate specified event (" + type + ") listener class: " + listenerClasses[i],
							e
						);
				}
			}
		}
		setListeners( type, listeners );
	}

	public void setListener(String type, Object listener) {
		Object[] listeners = null;
		if ( listener != null ) {
			listeners = (Object[]) Array.newInstance( eventListeners.getListenerClassFor(type), 1 );
			listeners[0] = listener;
		}
		setListeners( type, listeners );
	}

	public void setListeners(String type, Object[] listeners) {
		if ( "auto-flush".equals( type ) ) {
			if ( listeners == null ) {
				eventListeners.setAutoFlushEventListeners( new AutoFlushEventListener[]{} );
			}
			else {
				eventListeners.setAutoFlushEventListeners( (AutoFlushEventListener[]) listeners );
			}
		}
		else if ( "merge".equals( type ) ) {
			if ( listeners == null ) {
				eventListeners.setMergeEventListeners( new MergeEventListener[]{} );
			}
			else {
				eventListeners.setMergeEventListeners( (MergeEventListener[]) listeners );
			}
		}
		else if ( "create".equals( type ) ) {
			if ( listeners == null ) {
				eventListeners.setPersistEventListeners( new PersistEventListener[]{} );
			}
			else {
				eventListeners.setPersistEventListeners( (PersistEventListener[]) listeners );
			}
		}
		else if ( "create-onflush".equals( type ) ) {
			if ( listeners == null ) {
				eventListeners.setPersistOnFlushEventListeners( new PersistEventListener[]{} );
			}
			else {
				eventListeners.setPersistOnFlushEventListeners( (PersistEventListener[]) listeners );
			}
		}
		else if ( "delete".equals( type ) ) {
			if ( listeners == null ) {
				eventListeners.setDeleteEventListeners( new DeleteEventListener[]{} );
			}
			else {
				eventListeners.setDeleteEventListeners( (DeleteEventListener[]) listeners );
			}
		}
		else if ( "dirty-check".equals( type ) ) {
			if ( listeners == null ) {
				eventListeners.setDirtyCheckEventListeners( new DirtyCheckEventListener[]{} );
			}
			else {
				eventListeners.setDirtyCheckEventListeners( (DirtyCheckEventListener[]) listeners );
			}
		}
		else if ( "evict".equals( type ) ) {
			if ( listeners == null ) {
				eventListeners.setEvictEventListeners( new EvictEventListener[]{} );
			}
			else {
				eventListeners.setEvictEventListeners( (EvictEventListener[]) listeners );
			}
		}
		else if ( "flush".equals( type ) ) {
			if ( listeners == null ) {
				eventListeners.setFlushEventListeners( new FlushEventListener[]{} );
			}
			else {
				eventListeners.setFlushEventListeners( (FlushEventListener[]) listeners );
			}
		}
		else if ( "flush-entity".equals( type ) ) {
			if ( listeners == null ) {
				eventListeners.setFlushEntityEventListeners( new FlushEntityEventListener[]{} );
			}
			else {
				eventListeners.setFlushEntityEventListeners( (FlushEntityEventListener[]) listeners );
			}
		}
		else if ( "load".equals( type ) ) {
			if ( listeners == null ) {
				eventListeners.setLoadEventListeners( new LoadEventListener[]{} );
			}
			else {
				eventListeners.setLoadEventListeners( (LoadEventListener[]) listeners );
			}
		}
		else if ( "load-collection".equals( type ) ) {
			if ( listeners == null ) {
				eventListeners.setInitializeCollectionEventListeners(
						new InitializeCollectionEventListener[]{}
					);
			}
			else {
				eventListeners.setInitializeCollectionEventListeners(
						(InitializeCollectionEventListener[]) listeners
					);
			}
		}
		else if ( "lock".equals( type ) ) {
			if ( listeners == null ) {
				eventListeners.setLockEventListeners( new LockEventListener[]{} );
			}
			else {
				eventListeners.setLockEventListeners( (LockEventListener[]) listeners );
			}
		}
		else if ( "refresh".equals( type ) ) {
			if ( listeners == null ) {
				eventListeners.setRefreshEventListeners( new RefreshEventListener[]{} );
			}
			else {
				eventListeners.setRefreshEventListeners( (RefreshEventListener[]) listeners );
			}
		}
		else if ( "replicate".equals( type ) ) {
			if ( listeners == null ) {
				eventListeners.setReplicateEventListeners( new ReplicateEventListener[]{} );
			}
			else {
				eventListeners.setReplicateEventListeners( (ReplicateEventListener[]) listeners );
			}
		}
		else if ( "save-update".equals( type ) ) {
			if ( listeners == null ) {
				eventListeners.setSaveOrUpdateEventListeners( new SaveOrUpdateEventListener[]{} );
			}
			else {
				eventListeners.setSaveOrUpdateEventListeners( (SaveOrUpdateEventListener[]) listeners );
			}
		}
		else if ( "save".equals( type ) ) {
			if ( listeners == null ) {
				eventListeners.setSaveEventListeners( new SaveOrUpdateEventListener[]{} );
			}
			else {
				eventListeners.setSaveEventListeners( (SaveOrUpdateEventListener[]) listeners );
			}
		}
		else if ( "update".equals( type ) ) {
			if ( listeners == null ) {
				eventListeners.setUpdateEventListeners( new SaveOrUpdateEventListener[]{} );
			}
			else {
				eventListeners.setUpdateEventListeners( (SaveOrUpdateEventListener[]) listeners );
			}
		}
		else if ( "pre-load".equals( type ) ) {
			if ( listeners == null ) {
				eventListeners.setPreLoadEventListeners( new PreLoadEventListener[]{} );
			}
			else {
				eventListeners.setPreLoadEventListeners( (PreLoadEventListener[]) listeners );
			}
		}
		else if ( "pre-update".equals( type ) ) {
			if ( listeners == null ) {
				eventListeners.setPreUpdateEventListeners( new PreUpdateEventListener[]{} );
			}
			else {
				eventListeners.setPreUpdateEventListeners( (PreUpdateEventListener[]) listeners );
			}
		}
		else if ( "pre-delete".equals( type ) ) {
			if ( listeners == null ) {
				eventListeners.setPreDeleteEventListeners( new PreDeleteEventListener[]{} );
			}
			else {
				eventListeners.setPreDeleteEventListeners( (PreDeleteEventListener[]) listeners );
			}
		}
		else if ( "pre-insert".equals( type ) ) {
			if ( listeners == null ) {
				eventListeners.setPreInsertEventListeners( new PreInsertEventListener[]{} );
			}
			else {
				eventListeners.setPreInsertEventListeners( (PreInsertEventListener[]) listeners );
			}
		}
		else if ( "pre-collection-recreate".equals( type ) ) {
			if ( listeners == null ) {
				eventListeners.setPreCollectionRecreateEventListeners( new PreCollectionRecreateEventListener[]{} );
			}
			else {
				eventListeners.setPreCollectionRecreateEventListeners( (PreCollectionRecreateEventListener[]) listeners );
			}
		}
		else if ( "pre-collection-remove".equals( type ) ) {
			if ( listeners == null ) {
				eventListeners.setPreCollectionRemoveEventListeners( new PreCollectionRemoveEventListener[]{} );
			}
			else {
				eventListeners.setPreCollectionRemoveEventListeners( ( PreCollectionRemoveEventListener[]) listeners );
			}
		}
		else if ( "pre-collection-update".equals( type ) ) {
			if ( listeners == null ) {
				eventListeners.setPreCollectionUpdateEventListeners( new PreCollectionUpdateEventListener[]{} );
			}
			else {
				eventListeners.setPreCollectionUpdateEventListeners( ( PreCollectionUpdateEventListener[]) listeners );
			}
		}
		else if ( "post-load".equals( type ) ) {
			if ( listeners == null ) {
				eventListeners.setPostLoadEventListeners( new PostLoadEventListener[]{} );
			}
			else {
				eventListeners.setPostLoadEventListeners( (PostLoadEventListener[]) listeners );
			}
		}
		else if ( "post-update".equals( type ) ) {
			if ( listeners == null ) {
				eventListeners.setPostUpdateEventListeners( new PostUpdateEventListener[]{} );
			}
			else {
				eventListeners.setPostUpdateEventListeners( (PostUpdateEventListener[]) listeners );
			}
		}
		else if ( "post-delete".equals( type ) ) {
			if ( listeners == null ) {
				eventListeners.setPostDeleteEventListeners( new PostDeleteEventListener[]{} );
			}
			else {
				eventListeners.setPostDeleteEventListeners( (PostDeleteEventListener[]) listeners );
			}
		}
		else if ( "post-insert".equals( type ) ) {
			if ( listeners == null ) {
				eventListeners.setPostInsertEventListeners( new PostInsertEventListener[]{} );
			}
			else {
				eventListeners.setPostInsertEventListeners( (PostInsertEventListener[]) listeners );
			}
		}
		else if ( "post-commit-update".equals( type ) ) {
			if ( listeners == null ) {
				eventListeners.setPostCommitUpdateEventListeners(
						new PostUpdateEventListener[]{}
					);
			}
			else {
				eventListeners.setPostCommitUpdateEventListeners( (PostUpdateEventListener[]) listeners );
			}
		}
		else if ( "post-commit-delete".equals( type ) ) {
			if ( listeners == null ) {
				eventListeners.setPostCommitDeleteEventListeners(
						new PostDeleteEventListener[]{}
					);
			}
			else {
				eventListeners.setPostCommitDeleteEventListeners( (PostDeleteEventListener[]) listeners );
			}
		}
		else if ( "post-commit-insert".equals( type ) ) {
			if ( listeners == null ) {
				eventListeners.setPostCommitInsertEventListeners(
						new PostInsertEventListener[]{}
				);
			}
			else {
				eventListeners.setPostCommitInsertEventListeners( (PostInsertEventListener[]) listeners );
			}
		}
		else if ( "post-collection-recreate".equals( type ) ) {
			if ( listeners == null ) {
				eventListeners.setPostCollectionRecreateEventListeners( new PostCollectionRecreateEventListener[]{} );
			}
			else {
				eventListeners.setPostCollectionRecreateEventListeners( (PostCollectionRecreateEventListener[]) listeners );
			}
		}
		else if ( "post-collection-remove".equals( type ) ) {
			if ( listeners == null ) {
				eventListeners.setPostCollectionRemoveEventListeners( new PostCollectionRemoveEventListener[]{} );
			}
			else {
				eventListeners.setPostCollectionRemoveEventListeners( ( PostCollectionRemoveEventListener[]) listeners );
			}
		}
		else if ( "post-collection-update".equals( type ) ) {
			if ( listeners == null ) {
				eventListeners.setPostCollectionUpdateEventListeners( new PostCollectionUpdateEventListener[]{} );
			}
			else {
				eventListeners.setPostCollectionUpdateEventListeners( ( PostCollectionUpdateEventListener[]) listeners );
			}
		}
		else {
			throw new MappingException("Unrecognized listener type [" + type + "]");
		}
	}

	public EventListeners getEventListeners() {
		return eventListeners;
	}

	RootClass getRootClassMapping(String clazz) throws MappingException {
		try {
			return (RootClass) getClassMapping( clazz );
		}
		catch (ClassCastException cce) {
			throw new MappingException( "You may only specify a cache for root <class> mappings" );
		}
	}

	/**
	 * Set up a cache for an entity class
	 *
	 * @param clazz
	 * @param concurrencyStrategy
	 * @return Configuration
	 * @throws MappingException
	 */
	public Configuration setCacheConcurrencyStrategy(String clazz, String concurrencyStrategy)
			throws MappingException {
		setCacheConcurrencyStrategy( clazz, concurrencyStrategy, clazz );
		return this;
	}

	public void setCacheConcurrencyStrategy(String clazz, String concurrencyStrategy, String region)
			throws MappingException {
		setCacheConcurrencyStrategy( clazz, concurrencyStrategy, region, true );
	}

	void setCacheConcurrencyStrategy(String clazz, String concurrencyStrategy, String region, boolean includeLazy)
			throws MappingException {
		RootClass rootClass = getRootClassMapping( clazz );
		if ( rootClass == null ) {
			throw new MappingException( "Cannot cache an unknown entity: " + clazz );
		}
		rootClass.setCacheConcurrencyStrategy( concurrencyStrategy );
		rootClass.setCacheRegionName( region );
		rootClass.setLazyPropertiesCacheable( includeLazy );
	}

	/**
	 * Set up a cache for a collection role
	 *
	 * @param collectionRole
	 * @param concurrencyStrategy
	 * @return Configuration
	 * @throws MappingException
	 */
	public Configuration setCollectionCacheConcurrencyStrategy(String collectionRole, String concurrencyStrategy)
			throws MappingException {
		setCollectionCacheConcurrencyStrategy( collectionRole, concurrencyStrategy, collectionRole );
		return this;
	}

	public void setCollectionCacheConcurrencyStrategy(String collectionRole, String concurrencyStrategy, String region)
			throws MappingException {
		Collection collection = getCollectionMapping( collectionRole );
		if ( collection == null ) {
			throw new MappingException( "Cannot cache an unknown collection: " + collectionRole );
		}
		collection.setCacheConcurrencyStrategy( concurrencyStrategy );
		collection.setCacheRegionName( region );
	}

	/**
	 * Get the query language imports
	 *
	 * @return a mapping from "import" names to fully qualified class names
	 */
	public Map getImports() {
		return imports;
	}

	/**
	 * Create an object-oriented view of the configuration properties
	 */
	public Settings buildSettings() throws HibernateException {
		Properties clone = ( Properties ) properties.clone();
		PropertiesHelper.resolvePlaceHolders( clone );
		return buildSettingsInternal( clone );
	}

	public Settings buildSettings(Properties props) throws HibernateException {
		return buildSettingsInternal( props );
	}

	private Settings buildSettingsInternal(Properties props) {
		final Settings settings = settingsFactory.buildSettings( props );
		settings.setEntityTuplizerFactory( this.getEntityTuplizerFactory() );
//		settings.setComponentTuplizerFactory( this.getComponentTuplizerFactory() );
		return settings;
	}

	public Map getNamedSQLQueries() {
		return namedSqlQueries;
	}

	public Map getSqlResultSetMappings() {
		return sqlResultSetMappings;
	}

	/**
	 * @return the NamingStrategy.
	 */
	public NamingStrategy getNamingStrategy() {
		return namingStrategy;
	}

	/**
	 * Set a custom naming strategy
	 *
	 * @param namingStrategy the NamingStrategy to set
	 */
	public Configuration setNamingStrategy(NamingStrategy namingStrategy) {
		this.namingStrategy = namingStrategy;
		return this;
	}

	/**
	 * Retrieve the IdentifierGeneratorFactory in effect for this configuration.
	 *
	 * @return This configuration's IdentifierGeneratorFactory.
	 */
	public DefaultIdentifierGeneratorFactory getIdentifierGeneratorFactory() {
		return identifierGeneratorFactory;
	}

	public Mapping buildMapping() {
		return new Mapping() {
			public IdentifierGeneratorFactory getIdentifierGeneratorFactory() {
				return identifierGeneratorFactory;
			}

			/**
			 * Returns the identifier type of a mapped class
			 */
			public Type getIdentifierType(String persistentClass) throws MappingException {
				PersistentClass pc = ( (PersistentClass) classes.get( persistentClass ) );
				if ( pc == null ) {
					throw new MappingException( "persistent class not known: " + persistentClass );
				}
				return pc.getIdentifier().getType();
			}

			public String getIdentifierPropertyName(String persistentClass) throws MappingException {
				final PersistentClass pc = (PersistentClass) classes.get( persistentClass );
				if ( pc == null ) {
					throw new MappingException( "persistent class not known: " + persistentClass );
				}
				if ( !pc.hasIdentifierProperty() ) {
					return null;
				}
				return pc.getIdentifierProperty().getName();
			}

			public Type getReferencedPropertyType(String persistentClass, String propertyName) throws MappingException {
				final PersistentClass pc = (PersistentClass) classes.get( persistentClass );
				if ( pc == null ) {
					throw new MappingException( "persistent class not known: " + persistentClass );
				}
				Property prop = pc.getReferencedProperty( propertyName );
				if ( prop == null ) {
					throw new MappingException(
							"property not known: " +
							persistentClass + '.' + propertyName
						);
				}
				return prop.getType();
			}
		};
	}

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		ois.defaultReadObject();
		this.mapping = buildMapping();
		xmlHelper = new XMLHelper();
	}

	public Map getFilterDefinitions() {
		return filterDefinitions;
	}

	public void addFilterDefinition(FilterDefinition definition) {
		filterDefinitions.put( definition.getFilterName(), definition );
	}

	public Iterator iterateFetchProfiles() {
		return fetchProfiles.values().iterator();
	}

	public void addFetchProfile(FetchProfile fetchProfile) {
		fetchProfiles.put( fetchProfile.getName(), fetchProfile );
	}

	public void addAuxiliaryDatabaseObject(AuxiliaryDatabaseObject object) {
		auxiliaryDatabaseObjects.add( object );
	}

	public Map getSqlFunctions() {
		return sqlFunctions;
	}

	public void addSqlFunction(String functionName, SQLFunction function) {
		sqlFunctions.put( functionName, function );
	}

	public SessionFactoryObserver getSessionFactoryObserver() {
		return sessionFactoryObserver;
	}

	public void setSessionFactoryObserver(SessionFactoryObserver sessionFactoryObserver) {
		this.sessionFactoryObserver = sessionFactoryObserver;
	}


	// Mappings impl ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Internal implementation of the Mappings interface giving access to the Configuration's internal
	 * <tt>metadata repository</tt> state ({@link Configuration#classes}, {@link Configuration#tables}, etc).
	 */
	protected class MappingsImpl implements Mappings, Serializable {

		private String schemaName;

		public String getSchemaName() {
			return schemaName;
		}

		public void setSchemaName(String schemaName) {
			this.schemaName = schemaName;
		}


		private String catalogName;

		public String getCatalogName() {
			return catalogName;
		}

		public void setCatalogName(String catalogName) {
			this.catalogName = catalogName;
		}


		private String defaultPackage;

		public String getDefaultPackage() {
			return defaultPackage;
		}

		public void setDefaultPackage(String defaultPackage) {
			this.defaultPackage = defaultPackage;
		}


		private boolean autoImport;

		public boolean isAutoImport() {
			return autoImport;
		}

		public void setAutoImport(boolean autoImport) {
			this.autoImport = autoImport;
		}


		private boolean defaultLazy;

		public boolean isDefaultLazy() {
			return defaultLazy;
		}

		public void setDefaultLazy(boolean defaultLazy) {
			this.defaultLazy = defaultLazy;
		}


		private String defaultCascade;

		public String getDefaultCascade() {
			return defaultCascade;
		}

		public void setDefaultCascade(String defaultCascade) {
			this.defaultCascade = defaultCascade;
		}


		private String defaultAccess;

		public String getDefaultAccess() {
			return defaultAccess;
		}

		public void setDefaultAccess(String defaultAccess) {
			this.defaultAccess = defaultAccess;
		}


		public NamingStrategy getNamingStrategy() {
			return namingStrategy;
		}

		public void setNamingStrategy(NamingStrategy namingStrategy) {
			Configuration.this.namingStrategy = namingStrategy;
		}


		public Iterator iterateClasses() {
			return classes.values().iterator();
		}

		public PersistentClass getClass(String entityName) {
			return ( PersistentClass ) classes.get( entityName );
		}

		public PersistentClass locatePersistentClassByEntityName(String entityName) {
			PersistentClass persistentClass = ( PersistentClass ) classes.get( entityName );
			if ( persistentClass == null ) {
				String actualEntityName = ( String ) imports.get( entityName );
				if ( StringHelper.isNotEmpty( actualEntityName ) ) {
					persistentClass = ( PersistentClass ) classes.get( actualEntityName );
				}
			}
			return persistentClass;
		}

		public void addClass(PersistentClass persistentClass) throws DuplicateMappingException {
			Object old = classes.put( persistentClass.getEntityName(), persistentClass );
			if ( old != null ) {
				throw new DuplicateMappingException( "class/entity", persistentClass.getEntityName() );
			}
		}

		public void addImport(String entityName, String rename) throws DuplicateMappingException {
			String existing = ( String ) imports.put( rename, entityName );
			if ( existing != null ) {
				if ( existing.equals( entityName ) ) {
					log.info( "duplicate import: {} -> {}", entityName, rename );
				}
				else {
					throw new DuplicateMappingException(
							"duplicate import: " + rename + " refers to both " + entityName +
									" and " + existing + " (try using auto-import=\"false\")",
							"import",
							rename
					);
				}
			}
		}

		public Collection getCollection(String role) {
			return ( Collection ) collections.get( role );
		}

		public Iterator iterateCollections() {
			return collections.values().iterator();
		}

		public void addCollection(Collection collection) throws DuplicateMappingException {
			Object old = collections.put( collection.getRole(), collection );
			if ( old != null ) {
				throw new DuplicateMappingException( "collection role", collection.getRole() );
			}
		}

		public Table getTable(String schema, String catalog, String name) {
			String key = Table.qualify(catalog, schema, name);
			return (Table) tables.get(key);
		}

		public Iterator iterateTables() {
			return tables.values().iterator();
		}

		public Table addTable(
				String schema,
				String catalog,
				String name,
				String subselect,
				boolean isAbstract) {
			name = getObjectNameNormalizer().normalizeIdentifierQuoting( name );
			schema = getObjectNameNormalizer().normalizeIdentifierQuoting( schema );
			catalog = getObjectNameNormalizer().normalizeIdentifierQuoting( catalog );

			String key = subselect == null ? Table.qualify( catalog, schema, name ) : subselect;
			Table table = ( Table ) tables.get( key );

			if ( table == null ) {
				table = new Table();
				table.setAbstract( isAbstract );
				table.setName( name );
				table.setSchema( schema );
				table.setCatalog( catalog );
				table.setSubselect( subselect );
				tables.put( key, table );
			}
			else {
				if ( !isAbstract ) {
					table.setAbstract( false );
				}
			}

			return table;
		}

		public Table addDenormalizedTable(
				String schema,
				String catalog,
				String name,
				boolean isAbstract,
				String subselect,
				Table includedTable) throws DuplicateMappingException {
			name = getObjectNameNormalizer().normalizeIdentifierQuoting( name );
			schema = getObjectNameNormalizer().normalizeIdentifierQuoting( schema );
			catalog = getObjectNameNormalizer().normalizeIdentifierQuoting( catalog );

			String key = subselect == null ? Table.qualify(catalog, schema, name) : subselect;
			if ( tables.containsKey( key ) ) {
				throw new DuplicateMappingException( "table", name );
			}

			Table table = new DenormalizedTable( includedTable );
			table.setAbstract( isAbstract );
			table.setName( name );
			table.setSchema( schema );
			table.setCatalog( catalog );
			table.setSubselect( subselect );

			tables.put( key, table );
			return table;
		}

		public NamedQueryDefinition getQuery(String name) {
			return ( NamedQueryDefinition ) namedQueries.get( name );
		}

		public void addQuery(String name, NamedQueryDefinition query) throws DuplicateMappingException {
			checkQueryName( name );
			namedQueries.put( name.intern(), query );
		}

		private void checkQueryName(String name) throws DuplicateMappingException {
			if ( namedQueries.containsKey( name ) || namedSqlQueries.containsKey( name ) ) {
				throw new DuplicateMappingException( "query", name );
			}
		}

		public NamedSQLQueryDefinition getSQLQuery(String name) {
			return ( NamedSQLQueryDefinition ) namedSqlQueries.get( name );
		}

		public void addSQLQuery(String name, NamedSQLQueryDefinition query) throws DuplicateMappingException {
			checkQueryName( name );
			namedSqlQueries.put( name.intern(), query );
		}

		public ResultSetMappingDefinition getResultSetMapping(String name) {
			return (ResultSetMappingDefinition) sqlResultSetMappings.get(name);
		}

		public void addResultSetMapping(ResultSetMappingDefinition sqlResultSetMapping) throws DuplicateMappingException {
			Object old = sqlResultSetMappings.put( sqlResultSetMapping.getName(), sqlResultSetMapping );
			if ( old != null ) {
				throw new DuplicateMappingException( "resultSet",  sqlResultSetMapping.getName() );
			}
		}

		protected void removeResultSetMapping(String name) {
			sqlResultSetMappings.remove( name );
		}

		public TypeDef getTypeDef(String typeName) {
			return ( TypeDef ) typeDefs.get( typeName );
		}

		public void addTypeDef(String typeName, String typeClass, Properties paramMap) {
			TypeDef def = new TypeDef( typeClass, paramMap );
			typeDefs.put( typeName, def );
			log.debug( "Added " + typeName + " with class " + typeClass );
		}

		public Map getFilterDefinitions() {
			return filterDefinitions;
		}

		public FilterDefinition getFilterDefinition(String name) {
			return ( FilterDefinition ) filterDefinitions.get( name );
		}

		public void addFilterDefinition(FilterDefinition definition) {
			filterDefinitions.put( definition.getFilterName(), definition );
		}

		public FetchProfile findOrCreateFetchProfile(String name) {
			FetchProfile profile = ( FetchProfile ) fetchProfiles.get( name );
			if ( profile == null ) {
				profile = new FetchProfile( name );
				fetchProfiles.put( name, profile );
			}
			return profile;
		}

		public Iterator iterateAuxliaryDatabaseObjects() {
			return auxiliaryDatabaseObjects.iterator();
		}

		public ListIterator iterateAuxliaryDatabaseObjectsInReverse() {
			return auxiliaryDatabaseObjects.listIterator( auxiliaryDatabaseObjects.size() );
		}

		public void addAuxiliaryDatabaseObject(AuxiliaryDatabaseObject auxiliaryDatabaseObject) {
			auxiliaryDatabaseObjects.add( auxiliaryDatabaseObject );
		}

		/**
		 * Internal struct used to help track physical table names to logical table names.
		 */
		private class TableDescription implements Serializable {
			final String logicalName;
			final Table denormalizedSupertable;

			TableDescription(String logicalName, Table denormalizedSupertable) {
				this.logicalName = logicalName;
				this.denormalizedSupertable = denormalizedSupertable;
			}
		}

		public String getLogicalTableName(Table table) throws MappingException {
			return getLogicalTableName( table.getQuotedSchema(), table.getCatalog(), table.getQuotedName() );
		}

		private String getLogicalTableName(String schema, String catalog, String physicalName) throws MappingException {
			String key = buildTableNameKey( schema, catalog, physicalName );
			TableDescription descriptor = (TableDescription) tableNameBinding.get( key );
			if (descriptor == null) {
				throw new MappingException( "Unable to find physical table: " + physicalName);
			}
			return descriptor.logicalName;
		}

		public void addTableBinding(
				String schema,
				String catalog,
				String logicalName,
				String physicalName,
				Table denormalizedSuperTable) throws DuplicateMappingException {
			String key = buildTableNameKey( schema, catalog, physicalName );
			TableDescription tableDescription = new TableDescription( logicalName, denormalizedSuperTable );
			TableDescription oldDescriptor = ( TableDescription ) tableNameBinding.put( key, tableDescription );
			if ( oldDescriptor != null && ! oldDescriptor.logicalName.equals( logicalName ) ) {
				//TODO possibly relax that
				throw new DuplicateMappingException(
						"Same physical table name [" + physicalName + "] references several logical table names: [" +
								oldDescriptor.logicalName + "], [" + logicalName + ']',
						"table",
						physicalName
				);
			}
		}

		private String buildTableNameKey(String schema, String catalog, String finalName) {
			StringBuffer keyBuilder = new StringBuffer();
			if (schema != null) keyBuilder.append( schema );
			keyBuilder.append( ".");
			if (catalog != null) keyBuilder.append( catalog );
			keyBuilder.append( ".");
			keyBuilder.append( finalName );
			return keyBuilder.toString();
		}

		/**
		 * Internal struct used to maintain xref between physical and logical column
		 * names for a table.  Mainly this is used to ensure that the defined
		 * {@link NamingStrategy} is not creating duplicate column names.
		 */
		private class TableColumnNameBinding implements Serializable {
			private final String tableName;
			private Map/*<String, String>*/ logicalToPhysical = new HashMap();
			private Map/*<String, String>*/ physicalToLogical = new HashMap();

			private TableColumnNameBinding(String tableName) {
				this.tableName = tableName;
			}

			public void addBinding(String logicalName, Column physicalColumn) {
				bindLogicalToPhysical( logicalName, physicalColumn );
				bindPhysicalToLogical( logicalName, physicalColumn );
			}

			private void bindLogicalToPhysical(String logicalName, Column physicalColumn) throws DuplicateMappingException {
				final String logicalKey = logicalName.toLowerCase();
				final String physicalName = physicalColumn.getQuotedName();
				final String existingPhysicalName = ( String ) logicalToPhysical.put( logicalKey, physicalName );
				if ( existingPhysicalName != null ) {
					boolean areSamePhysicalColumn = physicalColumn.isQuoted()
							? existingPhysicalName.equals( physicalName )
							: existingPhysicalName.equalsIgnoreCase( physicalName );
					if ( ! areSamePhysicalColumn ) {
						throw new DuplicateMappingException(
								" Table [" + tableName + "] contains logical column name [" + logicalName
										+ "] referenced by multiple physical column names: [" + existingPhysicalName
										+ "], [" + physicalName + "]",
								"column-binding",
								tableName + "." + logicalName
						);
					}
				}
			}

			private void bindPhysicalToLogical(String logicalName, Column physicalColumn) throws DuplicateMappingException {
				final String physicalName = physicalColumn.getQuotedName();
				final String existingLogicalName = ( String ) physicalToLogical.put( physicalName, logicalName );
				if ( existingLogicalName != null && ! existingLogicalName.equals( logicalName ) ) {
					throw new DuplicateMappingException(
							" Table [" + tableName + "] contains phyical column name [" + physicalName
									+ "] represented by different logical column names: [" + existingLogicalName
									+ "], [" + logicalName + "]",
							"column-binding",
							tableName + "." + physicalName
					);
				}
			}
		}

		public void addColumnBinding(String logicalName, Column physicalColumn, Table table) throws DuplicateMappingException {
			TableColumnNameBinding binding = ( TableColumnNameBinding ) columnNameBindingPerTable.get( table );
			if ( binding == null ) {
				binding = new TableColumnNameBinding( table.getName() );
				columnNameBindingPerTable.put( table, binding );
			}
			binding.addBinding( logicalName, physicalColumn );
		}

		public String getPhysicalColumnName(String logicalName, Table table) throws MappingException {
			logicalName = logicalName.toLowerCase();
			String finalName = null;
			Table currentTable = table;
			do {
				TableColumnNameBinding binding = ( TableColumnNameBinding ) columnNameBindingPerTable.get( currentTable );
				if ( binding != null ) {
					finalName = ( String ) binding.logicalToPhysical.get( logicalName );
				}
				String key = buildTableNameKey(
						currentTable.getSchema(), currentTable.getCatalog(), currentTable.getName()
				);
				TableDescription description = ( TableDescription ) tableNameBinding.get( key );
				if ( description != null ) {
					currentTable = description.denormalizedSupertable;
				}
			} while ( finalName == null && currentTable != null );

			if ( finalName == null ) {
				throw new MappingException(
						"Unable to find column with logical name " + logicalName + " in table " + table.getName()
				);
			}
			return finalName;
		}

		public String getLogicalColumnName(String physicalName, Table table) throws MappingException {
			String logical = null;
			Table currentTable = table;
			TableDescription description = null;
			do {
				TableColumnNameBinding binding = ( TableColumnNameBinding ) columnNameBindingPerTable.get( currentTable );
				if ( binding != null ) {
					logical = ( String ) binding.physicalToLogical.get( physicalName );
				}
				String key = buildTableNameKey(
						currentTable.getSchema(), currentTable.getCatalog(), currentTable.getName()
				);
				description = ( TableDescription ) tableNameBinding.get( key );
				if ( description != null ) {
					currentTable = description.denormalizedSupertable;
				}
			}
			while ( logical == null && currentTable != null && description != null );
			if ( logical == null ) {
				throw new MappingException(
						"Unable to find logical column name from physical name "
								+ physicalName + " in table " + table.getName()
				);
			}
			return logical;
		}

		public void addSecondPass(SecondPass sp) {
			addSecondPass( sp, false );
		}

		public void addSecondPass(SecondPass sp, boolean onTopOfTheQueue) {
			if ( onTopOfTheQueue ) {
				secondPasses.add( 0, sp );
			}
			else {
				secondPasses.add( sp );
			}
		}

		public void addPropertyReference(String referencedClass, String propertyName) {
			propertyReferences.add( new PropertyReference( referencedClass, propertyName, false ) );
		}

		public void addUniquePropertyReference(String referencedClass, String propertyName) {
			propertyReferences.add( new PropertyReference( referencedClass, propertyName, true ) );
		}

		public void addToExtendsQueue(ExtendsQueueEntry entry) {
			extendsQueue.put( entry, null );
		}

		public DefaultIdentifierGeneratorFactory getIdentifierGeneratorFactory() {
			return identifierGeneratorFactory;
		}

		public void addMappedSuperclass(Class type, MappedSuperclass mappedSuperclass) {
			mappedSuperclasses.put( type, mappedSuperclass );
		}

		public MappedSuperclass getMappedSuperclass(Class type) {
			return (MappedSuperclass) mappedSuperclasses.get( type );
		}

		public ObjectNameNormalizer getObjectNameNormalizer() {
			return normalizer;
		}

		public Properties getConfigurationProperties() {
			return properties;
		}
	}

	final ObjectNameNormalizer normalizer = new ObjectNameNormalizerImpl();

	final class ObjectNameNormalizerImpl extends ObjectNameNormalizer implements Serializable {
		public boolean isUseQuotedIdentifiersGlobally() {
			//Do not cache this value as we lazily set it in Hibernate Annotation (AnnotationConfiguration)
			//TODO use a dedicated protected useQuotedIdentifier flag in Configuration (overriden by AnnotationConfiguration)
			String setting = (String) properties.get( Environment.GLOBALLY_QUOTED_IDENTIFIERS );
			return setting != null && Boolean.valueOf( setting ).booleanValue();
		}

		public NamingStrategy getNamingStrategy() {
			return namingStrategy;
		}
	}
}
