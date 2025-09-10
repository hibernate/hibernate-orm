/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.jboss.logging.Logger;

import static org.hibernate.testing.jdbc.GradleParallelTestingUsernameResolver.resolveFromSettings;

/**
 * @author Vlad Mihalcea
 */
public class JdbcProperties {

	private static final Logger log = Logger.getLogger( JdbcProperties.class );

	public static final JdbcProperties INSTANCE = new JdbcProperties();

	private final String url;

	private final String user;

	private final String password;

	public JdbcProperties() {
		Properties connectionProperties = new Properties();
		InputStream inputStream = null;
		try {
			inputStream = Thread.currentThread()
					.getContextClassLoader()
					.getResourceAsStream( "hibernate.properties" );
			try {
				connectionProperties.load( inputStream );
				url = connectionProperties.getProperty(
						"hibernate.connection.url" );
				resolveFromSettings(connectionProperties);
				user = connectionProperties.getProperty(
						"hibernate.connection.username" );
				password = connectionProperties.getProperty(
						"hibernate.connection.password" );
			}
			catch ( IOException e ) {
				throw new IllegalArgumentException( e );
			}
		}
		finally {
			try {
				if ( inputStream != null ) {
					inputStream.close();
				}
			}
			catch ( IOException ignore ) {
				log.error( ignore.getMessage() );
			}
		}
	}

	public String getUrl() {
		return url;
	}

	public String getUser() {
		return user;
	}

	public String getPassword() {
		return password;
	}
}
