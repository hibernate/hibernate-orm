/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;

import org.hibernate.loader.BatchFetchStyle;

/**
 * @author Steve Ebersole
 */
public interface AvailableSettings {
	/**
	 * Setting used to name the Hibernate {@link org.hibernate.SessionFactory}.
	 *
	 * Naming the SessionFactory allows for it to be properly serialized across JVMs as
	 * long as the same name is used on each JVM.
	 *
	 * If {@link #SESSION_FACTORY_NAME_IS_JNDI} is set to {@code true}, this is also the
	 * name under which the SessionFactory is bound into JNDI on startup and from which
	 * it can be obtained from JNDI.
	 *
	 * @see #SESSION_FACTORY_NAME_IS_JNDI
	 * @see org.hibernate.internal.SessionFactoryRegistry
	 */
	 String SESSION_FACTORY_NAME = "hibernate.session_factory_name";

	/**
	 * Does the value defined by {@link #SESSION_FACTORY_NAME} represent a JNDI namespace into which
	 * the {@link org.hibernate.SessionFactory} should be bound and made accessible?
	 *
	 * Defaults to {@code true} for backwards compatibility.
	 *
	 * Set this to {@code false} if naming a SessionFactory is needed for serialization purposes, but
	 * no writable JNDI context exists in the runtime environment or if the user simply does not want
	 * JNDI to be used.
	 *
	 * @see #SESSION_FACTORY_NAME
	 */
	String SESSION_FACTORY_NAME_IS_JNDI = "hibernate.session_factory_name_is_jndi";

	/**
	 * Names the {@link org.hibernate.engine.jdbc.connections.spi.ConnectionProvider} to use for obtaining
	 * JDBC connections.  Can either reference an instance of
	 * {@link org.hibernate.engine.jdbc.connections.spi.ConnectionProvider} or a {@link Class} or {@link String}
	 * reference to the {@link org.hibernate.engine.jdbc.connections.spi.ConnectionProvider} implementation
	 * class.
	 */
	String CONNECTION_PROVIDER ="hibernate.connection.provider_class";

	/**
	 * Names the {@literal JDBC} driver class
	 */
	String DRIVER ="hibernate.connection.driver_class";

	/**
	 * Names the {@literal JDBC} connection url.
	 */
	String URL ="hibernate.connection.url";

	/**
	 * Names the connection user.  This might mean one of 2 things in out-of-the-box Hibernate
	 * {@link org.hibernate.engine.jdbc.connections.spi.ConnectionProvider}: <ul>
	 *     <li>The username used to pass along to creating the JDBC connection</li>
	 *     <li>The username used to obtain a JDBC connection from a data source</li>
	 * </ul>
	 */
	String USER ="hibernate.connection.username";

	/**
	 * Names the connection password.  See usage discussion on {@link #USER}
	 */
	String PASS ="hibernate.connection.password";

	/**
	 * Names the {@literal JDBC} transaction isolation level
	 */
	String ISOLATION ="hibernate.connection.isolation";

	/**
	 * Names the {@literal JDBC} autocommit mode
	 */
	String AUTOCOMMIT ="hibernate.connection.autocommit";

	/**
	 * Maximum number of inactive connections for the built-in Hibernate connection pool.
	 */
	String POOL_SIZE ="hibernate.connection.pool_size";

	/**
	 * Names a {@link javax.sql.DataSource}.  Can either reference a {@link javax.sql.DataSource} instance or
	 * a {@literal JNDI} name under which to locate the {@link javax.sql.DataSource}.
	 */
	String DATASOURCE ="hibernate.connection.datasource";

	/**
	 * Names a prefix used to define arbitrary JDBC connection properties.  These properties are passed along to
	 * the {@literal JDBC} provider when creating a connection.
	 */
	String CONNECTION_PREFIX = "hibernate.connection";

	/**
	 * Names the {@literal JNDI} {@link javax.naming.InitialContext} class.
	 *
	 * @see javax.naming.Context#INITIAL_CONTEXT_FACTORY
	 */
	String JNDI_CLASS ="hibernate.jndi.class";

	/**
	 * Names the {@literal JNDI} provider/connection url
	 *
	 * @see javax.naming.Context#PROVIDER_URL
	 */
	String JNDI_URL ="hibernate.jndi.url";

	/**
	 * Names a prefix used to define arbitrary {@literal JNDI} {@link javax.naming.InitialContext} properties.  These
	 * properties are passed along to {@link javax.naming.InitialContext#InitialContext(java.util.Hashtable)}
	 */
	String JNDI_PREFIX = "hibernate.jndi";

	/**
	 * Names the Hibernate {@literal SQL} {@link org.hibernate.dialect.Dialect} class
	 */
	String DIALECT ="hibernate.dialect";

	/**
	 * Names any additional {@link org.hibernate.engine.jdbc.dialect.spi.DialectResolver} implementations to
	 * register with the standard {@link org.hibernate.engine.jdbc.dialect.spi.DialectFactory}.
	 */
	String DIALECT_RESOLVERS = "hibernate.dialect_resolvers";


	/**
	 * A default database schema (owner) name to use for unqualified tablenames
	 */
	String DEFAULT_SCHEMA = "hibernate.default_schema";
	/**
	 * A default database catalog name to use for unqualified tablenames
	 */
	String DEFAULT_CATALOG = "hibernate.default_catalog";

	/**
	 * Enable logging of generated SQL to the console
	 */
	String SHOW_SQL ="hibernate.show_sql";
	/**
	 * Enable formatting of SQL logged to the console
	 */
	String FORMAT_SQL ="hibernate.format_sql";
	/**
	 * Add comments to the generated SQL
	 */
	String USE_SQL_COMMENTS ="hibernate.use_sql_comments";
	/**
	 * Maximum depth of outer join fetching
	 */
	String MAX_FETCH_DEPTH = "hibernate.max_fetch_depth";
	/**
	 * The default batch size for batch fetching
	 */
	String DEFAULT_BATCH_FETCH_SIZE = "hibernate.default_batch_fetch_size";
	/**
	 * Use <tt>java.io</tt> streams to read / write binary data from / to JDBC
	 */
	String USE_STREAMS_FOR_BINARY = "hibernate.jdbc.use_streams_for_binary";
	/**
	 * Use JDBC scrollable <tt>ResultSet</tt>s. This property is only necessary when there is
	 * no <tt>ConnectionProvider</tt>, ie. the user is supplying JDBC connections.
	 */
	String USE_SCROLLABLE_RESULTSET = "hibernate.jdbc.use_scrollable_resultset";
	/**
	 * Tells the JDBC driver to attempt to retrieve row Id with the JDBC 3.0 PreparedStatement.getGeneratedKeys()
	 * method. In general, performance will be better if this property is set to true and the underlying
	 * JDBC driver supports getGeneratedKeys().
	 */
	String USE_GET_GENERATED_KEYS = "hibernate.jdbc.use_get_generated_keys";
	/**
	 * Gives the JDBC driver a hint as to the number of rows that should be fetched from the database
	 * when more rows are needed. If <tt>0</tt>, JDBC driver default settings will be used.
	 */
	String STATEMENT_FETCH_SIZE = "hibernate.jdbc.fetch_size";
	/**
	 * Maximum JDBC batch size. A nonzero value enables batch updates.
	 */
	String STATEMENT_BATCH_SIZE = "hibernate.jdbc.batch_size";
	/**
	 * Select a custom batcher.
	 */
	String BATCH_STRATEGY = "hibernate.jdbc.factory_class";
	/**
	 * Should versioned data be included in batching?
	 */
	String BATCH_VERSIONED_DATA = "hibernate.jdbc.batch_versioned_data";
	/**
	 * An XSLT resource used to generate "custom" XML
	 */
	String OUTPUT_STYLESHEET ="hibernate.xml.output_stylesheet";

	/**
	 * Maximum size of C3P0 connection pool
	 */
	String C3P0_MAX_SIZE = "hibernate.c3p0.max_size";
	/**
	 * Minimum size of C3P0 connection pool
	 */
	String C3P0_MIN_SIZE = "hibernate.c3p0.min_size";

	/**
	 * Maximum idle time for C3P0 connection pool
	 */
	String C3P0_TIMEOUT = "hibernate.c3p0.timeout";
	/**
	 * Maximum size of C3P0 statement cache
	 */
	String C3P0_MAX_STATEMENTS = "hibernate.c3p0.max_statements";
	/**
	 * Number of connections acquired when pool is exhausted
	 */
	String C3P0_ACQUIRE_INCREMENT = "hibernate.c3p0.acquire_increment";
	/**
	 * Idle time before a C3P0 pooled connection is validated
	 */
	String C3P0_IDLE_TEST_PERIOD = "hibernate.c3p0.idle_test_period";

	/**
	 * Proxool/Hibernate property prefix
	 * @deprecated Use {@link #PROXOOL_CONFIG_PREFIX} instead
	 */
	String PROXOOL_PREFIX = "hibernate.proxool";
	/**
	 * Proxool property to configure the Proxool Provider using an XML (<tt>/path/to/file.xml</tt>)
	 */
	String PROXOOL_XML = "hibernate.proxool.xml";
	/**
	 * Proxool property to configure the Proxool Provider  using a properties file (<tt>/path/to/proxool.properties</tt>)
	 */
	String PROXOOL_PROPERTIES = "hibernate.proxool.properties";
	/**
	 * Proxool property to configure the Proxool Provider from an already existing pool (<tt>true</tt> / <tt>false</tt>)
	 */
	String PROXOOL_EXISTING_POOL = "hibernate.proxool.existing_pool";
	/**
	 * Proxool property with the Proxool pool alias to use
	 * (Required for <tt>PROXOOL_EXISTING_POOL</tt>, <tt>PROXOOL_PROPERTIES</tt>, or
	 * <tt>PROXOOL_XML</tt>)
	 */
	String PROXOOL_POOL_ALIAS = "hibernate.proxool.pool_alias";

	/**
	 * Enable automatic session close at end of transaction
	 */
	String AUTO_CLOSE_SESSION = "hibernate.transaction.auto_close_session";
	/**
	 * Enable automatic flush during the JTA <tt>beforeCompletion()</tt> callback
	 */
	String FLUSH_BEFORE_COMPLETION = "hibernate.transaction.flush_before_completion";
	/**
	 * Specifies how Hibernate should release JDBC connections.
	 */
	String RELEASE_CONNECTIONS = "hibernate.connection.release_mode";
	/**
	 * Context scoping impl for {@link org.hibernate.SessionFactory#getCurrentSession()} processing.
	 */
	String CURRENT_SESSION_CONTEXT_CLASS = "hibernate.current_session_context_class";

	/**
	 * Names the implementation of {@link org.hibernate.resource.transaction.TransactionCoordinatorBuilder} to use for
	 * creating {@link org.hibernate.resource.transaction.TransactionCoordinator} instances.
	 * <p/>
	 * Can be<ul>
	 *     <li>TransactionCoordinatorBuilder instance</li>
	 *     <li>TransactionCoordinatorBuilder implementation {@link Class} reference</li>
	 *     <li>TransactionCoordinatorBuilder implementation class name (FQN)</li>
	 * </ul>
	 *
	 * @since 5.0
	 */
	String TRANSACTION_COORDINATOR_STRATEGY = "hibernate.transaction.coordinator_class";

	/**
	 * Names the {@link org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform} implementation to use for integrating
	 * with {@literal JTA} systems.  Can reference either a {@link org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform}
	 * instance or the name of the {@link org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform} implementation class
	 *
	 * @since 4.0
	 */
	String JTA_PLATFORM = "hibernate.transaction.jta.platform";

	/**
	 * Used to specify if using {@link javax.transaction.UserTransaction}  class to use for JTA transaction management.
	 *
	 * Default is <code>false</code>
	 *
	 * @since 5.0
	 */
	String PREFER_USER_TRANSACTION = "hibernate.jta.prefer_user_transaction";

	/**
	 * Names the {@link org.hibernate.engine.transaction.jta.platform.spi.JtaPlatformResolver} implementation to use.
	 * @since 4.3
	 */
	String JTA_PLATFORM_RESOLVER = "hibernate.transaction.jta.platform_resolver";

	/**
	 * The {@link org.hibernate.cache.spi.RegionFactory} implementation class
	 */
	String CACHE_REGION_FACTORY = "hibernate.cache.region.factory_class";

	/**
	 * The <tt>CacheProvider</tt> implementation class
	 */
	String CACHE_PROVIDER_CONFIG = "hibernate.cache.provider_configuration_file_resource_path";
	/**
	 * The <tt>CacheProvider</tt> JNDI namespace, if pre-bound to JNDI.
	 */
	String CACHE_NAMESPACE = "hibernate.cache.jndi";
	/**
	 * Enable the query cache (disabled by default)
	 */
	String USE_QUERY_CACHE = "hibernate.cache.use_query_cache";
	/**
	 * The <tt>QueryCacheFactory</tt> implementation class.
	 */
	String QUERY_CACHE_FACTORY = "hibernate.cache.query_cache_factory";
	/**
	 * Enable the second-level cache (enabled by default)
	 */
	String USE_SECOND_LEVEL_CACHE = "hibernate.cache.use_second_level_cache";
	/**
	 * Optimize the cache for minimal puts instead of minimal gets
	 */
	String USE_MINIMAL_PUTS = "hibernate.cache.use_minimal_puts";
	/**
	 * The <tt>CacheProvider</tt> region name prefix
	 */
	String CACHE_REGION_PREFIX = "hibernate.cache.region_prefix";
	/**
	 * Enable use of structured second-level cache entries
	 */
	String USE_STRUCTURED_CACHE = "hibernate.cache.use_structured_entries";
	/**
	 * Enables the automatic eviction of a bi-directional association's collection cache when an element in the
	 * ManyToOne collection is added/updated/removed without properly managing the change on the OneToMany side.
	 */
	String AUTO_EVICT_COLLECTION_CACHE = "hibernate.cache.auto_evict_collection_cache";
	/**
	 * Enable statistics collection
	 */
	String GENERATE_STATISTICS = "hibernate.generate_statistics";

	String USE_IDENTIFIER_ROLLBACK = "hibernate.use_identifier_rollback";

	/**
	 * Use bytecode libraries optimized property access
	 */
	String USE_REFLECTION_OPTIMIZER = "hibernate.bytecode.use_reflection_optimizer";

	/**
	 * The classname of the HQL query parser factory
	 */
	String QUERY_TRANSLATOR = "hibernate.query.factory_class";

	/**
	 * A comma-separated list of token substitutions to use when translating a Hibernate
	 * query to SQL
	 */
	String QUERY_SUBSTITUTIONS = "hibernate.query.substitutions";

	/**
	 * Should named queries be checked during startup (the default is enabled).
	 * <p/>
	 * Mainly intended for test environments.
	 */
	String QUERY_STARTUP_CHECKING = "hibernate.query.startup_check";

	/**
	 * Auto export/update schema using hbm2ddl tool. Valid values are <tt>update</tt>,
	 * <tt>create</tt>, <tt>create-drop</tt> and <tt>validate</tt>.
	 */
	String HBM2DDL_AUTO = "hibernate.hbm2ddl.auto";

	/**
	 * Comma-separated names of the optional files containing SQL DML statements executed
	 * during the SessionFactory creation.
	 * File order matters, the statements of a give file are executed before the statements of the
	 * following files.
	 *
	 * These statements are only executed if the schema is created ie if <tt>hibernate.hbm2ddl.auto</tt>
	 * is set to <tt>create</tt> or <tt>create-drop</tt>.
	 *
	 * The default value is <tt>/import.sql</tt>
	 */
	String HBM2DDL_IMPORT_FILES = "hibernate.hbm2ddl.import_files";

	/**
	 * {@link String} reference to {@link org.hibernate.tool.hbm2ddl.ImportSqlCommandExtractor} implementation class.
	 * Referenced implementation is required to provide non-argument constructor.
	 *
	 * The default value is <tt>org.hibernate.tool.hbm2ddl.SingleLineSqlCommandExtractor</tt>.
	 */
	String HBM2DDL_IMPORT_FILES_SQL_EXTRACTOR = "hibernate.hbm2ddl.import_files_sql_extractor";

	/**
	 * Specifies whether to automatically create also the database schema/catalog.
	 * The default is false.
	 *
	 * @since 5.0
	 */
	String HBM2DLL_CREATE_NAMESPACES = "hibernate.hbm2dll.create_namespaces";

	/**
	 * The {@link org.hibernate.exception.spi.SQLExceptionConverter} to use for converting SQLExceptions
	 * to Hibernate's JDBCException hierarchy.  The default is to use the configured
	 * {@link org.hibernate.dialect.Dialect}'s preferred SQLExceptionConverter.
	 */
	String SQL_EXCEPTION_CONVERTER = "hibernate.jdbc.sql_exception_converter";

	/**
	 * Enable wrapping of JDBC result sets in order to speed up column name lookups for
	 * broken JDBC drivers
	 */
	String WRAP_RESULT_SETS = "hibernate.jdbc.wrap_result_sets";

	/**
	 * Enable ordering of update statements by primary key value
	 */
	String ORDER_UPDATES = "hibernate.order_updates";

	/**
	 * Enable ordering of insert statements for the purpose of more efficient JDBC batching.
	 */
	String ORDER_INSERTS = "hibernate.order_inserts";

	/**
	 * Default precedence of null values in {@code ORDER BY} clause.  Supported options: {@code none} (default),
	 * {@code first}, {@code last}.
	 */
	String DEFAULT_NULL_ORDERING = "hibernate.order_by.default_null_ordering";

	/**
	 * The EntityMode in which set the Session opened from the SessionFactory.
	 */
    String DEFAULT_ENTITY_MODE = "hibernate.default_entity_mode";

	/**
	 * Should all database identifiers be quoted.
	 */
	String GLOBALLY_QUOTED_IDENTIFIERS = "hibernate.globally_quoted_identifiers";

	/**
	 * Enable nullability checking.
	 * Raises an exception if a property marked as not-null is null.
	 * Default to false if Bean Validation is present in the classpath and Hibernate Annotations is used,
	 * true otherwise.
	 */
	String CHECK_NULLABILITY = "hibernate.check_nullability";


	String BYTECODE_PROVIDER = "hibernate.bytecode.provider";

	String JPAQL_STRICT_COMPLIANCE= "hibernate.query.jpaql_strict_compliance";

	/**
	 * When using pooled {@link org.hibernate.id.enhanced.Optimizer optimizers}, prefer interpreting the
	 * database value as the lower (lo) boundary.  The default is to interpret it as the high boundary.
	 */
	String PREFER_POOLED_VALUES_LO = "hibernate.id.optimizer.pooled.prefer_lo";

	/**
	 * The maximum number of strong references maintained by {@link org.hibernate.engine.query.spi.QueryPlanCache}. Default is 128.
	 * @deprecated in favor of {@link #QUERY_PLAN_CACHE_PARAMETER_METADATA_MAX_SIZE}
	 */
	@Deprecated
	String QUERY_PLAN_CACHE_MAX_STRONG_REFERENCES = "hibernate.query.plan_cache_max_strong_references";

	/**
	 * The maximum number of soft references maintained by {@link org.hibernate.engine.query.spi.QueryPlanCache}. Default is 2048.
	 * @deprecated in favor of {@link #QUERY_PLAN_CACHE_MAX_SIZE}
	 */
	@Deprecated
	String QUERY_PLAN_CACHE_MAX_SOFT_REFERENCES = "hibernate.query.plan_cache_max_soft_references";

	/**
	 * The maximum number of entries including:
	 * <ul>
	 *     <li>{@link org.hibernate.engine.query.spi.HQLQueryPlan}</li>
	 *     <li>{@link org.hibernate.engine.query.spi.FilterQueryPlan}</li>
	 *     <li>{@link org.hibernate.engine.query.spi.NativeSQLQueryPlan}</li>
	 * </ul>
	 * 
	 * maintained by {@link org.hibernate.engine.query.spi.QueryPlanCache}. Default is 2048.
	 */
	String QUERY_PLAN_CACHE_MAX_SIZE = "hibernate.query.plan_cache_max_size";

	/**
	 * The maximum number of {@link org.hibernate.engine.query.spi.ParameterMetadata} maintained 
	 * by {@link org.hibernate.engine.query.spi.QueryPlanCache}. Default is 128.
	 */
	String QUERY_PLAN_CACHE_PARAMETER_METADATA_MAX_SIZE = "hibernate.query.plan_parameter_metadata_max_size";

	/**
	 * Should we not use contextual LOB creation (aka based on {@link java.sql.Connection#createBlob()} et al).
	 */
	String NON_CONTEXTUAL_LOB_CREATION = "hibernate.jdbc.lob.non_contextual_creation";

	/**
	 * Used to define a {@link java.util.Collection} of the {@link ClassLoader} instances Hibernate should use for
	 * class-loading and resource-lookups.
	 *
	 * @since 5.0
	 */
	String CLASSLOADERS = "hibernate.classLoaders";

	/**
	 * Names the {@link ClassLoader} used to load user application classes.
	 * @since 4.0
	 *
	 * @deprecated Use {@link #CLASSLOADERS} instead
	 */
	@Deprecated
	String APP_CLASSLOADER = "hibernate.classLoader.application";

	/**
	 * Names the {@link ClassLoader} Hibernate should use to perform resource loading.
	 * @since 4.0
	 * @deprecated Use {@link #CLASSLOADERS} instead
	 */
	@Deprecated
	String RESOURCES_CLASSLOADER = "hibernate.classLoader.resources";

	/**
	 * Names the {@link ClassLoader} responsible for loading Hibernate classes.  By default this is
	 * the {@link ClassLoader} that loaded this class.
	 * @since 4.0
	 * @deprecated Use {@link #CLASSLOADERS} instead
	 */
	@Deprecated
	String HIBERNATE_CLASSLOADER = "hibernate.classLoader.hibernate";

	/**
	 * Names the {@link ClassLoader} used when Hibernate is unable to locates classes on the
	 * {@link #APP_CLASSLOADER} or {@link #HIBERNATE_CLASSLOADER}.
	 * @since 4.0
	 * @deprecated Use {@link #CLASSLOADERS} instead
	 */
	@Deprecated
	String ENVIRONMENT_CLASSLOADER = "hibernate.classLoader.environment";


	String C3P0_CONFIG_PREFIX = "hibernate.c3p0";

	String PROXOOL_CONFIG_PREFIX = "hibernate.proxool";


	String JMX_ENABLED = "hibernate.jmx.enabled";
	String JMX_PLATFORM_SERVER = "hibernate.jmx.usePlatformServer";
	String JMX_AGENT_ID = "hibernate.jmx.agentId";
	String JMX_DOMAIN_NAME = "hibernate.jmx.defaultDomain";
	String JMX_SF_NAME = "hibernate.jmx.sessionFactoryName";
	String JMX_DEFAULT_OBJ_NAME_DOMAIN = "org.hibernate.core";

	/**
	 * A configuration value key used to indicate that it is safe to cache
	 * {@link javax.transaction.TransactionManager} references.
	 * @since 4.0
	 */
	String JTA_CACHE_TM = "hibernate.jta.cacheTransactionManager";

	/**
	 * A configuration value key used to indicate that it is safe to cache
	 * {@link javax.transaction.UserTransaction} references.
	 * @since 4.0
	 */
	String JTA_CACHE_UT = "hibernate.jta.cacheUserTransaction";

	/**
	 * Setting used to give the name of the default {@link org.hibernate.annotations.CacheConcurrencyStrategy}
	 * to use when either {@link javax.persistence.Cacheable @Cacheable} or
	 * {@link org.hibernate.annotations.Cache @Cache} is used.  {@link org.hibernate.annotations.Cache @Cache(strategy="..")} is used to override.
	 */
	String DEFAULT_CACHE_CONCURRENCY_STRATEGY = "hibernate.cache.default_cache_concurrency_strategy";

	/**
	 * Setting which indicates whether or not the new {@link org.hibernate.id.IdentifierGenerator} are used
	 * for AUTO, TABLE and SEQUENCE.
	 * Default to false to keep backward compatibility.
	 */
	String USE_NEW_ID_GENERATOR_MAPPINGS = "hibernate.id.new_generator_mappings";

	/**
	 * Setting to identify a {@link org.hibernate.CustomEntityDirtinessStrategy} to use.  May point to
	 * either a class name or instance.
	 */
	String CUSTOM_ENTITY_DIRTINESS_STRATEGY = "hibernate.entity_dirtiness_strategy";

	/**
	 * Strategy for multi-tenancy.

	 * @see org.hibernate.MultiTenancyStrategy
	 * @since 4.0
	 */
	String MULTI_TENANT = "hibernate.multiTenancy";

	/**
	 * Names a {@link org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider} implementation to
	 * use.  As MultiTenantConnectionProvider is also a service, can be configured directly through the
	 * {@link org.hibernate.boot.registry.StandardServiceRegistryBuilder}
	 *
	 * @since 4.1
	 */
	String MULTI_TENANT_CONNECTION_PROVIDER = "hibernate.multi_tenant_connection_provider";

	/**
	 * Names a {@link org.hibernate.context.spi.CurrentTenantIdentifierResolver} implementation to use.
	 * <p/>
	 * Can be<ul>
	 *     <li>CurrentTenantIdentifierResolver instance</li>
	 *     <li>CurrentTenantIdentifierResolver implementation {@link Class} reference</li>
	 *     <li>CurrentTenantIdentifierResolver implementation class name</li>
	 * </ul>
	 *
	 * @since 4.1
	 */
	String MULTI_TENANT_IDENTIFIER_RESOLVER = "hibernate.tenant_identifier_resolver";

	String FORCE_DISCRIMINATOR_IN_SELECTS_BY_DEFAULT = "hibernate.discriminator.force_in_select";

	/**
	 * The legacy behavior of Hibernate is to not use discriminators for joined inheritance (Hibernate does not need
	 * the discriminator...).  However, some JPA providers do need the discriminator for handling joined inheritance.
	 * In the interest of portability this capability has been added to Hibernate too.
	 * <p/>
	 * However, we want to make sure that legacy applications continue to work as well.  Which puts us in a bind in
	 * terms of how to handle "implicit" discriminator mappings.  The solution is to assume that the absence of
	 * discriminator metadata means to follow the legacy behavior *unless* this setting is enabled.  With this setting
	 * enabled, Hibernate will interpret the absence of discriminator metadata as an indication to use the JPA
	 * defined defaults for these absent annotations.
	 * <p/>
	 * See Hibernate Jira issue HHH-6911 for additional background info.
	 *
	 * @see #IGNORE_EXPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS
	 */
	String IMPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS = "hibernate.discriminator.implicit_for_joined";

	/**
	 * The legacy behavior of Hibernate is to not use discriminators for joined inheritance (Hibernate does not need
	 * the discriminator...).  However, some JPA providers do need the discriminator for handling joined inheritance.
	 * In the interest of portability this capability has been added to Hibernate too.
	 * <p/>
	 * Existing applications rely (implicitly or explicitly) on Hibernate ignoring any DiscriminatorColumn declarations
	 * on joined inheritance hierarchies.  This setting allows these applications to maintain the legacy behavior
	 * of DiscriminatorColumn annotations being ignored when paired with joined inheritance.
	 * <p/>
	 * See Hibernate Jira issue HHH-6911 for additional background info.
	 *
	 * @see #IMPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS
	 */
	String IGNORE_EXPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS = "hibernate.discriminator.ignore_explicit_for_joined";

	/**
	 * Names a {@link org.hibernate.Interceptor} implementation to be applied to the {@link org.hibernate.SessionFactory}
	 * Can reference<ul>
	 *     <li>Interceptor instance</li>
	 *     <li>Interceptor implementation {@link Class} reference</li>
	 *     <li>Interceptor implementation class name</li>
	 * </ul>
	 *
	 * @since 5.0
	 */
	String INTERCEPTOR = "hibernate.session_factory.interceptor";

	/**
	 * Names a {@link org.hibernate.resource.jdbc.spi.StatementInspector} implementation to be applied to
	 * the {@link org.hibernate.SessionFactory}.  Can reference<ul>
	 *     <li>StatementInspector instance</li>
	 *     <li>StatementInspector implementation {@link Class} reference</li>
	 *     <li>StatementInspector implementation class name (FQN)</li>
	 * </ul>
	 *
	 * @since 5.0
	 */
	String STATEMENT_INSPECTOR = "hibernate.session_factory.statement_inspector";

    String ENABLE_LAZY_LOAD_NO_TRANS = "hibernate.enable_lazy_load_no_trans";

	String HQL_BULK_ID_STRATEGY = "hibernate.hql.bulk_id_strategy";

	/**
	 * Names the {@link org.hibernate.loader.BatchFetchStyle} to use.  Can specify either the
	 * {@link org.hibernate.loader.BatchFetchStyle} name (insensitively), or a
	 * {@link org.hibernate.loader.BatchFetchStyle} instance.
	 * 
	 * {@code LEGACY} is the default value.
	 */
	String BATCH_FETCH_STYLE = "hibernate.batch_fetch_style";

	/**
	 * Enable direct storage of entity references into the second level cache when applicable (immutable data, etc).
	 * Default is to not store direct references.
	 */
	String USE_DIRECT_REFERENCE_CACHE_ENTRIES = "hibernate.cache.use_reference_entries";

	/**
	 * Enable nationalized character support on all string / clob based attribute ( string, char, clob, text etc ).
	 *
	 * Default is <clode>false</clode>.
	 */
	String USE_NATIONALIZED_CHARACTER_DATA = "hibernate.use_nationalized_character_data";
	
	/**
	 * A transaction can be rolled back by another thread ("tracking by thread")
	 * -- not the original application. Examples of this include a JTA
	 * transaction timeout handled by a background reaper thread.  The ability
	 * to handle this situation requires checking the Thread ID every time
	 * Session is called.  This can certainly have performance considerations.
	 * 
	 * Default is <code>true</code> (enabled).
	 */
	String JTA_TRACK_BY_THREAD = "hibernate.jta.track_by_thread";

	String JACC_CONTEXT_ID = "hibernate.jacc_context_id";
	String JACC_PREFIX = "hibernate.jacc";
	String JACC_ENABLED = "hibernate.jacc.enabled";

	/**
	 * If enabled, allows schema update and validation to support synonyms.  Due
	 * to the possibility that this would return duplicate tables (especially in
	 * Oracle), this is disabled by default.
	 */
	String ENABLE_SYNONYMS = "hibernate.synonyms";
	
	/**
	 * Unique columns and unique keys both use unique constraints in most dialects.
	 * SchemaUpdate needs to create these constraints, but DB's
	 * support for finding existing constraints is extremely inconsistent. Further,
	 * non-explicitly-named unique constraints use randomly generated characters.
	 * 
	 * Therefore, select from these strategies.
	 * {@link org.hibernate.tool.hbm2ddl.UniqueConstraintSchemaUpdateStrategy#DROP_RECREATE_QUIETLY} (DEFAULT):
	 * 			Attempt to drop, then (re-)create each unique constraint.
	 * 			Ignore any exceptions thrown.
	 * {@link org.hibernate.tool.hbm2ddl.UniqueConstraintSchemaUpdateStrategy#RECREATE_QUIETLY}:
	 * 			attempt to (re-)create unique constraints,
	 * 			ignoring exceptions thrown if the constraint already existed
	 * {@link org.hibernate.tool.hbm2ddl.UniqueConstraintSchemaUpdateStrategy#SKIP}:
	 * 			do not attempt to create unique constraints on a schema update
	 */
	String UNIQUE_CONSTRAINT_SCHEMA_UPDATE_STRATEGY = "hibernate.schema_update.unique_constraint_strategy";

	/**
	 * A setting to control whether to {@link org.hibernate.engine.internal.StatisticalLoggingSessionEventListener} is
	 * enabled on all Sessions (unless explicitly disabled for a given Session).  The default value of this
	 * setting is determined by the value for {@link #GENERATE_STATISTICS}, meaning that if collection of statistics
	 * is enabled logging of Session metrics is enabled by default too.
	 */
	String LOG_SESSION_METRICS = "hibernate.session.events.log";

	/**
	 * Defines a default {@link org.hibernate.SessionEventListener} to be applied to opened Sessions.
	 */
	String AUTO_SESSION_EVENTS_LISTENER = "hibernate.session.events.auto";

	/**
	 * The deprecated name.  Use {@link #SCANNER} or {@link #SCANNER_ARCHIVE_INTERPRETER} instead.
	 */
	String SCANNER_DEPRECATED = "hibernate.ejb.resource_scanner";

	/**
	 * Pass an implementation of {@link org.hibernate.boot.archive.scan.spi.Scanner}.
	 * Accepts either:<ul>
	 *     <li>an actual instance</li>
	 *     <li>a reference to a Class that implements Scanner</li>
	 *     <li>a fully qualified name (String) of a Class that implements Scanner</li>
	 * </ul>
	 */
	String SCANNER = "hibernate.archive.scanner";

	/**
	 * Pass {@link org.hibernate.boot.archive.spi.ArchiveDescriptorFactory} to use
	 * in the scanning process.  Accepts either:<ul>
	 *     <li>an ArchiveDescriptorFactory instance</li>
	 *     <li>a reference to a Class that implements ArchiveDescriptorFactory</li>
	 *     <li>a fully qualified name (String) of a Class that implements ArchiveDescriptorFactory</li>
	 * </ul>
	 * <p/>
	 * See information on {@link org.hibernate.boot.archive.scan.spi.Scanner}
	 * about expected constructor forms.
	 *
	 * @see #SCANNER
	 * @see org.hibernate.boot.archive.scan.spi.Scanner
	 * @see org.hibernate.boot.archive.scan.spi.AbstractScannerImpl
	 */
	String SCANNER_ARCHIVE_INTERPRETER = "hibernate.archive.interpreter";

	/**
	 * Identifies a comma-separate list of values indicating the types of
	 * things we should auto-detect during scanning.  Allowable values include:<ul>
	 *     <li>"class" - discover classes - .class files are discovered as managed classes</li>
	 *     <li>"hbm" - discover hbm mapping files - hbm.xml files are discovered as mapping files</li>
	 * </ul>
	 */
	String SCANNER_DISCOVERY = "hibernate.archive.autodetection";

	/**
	 * Used to specify the {@link org.hibernate.tool.schema.spi.SchemaManagementTool} to use for performing
	 * schema management.  The default is to use {@link org.hibernate.tool.schema.internal.HibernateSchemaManagementTool}
	 *
	 * @since 5.0
	 */
	String SCHEMA_MANAGEMENT_TOOL = "hibernate.schema_management_tool";

	/**
	 * Used to specify the {@link org.hibernate.boot.model.naming.ImplicitNamingStrategy} class to use.
	 *
	 * @since 5.0
	 */
	String IMPLICIT_NAMING_STRATEGY = "hibernate.implicit_naming_strategy";

	/**
	 * Used to specify the {@link org.hibernate.boot.model.naming.PhysicalNamingStrategy} class to use.
	 *
	 * @since 5.0
	 */
	String PHYSICAL_NAMING_STRATEGY = "hibernate.physical_naming_strategy";

	/**
	 * Used to specify the order in which metadata sources should be processed.  Value
	 * is a delimited-list whose elements are defined by {@link org.hibernate.cfg.MetadataSourceType}.
	 * <p/>
	 * Default is {@code "hbm,class"} which indicates to process {@code hbm.xml} files followed by
	 * annotations (combined with {@code orm.xml} mappings).
	 */
	String ARTIFACT_PROCESSING_ORDER = "hibernate.mapping.precedence";

	/**
	 * Specifies whether to automatically quote any names that are deemed keywords.  Auto-quoting
	 * is enabled by default.  Set to false to disable.
	 *
	 * @since 5.0
	 */
	String KEYWORD_AUTO_QUOTING_ENABLED = "hibernate.auto_quote_keyword";
}
