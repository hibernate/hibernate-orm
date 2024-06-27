/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

import org.hibernate.testing.jdbc.ConnectionProviderDelegate;
import org.hibernate.testing.jdbc.JdbcSpies;


/**
 * This {@link ConnectionProvider} extends any other ConnectionProvider that would be used by default taken the current configuration properties, and it
 * intercept the underlying {@link PreparedStatement} method calls.
 *
 * @author Vlad Mihalcea
 */
public class PreparedStatementSpyConnectionProvider
		extends ConnectionProviderDelegate {
	public final JdbcSpies.SpyContext spyContext = new JdbcSpies.SpyContext();

	private final List<Connection> acquiredConnections = new ArrayList<>( );
	private final List<Connection> releasedConnections = new ArrayList<>( );

	public PreparedStatementSpyConnectionProvider() {
	}

	protected Connection actualConnection() throws SQLException {
		return super.getConnection();
	}

	@Override
	public Connection getConnection() throws SQLException {
		Connection connection = spy( actualConnection() );
		acquiredConnections.add( connection );
		return connection;
	}

	@Override
	public void closeConnection(Connection connection) throws SQLException {
		acquiredConnections.remove( connection );
		releasedConnections.add( connection );
		super.closeConnection( spyContext.getSpiedInstance( connection ) );
	}

	@Override
	public void stop() {
		clear();
		super.stop();
	}

	private Connection spy(Connection connection) {
		return JdbcSpies.spy( connection, spyContext );
	}

	/**
	 * Clears the recorded PreparedStatements and reset the associated Mocks.
	 */
	public void clear() {
		acquiredConnections.clear();
		releasedConnections.clear();
		spyContext.clear();
	}

	/**
	 * Get a list of current acquired Connections.
	 * @return list of current acquired Connections
	 */
	public List<Connection> getAcquiredConnections() {
		return acquiredConnections;
	}

	/**
	 * Get a list of current released Connections.
	 * @return list of current released Connections
	 */
	public List<Connection> getReleasedConnections() {
		return releasedConnections;
	}
}
