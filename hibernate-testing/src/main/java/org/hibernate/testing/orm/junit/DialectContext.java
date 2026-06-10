/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.junit;

import java.lang.reflect.Constructor;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.spi.DatabaseMetaDataDialectResolutionInfoAdapter;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
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

	static void initDialectClass() {
		final var properties = Environment.getProperties();
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
		final var properties = Environment.getProperties();
		final String driverClassName = properties.getProperty( Environment.DRIVER );
		final String jdbcUrl =
				resolveUrl( properties.containsKey( Environment.URL )
						? properties.getProperty( Environment.URL )
						: properties.getProperty( Environment.JAKARTA_JDBC_URL ) );
		final var props = new Properties();
		resolveFromSettings(properties);
		props.setProperty( "user",
				properties.containsKey( Environment.USER )
						? properties.getProperty( Environment.USER )
						: properties.getProperty( Environment.JAKARTA_JDBC_USER ) );
		props.setProperty( "password",
				properties.containsKey( Environment.PASS )
						? properties.getProperty( Environment.PASS )
						: properties.getProperty( Environment.JAKARTA_JDBC_PASSWORD ) );
		final var dialectClass = getDialectClass();
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
		try ( var connection = driver.connect( jdbcUrl, props ) ) {
//			if ( jdbcUrl.startsWith( "jdbc:derby:" ) ) {
//				// Unfortunately we may only configure this once
//				try ( var s = connection.createStatement() ) {
//					s.execute( "CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY(\'derby.locks.waitTimeout\', \'10\')" );
//					if ( !connection.getAutoCommit() ) {
//						connection.commit();
//					}
//				}
//			}
			dialect = constructor.newInstance( new DatabaseMetaDataDialectResolutionInfoAdapter( connection.getMetaData() ) );
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

	public static void awaitTimestampTick() {
		final long sleepMillis =
				switch ( getDialect().getDefaultTimestampPrecision() ) {
					case 0 -> 1_000;
					case 1 -> 100;
					case 2 -> 10;
					// Instead of waiting 1 millisecond, let's wait 3 to not run into issues with
					// e.g. Sybase, which has a resolution of 1/300th of a second for timestamps
					default -> 3;
				};
		try {
			Thread.sleep( sleepMillis );
		}
		catch (InterruptedException e) {
			// Ignore
		}
	}
}
