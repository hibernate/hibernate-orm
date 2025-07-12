/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cfg;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import jakarta.persistence.PersistenceUnitTransactionType;
import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.EntityNameResolver;
import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.Interceptor;
import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.model.convert.internal.ConverterDescriptors;
import org.hibernate.boot.spi.ClassmateContext;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.boot.model.NamedEntityGraphDefinition;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.ColumnOrderingStrategy;
import org.hibernate.boot.query.NamedHqlQueryDefinition;
import org.hibernate.boot.query.NamedNativeQueryDefinition;
import org.hibernate.boot.query.NamedProcedureCallDefinition;
import org.hibernate.boot.query.NamedResultSetMappingDescriptor;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.XmlMappingBinderAccess;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.context.spi.TenantSchemaMapper;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.EmptyInterceptor;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.Action;
import org.hibernate.type.BasicType;
import org.hibernate.type.SerializationException;
import org.hibernate.usertype.UserType;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.SharedCacheMode;


/**
 * A convenience API making it easier to bootstrap an instance of Hibernate.
 * <p>
 * An instance of {@code Configuration} may be obtained simply by
 * {@linkplain #Configuration() instantiation}, and may be used to aggregate:
 * <ul>
 * <li>{@linkplain #setProperty(String, String) configuration properties}
 *     from various sources, and
 * <li>entity O/R mappings, defined in either {@linkplain #addAnnotatedClass
 *     annotated classes}, or {@linkplain #addFile XML mapping documents}.
 * </ul>
 * <p>
 * Note that XML mappings may be expressed using either:
 * <ul>
 * <li>the JPA-standard {@code orm.xml} format, or
 * <li>the legacy {@code .hbm.xml} format, which is considered deprecated.
 * </ul>
 * <p>
 * Configuration properties are enumerated by {@link AvailableSettings}.
 * <p>
 * When instantiated, an instance of {@code Configuration} has its properties
 * initially populated from the {@linkplain Environment#getProperties()
 * environment}, including:
 * <ul>
 * <li>JVM {@linkplain System#getProperties() system properties}, and
 * <li>properties specified in {@code hibernate.properties}.
 * </ul>
 * <p>
 * These initial properties may be completely discarded by calling
 * {@link #setProperties(Properties)}, or they may be overridden
 * individually by calling {@link #setProperty(String, String)}.
 * <p>
 * <pre>
 * SessionFactory factory = new Configuration()
 *     // scan classes for mapping annotations
 *     .addAnnotatedClass(Item.class)
 *     .addAnnotatedClass(Bid.class)
 *     .addAnnotatedClass(User.class)
 *     // read package-level annotations of the named package
 *     .addPackage("org.hibernate.auction")
 *     // set a configuration property
 *     .setProperty(AvailableSettings.DATASOURCE,
 *                  "java:comp/env/jdbc/test")
 *     .buildSessionFactory();
 * </pre>
 * <p>
 * In addition, there are convenience methods for adding
 * {@linkplain #addAttributeConverter attribute converters},
 * {@linkplain #registerTypeContributor type contributors},
 * {@linkplain #addEntityNameResolver entity name resolvers},
 * {@linkplain #addSqlFunction SQL function descriptors}, and
 * {@linkplain #addAuxiliaryDatabaseObject auxiliary database objects}, for
 * setting {@linkplain #setImplicitNamingStrategy naming strategies} and a
 * {@linkplain #setCurrentTenantIdentifierResolver tenant id resolver},
 * and more.
 * <p>
 * Finally, an instance of {@link SessionFactoryBuilder} is obtained by
 * calling {@link #buildSessionFactory()}.
 * <p>
 * Ultimately, this class simply delegates to {@link MetadataBuilder} and
 * {@link StandardServiceRegistryBuilder} to actually do the hard work of
 * {@linkplain #buildSessionFactory() building} the {@code SessionFactory}.
 * Programs may directly use the APIs defined under {@link org.hibernate.boot},
 * as an alternative to using an instance of this class.
 *
 * @apiNote The {@link org.hibernate.jpa.HibernatePersistenceConfiguration}
 * is a new alternative to this venerable API, and extends the JPA-standard
 * {@link jakarta.persistence.PersistenceConfiguration}.
 *
 * @author Gavin King
 * @author Steve Ebersole
 *
 * @see SessionFactory
 * @see AvailableSettings
 * @see org.hibernate.boot
 * @see org.hibernate.jpa.HibernatePersistenceConfiguration
 */
public class Configuration {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( Configuration.class );

	private final BootstrapServiceRegistry bootstrapServiceRegistry;
	private final MetadataSources metadataSources;
	final private StandardServiceRegistryBuilder standardServiceRegistryBuilder;
	private final ClassmateContext classmateContext = new ClassmateContext();

	// used during processing mappings
	private ImplicitNamingStrategy implicitNamingStrategy;
	private PhysicalNamingStrategy physicalNamingStrategy;
	private final List<BasicType<?>> basicTypes = new ArrayList<>();
	private List<UserTypeRegistration> userTypeRegistrations;
	private final List<TypeContributor> typeContributorRegistrations = new ArrayList<>();
	private final List<FunctionContributor> functionContributorRegistrations = new ArrayList<>();
	private final Map<String, NamedHqlQueryDefinition<?>> namedQueries = new HashMap<>();
	private final Map<String, NamedNativeQueryDefinition<?>> namedSqlQueries = new HashMap<>();
	private final Map<String, NamedProcedureCallDefinition> namedProcedureCallMap = new HashMap<>();
	private final Map<String, NamedResultSetMappingDescriptor> sqlResultSetMappings = new HashMap<>();
	private final Map<String, NamedEntityGraphDefinition> namedEntityGraphMap = new HashMap<>();

	private Map<String, SqmFunctionDescriptor> customFunctionDescriptors;
	private List<AuxiliaryDatabaseObject> auxiliaryDatabaseObjectList;
	private Map<Class<?>, ConverterDescriptor<?,?>> attributeConverterDescriptorsByClass;
	private List<EntityNameResolver> entityNameResolvers = new ArrayList<>();

	// used to build SF
	private Properties properties = new Properties();
	private Interceptor interceptor = EmptyInterceptor.INSTANCE;
	private EntityNotFoundDelegate entityNotFoundDelegate;
	private SessionFactoryObserver sessionFactoryObserver;
	private StatementInspector statementInspector;
	private CurrentTenantIdentifierResolver<?> currentTenantIdentifierResolver;
	private TenantSchemaMapper<?> tenantSchemaMapper;
	private CustomEntityDirtinessStrategy customEntityDirtinessStrategy;
	private ColumnOrderingStrategy columnOrderingStrategy;
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
		bootstrapServiceRegistry = serviceRegistry;
		metadataSources = new MetadataSources( serviceRegistry, createMappingBinderAccess( serviceRegistry ) );
		standardServiceRegistryBuilder = new StandardServiceRegistryBuilder( bootstrapServiceRegistry );
		properties.putAll( standardServiceRegistryBuilder.getSettings() );
	}

	private XmlMappingBinderAccess createMappingBinderAccess(BootstrapServiceRegistry serviceRegistry) {
		return new XmlMappingBinderAccess(
				serviceRegistry,
				(settingName) -> properties == null ? null : properties.get( settingName )
		);
	}

	/**
	 * Create a new instance, using the given {@link MetadataSources}, and a
	 * {@link BootstrapServiceRegistry} obtained from the {@link MetadataSources}.
	 */
	public Configuration(MetadataSources metadataSources) {
		this.metadataSources = metadataSources;
		bootstrapServiceRegistry = getBootstrapRegistry( metadataSources.getServiceRegistry() );
		standardServiceRegistryBuilder = new StandardServiceRegistryBuilder( bootstrapServiceRegistry );
		properties.putAll( standardServiceRegistryBuilder.getSettings() );
	}

	private static BootstrapServiceRegistry getBootstrapRegistry(ServiceRegistry serviceRegistry) {
		if ( serviceRegistry instanceof BootstrapServiceRegistry bootstrapServiceRegistry ) {
			return bootstrapServiceRegistry;
		}
		else if ( serviceRegistry instanceof StandardServiceRegistry ssr ) {
			return (BootstrapServiceRegistry) ssr.getParentServiceRegistry();
		}

		throw new HibernateException(
				"No ServiceRegistry was passed to Configuration#buildSessionFactory " +
						"and could not determine how to locate BootstrapServiceRegistry " +
						"from Configuration instantiation"
		);
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
	 * @return {@code this} for method chaining
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
		return properties.get( propertyName ) instanceof String property ? property : null;
	}

	/**
	 * Set a property value by name
	 *
	 * @param propertyName The name of the property to set
	 * @param value The new property value
	 *
	 * @return {@code this} for method chaining
	 */
	public Configuration setProperty(String propertyName, String value) {
		properties.setProperty( propertyName, value );
		return this;
	}

	/**
	 * Set a property to a boolean value by name
	 *
	 * @param propertyName The name of the property to set
	 * @param value The new boolean property value
	 *
	 * @return {@code this} for method chaining
	 *
	 * @since 6.5
	 */
	public Configuration setProperty(String propertyName, boolean value) {
		return setProperty( propertyName, Boolean.toString(value) );
	}

	/**
	 * Set a property to a Java class name
	 *
	 * @param propertyName The name of the property to set
	 * @param value The Java class
	 *
	 * @return {@code this} for method chaining
	 *
	 * @since 6.5
	 */
	public Configuration setProperty(String propertyName, Class<?> value) {
		return setProperty( propertyName, value.getName() );
	}

	/**
	 * Set a property to the name of a value of an enumerated type
	 *
	 * @param propertyName The name of the property to set
	 * @param value A value of an enumerated type
	 *
	 * @return {@code this} for method chaining
	 *
	 * @since 6.5
	 */
	public Configuration setProperty(String propertyName, Enum<?> value) {
		return setProperty( propertyName, value.name() );
	}

	/**
	 * Set a property to an integer value by name
	 *
	 * @param propertyName The name of the property to set
	 * @param value The new integer property value
	 *
	 * @return {@code this} for method chaining
	 *
	 * @since 6.5
	 */
	public Configuration setProperty(String propertyName, int value) {
		return setProperty( propertyName, Integer.toString(value) );
	}

	/**
	 * Add the given properties to ours.
	 *
	 * @param properties The properties to add.
	 *
	 * @return {@code this} for method chaining
	 */
	public Configuration addProperties(Properties properties) {
		this.properties.putAll( properties );
		return this;
	}

	/**
	 * The {@link ImplicitNamingStrategy}, if any, to use in this configuration.
	 */
	public ImplicitNamingStrategy getImplicitNamingStrategy() {
		return implicitNamingStrategy;
	}

	/**
	 * Set an {@link ImplicitNamingStrategy} to use in this configuration.
	 *
	 * @return {@code this} for method chaining
	 */
	public Configuration setImplicitNamingStrategy(ImplicitNamingStrategy implicitNamingStrategy) {
		this.implicitNamingStrategy = implicitNamingStrategy;
		return this;
	}

	/**
	 * The {@link PhysicalNamingStrategy}, if any, to use in this configuration.
	 */
	public PhysicalNamingStrategy getPhysicalNamingStrategy() {
		return physicalNamingStrategy;
	}

	/**
	 * Set a {@link PhysicalNamingStrategy} to use in this configuration.
	 *
	 * @return {@code this} for method chaining
	 */
	public Configuration setPhysicalNamingStrategy(PhysicalNamingStrategy physicalNamingStrategy) {
		this.physicalNamingStrategy = physicalNamingStrategy;
		return this;
	}

	/**
	 * Use the mappings and properties specified in an application resource named
	 * {@code hibernate.cfg.xml}.
	 *
	 * @return {@code this} for method chaining
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
	 * @return {@code this} for method chaining
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
	@Internal
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
	 * @return {@code this} for method chaining
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
	 * @return {@code this} for method chaining
	 *
	 * @throws HibernateException Generally indicates a problem access the file
	 */
	public Configuration configure(File configFile) throws HibernateException {
		standardServiceRegistryBuilder.configure( configFile );
		properties.putAll( standardServiceRegistryBuilder.getSettings() );
		return this;
	}

	// New typed property setters ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Set {@value AvailableSettings#SHOW_SQL}, {@value AvailableSettings#FORMAT_SQL},
	 * and {@value AvailableSettings#HIGHLIGHT_SQL}.
	 *
	 * @param showSql should SQL be logged to console?
	 * @param formatSql should logged SQL be formatted
	 * @param highlightSql should logged SQL be highlighted with pretty colors
	 */
	public Configuration showSql(boolean showSql, boolean formatSql, boolean highlightSql) {
		setProperty( AvailableSettings.SHOW_SQL, Boolean.toString(showSql) );
		setProperty( AvailableSettings.FORMAT_SQL, Boolean.toString(formatSql) );
		setProperty( AvailableSettings.HIGHLIGHT_SQL, Boolean.toString(highlightSql) );
		return this;
	}

	/**
	 * Set {@value AvailableSettings#HBM2DDL_AUTO}.
	 *
	 * @param action the {@link Action}
	 */
	public Configuration setSchemaExportAction(Action action) {
		setProperty( AvailableSettings.HBM2DDL_AUTO, action.getExternalHbm2ddlName() );
		return this;
	}

	/**
	 * Set {@value AvailableSettings#USER} and {@value AvailableSettings#PASS}.
	 *
	 * @param user the user id
	 * @param pass the password
	 */
	public Configuration setCredentials(String user, String pass) {
		setProperty( AvailableSettings.USER, user );
		setProperty( AvailableSettings.PASS, pass );
		return this;
	}

	/**
	 * Set {@value AvailableSettings#URL}.
	 *
	 * @param url the JDBC URL
	 */
	public Configuration setJdbcUrl(String url) {
		setProperty( AvailableSettings.URL, url );
		return this;
	}

	/**
	 * Set {@value AvailableSettings#DATASOURCE}.
	 *
	 * @param jndiName the JNDI name of the datasource
	 */
	public Configuration setDatasource(String jndiName) {
		setProperty( AvailableSettings.DATASOURCE, jndiName );
		return this;
	}

	/**
	 * Set {@value AvailableSettings#JAKARTA_TRANSACTION_TYPE}.
	 *
	 * @param transactionType the {@link PersistenceUnitTransactionType}
	 */
	public Configuration setTransactionType(PersistenceUnitTransactionType transactionType) {
		setProperty( AvailableSettings.JAKARTA_TRANSACTION_TYPE, transactionType.toString() );
		return this;
	}

	// MetadataSources ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Add a {@link TypeContributor} to this configuration.
	 *
	 * @return {@code this} for method chaining
	 */
	public Configuration registerTypeContributor(TypeContributor typeContributor) {
		typeContributorRegistrations.add( typeContributor );
		return this;
	}

	/**
	 * Add a {@link FunctionContributor} to this configuration.
	 *
	 * @return {@code this} for method chaining
	 */
	public Configuration registerFunctionContributor(FunctionContributor functionContributor) {
		functionContributorRegistrations.add( functionContributor );
		return this;
	}

	/**
	 * Register a {@linkplain BasicType type} into the type registry,
	 * potentially replacing a previously registered type.
	 *
	 * @param type The type to register.
	 *
	 * @return {@code this} for method chaining
	 */
	public Configuration registerTypeOverride(BasicType<?> type) {
		basicTypes.add( type );
		return this;
	}

	private interface UserTypeRegistration {
		void registerType(MetadataBuilder metadataBuilder);
	}

	/**
	 * Register a {@linkplain UserType type} into the type registry,
	 * potentially replacing a previously registered type.
	 *
	 * @param type The type to register.
	 * @param keys The keys under which to register the type.
	 *
	 * @return {@code this} for method chaining
	 */
	public Configuration registerTypeOverride(UserType<?> type, String[] keys) {
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
	 *
	 * @return {@code this} for method chaining
	 *
	 * @throws MappingException Indicates inability to locate or parse
	 * the specified mapping file.
	 *
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
	 * @return {@code this} for method chaining
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
	 * @return {@code this} for method chaining
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
	 * @return {@code this} for method chaining
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
	 * <p>
	 * Much like {@link #addCacheableFile(File)} except that here we will fail
	 * immediately if the cache version cannot be found or used for whatever
	 * reason.
	 *
	 * @param xmlFile The xml file, not the bin!
	 *
	 * @return {@code this} for method chaining
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
	 * @return {@code this} for method chaining
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
	 *
	 * @return {@code this} for method chaining
	 *
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
	 *
	 * @return {@code this} for method chaining
	 *
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
	 *
	 * @return {@code this} for method chaining
	 *
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
	 * @param entityClass The mapped class
	 *
	 * @return {@code this} for method chaining
	 *
	 * @throws MappingException Indicates problems locating the resource or
	 * processing the contained mapping document.
	 */
	public Configuration addClass(Class<?> entityClass) throws MappingException {
		if ( entityClass == null ) {
			throw new IllegalArgumentException( "The specified class cannot be null" );
		}
		return addResource( entityClass.getName().replace( '.', '/' ) + ".hbm.xml" );
	}

	/**
	 * Read metadata from the annotations associated with this class.
	 *
	 * @param annotatedClass The class containing annotations
	 *
	 * @return {@code this} for method chaining
	 */
	public Configuration addAnnotatedClass(Class<?> annotatedClass) {
		metadataSources.addAnnotatedClass( annotatedClass );
		return this;
	}

	/**
	 * Read metadata from the annotations associated with the given classes.
	 *
	 * @param annotatedClasses The classes containing annotations
	 *
	 * @return this (for method chaining)
	 */
	public Configuration addAnnotatedClasses(Class... annotatedClasses) {
		for (Class annotatedClass : annotatedClasses) {
			addAnnotatedClass( annotatedClass );
		}
		return this;
	}

	/**
	 * Read package-level metadata.
	 *
	 * @param packageName java package name
	 *
	 * @return {@code this} for method chaining
	 *
	 * @throws MappingException in case there is an error in the mapping data
	 */
	public Configuration addPackage(String packageName) throws MappingException {
		metadataSources.addPackage( packageName );
		return this;
	}

	/**
	 * Read package-level metadata.
	 *
	 * @param packageNames java package names
	 *
	 * @return this (for method chaining)
	 *
	 * @throws MappingException in case there is an error in the mapping data
	 */
	public Configuration addPackages(String... packageNames) throws MappingException {
		for (String packageName : packageNames) {
			addPackage( packageName );
		}
		return this;
	}

	/**
	 * Read all {@code .hbm.xml} mappings from a {@code .jar} file.
	 * <p>
	 * Assumes that any file named {@code *.hbm.xml} is a mapping document.
	 * This method does not support {@code orm.xml} files!
	 *
	 * @param jar a jar file
	 *
	 * @return {@code this} for method chaining
	 *
	 * @throws MappingException Indicates problems reading the jar file or
	 * processing the contained mapping documents.
	 */
	public Configuration addJar(File jar) throws MappingException {
		metadataSources.addJar( jar );
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
	 * @return {@code this} for method chaining
	 *
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
	 * @return {@code this} for method chaining
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
	 *
	 * @return {@code this} for method chaining
	 */
	public Configuration setEntityNotFoundDelegate(EntityNotFoundDelegate entityNotFoundDelegate) {
		this.entityNotFoundDelegate = entityNotFoundDelegate;
		return this;
	}

	/**
	 * The {@link SessionFactoryObserver}, if any, that was added to this configuration.
	 */
	public SessionFactoryObserver getSessionFactoryObserver() {
		return sessionFactoryObserver;
	}

	/**
	 * Specify a {@link SessionFactoryObserver} to be added to this configuration.
	 *
	 * @return {@code this} for method chaining
	 */
	public Configuration setSessionFactoryObserver(SessionFactoryObserver sessionFactoryObserver) {
		this.sessionFactoryObserver = sessionFactoryObserver;
		return this;
	}

	/**
	 * The {@link StatementInspector}, if any, that was added to this configuration.
	 */
	public StatementInspector getStatementInspector() {
		return statementInspector;
	}

	/**
	 * Specify a {@link StatementInspector} to be added to this configuration.
	 *
	 * @return {@code this} for method chaining
	 */
	public Configuration setStatementInspector(StatementInspector statementInspector) {
		this.statementInspector = statementInspector;
		return this;
	}

	/**
	 * The {@link CurrentTenantIdentifierResolver}, if any, that was added to this configuration.
	 */
	public CurrentTenantIdentifierResolver<?> getCurrentTenantIdentifierResolver() {
		return currentTenantIdentifierResolver;
	}

	/**
	 * Specify a {@link CurrentTenantIdentifierResolver} to be added to this configuration.
	 *
	 * @return {@code this} for method chaining
	 */
	public Configuration setCurrentTenantIdentifierResolver(CurrentTenantIdentifierResolver<?> currentTenantIdentifierResolver) {
		this.currentTenantIdentifierResolver = currentTenantIdentifierResolver;
		return this;
	}

	/**
	 * The {@link TenantSchemaMapper}, if any, that was added to this configuration.
	 *
	 * @since 7.1
	 */
	public TenantSchemaMapper<?> getTenantSchemaMapper() {
		return tenantSchemaMapper;
	}

	/**
	 * Specify a {@link TenantSchemaMapper} to be added to this configuration.
	 *
	 * @return {@code this} for method chaining
	 *
	 * @since 7.1
	 */
	public Configuration setTenantSchemaMapper(TenantSchemaMapper<?> tenantSchemaMapper) {
		this.tenantSchemaMapper = tenantSchemaMapper;
		return this;
	}

	/**
	 * The {@link CustomEntityDirtinessStrategy}, if any, that was added to this configuration.
	 */
	public CustomEntityDirtinessStrategy getCustomEntityDirtinessStrategy() {
		return customEntityDirtinessStrategy;
	}

	/**
	 * Specify a {@link CustomEntityDirtinessStrategy} to be added to this configuration.
	 *
	 * @return {@code this} for method chaining
	 */
	public Configuration setCustomEntityDirtinessStrategy(CustomEntityDirtinessStrategy customEntityDirtinessStrategy) {
		this.customEntityDirtinessStrategy = customEntityDirtinessStrategy;
		return this;
	}

	/**
	 * The {@link CustomEntityDirtinessStrategy}, if any, that was added to this configuration.
	 */
	@Incubating
	public ColumnOrderingStrategy getColumnOrderingStrategy() {
		return columnOrderingStrategy;
	}

	/**
	 * Specify a {@link CustomEntityDirtinessStrategy} to be added to this configuration.
	 *
	 * @return {@code this} for method chaining
	 */
	@Incubating
	public Configuration setColumnOrderingStrategy(ColumnOrderingStrategy columnOrderingStrategy) {
		this.columnOrderingStrategy = columnOrderingStrategy;
		return this;
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
		log.trace( "Building session factory using provided StandardServiceRegistry" );
		final MetadataBuilder metadataBuilder =
				metadataSources.getMetadataBuilder( (StandardServiceRegistry) serviceRegistry );

		if ( implicitNamingStrategy != null ) {
			metadataBuilder.applyImplicitNamingStrategy( implicitNamingStrategy );
		}

		if ( physicalNamingStrategy != null ) {
			metadataBuilder.applyPhysicalNamingStrategy( physicalNamingStrategy );
		}

		if ( columnOrderingStrategy != null ) {
			metadataBuilder.applyColumnOrderingStrategy( columnOrderingStrategy );
		}

		if ( sharedCacheMode != null ) {
			metadataBuilder.applySharedCacheMode( sharedCacheMode );
		}

		for ( TypeContributor typeContributor : typeContributorRegistrations ) {
			metadataBuilder.applyTypes( typeContributor );
		}

		for ( FunctionContributor functionContributor : functionContributorRegistrations ) {
			metadataBuilder.applyFunctions( functionContributor );
		}

		if ( userTypeRegistrations != null ) {
			userTypeRegistrations.forEach( registration ->  registration.registerType( metadataBuilder ) );
		}

		for ( BasicType<?> basicType : basicTypes ) {
			metadataBuilder.applyBasicType( basicType );
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
			attributeConverterDescriptorsByClass.values()
					.forEach( metadataBuilder::applyAttributeConverter );
		}

		final Metadata metadata = metadataBuilder.build();
		final SessionFactoryBuilder sessionFactoryBuilder = metadata.getSessionFactoryBuilder();

		if ( interceptor != null && interceptor != EmptyInterceptor.INSTANCE ) {
			sessionFactoryBuilder.applyInterceptor( interceptor );
		}

		if ( entityNameResolvers != null ) {
			sessionFactoryBuilder.addEntityNameResolver( entityNameResolvers.toArray(new EntityNameResolver[0]) );
		}

		if ( sessionFactoryObserver != null ) {
			sessionFactoryBuilder.addSessionFactoryObservers( sessionFactoryObserver );
		}

		if ( statementInspector != null ) {
			sessionFactoryBuilder.applyStatementInspector( statementInspector );
		}

		if ( entityNotFoundDelegate != null ) {
			sessionFactoryBuilder.applyEntityNotFoundDelegate( entityNotFoundDelegate );
		}

		if ( currentTenantIdentifierResolver != null ) {
			sessionFactoryBuilder.applyCurrentTenantIdentifierResolver( currentTenantIdentifierResolver );
		}

		if ( tenantSchemaMapper != null ) {
			sessionFactoryBuilder.applyTenantSchemaMapper( tenantSchemaMapper );
		}

		if ( customEntityDirtinessStrategy != null ) {
			sessionFactoryBuilder.applyCustomEntityDirtinessStrategy( customEntityDirtinessStrategy );
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
		log.trace( "Building session factory using internal StandardServiceRegistryBuilder" );
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

	/**
	 * Adds a {@linkplain SqmFunctionDescriptor function descriptor} to
	 * this configuration.
	 *
	 * @apiNote For historical reasons, this method is misnamed.
	 *          The function descriptor actually describes a function
	 *          available in HQL, and it may or may not map directly
	 *          to a function defined in SQL.
	 *
	 * @return {@code this} for method chaining
	 *
	 * @see SqmFunctionDescriptor
	 */
	public Configuration addSqlFunction(String functionName, SqmFunctionDescriptor function) {
		if ( customFunctionDescriptors == null ) {
			customFunctionDescriptors = new HashMap<>();
		}
		customFunctionDescriptors.put( functionName, function );
		return this;
	}

	/**
	 * Adds an {@link AuxiliaryDatabaseObject} to this configuration.
	 *
	 * @return {@code this} for method chaining
	 */
	public Configuration addAuxiliaryDatabaseObject(AuxiliaryDatabaseObject object) {
		if ( auxiliaryDatabaseObjectList == null ) {
			auxiliaryDatabaseObjectList = new ArrayList<>();
		}
		auxiliaryDatabaseObjectList.add( object );
		return this;
	}

	/**
	 * Adds an {@link AttributeConverter} to this configuration.
	 *
	 * @param attributeConverterClass The {@code AttributeConverter} class.
	 * @param autoApply Should the {@code AttributeConverter} be auto applied to
	 *                  property types as specified by its "entity attribute"
	 *                  parameterized type?
	 *
	 * @return {@code this} for method chaining
	 */
	public Configuration addAttributeConverter(Class<? extends AttributeConverter<?,?>> attributeConverterClass, boolean autoApply) {
		addAttributeConverter( ConverterDescriptors.of( attributeConverterClass, autoApply, false, classmateContext ) );
		return this;
	}

	/**
	 * Adds an {@link AttributeConverter} to this configuration.
	 *
	 * @param attributeConverterClass The {@code AttributeConverter} class.
	 *
	 * @return {@code this} for method chaining
	 */
	public Configuration addAttributeConverter(Class<? extends AttributeConverter<?, ?>> attributeConverterClass) {
		addAttributeConverter( ConverterDescriptors.of( attributeConverterClass, classmateContext ) );
		return this;
	}

	/**
	 * Adds an {@link AttributeConverter} instance to this configuration.
	 * This form is mainly intended for developers to programmatically add
	 * their own {@code AttributeConverter} instance.
	 *
	 * @param attributeConverter The {@code AttributeConverter} instance.
	 *
	 * @return {@code this} for method chaining
	 */
	public Configuration addAttributeConverter(AttributeConverter<?,?> attributeConverter) {
		addAttributeConverter( ConverterDescriptors.of( attributeConverter, classmateContext ) );
		return this;
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
	 *
	 * @return {@code this} for method chaining
	 */
	public Configuration addAttributeConverter(AttributeConverter<?,?> attributeConverter, boolean autoApply) {
		addAttributeConverter( ConverterDescriptors.of( attributeConverter, autoApply, classmateContext ) );
		return this;
	}

	/**
	 * Adds an {@link ConverterDescriptor} instance to this configuration.
	 *
	 * @param converterDescriptor The {@code ConverterDescriptor} instance.
	 *
	 * @return {@code this} for method chaining
	 */
	public Configuration addAttributeConverter(ConverterDescriptor<?,?> converterDescriptor) {
		if ( attributeConverterDescriptorsByClass == null ) {
			attributeConverterDescriptorsByClass = new HashMap<>();
		}
		attributeConverterDescriptorsByClass.put( converterDescriptor.getAttributeConverterClass(), converterDescriptor );
		return this;
	}

	/**
	 * Add an {@link EntityNameResolver} to this configuration.
	 *
	 * @param entityNameResolver The {@code EntityNameResolver} instance.
	 *
	 * @return {@code this} for method chaining
	 *
	 * @since 6.2
	 */
	public Configuration addEntityNameResolver(EntityNameResolver entityNameResolver) {
		if ( entityNameResolvers == null ) {
			entityNameResolvers = new ArrayList<>();
		}
		entityNameResolvers.add( entityNameResolver );
		return this;
	}

	/**
	 * Sets the {@link SharedCacheMode} to use.
	 * <p>
	 * Note that currently only {@link jakarta.persistence.SharedCacheMode#ALL}
	 * has any effect on {@code hbm.xml} binding.
	 *
	 * @param sharedCacheMode The SharedCacheMode to use
	 *
	 * @return {@code this} for method chaining
	 */
	public Configuration setSharedCacheMode(SharedCacheMode sharedCacheMode) {
		this.sharedCacheMode = sharedCacheMode;
		return this;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// todo : decide about these

	public Map<String, NamedNativeQueryDefinition<?>> getNamedSQLQueries() {
		return namedSqlQueries;
	}

	public Map<String, NamedResultSetMappingDescriptor> getSqlResultSetMappings() {
		return sqlResultSetMappings;
	}

	public java.util.Collection<NamedEntityGraphDefinition> getNamedEntityGraphs() {
		return namedEntityGraphMap.values();
	}


	public Map<String, NamedHqlQueryDefinition<?>> getNamedQueries() {
		return namedQueries;
	}

	public Map<String, NamedProcedureCallDefinition> getNamedProcedureCallMap() {
		return namedProcedureCallMap;
	}

	/**
	 * Adds the incoming properties to the internal properties structure,
	 * as long as the internal structure does <em>not</em> already contain
	 * an entry for the given key. If a given property is already set in
	 * this {@code Configuration}, ignore the setting specified in the
	 * argument {@link Properties} object.
	 *
	 * @apiNote You're probably looking for {@link #addProperties(Properties)}.
	 *
	 * @param properties The properties to merge
	 *
	 * @return {@code this} for method chaining
	 */
	public Configuration mergeProperties(Properties properties) {
		for ( String property : properties.stringPropertyNames() ) {
			if ( !this.properties.containsKey( property ) ) {
				this.properties.setProperty( property, properties.getProperty( property ) );
			}
		}
		return this;
	}
}
