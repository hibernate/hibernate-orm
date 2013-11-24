/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
