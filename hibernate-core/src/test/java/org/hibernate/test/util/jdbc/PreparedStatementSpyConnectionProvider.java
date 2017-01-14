/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.util.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

import org.mockito.Mockito;
import org.mockito.internal.util.MockUtil;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;

/**
 * This {@link ConnectionProvider} extends any other ConnectionProvider that would be used by default taken the current configuration properties, and it
 * intercept the underlying {@link PreparedStatement} method calls.
 *
 * @author Vlad Mihalcea
 */
public class PreparedStatementSpyConnectionProvider
		extends ConnectionProviderDelegate {

	private final Map<PreparedStatement, String> preparedStatementMap = new LinkedHashMap<>();

	private final List<String> executeStatements = new ArrayList<>();
	private final List<String> executeUpdateStatements = new ArrayList<>();

	private final List<Connection> acquiredConnections = new ArrayList<>( );

	@Override
	public Connection getConnection() throws SQLException {
		Connection connection = spy( super.getConnection() );
		acquiredConnections.add( connection );
		return connection;
	}

	@Override
	public void closeConnection(Connection conn) throws SQLException {
		acquiredConnections.remove( conn );
		super.closeConnection( conn );
	}

	private Connection spy(Connection connection) {
		if ( new MockUtil().isMock( connection ) ) {
			return connection;
		}
		Connection connectionSpy = Mockito.spy( connection );
		try {
			doAnswer( invocation -> {
				PreparedStatement statement = (PreparedStatement) invocation.callRealMethod();
				PreparedStatement statementSpy = Mockito.spy( statement );
				String sql = (String) invocation.getArguments()[0];
				preparedStatementMap.put( statementSpy, sql );
				return statementSpy;
			} ).when( connectionSpy ).prepareStatement( anyString() );

			doAnswer( invocation -> {
				Statement statement = (Statement) invocation.callRealMethod();
				Statement statementSpy = Mockito.spy( statement );
				doAnswer( statementInvocation -> {
					String sql = (String) statementInvocation.getArguments()[0];
					executeStatements.add( sql );
					return statementInvocation.callRealMethod();
			    }).when( statementSpy ).execute( anyString() );
				doAnswer( statementInvocation -> {
					String sql = (String) statementInvocation.getArguments()[0];
					executeUpdateStatements.add( sql );
					return statementInvocation.callRealMethod();
				}).when( statementSpy ).executeUpdate( anyString() );
				return statementSpy;
			} ).when( connectionSpy ).createStatement();
		}
		catch ( SQLException e ) {
			throw new IllegalArgumentException( e );
		}
		return connectionSpy;
	}

	/**
	 * Clears the recorded PreparedStatements and reset the associated Mocks.
	 */
	public void clear() {
		acquiredConnections.clear();
		preparedStatementMap.keySet().forEach( Mockito::reset );
		preparedStatementMap.clear();
	}

	/**
	 * Get one and only one PreparedStatement associated to the given SQL statement.
	 *
	 * @param sql SQL statement.
	 *
	 * @return matching PreparedStatement.
	 *
	 * @throws IllegalArgumentException If there is no matching PreparedStatement or multiple instances, an exception is being thrown.
	 */
	public PreparedStatement getPreparedStatement(String sql) {
		List<PreparedStatement> preparedStatements = getPreparedStatements( sql );
		if ( preparedStatements.isEmpty() ) {
			throw new IllegalArgumentException(
					"There is no PreparedStatement for this SQL statement " + sql );
		}
		else if ( preparedStatements.size() > 1 ) {
			throw new IllegalArgumentException( "There are " + preparedStatements
					.size() + " PreparedStatements for this SQL statement " + sql );
		}
		return preparedStatements.get( 0 );
	}

	/**
	 * Get the PreparedStatements that are associated to the following SQL statement.
	 *
	 * @param sql SQL statement.
	 *
	 * @return list of recorded PreparedStatements matching the SQL statement.
	 */
	public List<PreparedStatement> getPreparedStatements(String sql) {
		return preparedStatementMap.entrySet()
				.stream()
				.filter( entry -> entry.getValue().equals( sql ) )
				.map( Map.Entry::getKey )
				.collect( Collectors.toList() );
	}

	/**
	 * Get the PreparedStatements that were executed since the last clear operation.
	 *
	 * @return list of recorded PreparedStatements.
	 */
	public List<PreparedStatement> getPreparedStatements() {
		return new ArrayList<>( preparedStatementMap.keySet() );
	}

	/**
	 * Get the SQL statements that were executed since the last clear operation.
	 * @return list of recorded update statements.
	 */
	public List<String> getExecuteStatements() {
		return executeStatements;
	}

	/**
	 * Get the SQL update statements that were executed since the last clear operation.
	 * @return list of recorded update statements.
	 */
	public List<String> getExecuteUpdateStatements() {
		return executeUpdateStatements;
	}

	/**
	 * Get a list of current acquired Connections.
	 * @return list of current acquired Connections
	 */
	public List<Connection> getAcquiredConnections() {
		return acquiredConnections;
	}
}
