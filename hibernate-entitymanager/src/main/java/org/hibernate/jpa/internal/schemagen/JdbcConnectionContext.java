/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.internal.schemagen;

import java.sql.Connection;
import java.sql.SQLException;
import javax.persistence.PersistenceException;

import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.internal.DDLFormatterImpl;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;

import org.jboss.logging.Logger;

/**
 * Defines access to a JDBC Connection for use in Schema generation
 *
 * @author Steve Ebersole
 */
class JdbcConnectionContext {
	private static final Logger log = Logger.getLogger( JdbcConnectionContext.class );

	private final JdbcConnectionAccess jdbcConnectionAccess;
	private final SqlStatementLogger sqlStatementLogger;

	private Connection jdbcConnection;

	JdbcConnectionContext(JdbcConnectionAccess jdbcConnectionAccess, SqlStatementLogger sqlStatementLogger) {
		this.jdbcConnectionAccess = jdbcConnectionAccess;
		this.sqlStatementLogger = sqlStatementLogger;
	}

	public Connection getJdbcConnection() {
		if ( jdbcConnection == null ) {
			try {
				this.jdbcConnection = jdbcConnectionAccess.obtainConnection();
			}
			catch (SQLException e) {
				throw new PersistenceException( "Unable to obtain JDBC Connection", e );
			}
		}
		return jdbcConnection;
	}

	public void release() {
		if ( jdbcConnection != null ) {
			try {
				if ( ! jdbcConnection.getAutoCommit() ) {
					jdbcConnection.commit();
				}
			}
			catch (SQLException e) {
				log.debug( "Unable to commit JDBC transaction used for JPA schema export; may or may not be a problem" );
			}

			try {
				jdbcConnectionAccess.releaseConnection( jdbcConnection );
			}
			catch (SQLException e) {
				throw new PersistenceException( "Unable to release JDBC Connection", e );
			}
		}
	}

	public void logSqlStatement(String sqlStatement) {
		sqlStatementLogger.logStatement( sqlStatement, DDLFormatterImpl.INSTANCE );
	}
}
