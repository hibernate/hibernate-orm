/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.connection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import javax.sql.DataSource;

import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl;

import static org.mockito.Mockito.spy;

public class ConnectionProviderDecorator extends UserSuppliedConnectionProviderImpl {

	private final DataSource dataSource;

	private int connectionCount;

	public Connection connection;

	public ConnectionProviderDecorator(){
		String url = Environment.getProperties().getProperty( Environment.URL );

		Properties connectionProps = new Properties();
		connectionProps.put( "user", Environment.getProperties().getProperty( Environment.USER ) );
		connectionProps.put( "password", Environment.getProperties().getProperty( Environment.PASS ) );

		dataSource = new BaseDataSource() {
			@Override
			public Connection getConnection() throws SQLException {
				return DriverManager.getConnection( url, connectionProps );
			}

			@Override
			public Connection getConnection(String username, String password) throws SQLException {
				return DriverManager.getConnection( url, connectionProps );
			}
		};
	}

	@Override
	public Connection getConnection() throws SQLException {
		connectionCount++;
		connection = spy( dataSource.getConnection() );
		return connection;
	}

	@Override
	public void closeConnection(Connection connection) throws SQLException {
		connection.close();
	}

	public int getConnectionCount() {
		return this.connectionCount;
	}

	public void clear() {
		connectionCount = 0;
	}
}

