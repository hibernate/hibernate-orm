/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.util;

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
public class JdbcStatisticsConnectionProvider extends
		DriverManagerConnectionProviderImpl {

	public static class PreparedStatementStatistics {
		private final String sql;

		private int executeUpdateCount;

		private int addBatchCount;

		private int executeBatchCount;

		public PreparedStatementStatistics(String sql) {
			this.sql = sql;
		}

		public int getExecuteUpdateCount() {
			return executeUpdateCount;
		}

		public int getAddBatchCount() {
			return addBatchCount;
		}

		public int getExecuteBatchCount() {
			return executeBatchCount;
		}
	}

	private final Map<PreparedStatement, PreparedStatementStatistics> preparedStatementStatisticsMap = new HashMap<>();

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
				preparedStatementStatisticsMap.putIfAbsent( statementSpy,
															new PreparedStatementStatistics(
																	(String) invocation
																			.getArguments()[0] )
				);

				doAnswer( _invocation -> {
					Object mock = _invocation.getMock();
					preparedStatementStatisticsMap.get( mock ).executeUpdateCount++;
					return _invocation.callRealMethod();
				} ).when( statementSpy ).executeUpdate();

				doAnswer( _invocation -> {
					Object mock = _invocation.getMock();
					preparedStatementStatisticsMap.get( mock ).addBatchCount++;
					return _invocation.callRealMethod();
				} ).when( statementSpy ).addBatch();

				doAnswer( _invocation -> {
					Object mock = _invocation.getMock();
					preparedStatementStatisticsMap.get( mock ).executeBatchCount++;
					return _invocation.callRealMethod();
				} ).when( statementSpy ).executeBatch();

				return statementSpy;
			} ).when( connectionSpy ).prepareStatement( anyString() );
		}
		catch ( SQLException e ) {
			e.printStackTrace();
		}
		return connectionSpy;
	}

	public void clear() {
		preparedStatementStatisticsMap.clear();
	}

	public Map<String, PreparedStatementStatistics> getPreparedStatementStatistics() {
		Map<String, PreparedStatementStatistics> statisticsMap = new HashMap<>();
		preparedStatementStatisticsMap.values()
				.stream()
				.forEach( stats -> statisticsMap.put( stats.sql, stats ) );
		return statisticsMap;
	}
}
