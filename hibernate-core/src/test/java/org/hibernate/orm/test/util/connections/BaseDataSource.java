/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.util.connections;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;
import javax.sql.DataSource;

import org.hibernate.cfg.Environment;

import static org.hibernate.testing.jdbc.GradleParallelTestingResolver.*;

/**
 * Simple {@link DataSource} implementation useful in various integration tests,
 * or possibly to be used as base class to extend.
 */
public class BaseDataSource implements DataSource {

	private final Properties connectionProperties;
	private final String url;

	public BaseDataSource(Properties configuration) {
		url = resolveUrl( configuration.getProperty( Environment.URL ) );
		connectionProperties = new Properties();
		resolveFromSettings( configuration );
		connectionProperties.put( "user", configuration.getProperty( Environment.USER ) );
		connectionProperties.put( "password", configuration.getProperty( Environment.PASS ) );
	}

	@Override
	public Connection getConnection() throws SQLException {
		return DriverManager.getConnection( url, connectionProperties );
	}

	@Override
	public Connection getConnection(String username, String password) {
		throw new UnsupportedOperationException( "method not supported" );
	}

	@Override
	public PrintWriter getLogWriter() {
		return new PrintWriter( System.out );
	}

	@Override
	public void setLogWriter(PrintWriter out) {
	}

	@Override
	public void setLoginTimeout(int seconds) {
		throw new UnsupportedOperationException( "method not supported" );
	}

	@Override
	public int getLoginTimeout() {
		throw new UnsupportedOperationException("method not supported");
	}

	@Override
	public <T> T unwrap(Class<T> tClass) {
		return (T) this;
	}

	@Override
	public boolean isWrapperFor(Class<?> aClass) {
		return false;
	}

	@Override
	public Logger getParentLogger() {
		return null;
	}

}
