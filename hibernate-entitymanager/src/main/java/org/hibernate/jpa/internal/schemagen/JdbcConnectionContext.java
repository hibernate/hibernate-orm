/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.jpa.internal.schemagen;

import java.sql.Connection;
import java.sql.SQLException;
import javax.persistence.PersistenceException;

import org.hibernate.engine.jdbc.internal.DDLFormatterImpl;
import org.hibernate.engine.jdbc.spi.JdbcConnectionAccess;
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
