package org.hibernate.tool.hbm2ddl;

import org.hibernate.connection.ConnectionProvider;
import org.hibernate.util.JDBCExceptionReporter;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * A {@link ConnectionHelper} implementation based on a provided
 * {@link ConnectionProvider}.  Essentially, ensures that the connection
 * gets cleaned up, but that the provider itself remains usable since it
 * was externally provided to us.
 *
 * @author Steve Ebersole
 */
class SuppliedConnectionProviderConnectionHelper implements ConnectionHelper {
	private ConnectionProvider provider;
	private Connection connection;
	private boolean toggleAutoCommit;

	public SuppliedConnectionProviderConnectionHelper(ConnectionProvider provider) {
		this.provider = provider;
	}

	public void prepare(boolean needsAutoCommit) throws SQLException {
		connection = provider.getConnection();
		toggleAutoCommit = needsAutoCommit && !connection.getAutoCommit();
		if ( toggleAutoCommit ) {
			try {
				connection.commit();
			}
			catch( Throwable ignore ) {
				// might happen with a managed connection
			}
			connection.setAutoCommit( true );
		}
	}

	public Connection getConnection() throws SQLException {
		return connection;
	}

	public void release() throws SQLException {
		// we only release the connection
		if ( connection != null ) {
			JDBCExceptionReporter.logAndClearWarnings( connection );
			if ( toggleAutoCommit ) {
				connection.setAutoCommit( false );
			}
			provider.closeConnection( connection );
			connection = null;
		}
	}
}
