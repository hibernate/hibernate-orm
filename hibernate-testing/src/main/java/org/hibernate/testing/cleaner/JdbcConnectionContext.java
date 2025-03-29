/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.cleaner;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.Driver;
import java.util.Properties;

import org.hibernate.cfg.AvailableSettings;

/**
 * @author Christian Beikov
 */
public final class JdbcConnectionContext {
	private static final Driver driver;
	private static final String url;
	private static final String user;
	private static final String password;
	private static final Properties properties;
	static {
		final Properties connectionProperties = new Properties();
		try (InputStream inputStream = Thread.currentThread()
				.getContextClassLoader()
				.getResourceAsStream( "hibernate.properties" )) {
			connectionProperties.load( inputStream );
			final String driverClassName = connectionProperties.getProperty(
					AvailableSettings.DRIVER );
			driver = (Driver) Class.forName( driverClassName ).newInstance();
			url = connectionProperties.getProperty(
					AvailableSettings.URL );
			user = connectionProperties.getProperty(
					AvailableSettings.USER );
			password = connectionProperties.getProperty(
					AvailableSettings.PASS );
			Properties p = new Properties();
			if ( user != null ) {
				p.put( "user", user );
			}
			if ( password != null ) {
				p.put( "password", password );
			}
			properties = p;
		}
		catch (Exception e) {
			throw new IllegalArgumentException( e );
		}
	}

	public static void work(ConnectionConsumer work) {
		try (Connection connection = driver.connect( url, properties )) {
			connection.setAutoCommit( false );
			work.consume( connection );
		}
		catch (Exception e) {
			throw new IllegalArgumentException( e );
		}
	}

	public static <R> R workReturning(ConnectionFunction<R> work) {
		try (Connection connection = driver.connect( url, properties )) {
			connection.setAutoCommit( false );
			return work.apply( connection );
		}
		catch (Exception e) {
			throw new IllegalArgumentException( e );
		}
	}

	public static interface ConnectionConsumer {
		void consume(Connection c) throws Exception;
	}
	public static interface ConnectionFunction<R> {
		R apply(Connection c) throws Exception;
	}

	private JdbcConnectionContext() {
	}
}
