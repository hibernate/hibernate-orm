/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal.exec;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.tool.schema.spi.SchemaManagementException;

/**
 * Basic support for JdbcConnectionContext implementations
 *
 * @author Steve Ebersole
 */
public abstract class AbstractJdbcConnectionContextImpl implements JdbcConnectionContext {
	private final JdbcConnectionAccess jdbcConnectionAccess;
	private final SqlStatementLogger sqlStatementLogger;
	private final boolean needsAutoCommit;

	private Connection jdbcConnection;
	private boolean wasInitiallyAutoCommit;

	public AbstractJdbcConnectionContextImpl(
			JdbcConnectionAccess jdbcConnectionAccess,
			SqlStatementLogger sqlStatementLogger,
			boolean needsAutoCommit) {
		this.jdbcConnectionAccess = jdbcConnectionAccess;
		this.sqlStatementLogger = sqlStatementLogger;
		this.needsAutoCommit = needsAutoCommit;
	}

	@Override
	public Connection getConnection() {
		if ( jdbcConnection == null ) {
			try {
				this.jdbcConnection = jdbcConnectionAccess.obtainConnection();
			}
			catch (SQLException e) {
				throw new SchemaManagementException( "Unable to obtain JDBC Connection", e );
			}
			try {
				if ( needsAutoCommit ) {
					wasInitiallyAutoCommit = jdbcConnection.getAutoCommit();
					jdbcConnection.setAutoCommit( true );
				}
			}
			catch (SQLException e) {
				throw new SchemaManagementException( "Unable to manage auto-commit", e );
			}
		}
		return jdbcConnection;
	}

	@Override
	public void logSqlStatement(String sqlStatement) {
		// we explicitly use no formatting here because the statements we get
		// will already be formatted if need be
		sqlStatementLogger.logStatement( sqlStatement, FormatStyle.NONE.getFormatter() );
	}

	protected void reallyRelease() {
		if ( jdbcConnection != null ) {
			try {
				if ( ! jdbcConnection.getAutoCommit() ) {
					jdbcConnection.commit();
				}
				else {
					// we possibly enabled auto-commit on the Connection, reset if needed
					if ( needsAutoCommit && !wasInitiallyAutoCommit ) {
						jdbcConnection.setAutoCommit( false );
					}
				}
			}
			catch (SQLException e) {
				throw new SchemaManagementException(
						"Unable to reset auto-commit afterQuery schema management;  may or may not be a problem",
						e
				);
			}

			try {
				jdbcConnectionAccess.releaseConnection( jdbcConnection );
			}
			catch (SQLException e) {
				throw new SchemaManagementException( "Unable to release JDBC Connection", e );
			}
		}
	}
}
