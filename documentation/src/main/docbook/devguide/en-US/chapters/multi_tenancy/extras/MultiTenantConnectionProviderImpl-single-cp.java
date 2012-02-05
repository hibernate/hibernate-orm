/**
 * Simplisitc implementation for illustration purposes showing a single connection pool used to serve
 * multiple schemas using "connection altering".  Here we use the T-SQL specific USE command; Oracle
 * users might use the ALTER SESSION SET SCHEMA command; etc.
 */
public class MultiTenantConnectionProviderImpl
		implements MultiTenantConnectionProvider, Stoppable {
	private final ConnectionProvider connectionProvider = ConnectionProviderUtils.buildConnectionProvider( "master" );

	@Override
	public Connection getAnyConnection() throws SQLException {
		return connectionProvider.getConnection();
	}

	@Override
	public void releaseAnyConnection(Connection connection) throws SQLException {
		connectionProvider.closeConnection( connection );
	}

	@Override
	public Connection getConnection(String tenantIdentifier) throws SQLException {
		final Connection connection = getAnyConnection();
		try {
			connection.createStatement().execute( "USE " + tenanantIdentifier );
		}
		catch ( SQLException e ) {
			throw new HibernateException(
					"Could not alter JDBC connection to specified schema [" +
							tenantIdentifier + "]",
					e
			);
		}
		return connection;
	}

	@Override
	public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
		try {
			connection.createStatement().execute( "USE master" );
		}
		catch ( SQLException e ) {
			// on error, throw an exception to make sure the connection is not returned to the pool.
			// your requirements may differ
			throw new HibernateException(
					"Could not alter JDBC connection to specified schema [" +
							tenantIdentifier + "]",
					e
			);
		}
		connectionProvider.closeConnection( connection );
	}

	...
}