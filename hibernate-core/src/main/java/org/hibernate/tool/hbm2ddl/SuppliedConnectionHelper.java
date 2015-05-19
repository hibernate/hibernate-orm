/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.hbm2ddl;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;

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
		new SqlExceptionHelper().logAndClearWarnings( connection );
		if ( toggleAutoCommit ) {
			connection.setAutoCommit( false );
		}
		connection = null;
	}
}
