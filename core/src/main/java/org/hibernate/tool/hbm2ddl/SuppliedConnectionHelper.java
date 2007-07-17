package org.hibernate.tool.hbm2ddl;

import org.hibernate.util.JDBCExceptionReporter;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * A {@link ConnectionHelper} implementation based on an explicitly supplied
 * connection.
 *
 * @author Steve Ebersole
 */
class SuppliedConnectionHelper implements ConnectionHelper {
	private Connection connection;
	private boolean toggleAutoCommit;

	public SuppliedConnectionHelper(Connection connection) {
		this.connection = connection;
	}

	public void prepare(boolean needsAutoCommit) throws SQLException {
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

	public Connection getConnection() {
		return connection;
	}

	public void release() throws SQLException {
		JDBCExceptionReporter.logAndClearWarnings( connection );
		if ( toggleAutoCommit ) {
			connection.setAutoCommit( false );
		}
		connection = null;
	}
}
