/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.junit;

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Properties;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.hibernate.HibernateError;
import org.hibernate.HibernateException;
import org.hibernate.cfg.Environment;
import org.hibernate.community.dialect.OracleLegacyDialect;
import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.engine.jdbc.dialect.spi.DatabaseMetaDataDialectResolutionInfoAdapter;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.exception.JDBCConnectionException;
import org.hibernate.internal.util.ReflectHelper;

import static org.hibernate.testing.jdbc.GradleParallelTestingResolver.resolveFromSettings;
import static org.hibernate.testing.jdbc.GradleParallelTestingResolver.resolveUrl;

/**
 * @author Christian Beikov
 */
public final class DialectContext {

	private static Class<? extends Dialect> dialectClass;
	private static Dialect dialect;
	private static boolean isOracleRac;

	static void initDialectClass() {
		final Properties properties = Environment.getProperties();
		final String dialectName = properties.getProperty( Environment.DIALECT );
		if ( dialectName == null ) {
			throw new HibernateException( "The dialect was not set. Set the property hibernate.dialect." );
		}
		try {
			dialectClass = (Class<? extends Dialect>) ReflectHelper.classForName( dialectName );
		}
		catch (ClassNotFoundException cnfe) {
			throw new HibernateException( "Dialect class not found: " + dialectName, cnfe );
		}
	}

	static void init() {
		final Properties properties = Environment.getProperties();
		final String driverClassName = properties.getProperty( Environment.DRIVER );
		final String jdbcUrl = resolveUrl( properties.containsKey( Environment.URL )
				? properties.getProperty( Environment.URL )
				: properties.getProperty( Environment.JAKARTA_JDBC_URL ) );
		final Properties props = new Properties();
		resolveFromSettings(properties);
		props.setProperty( "user", properties.containsKey( Environment.USER )
				? properties.getProperty( Environment.USER )
				: properties.getProperty( Environment.JAKARTA_JDBC_USER ) );
		props.setProperty( "password", properties.containsKey( Environment.PASS )
				? properties.getProperty( Environment.PASS )
				: properties.getProperty( Environment.JAKARTA_JDBC_PASSWORD ) );
		final Class<? extends Dialect> dialectClass = getDialectClass();
		final Constructor<? extends Dialect> constructor;
		try {
			constructor = dialectClass.getConstructor( DialectResolutionInfo.class );
		}
		catch (Exception e) {
			throw new HibernateException( "Could not instantiate given dialect class: " + dialectClass, e );
		}
		final Driver driver;
		try {
			driver = (Driver) Class.forName( driverClassName ).newInstance();
		}
		catch (ClassNotFoundException cnfe) {
			throw new HibernateException( "JDBC Driver class not found: " + driverClassName, cnfe );
		}
		catch (Exception e) {
			throw new HibernateException( "Could not instantiate given JDBC driver class: " + driverClassName, e );
		}
		try ( Connection connection = driver.connect( jdbcUrl, props ) ) {
//			if ( jdbcUrl.startsWith( "jdbc:derby:" ) ) {
//				// Unfortunately we may only configure this once
//				try ( Statement s = connection.createStatement() ) {
//					s.execute( "CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY(\'derby.locks.waitTimeout\', \'10\')" );
//					if ( !connection.getAutoCommit() ) {
//						connection.commit();
//					}
//				}
//			}
			dialect = constructor.newInstance( new DatabaseMetaDataDialectResolutionInfoAdapter( connection.getMetaData() ) );
			boolean isOracleRac = false;
			if ( dialect instanceof OracleDialect || dialect instanceof OracleLegacyDialect ) {
				try (Statement statement = connection.createStatement();
					final var resultSet =
							statement.executeQuery( "select count(*) from gv$parameter where name='cpu_count'" )) {
					isOracleRac = resultSet.next()
						&& resultSet.getLong( 1 ) > 1L;
				}
				catch (SQLException ex) {
					// No-op
				}
			}
			DialectContext.isOracleRac = isOracleRac;
		}
		catch (SQLException sqle) {
			throw new JDBCConnectionException( "Could not connect to database with JDBC URL '"
					+ jdbcUrl + "' [" + sqle.getMessage() + "]", sqle );
		}
		catch (Exception e) {
			throw new HibernateException( "Could not connect to database with dialect class: " + dialectClass.getName(), e );
		}
	}

	private DialectContext() {
	}

	public static synchronized Class<? extends Dialect> getDialectClass() {
		if ( dialectClass == null ) {
			initDialectClass();
		}
		return dialectClass;
	}

	public static synchronized Dialect getDialect() {
		if (dialect == null) {
			init();
		}
		return dialect;
	}

	public static synchronized boolean isOracleRAC() {
		if (dialect == null) {
			init();
		}
		return isOracleRac;
	}

	public static void awaitTimestampTick() {
		awaitTimestampTick( getDialect() );
	}

	private static void awaitTimestampTick(Dialect dialect) {
		final long sleepMillis = switch ( dialect.getTypeSizingProfile().defaultTimestampPrecision() ) {
			case 0 -> 1_000;
			case 1 -> 100;
			case 2 -> 10;
			default -> dialect instanceof InformixDialect
					// informix clock has low resolution on Mac, so wait longer
					? 1_200
					// Instead of waiting 1 millisecond, let's wait 3 to not run into issues with e.g. Sybase,
					// which has a resolution of 1/300th of a second for timestamps
					: 3;
		};
		try {
			Thread.sleep( sleepMillis );
		}
		catch (InterruptedException e) {
			throw new HibernateError( "Unexpected wakeup from test sleep" );
		}
	}

	public static Instant awaitServerTimestampTick(SessionFactoryScope scope) {
		return scope.fromSession( DialectContext::awaitServerTimestampTick );
	}

	public static Instant awaitServerTimestampTick(EntityManagerFactoryScope scope) {
		return scope.fromEntityManager( DialectContext::awaitServerTimestampTick );
	}

	private static final int MAX_AWAIT_RETRIES = 100;

	public static Instant awaitServerTimestampTick(EntityManager em) {
		final TypedQuery<Instant> query = em.createQuery( "select instant", Instant.class );
		// Start off with the current instant
		final Instant firstInstant = query.getSingleResult();

		final Dialect dialect = em.unwrap( SessionImplementor.class ).getDialect();
		if ( dialect instanceof MySQLDialect ) {
			// For MySQL we need to sleep 1 second, because the query to retrieve a server timestamp has no fractions
			// See https://hibernate.atlassian.net/browse/HHH-20856 for details
			try {
				Thread.sleep( 1000 );
			}
			catch (InterruptedException e) {
				throw new HibernateError( "Unexpected wakeup from test sleep" );
			}
		}

		// Wait until the instant changes and use this new instant as value to return
		Instant nextInstant;
		int retries = 0;
		do {
			awaitTimestampTick( dialect );
			nextInstant = query.getSingleResult();
			if (retries++ == MAX_AWAIT_RETRIES) {
				throw new HibernateError( "Server timestamp did not tick after " +  MAX_AWAIT_RETRIES + " retries" );
			}
		} while (!nextInstant.isAfter(firstInstant));

		// Wait another time until the instant changes to ensure following statements will happen with a new instant
		final Instant instantToReturn = nextInstant;
		retries = 0;
		do {
			awaitTimestampTick( dialect );
			nextInstant = query.getSingleResult();
			if (retries++ == MAX_AWAIT_RETRIES) {
				throw new HibernateError( "Server timestamp did not tick after " +  MAX_AWAIT_RETRIES + " retries" );
			}
		} while (!nextInstant.isAfter(instantToReturn));
		return instantToReturn;
	}

	public static void awaitHistoryTimestampTick() {
		// Default to sleeping 250 milliseconds like tests that use this were doing before
		try {
			Thread.sleep( 250 );
		}
		catch (InterruptedException e) {
			throw new HibernateError( "Unexpected wakeup from test sleep" );
		}
	}
}
