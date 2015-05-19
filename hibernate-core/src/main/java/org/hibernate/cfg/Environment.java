/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.Version;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ConfigHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;

import org.jboss.logging.Logger;


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
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, Environment.class.getName());

	private static final BytecodeProvider BYTECODE_PROVIDER_INSTANCE;
	private static final boolean ENABLE_BINARY_STREAMS;
	private static final boolean ENABLE_REFLECTION_OPTIMIZER;
	private static final boolean JVM_HAS_TIMESTAMP_BUG;

	private static final Properties GLOBAL_PROPERTIES;
	private static final Map<Integer,String> ISOLATION_LEVELS;

	private static final Map OBSOLETE_PROPERTIES = new HashMap();
	private static final Map RENAMED_PROPERTIES = new HashMap();

	/**
	 * Issues warnings to the user when any obsolete or renamed property names are used.
	 *
	 * @param configurationValues The specified properties.
	 */
	public static void verifyProperties(Map<?,?> configurationValues) {
		final Map propertiesToAdd = new HashMap();
		for ( Map.Entry entry : configurationValues.entrySet() ) {
			final Object replacementKey = OBSOLETE_PROPERTIES.get( entry.getKey() );
			if ( replacementKey != null ) {
				LOG.unsupportedProperty( entry.getKey(), replacementKey );
			}
			final Object renamedKey = RENAMED_PROPERTIES.get( entry.getKey() );
			if ( renamedKey != null ) {
				LOG.renamedProperty( entry.getKey(), renamedKey );
				propertiesToAdd.put( renamedKey, entry.getValue() );
			}
		}
		configurationValues.putAll( propertiesToAdd );
	}

	static {
		Version.logVersion();

		Map<Integer,String> temp = new HashMap<Integer,String>();
		temp.put( Connection.TRANSACTION_NONE, "NONE" );
		temp.put( Connection.TRANSACTION_READ_UNCOMMITTED, "READ_UNCOMMITTED" );
		temp.put( Connection.TRANSACTION_READ_COMMITTED, "READ_COMMITTED" );
		temp.put( Connection.TRANSACTION_REPEATABLE_READ, "REPEATABLE_READ" );
		temp.put( Connection.TRANSACTION_SERIALIZABLE, "SERIALIZABLE" );
		ISOLATION_LEVELS = Collections.unmodifiableMap( temp );
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
		} catch (SecurityException se) {
		    LOG.unableToCopySystemProperties();
		}

		verifyProperties(GLOBAL_PROPERTIES);

		ENABLE_BINARY_STREAMS = ConfigurationHelper.getBoolean(USE_STREAMS_FOR_BINARY, GLOBAL_PROPERTIES);
		if ( ENABLE_BINARY_STREAMS ) {
			LOG.usingStreams();
		}

		ENABLE_REFLECTION_OPTIMIZER = ConfigurationHelper.getBoolean(USE_REFLECTION_OPTIMIZER, GLOBAL_PROPERTIES);
		if ( ENABLE_REFLECTION_OPTIMIZER ) {
			LOG.usingReflectionOptimizer();
		}

		BYTECODE_PROVIDER_INSTANCE = buildBytecodeProvider( GLOBAL_PROPERTIES );

		long x = 123456789;
		JVM_HAS_TIMESTAMP_BUG = new Timestamp(x).getTime() != x;
		if ( JVM_HAS_TIMESTAMP_BUG ) {
			LOG.usingTimestampWorkaround();
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
		return ISOLATION_LEVELS.get( isolation );
	}

	public static BytecodeProvider buildBytecodeProvider(Properties properties) {
		String provider = ConfigurationHelper.getString( BYTECODE_PROVIDER, properties, "javassist" );
		LOG.bytecodeProvider( provider );
		return buildBytecodeProvider( provider );
	}

	private static BytecodeProvider buildBytecodeProvider(String providerName) {
		if ( "javassist".equals( providerName ) ) {
			return new org.hibernate.bytecode.internal.javassist.BytecodeProviderImpl();
		}

		LOG.unknownBytecodeProvider( providerName );
		return new org.hibernate.bytecode.internal.javassist.BytecodeProviderImpl();
	}
}
