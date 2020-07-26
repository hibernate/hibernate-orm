/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.Version;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.log.UnsupportedLogger;
import org.hibernate.internal.util.ConfigHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;

import org.jboss.logging.Logger;


/**
 * Provides access to configuration info passed in <tt>Properties</tt> objects.
 * <br><br>
 * Hibernate has two property scopes:
 * <ul>
 * <li><b>Factory-level</b> properties may be passed to the <tt>SessionFactory</tt> when it
 * is instantiated. Each instance might have different property values. If no
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
 * <tt>Configuration.build()</tt><br>
 * <br>
 * <table>
 * <tr><td><b>property</b></td><td><b>meaning</b></td></tr>
 * <tr>
 *   <td><tt>hibernate.dialect</tt></td>
 *   <td>classname of <tt>org.hibernate.dialect.Dialect</tt> subclass</td>
 * </tr>
 * <tr>
 *   <td><tt>hibernate.connection.provider_class</tt></td>
 *   <td>classname of <tt>ConnectionProvider</tt>
 *   subclass (if not specified heuristics are used)</td>
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
 *   <td>datasource JNDI name (when using <tt>javax.sql.Datasource</tt>)</td>
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
 *   <td>enable use of JDBC2 scrollable resultsets (you only need to specify
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
 *   <td><tt>hibernate.transaction.jta.platform</tt></td>
 *   <td>classname of <tt>org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform</tt>
 *   implementor</td>
 * </tr>
 * <tr>
 *   <td><tt>hibernate.transaction.factory_class</tt></td>
 *   <td>the factory to use for instantiating <tt>Transaction</tt>s.
 *   (Defaults to <tt>JdbcTransactionFactory</tt>.)</td>
 * </tr>
 * <tr>
 *   <td><tt>hibernate.query.substitutions</tt></td><td>query language token substitutions</td>
 * </tr>
 * </table>
 *
 * @see org.hibernate.SessionFactory
 * @author Gavin King
 */
public final class Environment implements AvailableSettings {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger( CoreMessageLogger.class, Environment.class.getName());

	private static final BytecodeProvider BYTECODE_PROVIDER_INSTANCE;
	private static final boolean ENABLE_BINARY_STREAMS;
	private static final boolean ENABLE_REFLECTION_OPTIMIZER;
	private static final boolean ENABLE_LEGACY_PROXY_CLASSNAMES;

	private static final Properties GLOBAL_PROPERTIES;

	/**
	 * No longer effective.
	 *
	 * @param configurationValues The specified properties.
	 * @deprecated without replacement. Such verification is best done ad hoc, case by case.
	 */
	@Deprecated
	public static void verifyProperties(Map<?,?> configurationValues) {
		//Obsolete and Renamed properties are no longer handled here
	}

	static {
		Version.logVersion();

		GLOBAL_PROPERTIES = new Properties();
		//Set USE_REFLECTION_OPTIMIZER to false to fix HHH-227
		GLOBAL_PROPERTIES.setProperty( USE_REFLECTION_OPTIMIZER, Boolean.FALSE.toString() );

		try {
			InputStream stream = ConfigHelper.getResourceAsStream( "/hibernate.properties" );
			try {
				GLOBAL_PROPERTIES.load(stream);
				LOG.propertiesLoaded( ConfigurationHelper.maskOut( GLOBAL_PROPERTIES, PASS ) );
			}
			catch (Exception e) {
				LOG.unableToLoadProperties();
			}
			finally {
				try{
					stream.close();
				}
				catch (IOException ioe){
					LOG.unableToCloseStreamError( ioe );
				}
			}
		}
		catch (HibernateException he) {
			LOG.propertiesNotFound();
		}

		try {
			Properties systemProperties = System.getProperties();
		    // Must be thread-safe in case an application changes System properties during Hibernate initialization.
		    // See HHH-8383.
			synchronized (systemProperties) {
				GLOBAL_PROPERTIES.putAll(systemProperties);
			}
		}
		catch (SecurityException se) {
			LOG.unableToCopySystemProperties();
		}

		ENABLE_BINARY_STREAMS = ConfigurationHelper.getBoolean(USE_STREAMS_FOR_BINARY, GLOBAL_PROPERTIES);
		if ( ENABLE_BINARY_STREAMS ) {
			LOG.usingStreams();
		}

		ENABLE_REFLECTION_OPTIMIZER = ConfigurationHelper.getBoolean(USE_REFLECTION_OPTIMIZER, GLOBAL_PROPERTIES);
		if ( ENABLE_REFLECTION_OPTIMIZER ) {
			LOG.usingReflectionOptimizer();
		}

		ENABLE_LEGACY_PROXY_CLASSNAMES = ConfigurationHelper.getBoolean( ENFORCE_LEGACY_PROXY_CLASSNAMES, GLOBAL_PROPERTIES );
		if ( ENABLE_LEGACY_PROXY_CLASSNAMES ) {
			final UnsupportedLogger unsupportedLogger = Logger.getMessageLogger( UnsupportedLogger.class, Environment.class.getName() );
			unsupportedLogger.usingLegacyClassnamesForProxies();
		}

		BYTECODE_PROVIDER_INSTANCE = buildBytecodeProvider( GLOBAL_PROPERTIES );
	}

	/**
	 * This will be removed soon; currently just returns false as no known JVM exhibits this bug
	 * and is also able to run this version of Hibernate ORM.
	 * @deprecated removed as unnecessary
	 * @return false
	 */
	@Deprecated
	public static boolean jvmHasTimestampBug() {
		return false;
	}

	/**
	 * Should we use streams to bind binary types to JDBC IN parameters?
	 *
	 * @return True if streams should be used for binary data handling; false otherwise.
	 *
	 * @see #USE_STREAMS_FOR_BINARY
	 *
	 * @deprecated Deprecated to indicate that the method will be moved to
	 * {@link org.hibernate.boot.spi.SessionFactoryOptions} /
	 * {@link org.hibernate.boot.SessionFactoryBuilder} - probably in 6.0.
	 * See <a href="https://hibernate.atlassian.net/browse/HHH-12194">HHH-12194</a> and
	 * <a href="https://hibernate.atlassian.net/browse/HHH-12193">HHH-12193</a> for details
	 */
	@Deprecated
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
	 *
	 * @deprecated Deprecated to indicate that the method will be moved to
	 * {@link org.hibernate.boot.spi.SessionFactoryOptions} /
	 * {@link org.hibernate.boot.SessionFactoryBuilder} - probably in 6.0.
	 * See <a href="https://hibernate.atlassian.net/browse/HHH-12194">HHH-12194</a> and
	 * <a href="https://hibernate.atlassian.net/browse/HHH-12193">HHH-12193</a> for details
	 */
	@Deprecated
	public static boolean useReflectionOptimizer() {
		return ENABLE_REFLECTION_OPTIMIZER;
	}

	/**
	 * @deprecated Deprecated to indicate that the method will be moved to
	 * {@link org.hibernate.boot.spi.SessionFactoryOptions} /
	 * {@link org.hibernate.boot.SessionFactoryBuilder} - probably in 6.0.
	 * See <a href="https://hibernate.atlassian.net/browse/HHH-12194">HHH-12194</a> and
	 * <a href="https://hibernate.atlassian.net/browse/HHH-12193">HHH-12193</a> for details
	 */
	@Deprecated
	public static BytecodeProvider getBytecodeProvider() {
		return BYTECODE_PROVIDER_INSTANCE;
	}

	/**
	 * @return True if global option org.hibernate.cfg.AvailableSettings#ENFORCE_LEGACY_PROXY_CLASSNAMES was enabled
	 * @deprecated This option will be removed soon and should not be relied on.
	 */
	@Deprecated
	public static boolean useLegacyProxyClassnames() {
		return ENABLE_LEGACY_PROXY_CLASSNAMES;
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
	 * @deprecated Use {@link ConnectionProviderInitiator#toIsolationNiceName} instead
	 */
	@Deprecated
	public static String isolationLevelToString(int isolation) {
		return ConnectionProviderInitiator.toIsolationNiceName( isolation );
	}


	public static final String BYTECODE_PROVIDER_NAME_JAVASSIST = "javassist";
	public static final String BYTECODE_PROVIDER_NAME_BYTEBUDDY = "bytebuddy";
	public static final String BYTECODE_PROVIDER_NAME_NONE = "none";
	public static final String BYTECODE_PROVIDER_NAME_DEFAULT = BYTECODE_PROVIDER_NAME_BYTEBUDDY;

	public static BytecodeProvider buildBytecodeProvider(Properties properties) {
		String provider = ConfigurationHelper.getString( BYTECODE_PROVIDER, properties, BYTECODE_PROVIDER_NAME_DEFAULT );
		return buildBytecodeProvider( provider );
	}

	private static BytecodeProvider buildBytecodeProvider(String providerName) {
		if ( BYTECODE_PROVIDER_NAME_NONE.equals( providerName ) ) {
			return new org.hibernate.bytecode.internal.none.BytecodeProviderImpl();
		}
		if ( BYTECODE_PROVIDER_NAME_BYTEBUDDY.equals( providerName ) ) {
			return new org.hibernate.bytecode.internal.bytebuddy.BytecodeProviderImpl();
		}

		if ( BYTECODE_PROVIDER_NAME_JAVASSIST.equals( providerName ) ) {
			return new org.hibernate.bytecode.internal.javassist.BytecodeProviderImpl();
		}

		LOG.bytecodeProvider( providerName );

		// there is no need to support plugging in a custom BytecodeProvider via FQCN:
		// - the static helper methods on this class are deprecated
		// - it's possible to plug a custom BytecodeProvider directly into the ServiceRegistry
		//
		// This also allows integrators to inject a BytecodeProvider instance which has some
		// state; particularly useful to inject proxy definitions which have been prepared in
		// advance.
		// See also https://hibernate.atlassian.net/browse/HHH-13804 and how this was solved in
		// Quarkus.

		LOG.unknownBytecodeProvider( providerName, BYTECODE_PROVIDER_NAME_DEFAULT );
		return new org.hibernate.bytecode.internal.bytebuddy.BytecodeProviderImpl();
	}
}
