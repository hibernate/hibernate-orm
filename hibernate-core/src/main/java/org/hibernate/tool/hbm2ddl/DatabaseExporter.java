/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.hbm2ddl;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.internal.CoreMessageLogger;

import org.jboss.logging.Logger;

/**
* @author Steve Ebersole
*/
class DatabaseExporter implements Exporter {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger( CoreMessageLogger.class, DatabaseExporter.class.getName() );

	private final ConnectionHelper connectionHelper;
	private final SqlExceptionHelper sqlExceptionHelper;

	private final Connection connection;
	private final Statement statement;

	public DatabaseExporter(ConnectionHelper connectionHelper, SqlExceptionHelper sqlExceptionHelper) throws SQLException {
		this.connectionHelper = connectionHelper;
		this.sqlExceptionHelper = sqlExceptionHelper;

		connectionHelper.prepare( true );
		connection = connectionHelper.getConnection();
		statement = connection.createStatement();
	}

	@Override
	public boolean acceptsImportScripts() {
		return true;
	}

	@Override
	public void export(String string) throws Exception {
		statement.executeUpdate( string );
		try {
			SQLWarning warnings = statement.getWarnings();
			if ( warnings != null) {
				sqlExceptionHelper.logAndClearWarnings( connection );
			}
		}
		catch( SQLException e ) {
			LOG.unableToLogSqlWarnings( e );
		}
	}

	@Override
	public void release() throws Exception {
		try {
			statement.close();
		}
		finally {
			connectionHelper.release();
		}
	}
}
