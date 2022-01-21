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
 * Provides access to configuration info passed in {@code Properties} objects.
 * <p>
 * Hibernate has two property scopes:
 * <ul>
 * <li><em>Factory-level</em> properties may be passed to the {@code SessionFactory} when it
 * is instantiated. Each instance might have different property values. If no
 * properties are specified, the factory calls {@code Environment.getProperties()}.
 * <li><em>System-level</em> properties are shared by all factory instances and are always
 * determined by the {@code Environment} properties.
 * </ul>
 * <p>
 * The only system-level property is {@value #USE_REFLECTION_OPTIMIZER}.
 * </p>
 * {@code Environment} properties are populated by calling {@link System#getProperties()}
 * and then from a resource named {@code /hibernate.properties} if it exists. System
 * properties override properties specified in {@code hibernate.properties}.
 * <p>
 * The {@link org.hibernate.SessionFactory} is controlled by the following properties.
 * Properties may be either be {@link System} properties, properties defined in a
 * resource named {@code /hibernate.properties} or an instance of
 * {@link java.util.Properties} passed to {@link Configuration#addProperties(Properties)}.
 * <p>
 * <table>
 * <tr><td><b>property</b></td><td><b>meaning</b></td></tr>
 * <tr>
 *   <td>{@code hibernate.dialect}</td>
 *   <td>classname of {@link org.hibernate.dialect.Dialect} subclass</td>
 * </tr>
 * <tr>
 *   <td>{@code hibernate.connection.provider_class}</td>
 *   <td>name of a {@link org.hibernate.engine.jdbc.connections.spi.ConnectionProvider}
 *   subclass (if not specified heuristics are used)</td>
 * </tr>
 * <tr><td>{@code hibernate.connection.username}</td><td>database username</td></tr>
 * <tr><td>{@code hibernate.connection.password}</td><td>database password</td></tr>
 * <tr>
 *   <td>{@code hibernate.connection.url}</td>
 *   <td>JDBC URL (when using {@link java.sql.DriverManager})</td>
 * </tr>
 * <tr>
 *   <td>{@code hibernate.connection.driver_class}</td>
 *   <td>classname of JDBC driver</td>
 * </tr>
 * <tr>
 *   <td>{@code hibernate.connection.isolation}</td>
 *   <td>JDBC transaction isolation level (only when using
 *     {@link java.sql.DriverManager})
 *   </td>
 * </tr>
 *   <td>{@code hibernate.connection.pool_size}</td>
 *   <td>the maximum size of the connection pool (only when using
 *     {@link java.sql.DriverManager})
 *   </td>
 * </tr>
 * <tr>
 *   <td>{@code hibernate.connection.datasource}</td>
 *   <td>datasource JNDI name (when using {@link javax.sql.DataSource})</td>
 * </tr>
 * <tr>
 *   <td>{@code hibernate.jndi.url}</td><td>JNDI {@link javax.naming.InitialContext} URL</td>
 * </tr>
 * <tr>
 *   <td>{@code hibernate.jndi.class}</td><td>JNDI {@link javax.naming.InitialContext} classname</td>
 * </tr>
 * <tr>
 *   <td>{@code hibernate.max_fetch_depth}</td>
 *   <td>maximum depth of outer join fetching</td>
 * </tr>
 * <tr>
 *   <td>{@code hibernate.jdbc.batch_size}</td>
 *   <td>enable use of JDBC2 batch API for drivers which support it</td>
 * </tr>
 * <tr>
 *   <td>{@code hibernate.jdbc.fetch_size}</td>
 *   <td>set the JDBC fetch size</td>
 * </tr>
 * <tr>
 *   <td>{@code hibernate.jdbc.use_scrollable_resultset}</td>
 *   <td>enable use of JDBC2 scrollable resultsets (you only need to specify
 *   this property when using user supplied connections)</td>
 * </tr>
 * <tr>
 *   <td>{@code hibernate.jdbc.use_getGeneratedKeys}</td>
 *   <td>enable use of JDBC3 PreparedStatement.getGeneratedKeys() to retrieve
 *   natively generated keys after insert. Requires JDBC3+ driver and JRE1.4+</td>
 * </tr>
 * <tr>
 *   <td>{@code hibernate.hbm2ddl.auto}</td>
 *   <td>enable auto DDL export</td>
 * </tr>
 * <tr>
 *   <td>{@code hibernate.default_schema}</td>
 *   <td>use given schema name for unqualified tables (always optional)</td>
 * </tr>
 * <tr>
 *   <td>{@code hibernate.default_catalog}</td>
 *   <td>use given catalog name for unqualified tables (always optional)</td>
 * </tr>
 * <tr>
 *   <td>{@code hibernate.session_factory_name}</td>
 *   <td>If set, the factory attempts to bind this name to itself in the
 *   JNDI context. This name is also used to support cross JVM {@code 
 *   Session} (de)serialization.</td>
 * </tr>
 * <tr>
 *   <td>{@code hibernate.transaction.jta.platform}</td>
 *   <td>classname of {@link org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform}
 *   implementor</td>
 * </tr>
 * </table>
 *
 * @see org.hibernate.SessionFactory
 * @author Gavin King
 */
public final class Environment implements AvailableSettings {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger( CoreMessageLogger.class, Environment.class.getName());

	private static final BytecodeProvider BYTECODE_PROVIDER_INSTANCE;
	private static final boolean ENABLE_REFLECTION_OPTIMIZER;
	private static final boolean ENABLE_LEGACY_PROXY_CLASSNAMES;

	private static final Properties GLOBAL_PROPERTIES;

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
	 * Return {@code System} properties, extended by any properties specified
	 * in {@code hibernate.properties}.
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
