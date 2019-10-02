/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.jdbc;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;
import javax.sql.DataSource;

import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl;

import org.hibernate.testing.env.ConnectionProviderBuilder;

/**
 * @author Steve Ebersole
 */
public class DataSourceStub implements DataSource {
	private final String id;
	private final DriverManagerConnectionProviderImpl connectionProvider;
	private PrintWriter printWriter;

	public DataSourceStub(String id) {
		this.id = id;
		connectionProvider = new DriverManagerConnectionProviderImpl();
		connectionProvider.configure( ConnectionProviderBuilder.getConnectionProviderProperties() );

		printWriter = null;
	}

	public String getId() {
		return id;
	}

	@Override
	public Connection getConnection() throws SQLException {
		return connectionProvider.getConnection();
	}

	@Override
	public Connection getConnection(String username, String password) {
		throw new UnsupportedOperationException();
	}

	@Override
	public PrintWriter getLogWriter() {
		return printWriter;
	}

	@Override
	public void setLogWriter(PrintWriter out) {
		this.printWriter = out;
	}

	@Override
	public void setLoginTimeout(int seconds) {
	}

	@Override
	public int getLoginTimeout() {
		return -1;
	}

	@Override
	public Logger getParentLogger() {
		return Logger.getGlobal();
	}

	@Override
	public <T> T unwrap(Class<T> iface) {
		//noinspection unchecked
		return (T) this;
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) {
		return iface.isAssignableFrom( getClass() );
	}

	@Override
	public String toString() {
		return "DataSourceImpl(" + id + ")";
	}
}
