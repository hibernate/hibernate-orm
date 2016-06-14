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
import java.util.HashMap;
import java.util.Map;

import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl;

import org.mockito.Mockito;
import org.mockito.internal.util.MockUtil;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;

/**
 * @author Vlad Mihalcea
 */
public class PreparedStatementSpyConnectionProvider extends
		DriverManagerConnectionProviderImpl {

	private final Map<String, PreparedStatement> preparedStatementStatisticsMap = new HashMap<>();

	@Override
	public Connection getConnection() throws SQLException {
		return spy( super.getConnection() );
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
				preparedStatementStatisticsMap.put( sql, statementSpy );
				return statementSpy;
			} ).when( connectionSpy ).prepareStatement( anyString() );
		}
		catch ( SQLException e ) {
			throw new IllegalArgumentException( e );
		}
		return connectionSpy;
	}

	public void clear() {
		preparedStatementStatisticsMap.values().forEach( Mockito::reset );
		preparedStatementStatisticsMap.clear();
	}

	public PreparedStatement getPreparedStatement(String sql) {
		return preparedStatementStatisticsMap.get( sql );
	}
}
