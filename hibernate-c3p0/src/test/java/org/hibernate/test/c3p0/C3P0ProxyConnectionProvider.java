package org.hibernate.test.c3p0;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;

import org.hibernate.c3p0.internal.C3P0ConnectionProvider;

import org.hibernate.testing.util.ReflectionUtil;

import org.mockito.MockSettings;
import org.mockito.Mockito;

/**
 * @author Vlad Mihalcea
 */
public class C3P0ProxyConnectionProvider extends C3P0ConnectionProvider {

	private static final MockSettings VERIFIEABLE_MOCK_SETTINGS = Mockito.withSettings()
			.defaultAnswer( org.mockito.Answers.CALLS_REAL_METHODS );

	private final Map<Connection, Connection> connectionSpyMap = new HashMap<>();

	private static <T> T spy(T subject) {
		return Mockito.mock( (Class<T>) subject.getClass(), VERIFIEABLE_MOCK_SETTINGS.spiedInstance( subject ) );
	}

	@Override
	public void configure(Map props) {
		super.configure( props );
		DataSource ds = unwrap( DataSource.class );
		DataSource dataSource = spy( ds );

		try {
			Mockito.doAnswer( invocation -> {
				Connection connection = (Connection) invocation.callRealMethod();
				Connection connectionSpy = spy( connection );
				connectionSpyMap.put( connectionSpy, connection );
				return connectionSpy;
			} ).when( dataSource ).getConnection();
		}
		catch (SQLException e) {
			throw new IllegalStateException( e );
		}

		ReflectionUtil.setField( C3P0ConnectionProvider.class.cast( this ), "ds", dataSource );
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
