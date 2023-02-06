package org.hibernate.test.c3p0;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;

import org.hibernate.c3p0.internal.C3P0ConnectionProvider;

import org.hibernate.testing.util.ReflectionUtil;

import org.mockito.Answers;
import org.mockito.MockSettings;
import org.mockito.Mockito;

/**
 * @author Vlad Mihalcea
 */
public class C3P0ProxyConnectionProvider extends C3P0ConnectionProvider {

	private final Map<Connection, Connection> connectionSpyMap = new HashMap<>();

	@Override
	public void configure(Map<String, Object> props) {
		super.configure( props );
		DataSource ds = unwrap( DataSource.class );
		DataSource dataSource = Mockito.mock(
				DataSource.class,
				Mockito.withSettings().defaultAnswer( Answers.CALLS_REAL_METHODS ).spiedInstance( ds )
		);

		try {
			Mockito.doAnswer( invocation -> {
				Connection connection = (Connection) invocation.callRealMethod();
				Connection connectionSpy = Mockito.mock(
						Connection.class,
						Mockito.withSettings().defaultAnswer( Answers.CALLS_REAL_METHODS ).spiedInstance( connection )
				);
				connectionSpyMap.put( connectionSpy, connection );
				return connectionSpy;
			} ).when( dataSource ).getConnection();
		}
		catch (SQLException e) {
			throw new IllegalStateException( e );
		}

		ReflectionUtil.setField( this, "ds", dataSource );
	}

	@Override
	public void closeConnection(Connection conn) throws SQLException {
		Connection originalConnection = connectionSpyMap.get( conn );

		super.closeConnection( originalConnection != null ? originalConnection : conn );
	}

	public Map<Connection, Connection> getConnectionSpyMap() {
		return connectionSpyMap;
	}

	public void clear() {
		connectionSpyMap.clear();
	}
}
