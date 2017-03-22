/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.util.jdbc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

import org.hibernate.testing.jdbc.ConnectionProviderDelegate;

/**
 * This {@link ConnectionProvider} extends any other ConnectionProvider that would be used by default taken the current configuration properties, and it
 * intercept the underlying {@link PreparedStatement} method calls.
 *
 * @author Gail Badner
 */

public class PreparedStatementProxyConnectionProvider extends ConnectionProviderDelegate {

	private final Map<Connection, Connection> acquiredConnectionProxyByConnection = new LinkedHashMap<Connection,Connection>();
	private final PreparedStatementObserver preparedStatementObserver;

	public PreparedStatementProxyConnectionProvider(BasicPreparedStatementObserver preparedStatementObserver) {
		this.preparedStatementObserver = preparedStatementObserver;
	}

	protected Connection actualConnection() throws SQLException {
		return super.getConnection();
	}

	@Override
	public Connection getConnection() throws SQLException {

		Connection actualConnection = actualConnection();
		Connection connectionProxy = acquiredConnectionProxyByConnection.get( actualConnection );
		if ( connectionProxy == null ) {
			connectionProxy = (Connection) Proxy.newProxyInstance(
					ClassLoader.getSystemClassLoader(),
					new Class[] { Connection.class },
					new ConnectionHandler( actualConnection, preparedStatementObserver )
			);
			acquiredConnectionProxyByConnection.put( actualConnection, connectionProxy );
		}
		return connectionProxy;
	}

	@Override
	public void closeConnection(Connection conn) throws SQLException {
		final Connection actualConnection =
				Proxy.isProxyClass( conn.getClass() ) ?
						( (ConnectionHandler) Proxy.getInvocationHandler( conn ) ).actualConnection :
						conn;
		acquiredConnectionProxyByConnection.remove( actualConnection );
		super.closeConnection( actualConnection );
	}

	@Override
	public void stop() {
		clear();
		super.stop();
		preparedStatementObserver.connectionProviderStopped();
	}

	/**
	 * Clears the recorded PreparedStatements.
	 */
	public void clear() {
		acquiredConnectionProxyByConnection.clear();
	}

	private static class ConnectionHandler implements InvocationHandler {

		private final Connection actualConnection;
		private final PreparedStatementObserver preparedStatementObserver;

		ConnectionHandler(Connection actualConnection, PreparedStatementObserver preparedStatementObserver) {
			this.actualConnection = actualConnection;
			this.preparedStatementObserver = preparedStatementObserver;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			final String methodName = method.getName();
			if ( "prepareStatement".equals( methodName ) ) {
				String sql = (String) args[0];
				final PreparedStatement preparedStatement = (PreparedStatement) Proxy.newProxyInstance(
								ClassLoader.getSystemClassLoader(),
								new Class[] { PreparedStatement.class },
								new PreparedStatementHandler( actualConnection.prepareStatement( sql ),
															  preparedStatementObserver
								)
						);
				preparedStatementObserver.preparedStatementCreated( preparedStatement, sql );
				return preparedStatement;
			}
			return method.invoke( actualConnection, args );
		}
	}

	private static class PreparedStatementHandler implements InvocationHandler {
		private final PreparedStatement actualPreparedStatement;
		private final PreparedStatementObserver preparedStatementObserver;

		PreparedStatementHandler(PreparedStatement actualPreparedStatement, PreparedStatementObserver preparedStatementObserver) {
			this.actualPreparedStatement = actualPreparedStatement;
			this.preparedStatementObserver = preparedStatementObserver;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			final Object returnValue = method.invoke( actualPreparedStatement, args );
			preparedStatementObserver.preparedStatementMethodInvoked(
					(PreparedStatement) proxy,
					method,
					args,
					returnValue
			);
			return returnValue;
		}
	}
}
