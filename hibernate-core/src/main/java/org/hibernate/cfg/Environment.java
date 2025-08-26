/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cfg;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.Internal;
import org.hibernate.Version;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ConfigHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;

import org.jboss.logging.Logger;

/**
 * Provides access to configuration properties passed in {@link Properties} objects.
 * <p>
 * Hibernate has two property scopes:
 * <ul>
 * <li><em>Factory-level</em> properties are specified when a
 * {@link org.hibernate.SessionFactory} is configured and instantiated. Each instance
 * might have different property values.
 * <li><em>System-level</em> properties are shared by all factory instances and are
 * always determined by the {@code Environment} properties in {@link #getProperties()}.
 * </ul>
 * <p>
 * {@code Environment} properties are populated by calling {@link System#getProperties()}
 * and then from a resource named {@code /hibernate.properties}, if it exists. System
 * properties override properties specified in {@code hibernate.properties}.
 * <p>
 * The {@link org.hibernate.SessionFactory} obtains properties from:
 * <ul>
 * <li>{@link System#getProperties() system properties},
 * <li>properties defined in a resource named {@code /hibernate.properties}, and
 * <li>any instance of {@link Properties} passed to {@link Configuration#addProperties}.
 * </ul>
 * <table>
 * <caption>Configuration properties</caption>
 * <tr><td><b>Property</b></td><td><b>Interpretation</b></td></tr>
 * <tr>
 *   <td>{@value #DIALECT}</td>
 *   <td>name of {@link org.hibernate.dialect.Dialect} subclass</td>
 * </tr>
 * <tr>
 *   <td>{@value #CONNECTION_PROVIDER}</td>
 *   <td>name of a {@link org.hibernate.engine.jdbc.connections.spi.ConnectionProvider}
 *   subclass (if not specified, heuristics are used)</td>
 * </tr>
 * <tr><td>{@value #USER}</td><td>database username</td></tr>
 * <tr><td>{@value #PASS}</td><td>database password</td></tr>
 * <tr>
 *   <td>{@value #URL}</td>
 *   <td>JDBC URL (when using {@link java.sql.DriverManager})</td>
 * </tr>
 * <tr>
 *   <td>{@value #DRIVER}</td>
 *   <td>classname of JDBC driver</td>
 * </tr>
 * <tr>
 *   <td>{@value #ISOLATION}</td>
 *   <td>JDBC transaction isolation level (only when using
 *     {@link java.sql.DriverManager})
 *   </td>
 * </tr>
 * <tr>
 *   <td>{@value #POOL_SIZE}</td>
 *   <td>the maximum size of the connection pool (only when using
 *     {@link java.sql.DriverManager})
 *   </td>
 * </tr>
 * <tr>
 *   <td>{@value #DATASOURCE}</td>
 *   <td>datasource JNDI name (when using {@link javax.sql.DataSource})</td>
 * </tr>
 * <tr>
 *   <td>{@value #JNDI_URL}</td><td>JNDI {@link javax.naming.InitialContext} URL</td>
 * </tr>
 * <tr>
 *   <td>{@value #JNDI_CLASS}</td><td>JNDI {@link javax.naming.InitialContext} class name</td>
 * </tr>
 * <tr>
 *   <td>{@value #MAX_FETCH_DEPTH}</td>
 *   <td>maximum depth of outer join fetching</td>
 * </tr>
 * <tr>
 *   <td>{@value #STATEMENT_BATCH_SIZE}</td>
 *   <td>enable use of JDBC2 batch API for drivers which support it</td>
 * </tr>
 * <tr>
 *   <td>{@value #STATEMENT_FETCH_SIZE}</td>
 *   <td>set the JDBC fetch size</td>
 * </tr>
 * <tr>
 *   <td>{@value #USE_GET_GENERATED_KEYS}</td>
 *   <td>enable use of JDBC3 {@link java.sql.PreparedStatement#getGeneratedKeys()}
 *   to retrieve natively generated keys after insert. Requires JDBC3+ driver and
 *   JRE1.4+</td>
 * </tr>
 * <tr>
 *   <td>{@value #HBM2DDL_AUTO}</td>
 *   <td>enable auto DDL export</td>
 * </tr>
 * <tr>
 *   <td>{@value #DEFAULT_SCHEMA}</td>
 *   <td>use given schema name for unqualified tables (always optional)</td>
 * </tr>
 * <tr>
 *   <td>{@value #DEFAULT_CATALOG}</td>
 *   <td>use given catalog name for unqualified tables (always optional)</td>
 * </tr>
 * <tr>
 *   <td>{@value #JTA_PLATFORM}</td>
 *   <td>name of {@link org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform}
 *   implementation</td>
 * </tr>
 * </table>
 *
 * @see org.hibernate.SessionFactory
 *
 * @apiNote This is really considered an internal contract, but leaving in place in this
 * package as many applications use it historically.  However, consider migrating to use
 * {@link AvailableSettings} instead.
 *
 * @author Gavin King
 */
@Internal
public final class Environment implements AvailableSettings {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger( MethodHandles.lookup(), CoreMessageLogger.class, Environment.class.getName());

	private static final Properties GLOBAL_PROPERTIES;

	static {
		Version.logVersion();

		GLOBAL_PROPERTIES = new Properties();

		try {
			InputStream stream = ConfigHelper.getResourceAsStream( "/hibernate.properties" );
			try {
				GLOBAL_PROPERTIES.load(stream);
				LOG.propertiesLoaded( ConfigurationHelper.maskOut( GLOBAL_PROPERTIES,
						PASS, JAKARTA_JDBC_PASSWORD, JPA_JDBC_PASSWORD ) );
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
			final Properties systemProperties = System.getProperties();
			// Must be thread-safe in case an application changes System properties during Hibernate initialization.
			// See HHH-8383.
			synchronized (systemProperties) {
				GLOBAL_PROPERTIES.putAll(systemProperties);
			}
		}
		catch (SecurityException se) {
			LOG.unableToCopySystemProperties();
		}
	}

	/**
	 * Disallow instantiation
	 */
	private Environment() {
		//not to be constructed
	}

	/**
	 * The {@linkplain System#getProperties() system properties}, extended
	 * with all additional properties specified in {@code hibernate.properties}.
	 */
	public static Properties getProperties() {
		final Properties copy = new Properties();
		copy.putAll(GLOBAL_PROPERTIES);
		return copy;
	}

}
