/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.SharedCacheMode;

import org.hibernate.EmptyInterceptor;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.internal.ClassmateContext;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.boot.model.convert.internal.ClassBasedConverterDescriptor;
import org.hibernate.boot.model.convert.internal.InstanceBasedConverterDescriptor;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.query.NamedHqlQueryDefinition;
import org.hibernate.boot.query.NamedNativeQueryDefinition;
import org.hibernate.boot.query.NamedProcedureCallDefinition;
import org.hibernate.boot.query.NamedResultSetMappingDescriptor;
import org.hibernate.boot.spi.XmlMappingBinderAccess;
import org.hibernate.cfg.annotations.NamedEntityGraphDefinition;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.BasicType;
import org.hibernate.type.SerializationException;
import org.hibernate.usertype.UserType;

/**
 * A convenience API making it easier to bootstrap an instance of Hibernate
 * using {@link MetadataBuilder} and {@link StandardServiceRegistryBuilder}
 * under the covers.
 * <p>
 * An instance of {@code Configuration} may be obtained simply by
 * instantiation, using {@link #Configuration() new Configuration()}.
 * <p>
 * A {@code Configuration} may be used to aggregate:
 * <ul>
 * <li>{@linkplain #setProperty(String, String) configuration properties}
 *     from various sources, and
 * <li>entity O/R mappings, defined in either {@linkplain #addAnnotatedClass
 *    annotated classes}, or {@linkplain #addFile XML mapping documents}.
 * </ul>
 * In addition, there are convenience methods for adding
 * {@link #addAttributeConverter attribute converters},
 * {@link #registerTypeContributor type contributors},
 * {@link #addSqlFunction SQL function descriptors}, and
 * {@link #addAuxiliaryDatabaseObject auxiliary database objects}, for
 * setting {@link #setImplicitNamingStrategy naming strategies} and a
 * {@link #setCurrentTenantIdentifierResolver tenant id resolver},
 * and more.
 * <p>
 * Finally, an instance of {@link SessionFactoryBuilder} is obtained by
 * calling {@link #buildSessionFactory()}.
 * <p>
 * Ultimately, this class simply delegates to {@link MetadataBuilder} and
 * {@link StandardServiceRegistryBuilder} to actually do the hard work of
 * {@linkplain #buildSessionFactory() building} the {@code SessionFactory}.
 * <p>
 * Configuration properties are enumerated by {@link AvailableSettings}.
 *
 * @author Gavin King
 * @author Steve Ebersole
 *
 * @see SessionFactory
 * @see AvailableSettings
 */
public class Configuration {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( Configuration.class );

	public static final String ARTEFACT_PROCESSING_ORDER = AvailableSettings.ARTIFACT_PROCESSING_ORDER;

	private final BootstrapServiceRegistry bootstrapServiceRegistry;
	private final MetadataSources metadataSources;
	private final ClassmateContext classmateContext;

	// used during processing mappings
	private ImplicitNamingStrategy implicitNamingStrategy;
	private PhysicalNamingStrategy physicalNamingStrategy;
	private final List<BasicType<?>> basicTypes = new ArrayList<>();
	private List<UserTypeRegistration> userTypeRegistrations;
	private final List<TypeContributor> typeContributorRegistrations = new ArrayList<>();
	private Map<String, NamedHqlQueryDefinition> namedQueries;
	private Map<String, NamedNativeQueryDefinition> namedSqlQueries;
	private Map<String, NamedProcedureCallDefinition> namedProcedureCallMap;
	private Map<String, NamedResultSetMappingDescriptor> sqlResultSetMappings;
	private Map<String, NamedEntityGraphDefinition> namedEntityGraphMap;

	private Map<String, SqmFunctionDescriptor> customFunctionDescriptors;
	private List<AuxiliaryDatabaseObject> auxiliaryDatabaseObjectList;
	private HashMap<Class<?>, ConverterDescriptor> attributeConverterDescriptorsByClass;

	// used to build SF
	private StandardServiceRegistryBuilder standardServiceRegistryBuilder;
	private EntityNotFoundDelegate entityNotFoundDelegate;
	private Interceptor interceptor;
	private SessionFactoryObserver sessionFactoryObserver;
	private CurrentTenantIdentifierResolver currentTenantIdentifierResolver;
	private Properties properties;
	private SharedCacheMode sharedCacheMode;

	/**
	 * Create a new instance, using a default {@link BootstrapServiceRegistry}
	 * and a newly instantiated {@link MetadataSources}.
	 * <p>
	 * This is the usual method of obtaining a {@code Configuration}.
	 */
	public Configuration() {
		this( new BootstrapServiceRegistryBuilder().build() );
	}

	/**
	 * Create a new instance, using the given {@link BootstrapServiceRegistry}
	 * and a newly instantiated {@link MetadataSources}.
	 */
	public Configuration(BootstrapServiceRegistry serviceRegistry) {
		this.bootstrapServiceRegistry = serviceRegistry;
		this.metadataSources = new MetadataSources( serviceRegistry );
		this.classmateContext = new ClassmateContext();
		reset();
	}

	/**
	 * Create a new instance, using the given {@link MetadataSources}, and a
	 * {@link BootstrapServiceRegistry} obtained from the {@link MetadataSources}.
	 */
	public Configuration(MetadataSources metadataSources) {
		this.bootstrapServiceRegistry = getBootstrapRegistry( metadataSources.getServiceRegistry() );
		this.metadataSources = metadataSources;
		this.classmateContext = new ClassmateContext();
		reset();
	}

	private static BootstrapServiceRegistry getBootstrapRegistry(ServiceRegistry serviceRegistry) {
		if ( serviceRegistry instanceof BootstrapServiceRegistry ) {
			return (BootstrapServiceRegistry) serviceRegistry;
		}
		else if ( serviceRegistry instanceof StandardServiceRegistry ) {
			final StandardServiceRegistry ssr = (StandardServiceRegistry) serviceRegistry;
			return (BootstrapServiceRegistry) ssr.getParentServiceRegistry();
		}

		throw new HibernateException(
				"No ServiceRegistry was passed to Configuration#buildSessionFactory " +
						"and could not determine how to locate BootstrapServiceRegistry " +
						"from Configuration instantiation"
		);
	}

	protected void reset() {
		implicitNamingStrategy = ImplicitNamingStrategyJpaCompliantImpl.INSTANCE;
		physicalNamingStrategy = PhysicalNamingStrategyStandardImpl.INSTANCE;
		namedQueries = new HashMap<>();
		namedSqlQueries = new HashMap<>();
		sqlResultSetMappings = new HashMap<>();
		namedEntityGraphMap = new HashMap<>();
		namedProcedureCallMap = new HashMap<>();

		standardServiceRegistryBuilder = new StandardServiceRegistryBuilder( bootstrapServiceRegistry );
		interceptor = EmptyInterceptor.INSTANCE;
		properties = new Properties(  );
		properties.putAll( standardServiceRegistryBuilder.getSettings() );
	}


	// properties/settings ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Get all properties
	 *
	 * @return all properties
	 */
	public Properties getProperties() {
		return properties;
	}

	/**
	 * Specify a completely new set of properties
	 *
	 * @param properties The new set of properties
	 *
	 * @return this for method chaining
	 */
	public Configuration setProperties(Properties properties) {
		this.properties = properties;
		return this;
	}

	/**
	 * Get a property value by name
	 *
	 * @param propertyName The name of the property
	 *
	 * @return The value currently associated with that property name; may be null.
	 */
	public String getProperty(String propertyName) {
		Object o = properties.get( propertyName );
		return o instanceof String ? (String) o : null;
	}

	/**
	 * Set a property value by name
	 *
	 * @param propertyName The name of the property to set
	 * @param value The new property value
	 *
	 * @return this for method chaining
	 */
	public Configuration setProperty(String propertyName, String value) {
		properties.setProperty( propertyName, value );
		return this;
	}

	/**
	 * Add the given properties to ours.
	 *
	 * @param properties The properties to add.
	 *
	 * @return this for method chaining
	 */
	public Configuration addProperties(Properties properties) {
		this.properties.putAll( properties );
		return this;
	}

	public void setImplicitNamingStrategy(ImplicitNamingStrategy implicitNamingStrategy) {
		this.implicitNamingStrategy = implicitNamingStrategy;
	}

	public void setPhysicalNamingStrategy(PhysicalNamingStrategy physicalNamingStrategy) {
		this.physicalNamingStrategy = physicalNamingStrategy;
	}

	/**
	 * Use the mappings and properties specified in an application resource named
	 * {@code hibernate.cfg.xml}.
	 *
	 * @return this for method chaining
	 *
	 * @throws HibernateException Generally indicates we cannot find {@code hibernate.cfg.xml}
	 *
	 * @see #configure(String)
	 */
	public Configuration configure() throws HibernateException {
		return configure( StandardServiceRegistryBuilder.DEFAULT_CFG_RESOURCE_NAME );
	}

	/**
	 * Use the mappings and properties specified in the given application resource.
	 * <p>
	 * The format of the resource is defined by {@code hibernate-configuration-3.0.dtd}.
	 *
	 * @param resource The resource to use
	 *
	 * @return this for method chaining
	 *
	 * @throws HibernateException Generally indicates we cannot find the named resource
	 */
	public Configuration configure(String resource) throws HibernateException {
		standardServiceRegistryBuilder.configure( resource );
		// todo : still need to have StandardServiceRegistryBuilder handle the "other cfg.xml" elements.
		//		currently it just reads the config properties
		properties.putAll( standardServiceRegistryBuilder.getSettings() );
		return this;
	}

	/**
	 * Intended for internal testing use only!!!
	 */
	public StandardServiceRegistryBuilder getStandardServiceRegistryBuilder() {
		return standardServiceRegistryBuilder;
	}

	/**
	 * Use the mappings and properties specified in the given document.
	 * <p>
	 * The format of the document is defined by {@code hibernate-configuration-3.0.dtd}.
	 *
	 * @param url URL from which you wish to load the configuration
	 *
	 * @return this for method chaining
	 *
	 * @throws HibernateException Generally indicates a problem access the url
	 */
	public Configuration configure(URL url) throws HibernateException {
		standardServiceRegistryBuilder.configure( url );
		properties.putAll( standardServiceRegistryBuilder.getSettings() );
		return this;
	}

	/**
	 * Use the mappings and properties specified in the given application file.
	 * <p>
	 * The format of the file is defined by {@code hibernate-configuration-3.0.dtd}.
	 *
	 * @param configFile File from which you wish to load the configuration
	 *
	 * @return this for method chaining
	 *
	 * @throws HibernateException Generally indicates a problem access the file
	 */
	public Configuration configure(File configFile) throws HibernateException {
		standardServiceRegistryBuilder.configure( configFile );
		properties.putAll( standardServiceRegistryBuilder.getSettings() );
		return this;
	}

	// MetadataSources ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public Configuration registerTypeContributor(TypeContributor typeContributor) {
		typeContributorRegistrations.add( typeContributor );
		return this;
	}

	/**
	 * Register a {@linkplain BasicType type} into the type registry,
	 * potentially replacing a previously registered type.
	 *
	 * @param type The type to register.
	 */
	public Configuration registerTypeOverride(BasicType<?> type) {
		basicTypes.add( type );
		return this;
	}

	private interface UserTypeRegistration {
		void registerType(MetadataBuilder metadataBuilder);
	}

	public Configuration registerTypeOverride(UserType type, String[] keys) {
		if ( userTypeRegistrations == null ) {
			userTypeRegistrations = new ArrayList<>();
		}
		userTypeRegistrations.add(
				metadataBuilder -> metadataBuilder.applyBasicType( type, keys )
		);
		return this;
	}

	/**
	 * Read mappings from a particular XML file
	 *
	 * @param xmlFile a path to a file
	 * @return this (for method chaining purposes)
	 * @throws MappingException Indicates inability to locate or parse
	 * the specified mapping file.
	 * @see #addFile(File)
	 */
	public Configuration addFile(String xmlFile) throws MappingException {
		metadataSources.addFile( xmlFile );
		return this;
	}
	/**
	 * Read mappings from a particular XML file.
	 *
	 * @param xmlFile a path to a file
	 *
	 * @return this (for method chaining purposes)
	 *
	 * @throws MappingException Indicates inability to locate the specified mapping file
	 */
	public Configuration addFile(File xmlFile) throws MappingException {
		metadataSources.addFile( xmlFile );
		return this;
	}

	/**
	 * An object capable of parsing XML mapping files that can then be passed
	 * to {@link #addXmlMapping(Binding)}.
	 */
	public XmlMappingBinderAccess getXmlMappingBinderAccess() {
		return metadataSources.getXmlMappingBinderAccess();
	}

	/**
	 * Read mappings that were parsed using {@link #getXmlMappingBinderAccess()}.
	 *
	 * @param binding the parsed mapping
	 *
	 * @return this (for method chaining purposes)
	 */
	public Configuration addXmlMapping(Binding<?> binding) {
		metadataSources.addXmlBinding( binding );
		return this;
	}

	/**
	 * Add a cacheable mapping file.
	 * <p>
	 * A cached file is a serialized representation of the DOM structure of a
	 * particular mapping. It is saved from a previous call as a file with the
	 * name {@code xmlFile + ".bin"} where {@code xmlFile} is the name of the
	 * original mapping file.
	 * </p>
	 * If a cached {@code xmlFile + ".bin"} exists and is newer than {@code xmlFile},
	 * the {@code ".bin"} file will be read directly. Otherwise, {@code xmlFile} is
	 * read and then serialized to {@code xmlFile + ".bin"} for use the next time.
	 *
	 * @param xmlFile The cacheable mapping file to be added.
	 *
	 * @return this (for method chaining purposes)
	 *
	 * @throws MappingException Indicates problems reading the cached file or
	 * processing the non-cached file.
	 */
	public Configuration addCacheableFile(File xmlFile) throws MappingException {
		metadataSources.addCacheableFile( xmlFile );
		return this;
	}

	/**
	 * <b>INTENDED FOR TESTSUITE USE ONLY!</b>
	 * <p/>
	 * Much like {@link #addCacheableFile(File)} except that here we will fail
	 * immediately if the cache version cannot be found or used for whatever
	 * reason.
	 *
	 * @param xmlFile The xml file, not the bin!
	 *
	 * @return The dom "deserialized" from the cached file.
	 *
	 * @throws SerializationException Indicates a problem deserializing the cached dom tree
	 */
	public Configuration addCacheableFileStrictly(File xmlFile) throws SerializationException {
		metadataSources.addCacheableFileStrictly( xmlFile );
		return this;
	}

	/**
	 * Add a cacheable mapping file.
	 *
	 * @param xmlFile The name of the file to be added, in a form usable to
	 *                simply construct a {@link File} instance
	 *
	 * @return this (for method chaining purposes)
	 *
	 * @throws MappingException Indicates problems reading the cached file or
	 * processing the non-cached file
	 *
	 * @see #addCacheableFile(File)
	 */
	public Configuration addCacheableFile(String xmlFile) throws MappingException {
		metadataSources.addCacheableFile( xmlFile );
		return this;
	}

	/**
	 * Read mappings from a {@code URL}.
	 *
	 * @param url The url for the mapping document to be read.
	 * @return this (for method chaining purposes)
	 * @throws MappingException Indicates problems reading the URL or processing
	 * the mapping document.
	 */
	public Configuration addURL(URL url) throws MappingException {
		metadataSources.addURL( url );
		return this;
	}

	/**
	 * Read mappings from an {@link InputStream}.
	 *
	 * @param xmlInputStream The input stream containing a DOM.
	 * @return this (for method chaining purposes)
	 * @throws MappingException Indicates problems reading the stream, or
	 * processing the contained mapping document.
	 */
	public Configuration addInputStream(InputStream xmlInputStream) throws MappingException {
		metadataSources.addInputStream( xmlInputStream );
		return this;
	}

	/**
	 * Read mappings as an application resource name, that is, using a
	 * {@linkplain ClassLoader#getResource(String) classpath lookup}, trying
	 * different class loaders in turn.
	 *
	 * @param resourceName The resource name
	 * @return this (for method chaining purposes)
	 * @throws MappingException Indicates problems locating the resource or
	 * processing the contained mapping document.
	 */
	public Configuration addResource(String resourceName) throws MappingException {
		metadataSources.addResource( resourceName );
		return this;
	}

	/**
	 * Read a mapping as an application resource using the convention that a class
	 * named {@code foo.bar.Foo} is mapped by a file {@code foo/bar/Foo.hbm.xml}
	 * which can be resolved as a {@linkplain ClassLoader#getResource(String)
	 * classpath resource}.
	 *
	 * @param persistentClass The mapped class
	 * @return this (for method chaining purposes)
	 * @throws MappingException Indicates problems locating the resource or
	 * processing the contained mapping document.
	 */
	public Configuration addClass(Class persistentClass) throws MappingException {
		metadataSources.addClass( persistentClass );
		return this;
	}

	/**
	 * Read metadata from the annotations associated with this class.
	 *
	 * @param annotatedClass The class containing annotations
	 *
	 * @return this (for method chaining)
	 */
	public Configuration addAnnotatedClass(Class annotatedClass) {
		metadataSources.addAnnotatedClass( annotatedClass );
		return this;
	}

	/**
	 * Read package-level metadata.
	 *
	 * @param packageName java package name
	 *
	 * @return this (for method chaining)
	 *
	 * @throws MappingException in case there is an error in the mapping data
	 */
	public Configuration addPackage(String packageName) throws MappingException {
		metadataSources.addPackage( packageName );
		return this;
	}

	/**
	 * Read all {@code .hbm.xml} mappings from a {@code .jar} file.
	 * <p/>
	 * Assumes that any file named {@code *.hbm.xml} is a mapping document.
	 * This method does not support {@code orm.xml} files!
	 *
	 * @param jar a jar file
	 * @return this (for method chaining purposes)
	 * @throws MappingException Indicates problems reading the jar file or
	 * processing the contained mapping documents.
	 */
	public Configuration addJar(File jar) throws MappingException {
		metadataSources.addJar( jar );
		return this;
	}

	/**
	 * Read all {@code .hbm.xml} mapping documents from a directory tree.
	 * <p/>
	 * Assumes that any file named {@code *.hbm.xml} is a mapping document.
	 * This method does not support {@code orm.xml} files!
	 *
	 * @param dir The directory
	 * @return this (for method chaining purposes)
	 * @throws MappingException Indicates problems reading the jar file or
	 * processing the contained mapping documents.
	 */
	public Configuration addDirectory(File dir) throws MappingException {
		metadataSources.addDirectory( dir );
		return this;
	}


	// SessionFactory building ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Retrieve the configured {@link Interceptor}.
	 *
	 * @return The current {@link Interceptor}
	 */
	public Interceptor getInterceptor() {
		return interceptor;
	}

	/**
	 * Set the current {@link Interceptor}.
	 *
	 * @param interceptor The {@link Interceptor} to use
	 *
	 * @return this for method chaining
	 */
	public Configuration setInterceptor(Interceptor interceptor) {
		this.interceptor = interceptor;
		return this;
	}

	/**
	 * Retrieve the user-supplied {@link EntityNotFoundDelegate}, or
	 * {@code null} if no delegate has been specified.
	 *
	 * @return The user-supplied delegate
	 */
	public EntityNotFoundDelegate getEntityNotFoundDelegate() {
		return entityNotFoundDelegate;
	}

	/**
	 * Specify a user-supplied {@link EntityNotFoundDelegate} to be
	 * used to handle scenarios where an entity could not be located
	 * by specified id.
	 *
	 * @param entityNotFoundDelegate The delegate to use
	 */
	public void setEntityNotFoundDelegate(EntityNotFoundDelegate entityNotFoundDelegate) {
		this.entityNotFoundDelegate = entityNotFoundDelegate;
	}

	public SessionFactoryObserver getSessionFactoryObserver() {
		return sessionFactoryObserver;
	}

	public void setSessionFactoryObserver(SessionFactoryObserver sessionFactoryObserver) {
		this.sessionFactoryObserver = sessionFactoryObserver;
	}

	public CurrentTenantIdentifierResolver getCurrentTenantIdentifierResolver() {
		return currentTenantIdentifierResolver;
	}

	public void setCurrentTenantIdentifierResolver(CurrentTenantIdentifierResolver currentTenantIdentifierResolver) {
		this.currentTenantIdentifierResolver = currentTenantIdentifierResolver;
	}

	/**
	 * Create a {@link SessionFactory} using the properties and mappings
	 * in this configuration. The {@code SessionFactory} will be immutable,
	 * so changes made to this {@code Configuration} after building the
	 * factory will not affect it.
	 *
	 * @param serviceRegistry The registry of services to be used in creating this session factory.
	 *
	 * @return The newly-built {@link SessionFactory}
	 *
	 * @throws HibernateException usually indicates an invalid configuration or invalid mapping information
	 */
	public SessionFactory buildSessionFactory(ServiceRegistry serviceRegistry) throws HibernateException {
		log.debug( "Building session factory using provided StandardServiceRegistry" );
		final MetadataBuilder metadataBuilder =
				metadataSources.getMetadataBuilder( (StandardServiceRegistry) serviceRegistry );

		if ( implicitNamingStrategy != null ) {
			metadataBuilder.applyImplicitNamingStrategy( implicitNamingStrategy );
		}

		if ( physicalNamingStrategy != null ) {
			metadataBuilder.applyPhysicalNamingStrategy( physicalNamingStrategy );
		}

		if ( sharedCacheMode != null ) {
			metadataBuilder.applySharedCacheMode( sharedCacheMode );
		}

		if ( !typeContributorRegistrations.isEmpty() ) {
			for ( TypeContributor typeContributor : typeContributorRegistrations ) {
				metadataBuilder.applyTypes( typeContributor );
			}
		}

		if ( userTypeRegistrations != null ) {
			userTypeRegistrations.forEach( registration ->  registration.registerType( metadataBuilder ) );
		}

		if ( !basicTypes.isEmpty() ) {
			for ( BasicType<?> basicType : basicTypes ) {
				metadataBuilder.applyBasicType( basicType );
			}
		}

		if ( customFunctionDescriptors != null ) {
			for ( Map.Entry<String, SqmFunctionDescriptor> entry : customFunctionDescriptors.entrySet() ) {
				metadataBuilder.applySqlFunction( entry.getKey(), entry.getValue() );
			}
		}

		if ( auxiliaryDatabaseObjectList != null ) {
			for ( AuxiliaryDatabaseObject auxiliaryDatabaseObject : auxiliaryDatabaseObjectList ) {
				metadataBuilder.applyAuxiliaryDatabaseObject( auxiliaryDatabaseObject );
			}
		}

		if ( attributeConverterDescriptorsByClass != null ) {
			attributeConverterDescriptorsByClass.values().forEach( metadataBuilder::applyAttributeConverter );
		}

		final Metadata metadata = metadataBuilder.build();
		final SessionFactoryBuilder sessionFactoryBuilder = metadata.getSessionFactoryBuilder();

		if ( interceptor != null && interceptor != EmptyInterceptor.INSTANCE ) {
			sessionFactoryBuilder.applyInterceptor( interceptor );
		}

		if ( getSessionFactoryObserver() != null ) {
			sessionFactoryBuilder.addSessionFactoryObservers( getSessionFactoryObserver() );
		}

		if ( getEntityNotFoundDelegate() != null ) {
			sessionFactoryBuilder.applyEntityNotFoundDelegate( getEntityNotFoundDelegate() );
		}

		if ( getCurrentTenantIdentifierResolver() != null ) {
			sessionFactoryBuilder.applyCurrentTenantIdentifierResolver( getCurrentTenantIdentifierResolver() );
		}

		return sessionFactoryBuilder.build();
	}


	/**
	 * Create a {@link SessionFactory} using the properties and mappings
	 * in this configuration. The {@link SessionFactory} will be immutable,
	 * so changes made to this {@link Configuration} after building the
	 * factory will not affect it.
	 *
	 * @return The newly-built {@link SessionFactory}
	 *
	 * @throws HibernateException usually indicates an invalid configuration or invalid mapping information
	 */
	public SessionFactory buildSessionFactory() throws HibernateException {
		log.debug( "Building session factory using internal StandardServiceRegistryBuilder" );
		standardServiceRegistryBuilder.applySettings( properties );
		StandardServiceRegistry serviceRegistry = standardServiceRegistryBuilder.build();
		try {
			return buildSessionFactory( serviceRegistry );
		}
		catch (Throwable t) {
			serviceRegistry.close();
			throw t;
		}
	}

	public Map<String,SqmFunctionDescriptor> getSqlFunctions() {
		return customFunctionDescriptors;
	}

	public void addSqlFunction(String functionName, SqmFunctionDescriptor function) {
		if ( customFunctionDescriptors == null ) {
			customFunctionDescriptors = new HashMap<>();
		}
		customFunctionDescriptors.put( functionName, function );
	}

	public void addAuxiliaryDatabaseObject(AuxiliaryDatabaseObject object) {
		if ( auxiliaryDatabaseObjectList == null ) {
			auxiliaryDatabaseObjectList = new ArrayList<>();
		}
		auxiliaryDatabaseObjectList.add( object );
	}

	/**
	 * Adds an {@link AttributeConverter} to this configuration.
	 *
	 * @param attributeConverterClass The {@code AttributeConverter} class.
	 * @param autoApply Should the AttributeConverter be auto applied to
	 *                  property types as specified by its "entity attribute"
	 *                  parameterized type?
	 */
	public void addAttributeConverter(Class<? extends AttributeConverter> attributeConverterClass, boolean autoApply) {
		addAttributeConverter( new ClassBasedConverterDescriptor( attributeConverterClass, autoApply, classmateContext ) );
	}

	/**
	 * Adds an {@link AttributeConverter} to this configuration.
	 *
	 * @param attributeConverterClass The {@code AttributeConverter} class.
	 */
	public void addAttributeConverter(Class<? extends AttributeConverter<?,?>> attributeConverterClass) {
		addAttributeConverter( new ClassBasedConverterDescriptor( attributeConverterClass, classmateContext ) );
	}

	/**
	 * Adds an {@link AttributeConverter} instance to this configuration.
	 * This form is mainly intended for developers to programmatically add
	 * their own {@code AttributeConverter} instance.
	 *
	 * @param attributeConverter The {@code AttributeConverter} instance.
	 */
	public void addAttributeConverter(AttributeConverter<?,?> attributeConverter) {
		addAttributeConverter( new InstanceBasedConverterDescriptor( attributeConverter, classmateContext ) );
	}

	/**
	 * Adds an {@link AttributeConverter} instance to this configuration.
	 * This form is mainly intended for developers to programmatically add
	 * their own {@code AttributeConverter} instance.
	 *
	 * @param attributeConverter The {@code AttributeConverter} instance.
	 * @param autoApply Should the {@code AttributeConverter} be auto applied
	 *                  to property types as specified by its "entity attribute"
	 *                  parameterized type?
	 */
	public void addAttributeConverter(AttributeConverter<?,?> attributeConverter, boolean autoApply) {
		addAttributeConverter( new InstanceBasedConverterDescriptor( attributeConverter, autoApply, classmateContext ) );
	}

	public void addAttributeConverter(ConverterDescriptor converterDescriptor) {
		if ( attributeConverterDescriptorsByClass == null ) {
			attributeConverterDescriptorsByClass = new HashMap<>();
		}
		attributeConverterDescriptorsByClass.put( converterDescriptor.getAttributeConverterClass(), converterDescriptor );
	}

	/**
	 * Sets the {@link SharedCacheMode} to use.
	 * <p>
	 * Note that currently only {@link jakarta.persistence.SharedCacheMode#ALL}
	 * has any effect in terms of {@code hbm.xml} binding.
	 *
	 * @param sharedCacheMode The SharedCacheMode to use
	 */
	public void setSharedCacheMode(SharedCacheMode sharedCacheMode) {
		this.sharedCacheMode = sharedCacheMode;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// todo : decide about these

	public Map getNamedSQLQueries() {
		return namedSqlQueries;
	}

	public Map getSqlResultSetMappings() {
		return sqlResultSetMappings;
	}

	public java.util.Collection<NamedEntityGraphDefinition> getNamedEntityGraphs() {
		return namedEntityGraphMap == null
				? Collections.emptyList()
				: namedEntityGraphMap.values();
	}


	public Map<String, NamedHqlQueryDefinition> getNamedQueries() {
		return namedQueries;
	}

	public Map<String, NamedProcedureCallDefinition> getNamedProcedureCallMap() {
		return namedProcedureCallMap;
	}

	/**
	 * Adds the incoming properties to the internal properties structure, as
	 * long as the internal structure does not already contain an entry for
	 * the given key.
	 *
	 * @param properties The properties to merge
	 *
	 * @return this for method chaining
	 */
	public Configuration mergeProperties(Properties properties) {
		for ( Map.Entry<Object,Object> entry : properties.entrySet() ) {
			if ( this.properties.containsKey( entry.getKey() ) ) {
				continue;
			}
			this.properties.setProperty( (String) entry.getKey(), (String) entry.getValue() );
		}
		return this;
	}
}
