/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.datasource;

import org.hibernate.engine.jdbc.connections.internal.DatasourceConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Map;
import java.util.logging.Logger;

@SuppressWarnings( "unused" ) // used by DatasourceTest in this package
public class TestDataSourceConnectionProvider
		extends DatasourceConnectionProviderImpl
		implements ServiceRegistryAwareService {

	final DriverManagerConnectionProviderImpl delegate = new DriverManagerConnectionProviderImpl();

	@Override
	public void configure(Map<String, Object> configValues) {
		delegate.configure(configValues);
		setDataSource( new DataSource() {
			PrintWriter logWriter = new PrintWriter( System.out );
			@Override
			public Connection getConnection() throws SQLException {
				return delegate.getConnection();
			}

			@Override
			public Connection getConnection(String username, String password) throws SQLException {
				return delegate.getConnection();
			}

			@Override
			public PrintWriter getLogWriter() {
				return logWriter;
			}

			@Override
			public void setLogWriter(PrintWriter out) {
				this.logWriter = out;
			}

			@Override
			public void setLoginTimeout(int seconds) {

			}

			@Override
			public int getLoginTimeout() {
				return -1;
			}

			@Override
			public <T> T unwrap(Class<T> iface) throws SQLException {
				throw new SQLFeatureNotSupportedException();
			}

			@Override
			public boolean isWrapperFor(Class<?> iface) {
				return false;
			}

			@Override
			public Logger getParentLogger() throws SQLFeatureNotSupportedException {
				throw new SQLFeatureNotSupportedException();
			}
		} );
		super.configure( configValues );
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		delegate.injectServices( serviceRegistry );
	}
}
