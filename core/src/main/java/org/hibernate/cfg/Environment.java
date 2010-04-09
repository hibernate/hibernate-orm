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

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.HibernateException;
import org.hibernate.Version;
import org.hibernate.bytecode.BytecodeProvider;
import org.hibernate.util.ConfigHelper;
import org.hibernate.util.PropertiesHelper;


/**
 * Provides access to configuration info passed in <tt>Properties</tt> objects.
 * <br><br>
 * Hibernate has two property scopes:
 * <ul>
 * <li><b>Factory-level</b> properties may be passed to the <tt>SessionFactory</tt> when it
 * instantiated. Each instance might have different property values. If no
 * properties are specified, the factory calls <tt>Environment.getProperties()</tt>.
 * <li><b>System-level</b> properties are shared by all factory instances and are always
 * determined by the <tt>Environment</tt> properties.
 * </ul>
 * The only system-level properties are
 * <ul>
 * <li><tt>hibernate.jdbc.use_streams_for_binary</tt>
 * <li><tt>hibernate.cglib.use_reflection_optimizer</tt>
 * </ul>
 * <tt>Environment</tt> properties are populated by calling <tt>System.getProperties()</tt>
 * and then from a resource named <tt>/hibernate.properties</tt> if it exists. System
 * properties override properties specified in <tt>hibernate.properties</tt>.<br>
 * <br>
 * The <tt>SessionFactory</tt> is controlled by the following properties.
 * Properties may be either be <tt>System</tt> properties, properties
 * defined in a resource named <tt>/hibernate.properties</tt> or an instance of
 * <tt>java.util.Properties</tt> passed to
 * <tt>Configuration.buildSessionFactory()</tt><br>
 * <br>
 * <table>
 * <tr><td><b>property</b></td><td><b>meaning</b></td></tr>
 * <tr>
 *   <td><tt>hibernate.dialect</tt></td>
 *   <td>classname of <tt>org.hibernate.dialect.Dialect</tt> subclass</td>
 * </tr>
 * <tr>
 *   <td><tt>hibernate.cache.provider_class</tt></td>
 *   <td>classname of <tt>org.hibernate.cache.CacheProvider</tt>
 *   subclass (if not specified EHCache is used)</td>
 * </tr>
 * <tr>
 *   <td><tt>hibernate.connection.provider_class</tt></td>
 *   <td>classname of <tt>org.hibernate.connection.ConnectionProvider</tt>
 *   subclass (if not specified hueristics are used)</td>
 * </tr>
 * <tr><td><tt>hibernate.connection.username</tt></td><td>database username</td></tr>
 * <tr><td><tt>hibernate.connection.password</tt></td><td>database password</td></tr>
 * <tr>
 *   <td><tt>hibernate.connection.url</tt></td>
 *   <td>JDBC URL (when using <tt>java.sql.DriverManager</tt>)</td>
 * </tr>
 * <tr>
 *   <td><tt>hibernate.connection.driver_class</tt></td>
 *   <td>classname of JDBC driver</td>
 * </tr>
 * <tr>
 *   <td><tt>hibernate.connection.isolation</tt></td>
 *   <td>JDBC transaction isolation level (only when using
 *     <tt>java.sql.DriverManager</tt>)
 *   </td>
 * </tr>
 *   <td><tt>hibernate.connection.pool_size</tt></td>
 *   <td>the maximum size of the connection pool (only when using
 *     <tt>java.sql.DriverManager</tt>)
 *   </td>
 * </tr>
 * <tr>
 *   <td><tt>hibernate.connection.datasource</tt></td>
 *   <td>databasource JNDI name (when using <tt>javax.sql.Datasource</tt>)</td>
 * </tr>
 * <tr>
 *   <td><tt>hibernate.jndi.url</tt></td><td>JNDI <tt>InitialContext</tt> URL</td>
 * </tr>
 * <tr>
 *   <td><tt>hibernate.jndi.class</tt></td><td>JNDI <tt>InitialContext</tt> classname</td>
 * </tr>
 * <tr>
 *   <td><tt>hibernate.max_fetch_depth</tt></td>
 *   <td>maximum depth of outer join fetching</td>
 * </tr>
 * <tr>
 *   <td><tt>hibernate.jdbc.batch_size</tt></td>
 *   <td>enable use of JDBC2 batch API for drivers which support it</td>
 * </tr>
 * <tr>
 *   <td><tt>hibernate.jdbc.fetch_size</tt></td>
 *   <td>set the JDBC fetch size</td>
 * </tr>
 * <tr>
 *   <td><tt>hibernate.jdbc.use_scrollable_resultset</tt></td>
 *   <td>enable use of JDBC2 scrollable resultsets (you only need this specify
 *   this property when using user supplied connections)</td>
 * </tr>
 * <tr>
 *   <td><tt>hibernate.jdbc.use_getGeneratedKeys</tt></td>
 *   <td>enable use of JDBC3 PreparedStatement.getGeneratedKeys() to retrieve
 *   natively generated keys after insert. Requires JDBC3+ driver and JRE1.4+</td>
 * </tr>
 * <tr>
 *   <td><tt>hibernate.hbm2ddl.auto</tt></td>
 *   <td>enable auto DDL export</td>
 * </tr>
 * <tr>
 *   <td><tt>hibernate.default_schema</tt></td>
 *   <td>use given schema name for unqualified tables (always optional)</td>
 * </tr>
 * <tr>
 *   <td><tt>hibernate.default_catalog</tt></td>
 *   <td>use given catalog name for unqualified tables (always optional)</td>
 * </tr>
 * <tr>
 *   <td><tt>hibernate.session_factory_name</tt></td>
 *   <td>If set, the factory attempts to bind this name to itself in the
 *   JNDI context. This name is also used to support cross JVM <tt>
 *   Session</tt> (de)serialization.</td>
 * </tr>
 * <tr>
 *   <td><tt>hibernate.transaction.manager_lookup_class</tt></td>
 *   <td>classname of <tt>org.hibernate.transaction.TransactionManagerLookup</tt>
 *   implementor</td>
 * </tr>
 * <tr>
 *   <td><tt>hibernate.transaction.factory_class</tt></td>
 *   <td>the factory to use for instantiating <tt>Transaction</tt>s.
 *   (Defaults to <tt>JDBCTransactionFactory</tt>.)</td>
 * </tr>
 * <tr>
 *   <td><tt>hibernate.query.substitutions</tt></td><td>query language token substitutions</td>
 * </tr>
 * </table>
 *
 * @see org.hibernate.SessionFactory
 * @author Gavin King
 */
public final class Environment {
	/**
	 * <tt>ConnectionProvider</tt> implementor to use when obtaining connections
	 */
	public static final String CONNECTION_PROVIDER ="hibernate.connection.provider_class";
	/**
	 * JDBC driver class
	 */
	public static final String DRIVER ="hibernate.connection.driver_class";
	/**
	 * JDBC transaction isolation level
	 */
	public static final String ISOLATION ="hibernate.connection.isolation";
	/**
	 * JDBC URL
	 */
	public static final String URL ="hibernate.connection.url";
	/**
	 * JDBC user
	 */
	public static final String USER ="hibernate.connection.username";
	/**
	 * JDBC password
	 */
	public static final String PASS ="hibernate.connection.password";
	/**
	 * JDBC autocommit mode
	 */
	public static final String AUTOCOMMIT ="hibernate.connection.autocommit";
	/**
	 * Maximum number of inactive connections for Hibernate's connection pool
	 */
	public static final String POOL_SIZE ="hibernate.connection.pool_size";
	/**
	 * <tt>java.sql.Datasource</tt> JNDI name
	 */
	public static final String DATASOURCE ="hibernate.connection.datasource";
	/**
	 * prefix for arbitrary JDBC connection properties
	 */
	public static final String CONNECTION_PREFIX = "hibernate.connection";

	/**
	 * JNDI initial context class, <tt>Context.INITIAL_CONTEXT_FACTORY</tt>
	 */
	public static final String JNDI_CLASS ="hibernate.jndi.class";
	/**
	 * JNDI provider URL, <tt>Context.PROVIDER_URL</tt>
	 */
	public static final String JNDI_URL ="hibernate.jndi.url";
	/**
	 * prefix for arbitrary JNDI <tt>InitialContext</tt> properties
	 */
	public static final String JNDI_PREFIX = "hibernate.jndi";
	/**
	 * JNDI name to bind to <tt>SessionFactory</tt>
	 */
	public static final String SESSION_FACTORY_NAME = "hibernate.session_factory_name";

	/**
	 * Hibernate SQL {@link org.hibernate.dialect.Dialect} class
	 */
	public static final String DIALECT ="hibernate.dialect";

	/**
	 * {@link org.hibernate.dialect.resolver.DialectResolver} classes to register with the
	 * {@link org.hibernate.dialect.resolver.DialectFactory}
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
	 * <tt>TransactionFactory</tt> implementor to use for creating <tt>Transaction</tt>s
	 */
	public static final String TRANSACTION_STRATEGY = "hibernate.transaction.factory_class";
	/**
	 * <tt>TransactionManagerLookup</tt> implementor to use for obtaining the <tt>TransactionManager</tt>
	 */
	public static final String TRANSACTION_MANAGER_STRATEGY = "hibernate.transaction.manager_lookup_class";
	/**
	 * JNDI name of JTA <tt>UserTransaction</tt> object
	 */
	public static final String USER_TRANSACTION = "jta.UserTransaction";

	/**
	 * The <tt>CacheProvider</tt> implementation class
	 */
	public static final String CACHE_PROVIDER = "hibernate.cache.provider_class";

	/**
	 * The {@link org.hibernate.cache.RegionFactory} implementation class
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
	 * The {@link org.hibernate.exception.SQLExceptionConverter} to use for converting SQLExceptions
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

	private static final BytecodeProvider BYTECODE_PROVIDER_INSTANCE;
	private static final boolean ENABLE_BINARY_STREAMS;
	private static final boolean ENABLE_REFLECTION_OPTIMIZER;
	private static final boolean JVM_SUPPORTS_LINKED_HASH_COLLECTIONS;
	private static final boolean JVM_HAS_TIMESTAMP_BUG;
	private static final boolean JVM_HAS_JDK14_TIMESTAMP;
	private static final boolean JVM_SUPPORTS_GET_GENERATED_KEYS;

	private static final Properties GLOBAL_PROPERTIES;
	private static final HashMap ISOLATION_LEVELS = new HashMap();
	private static final Map OBSOLETE_PROPERTIES = new HashMap();
	private static final Map RENAMED_PROPERTIES = new HashMap();

	private static final Logger log = LoggerFactory.getLogger(Environment.class);

	/**
	 * Issues warnings to the user when any obsolete or renamed property names are used.
	 *
	 * @param props The specified properties.
	 */
	public static void verifyProperties(Properties props) {
		Iterator iter = props.keySet().iterator();
		Map propertiesToAdd = new HashMap();
		while ( iter.hasNext() ) {
			final Object propertyName = iter.next();
			Object newPropertyName = OBSOLETE_PROPERTIES.get( propertyName );
			if ( newPropertyName != null ) {
				log.warn( "Usage of obsolete property: " + propertyName + " no longer supported, use: " + newPropertyName );
			}
			newPropertyName = RENAMED_PROPERTIES.get( propertyName );
			if ( newPropertyName != null ) {
				log.warn( "Property [" + propertyName + "] has been renamed to [" + newPropertyName + "]; update your properties appropriately" );
				if ( ! props.containsKey( newPropertyName ) ) {
					propertiesToAdd.put( newPropertyName, props.get( propertyName ) );
				}
			}
		}
		props.putAll(propertiesToAdd);
	}

	static {

		log.info( "Hibernate " + Version.getVersionString() );

		RENAMED_PROPERTIES.put( "hibernate.cglib.use_reflection_optimizer", USE_REFLECTION_OPTIMIZER );

		ISOLATION_LEVELS.put( new Integer(Connection.TRANSACTION_NONE), "NONE" );
		ISOLATION_LEVELS.put( new Integer(Connection.TRANSACTION_READ_UNCOMMITTED), "READ_UNCOMMITTED" );
		ISOLATION_LEVELS.put( new Integer(Connection.TRANSACTION_READ_COMMITTED), "READ_COMMITTED" );
		ISOLATION_LEVELS.put( new Integer(Connection.TRANSACTION_REPEATABLE_READ), "REPEATABLE_READ" );
		ISOLATION_LEVELS.put( new Integer(Connection.TRANSACTION_SERIALIZABLE), "SERIALIZABLE" );

		GLOBAL_PROPERTIES = new Properties();
		//Set USE_REFLECTION_OPTIMIZER to false to fix HHH-227
		GLOBAL_PROPERTIES.setProperty( USE_REFLECTION_OPTIMIZER, Boolean.FALSE.toString() );

		try {
			InputStream stream = ConfigHelper.getResourceAsStream("/hibernate.properties");
			try {
				GLOBAL_PROPERTIES.load(stream);
				log.info( "loaded properties from resource hibernate.properties: " + PropertiesHelper.maskOut(GLOBAL_PROPERTIES, PASS) );
			}
			catch (Exception e) {
				log.error("problem loading properties from hibernate.properties");
			}
			finally {
				try{
					stream.close();
				}
				catch (IOException ioe){
					log.error("could not close stream on hibernate.properties", ioe);
				}
			}
		}
		catch (HibernateException he) {
			log.info("hibernate.properties not found");
		}

		try {
			GLOBAL_PROPERTIES.putAll( System.getProperties() );
		}
		catch (SecurityException se) {
			log.warn("could not copy system properties, system properties will be ignored");
		}

		verifyProperties(GLOBAL_PROPERTIES);

		ENABLE_BINARY_STREAMS = PropertiesHelper.getBoolean(USE_STREAMS_FOR_BINARY, GLOBAL_PROPERTIES);
		ENABLE_REFLECTION_OPTIMIZER = PropertiesHelper.getBoolean(USE_REFLECTION_OPTIMIZER, GLOBAL_PROPERTIES);

		if (ENABLE_BINARY_STREAMS) {
			log.info("using java.io streams to persist binary types");
		}
		if (ENABLE_REFLECTION_OPTIMIZER) {
			log.info("using bytecode reflection optimizer");
		}
		BYTECODE_PROVIDER_INSTANCE = buildBytecodeProvider( GLOBAL_PROPERTIES );

		boolean getGeneratedKeysSupport;
		try {
			Statement.class.getMethod("getGeneratedKeys", null);
			getGeneratedKeysSupport = true;
		}
		catch (NoSuchMethodException nsme) {
			getGeneratedKeysSupport = false;
		}
		JVM_SUPPORTS_GET_GENERATED_KEYS = getGeneratedKeysSupport;
		if (!JVM_SUPPORTS_GET_GENERATED_KEYS) {
			log.info("JVM does not support Statement.getGeneratedKeys()");
		}

		boolean linkedHashSupport;
		try {
			Class.forName("java.util.LinkedHashSet");
			linkedHashSupport = true;
		}
		catch (ClassNotFoundException cnfe) {
			linkedHashSupport = false;
		}
		JVM_SUPPORTS_LINKED_HASH_COLLECTIONS = linkedHashSupport;
		if (!JVM_SUPPORTS_LINKED_HASH_COLLECTIONS) {
			log.info("JVM does not support LinkedHasMap, LinkedHashSet - ordered maps and sets disabled");
		}

		long x = 123456789;
		JVM_HAS_TIMESTAMP_BUG = new Timestamp(x).getTime() != x;
		if (JVM_HAS_TIMESTAMP_BUG) {
			log.info("using workaround for JVM bug in java.sql.Timestamp");
		}

		Timestamp t = new Timestamp(0);
		t.setNanos(5 * 1000000);
		JVM_HAS_JDK14_TIMESTAMP = t.getTime() == 5;
		if (JVM_HAS_JDK14_TIMESTAMP) {
			log.info("using JDK 1.4 java.sql.Timestamp handling");
		}
		else {
			log.info("using pre JDK 1.4 java.sql.Timestamp handling");
		}
	}

	public static BytecodeProvider getBytecodeProvider() {
		return BYTECODE_PROVIDER_INSTANCE;
	}

	/**
	 * Does this JVM's implementation of {@link java.sql.Timestamp} have a bug in which the following is true:<code>
	 * new java.sql.Timestamp( x ).getTime() != x
	 * </code>
	 * <p/>
	 * NOTE : IBM JDK 1.3.1 the only known JVM to exhibit this behavior.
	 *
	 * @return True if the JVM's {@link Timestamp} implementa
	 */
	public static boolean jvmHasTimestampBug() {
		return JVM_HAS_TIMESTAMP_BUG;
	}

	/**
	 * Does this JVM handle {@link java.sql.Timestamp} in the JDK 1.4 compliant way wrt to nano rolling>
	 *
	 * @return True if the JDK 1.4 (JDBC3) specification for {@link java.sql.Timestamp} nano rolling is adhered to.
	 *
	 * @deprecated Starting with 3.3 Hibernate requires JDK 1.4 or higher
	 */
	public static boolean jvmHasJDK14Timestamp() {
		return JVM_HAS_JDK14_TIMESTAMP;
	}

	/**
	 * Does this JVM support {@link java.util.LinkedHashSet} and {@link java.util.LinkedHashMap}?
	 * <p/>
	 * Note, this is true for JDK 1.4 and above; hence the deprecation.
	 *
	 * @return True if {@link java.util.LinkedHashSet} and {@link java.util.LinkedHashMap} are available.
	 *
	 * @deprecated Starting with 3.3 Hibernate requires JDK 1.4 or higher
	 * @see java.util.LinkedHashSet
	 * @see java.util.LinkedHashMap
	 */
	public static boolean jvmSupportsLinkedHashCollections() {
		return JVM_SUPPORTS_LINKED_HASH_COLLECTIONS;
	}

	/**
	 * Does this JDK/JVM define the JDBC {@link Statement} interface with a 'getGeneratedKeys' method?
	 * <p/>
	 * Note, this is true for JDK 1.4 and above; hence the deprecation.
	 *
	 * @return True if generated keys can be retrieved via Statement; false otherwise.
	 *
	 * @see Statement
	 * @deprecated Starting with 3.3 Hibernate requires JDK 1.4 or higher
	 */
	public static boolean jvmSupportsGetGeneratedKeys() {
		return JVM_SUPPORTS_GET_GENERATED_KEYS;
	}

	/**
	 * Should we use streams to bind binary types to JDBC IN parameters?
	 *
	 * @return True if streams should be used for binary data handling; false otherwise.
	 *
	 * @see #USE_STREAMS_FOR_BINARY
	 */
	public static boolean useStreamsForBinary() {
		return ENABLE_BINARY_STREAMS;
	}

	/**
	 * Should we use reflection optimization?
	 *
	 * @return True if reflection optimization should be used; false otherwise.
	 *
	 * @see #USE_REFLECTION_OPTIMIZER
	 * @see #getBytecodeProvider()
	 * @see BytecodeProvider#getReflectionOptimizer
	 */
	public static boolean useReflectionOptimizer() {
		return ENABLE_REFLECTION_OPTIMIZER;
	}

	/**
	 * Disallow instantiation
	 */
	private Environment() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Return <tt>System</tt> properties, extended by any properties specified
	 * in <tt>hibernate.properties</tt>.
	 * @return Properties
	 */
	public static Properties getProperties() {
		Properties copy = new Properties();
		copy.putAll(GLOBAL_PROPERTIES);
		return copy;
	}

	/**
	 * Get the name of a JDBC transaction isolation level
	 *
	 * @see java.sql.Connection
	 * @param isolation as defined by <tt>java.sql.Connection</tt>
	 * @return a human-readable name
	 */
	public static String isolationLevelToString(int isolation) {
		return (String) ISOLATION_LEVELS.get( new Integer(isolation) );
	}

	public static BytecodeProvider buildBytecodeProvider(Properties properties) {
		String provider = PropertiesHelper.getString( BYTECODE_PROVIDER, properties, "javassist" );
		log.info( "Bytecode provider name : " + provider );
		return buildBytecodeProvider( provider );
	}

	private static BytecodeProvider buildBytecodeProvider(String providerName) {
		if ( "javassist".equals( providerName ) ) {
			return new org.hibernate.bytecode.javassist.BytecodeProviderImpl();
		}
		else if ( "cglib".equals( providerName ) ) {
			return new org.hibernate.bytecode.cglib.BytecodeProviderImpl();
		}

		log.warn( "unrecognized bytecode provider [" + providerName + "], using javassist by default" );
		return new org.hibernate.bytecode.javassist.BytecodeProviderImpl();
	}

}
