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
package org.hibernate.cfg;

/**
 * @author Steve Ebersole
 */
public interface AvailableSettings {
	/**
	 * Defines a name for the {@link org.hibernate.SessionFactory}.  Useful both to<ul>
	 *     <li>allow serialization and deserialization to work across different jvms</li>
	 *     <li>optionally allow the SessionFactory to be bound into JNDI</li>
	 * </ul>
	 *
	 * @see #SESSION_FACTORY_NAME_IS_JNDI
	 */
	public static final String SESSION_FACTORY_NAME = "hibernate.session_factory_name";

	/**
	 * Does the value defined by {@link #SESSION_FACTORY_NAME} represent a {@literal JNDI} namespace into which
	 * the {@link org.hibernate.SessionFactory} should be bound?
	 */
	public static final String SESSION_FACTORY_NAME_IS_JNDI = "hibernate.session_factory_name_is_jndi";

	/**
	 * Names the {@link org.hibernate.service.jdbc.connections.spi.ConnectionProvider} to use for obtaining
	 * JDBC connections.  Can either reference an instance of
	 * {@link org.hibernate.service.jdbc.connections.spi.ConnectionProvider} or a {@link Class} or {@link String}
	 * reference to the {@link org.hibernate.service.jdbc.connections.spi.ConnectionProvider} implementation
	 * class.
	 */
	public static final String CONNECTION_PROVIDER ="hibernate.connection.provider_class";

	/**
	 * Names the {@literal JDBC} driver class
	 */
	public static final String DRIVER ="hibernate.connection.driver_class";

	/**
	 * Names the {@literal JDBC} connection url.
	 */
	public static final String URL ="hibernate.connection.url";

	/**
	 * Names the connection user.  This might mean one of 2 things in out-of-the-box Hibernate
	 * {@link org.hibernate.service.jdbc.connections.spi.ConnectionProvider}: <ul>
	 *     <li>The username used to pass along to creating the JDBC connection</li>
	 *     <li>The username used to obtain a JDBC connection from a data source</li>
	 * </ul>
	 */
	public static final String USER ="hibernate.connection.username";

	/**
	 * Names the connection password.  See usage discussion on {@link #USER}
	 */
	public static final String PASS ="hibernate.connection.password";

	/**
	 * Names the {@literal JDBC} transaction isolation level
	 */
	public static final String ISOLATION ="hibernate.connection.isolation";

	/**
	 * Names the {@literal JDBC} autocommit mode
	 */
	public static final String AUTOCOMMIT ="hibernate.connection.autocommit";

	/**
	 * Maximum number of inactive connections for the built-in Hibernate connection pool.
	 */
	public static final String POOL_SIZE ="hibernate.connection.pool_size";

	/**
	 * Names a {@link javax.sql.DataSource}.  Can either reference a {@link javax.sql.DataSource} instance or
	 * a {@literal JNDI} name under which to locate the {@link javax.sql.DataSource}.
	 */
	public static final String DATASOURCE ="hibernate.connection.datasource";

	/**
	 * Names a prefix used to define arbitrary JDBC connection properties.  These properties are passed along to
	 * the {@literal JDBC} provider when creating a connection.
	 */
	public static final String CONNECTION_PREFIX = "hibernate.connection";

	/**
	 * Names the {@literal JNDI} {@link javax.naming.InitialContext} class.
	 *
	 * @see javax.naming.Context#INITIAL_CONTEXT_FACTORY
	 */
	public static final String JNDI_CLASS ="hibernate.jndi.class";

	/**
	 * Names the {@literal JNDI} provider/connection url
	 *
	 * @see javax.naming.Context#PROVIDER_URL
	 */
	public static final String JNDI_URL ="hibernate.jndi.url";

	/**
	 * Names a prefix used to define arbitrary {@literal JNDI} {@link javax.naming.InitialContext} properties.  These
	 * properties are passed along to {@link javax.naming.InitialContext#InitialContext(java.util.Hashtable)}
	 */
	public static final String JNDI_PREFIX = "hibernate.jndi";

	/**
	 * Names the Hibernate {@literal SQL} {@link org.hibernate.dialect.Dialect} class
	 */
	public static final String DIALECT ="hibernate.dialect";

	/**
	 * Names any additional {@link org.hibernate.service.jdbc.dialect.spi.DialectResolver} implementations to
	 * register with the standard {@link org.hibernate.service.jdbc.dialect.spi.DialectFactory}.
	 */
	public static final String DIALECT_RESOLVERS = "hibernate.dialect_resolvers";


	/**
	 * A default database schema (owner) name to use for unqualified tablenames
	 */
	public static final String DEFAULT_SCHEMA = "hibernate.default_schema";
	/**
	 * A default database catalog name to use for unqualified tablenames
	 */
	public static final String DEFAULT_CATALOG = "hibernate.default_catalog";

	/**
	 * Enable logging of generated SQL to the console
	 */
	public static final String SHOW_SQL ="hibernate.show_sql";
	/**
	 * Enable formatting of SQL logged to the console
	 */
	public static final String FORMAT_SQL ="hibernate.format_sql";
	/**
	 * Add comments to the generated SQL
	 */
	public static final String USE_SQL_COMMENTS ="hibernate.use_sql_comments";
	/**
	 * Maximum depth of outer join fetching
	 */
	public static final String MAX_FETCH_DEPTH = "hibernate.max_fetch_depth";
	/**
	 * The default batch size for batch fetching
	 */
	public static final String DEFAULT_BATCH_FETCH_SIZE = "hibernate.default_batch_fetch_size";
	/**
	 * Use <tt>java.io</tt> streams to read / write binary data from / to JDBC
	 */
	public static final String USE_STREAMS_FOR_BINARY = "hibernate.jdbc.use_streams_for_binary";
	/**
	 * Use JDBC scrollable <tt>ResultSet</tt>s. This property is only necessary when there is
	 * no <tt>ConnectionProvider</tt>, ie. the user is supplying JDBC connections.
	 */
	public static final String USE_SCROLLABLE_RESULTSET = "hibernate.jdbc.use_scrollable_resultset";
	/**
	 * Tells the JDBC driver to attempt to retrieve row Id with the JDBC 3.0 PreparedStatement.getGeneratedKeys()
	 * method. In general, performance will be better if this property is set to true and the underlying
	 * JDBC driver supports getGeneratedKeys().
	 */
	public static final String USE_GET_GENERATED_KEYS = "hibernate.jdbc.use_get_generated_keys";
	/**
	 * Gives the JDBC driver a hint as to the number of rows that should be fetched from the database
	 * when more rows are needed. If <tt>0</tt>, JDBC driver default settings will be used.
	 */
	public static final String STATEMENT_FETCH_SIZE = "hibernate.jdbc.fetch_size";
	/**
	 * Maximum JDBC batch size. A nonzero value enables batch updates.
	 */
	public static final String STATEMENT_BATCH_SIZE = "hibernate.jdbc.batch_size";
	/**
	 * Select a custom batcher.
	 */
	public static final String BATCH_STRATEGY = "hibernate.jdbc.factory_class";
	/**
	 * Should versioned data be included in batching?
	 */
	public static final String BATCH_VERSIONED_DATA = "hibernate.jdbc.batch_versioned_data";
	/**
	 * An XSLT resource used to generate "custom" XML
	 */
	public static final String OUTPUT_STYLESHEET ="hibernate.xml.output_stylesheet";

	/**
	 * Maximum size of C3P0 connection pool
	 */
	public static final String C3P0_MAX_SIZE = "hibernate.c3p0.max_size";
	/**
	 * Minimum size of C3P0 connection pool
	 */
	public static final String C3P0_MIN_SIZE = "hibernate.c3p0.min_size";

	/**
	 * Maximum idle time for C3P0 connection pool
	 */
	public static final String C3P0_TIMEOUT = "hibernate.c3p0.timeout";
	/**
	 * Maximum size of C3P0 statement cache
	 */
	public static final String C3P0_MAX_STATEMENTS = "hibernate.c3p0.max_statements";
	/**
	 * Number of connections acquired when pool is exhausted
	 */
	public static final String C3P0_ACQUIRE_INCREMENT = "hibernate.c3p0.acquire_increment";
	/**
	 * Idle time before a C3P0 pooled connection is validated
	 */
	public static final String C3P0_IDLE_TEST_PERIOD = "hibernate.c3p0.idle_test_period";

	/**
	 * Proxool/Hibernate property prefix
	 * @deprecated Use {@link #PROXOOL_CONFIG_PREFIX} instead
	 */
	public static final String PROXOOL_PREFIX = "hibernate.proxool";
	/**
	 * Proxool property to configure the Proxool Provider using an XML (<tt>/path/to/file.xml</tt>)
	 */
	public static final String PROXOOL_XML = "hibernate.proxool.xml";
	/**
	 * Proxool property to configure the Proxool Provider  using a properties file (<tt>/path/to/proxool.properties</tt>)
	 */
	public static final String PROXOOL_PROPERTIES = "hibernate.proxool.properties";
	/**
	 * Proxool property to configure the Proxool Provider from an already existing pool (<tt>true</tt> / <tt>false</tt>)
	 */
	public static final String PROXOOL_EXISTING_POOL = "hibernate.proxool.existing_pool";
	/**
	 * Proxool property with the Proxool pool alias to use
	 * (Required for <tt>PROXOOL_EXISTING_POOL</tt>, <tt>PROXOOL_PROPERTIES</tt>, or
	 * <tt>PROXOOL_XML</tt>)
	 */
	public static final String PROXOOL_POOL_ALIAS = "hibernate.proxool.pool_alias";

	/**
	 * Enable automatic session close at end of transaction
	 */
	public static final String AUTO_CLOSE_SESSION = "hibernate.transaction.auto_close_session";
	/**
	 * Enable automatic flush during the JTA <tt>beforeCompletion()</tt> callback
	 */
	public static final String FLUSH_BEFORE_COMPLETION = "hibernate.transaction.flush_before_completion";
	/**
	 * Specifies how Hibernate should release JDBC connections.
	 */
	public static final String RELEASE_CONNECTIONS = "hibernate.connection.release_mode";
	/**
	 * Context scoping impl for {@link org.hibernate.SessionFactory#getCurrentSession()} processing.
	 */
	public static final String CURRENT_SESSION_CONTEXT_CLASS = "hibernate.current_session_context_class";

	/**
	 * Names the implementation of {@link org.hibernate.engine.transaction.spi.TransactionContext} to use for
	 * creating {@link org.hibernate.Transaction} instances
	 */
	public static final String TRANSACTION_STRATEGY = "hibernate.transaction.factory_class";

	/**
	 * Names the {@link org.hibernate.service.jta.platform.spi.JtaPlatform} implementation to use for integrating
	 * with {@literal JTA} systems.  Can reference either a {@link org.hibernate.service.jta.platform.spi.JtaPlatform}
	 * instance or the name of the {@link org.hibernate.service.jta.platform.spi.JtaPlatform} implementation class
	 * @since 4.0
	 */
	public static final String JTA_PLATFORM = "hibernate.transaction.jta.platform";

	/**
	 * Names the {@link org.hibernate.transaction.TransactionManagerLookup} implementation to use for obtaining
	 * reference to the {@literal JTA} {@link javax.transaction.TransactionManager}
	 *
	 * @deprecated See {@link #JTA_PLATFORM}
	 */
	@Deprecated
	public static final String TRANSACTION_MANAGER_STRATEGY = "hibernate.transaction.manager_lookup_class";

	/**
	 * JNDI name of JTA <tt>UserTransaction</tt> object
	 *
	 * @deprecated See {@link #JTA_PLATFORM}
	 */
	@Deprecated
	public static final String USER_TRANSACTION = "jta.UserTransaction";

	/**
	 * The {@link org.hibernate.cache.spi.RegionFactory} implementation class
	 */
	public static final String CACHE_REGION_FACTORY = "hibernate.cache.region.factory_class";

	/**
	 * The <tt>CacheProvider</tt> implementation class
	 */
	public static final String CACHE_PROVIDER_CONFIG = "hibernate.cache.provider_configuration_file_resource_path";
	/**
	 * The <tt>CacheProvider</tt> JNDI namespace, if pre-bound to JNDI.
	 */
	public static final String CACHE_NAMESPACE = "hibernate.cache.jndi";
	/**
	 * Enable the query cache (disabled by default)
	 */
	public static final String USE_QUERY_CACHE = "hibernate.cache.use_query_cache";
	/**
	 * The <tt>QueryCacheFactory</tt> implementation class.
	 */
	public static final String QUERY_CACHE_FACTORY = "hibernate.cache.query_cache_factory";
	/**
	 * Enable the second-level cache (enabled by default)
	 */
	public static final String USE_SECOND_LEVEL_CACHE = "hibernate.cache.use_second_level_cache";
	/**
	 * Optimize the cache for minimal puts instead of minimal gets
	 */
	public static final String USE_MINIMAL_PUTS = "hibernate.cache.use_minimal_puts";
	/**
	 * The <tt>CacheProvider</tt> region name prefix
	 */
	public static final String CACHE_REGION_PREFIX = "hibernate.cache.region_prefix";
	/**
	 * Enable use of structured second-level cache entries
	 */
	public static final String USE_STRUCTURED_CACHE = "hibernate.cache.use_structured_entries";

	/**
	 * Enable statistics collection
	 */
	public static final String GENERATE_STATISTICS = "hibernate.generate_statistics";

	public static final String USE_IDENTIFIER_ROLLBACK = "hibernate.use_identifier_rollback";

	/**
	 * Use bytecode libraries optimized property access
	 */
	public static final String USE_REFLECTION_OPTIMIZER = "hibernate.bytecode.use_reflection_optimizer";

	/**
	 * The classname of the HQL query parser factory
	 */
	public static final String QUERY_TRANSLATOR = "hibernate.query.factory_class";

	/**
	 * A comma-separated list of token substitutions to use when translating a Hibernate
	 * query to SQL
	 */
	public static final String QUERY_SUBSTITUTIONS = "hibernate.query.substitutions";

	/**
	 * Should named queries be checked during startup (the default is enabled).
	 * <p/>
	 * Mainly intended for test environments.
	 */
	public static final String QUERY_STARTUP_CHECKING = "hibernate.query.startup_check";

	/**
	 * Auto export/update schema using hbm2ddl tool. Valid values are <tt>update</tt>,
	 * <tt>create</tt>, <tt>create-drop</tt> and <tt>validate</tt>.
	 */
	public static final String HBM2DDL_AUTO = "hibernate.hbm2ddl.auto";

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
	public static final String HBM2DDL_IMPORT_FILES = "hibernate.hbm2ddl.import_files";

	/**
	 * {@link String} reference to {@link org.hibernate.tool.hbm2ddl.ImportSqlCommandExtractor} implementation class.
	 * Referenced implementation is required to provide non-argument constructor.
	 *
	 * The default value is <tt>org.hibernate.tool.hbm2ddl.SingleLineSqlCommandExtractor</tt>.
	 */
	public static final String HBM2DDL_IMPORT_FILES_SQL_EXTRACTOR = "hibernate.hbm2ddl.import_files_sql_extractor";

	/**
	 * The {@link org.hibernate.exception.spi.SQLExceptionConverter} to use for converting SQLExceptions
	 * to Hibernate's JDBCException hierarchy.  The default is to use the configured
	 * {@link org.hibernate.dialect.Dialect}'s preferred SQLExceptionConverter.
	 */
	public static final String SQL_EXCEPTION_CONVERTER = "hibernate.jdbc.sql_exception_converter";

	/**
	 * Enable wrapping of JDBC result sets in order to speed up column name lookups for
	 * broken JDBC drivers
	 */
	public static final String WRAP_RESULT_SETS = "hibernate.jdbc.wrap_result_sets";

	/**
	 * Enable ordering of update statements by primary key value
	 */
	public static final String ORDER_UPDATES = "hibernate.order_updates";

	/**
	 * Enable ordering of insert statements for the purpose of more efficient JDBC batching.
	 */
	public static final String ORDER_INSERTS = "hibernate.order_inserts";

	/**
	 * The EntityMode in which set the Session opened from the SessionFactory.
	 */
    public static final String DEFAULT_ENTITY_MODE = "hibernate.default_entity_mode";

    /**
     * The jacc context id of the deployment
     */
    public static final String JACC_CONTEXTID = "hibernate.jacc_context_id";

	/**
	 * Should all database identifiers be quoted.
	 */
	public static final String GLOBALLY_QUOTED_IDENTIFIERS = "hibernate.globally_quoted_identifiers";

	/**
	 * Enable nullability checking.
	 * Raises an exception if a property marked as not-null is null.
	 * Default to false if Bean Validation is present in the classpath and Hibernate Annotations is used,
	 * true otherwise.
	 */
	public static final String CHECK_NULLABILITY = "hibernate.check_nullability";


	public static final String BYTECODE_PROVIDER = "hibernate.bytecode.provider";

	public static final String JPAQL_STRICT_COMPLIANCE= "hibernate.query.jpaql_strict_compliance";

	/**
	 * When using pooled {@link org.hibernate.id.enhanced.Optimizer optimizers}, prefer interpreting the
	 * database value as the lower (lo) boundary.  The default is to interpret it as the high boundary.
	 */
	public static final String PREFER_POOLED_VALUES_LO = "hibernate.id.optimizer.pooled.prefer_lo";

	/**
	 * The maximum number of strong references maintained by {@link org.hibernate.engine.query.spi.QueryPlanCache}. Default is 128.
	 * @deprecated in favor of {@link #QUERY_PLAN_CACHE_PARAMETER_METADATA_MAX_SIZE}
	 */
	@Deprecated
	public static final String QUERY_PLAN_CACHE_MAX_STRONG_REFERENCES = "hibernate.query.plan_cache_max_strong_references";

	/**
	 * The maximum number of soft references maintained by {@link org.hibernate.engine.query.spi.QueryPlanCache}. Default is 2048.
	 * @deprecated in favor of {@link #QUERY_PLAN_CACHE_MAX_SIZE}
	 */
	@Deprecated
	public static final String QUERY_PLAN_CACHE_MAX_SOFT_REFERENCES = "hibernate.query.plan_cache_max_soft_references";

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
	public static final String QUERY_PLAN_CACHE_MAX_SIZE = "hibernate.query.plan_cache_max_size";

	/**
	 * The maximum number of {@link org.hibernate.engine.query.spi.ParameterMetadata} maintained 
	 * by {@link org.hibernate.engine.query.spi.QueryPlanCache}. Default is 128.
	 */
	public static final String QUERY_PLAN_CACHE_PARAMETER_METADATA_MAX_SIZE = "hibernate.query.plan_parameter_metadata_max_size";

	/**
	 * Should we not use contextual LOB creation (aka based on {@link java.sql.Connection#createBlob()} et al).
	 */
	public static final String NON_CONTEXTUAL_LOB_CREATION = "hibernate.jdbc.lob.non_contextual_creation";

	/**
	 * Names the {@link ClassLoader} used to load user application classes.
	 * @since 4.0
	 */
	public static final String APP_CLASSLOADER = "hibernate.classLoader.application";

	/**
	 * Names the {@link ClassLoader} Hibernate should use to perform resource loading.
	 * @since 4.0
	 */
	public static final String RESOURCES_CLASSLOADER = "hibernate.classLoader.resources";

	/**
	 * Names the {@link ClassLoader} responsible for loading Hibernate classes.  By default this is
	 * the {@link ClassLoader} that loaded this class.
	 * @since 4.0
	 */
	public static final String HIBERNATE_CLASSLOADER = "hibernate.classLoader.hibernate";

	/**
	 * Names the {@link ClassLoader} used when Hibernate is unable to locates classes on the
	 * {@link #APP_CLASSLOADER} or {@link #HIBERNATE_CLASSLOADER}.
	 * @since 4.0
	 */
	public static final String ENVIRONMENT_CLASSLOADER = "hibernate.classLoader.environment";


	public static final String C3P0_CONFIG_PREFIX = "hibernate.c3p0";

	public static final String PROXOOL_CONFIG_PREFIX = "hibernate.proxool";


	public static final String JMX_ENABLED = "hibernate.jmx.enabled";
	public static final String JMX_PLATFORM_SERVER = "hibernate.jmx.usePlatformServer";
	public static final String JMX_AGENT_ID = "hibernate.jmx.agentId";
	public static final String JMX_DOMAIN_NAME = "hibernate.jmx.defaultDomain";
	public static final String JMX_SF_NAME = "hibernate.jmx.sessionFactoryName";
	public static final String JMX_DEFAULT_OBJ_NAME_DOMAIN = "org.hibernate.core";

	/**
	 * A configuration value key used to indicate that it is safe to cache
	 * {@link javax.transaction.TransactionManager} references.
	 * @since 4.0
	 */
	public static final String JTA_CACHE_TM = "hibernate.jta.cacheTransactionManager";

	/**
	 * A configuration value key used to indicate that it is safe to cache
	 * {@link javax.transaction.UserTransaction} references.
	 * @since 4.0
	 */
	public static final String JTA_CACHE_UT = "hibernate.jta.cacheUserTransaction";

	/**
	 * Setting used to give the name of the default {@link org.hibernate.annotations.CacheConcurrencyStrategy}
	 * to use when either {@link javax.persistence.Cacheable @Cacheable} or
	 * {@link org.hibernate.annotations.Cache @Cache} is used.  {@link org.hibernate.annotations.Cache @Cache(strategy="..")} is used to override.
	 */
	public static final String DEFAULT_CACHE_CONCURRENCY_STRATEGY = "hibernate.cache.default_cache_concurrency_strategy";

	/**
	 * Setting which indicates whether or not the new {@link org.hibernate.id.IdentifierGenerator} are used
	 * for AUTO, TABLE and SEQUENCE.
	 * Default to false to keep backward compatibility.
	 */
	public static final String USE_NEW_ID_GENERATOR_MAPPINGS = "hibernate.id.new_generator_mappings";

	/**
	 * Setting to identify a {@link org.hibernate.CustomEntityDirtinessStrategy} to use.  May point to
	 * either a class name or instance.
	 */
	public static final String CUSTOM_ENTITY_DIRTINESS_STRATEGY = "hibernate.entity_dirtiness_strategy";

	/**
	 * Strategy for multi-tenancy.

	 * @see org.hibernate.MultiTenancyStrategy
	 * @since 4.0
	 */
	public static final String MULTI_TENANT = "hibernate.multiTenancy";

	/**
	 * Names a {@link org.hibernate.service.jdbc.connections.spi.MultiTenantConnectionProvider} implementation to
	 * use.  As MultiTenantConnectionProvider is also a service, can be configured directly through the
	 * {@link org.hibernate.service.ServiceRegistryBuilder}
	 *
	 * @since 4.1
	 */
	public static final String MULTI_TENANT_CONNECTION_PROVIDER = "hibernate.multi_tenant_connection_provider";

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
	public static final String MULTI_TENANT_IDENTIFIER_RESOLVER = "hibernate.tenant_identifier_resolver";

	public static final String FORCE_DISCRIMINATOR_IN_SELECTS_BY_DEFAULT = "hibernate.discriminator.force_in_select";

    public static final String ENABLE_LAZY_LOAD_NO_TRANS = "hibernate.enable_lazy_load_no_trans";

	public static final String HQL_BULK_ID_STRATEGY = "hibernate.hql.bulk_id_strategy";
}
