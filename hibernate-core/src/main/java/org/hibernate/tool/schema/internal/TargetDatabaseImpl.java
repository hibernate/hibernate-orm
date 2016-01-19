/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.tool.schema.spi.SchemaManagementException;

/**
 * @author Steve Ebersole
 */
public class TargetDatabaseImpl extends TargetBase {
	private final JdbcConnectionAccess connectionAccess;

	private Connection connection;
	private Statement statement;

	/**
	 * For testing
	 */
	public TargetDatabaseImpl(JdbcConnectionAccess connectionAccess) {
		this( new ArrayList<Exception>(), true, new SqlStatementLogger(), FormatStyle.DDL.getFormatter(), connectionAccess );
	}

	public TargetDatabaseImpl(List<Exception> exceptions, boolean haltOnError, SqlStatementLogger sqlStatementLogger, Formatter formatter, JdbcConnectionAccess connectionAccess) {
		super( exceptions, haltOnError, sqlStatementLogger, formatter );
		this.connectionAccess = connectionAccess;
	}

	@Override
	public boolean acceptsImportScriptActions() {
		return true;
	}

	@Override
	public void prepare() {
		try {
			connection = connectionAccess.obtainConnection();
			connection.setAutoCommit( true );
		}
		catch (SQLException e) {
			throw new SchemaManagementException( "Unable to open JDBC connection for schema management target", e );
		}

		try {
			statement = connection.createStatement();
		}
		catch (SQLException e) {
			throw new SchemaManagementException( "Unable to create JDBC Statement for schema management target", e );
		}
	}

	@Override
	public void doAccept(String action) {
		try {
			statement.executeUpdate( action );
		}
		catch (SQLException e) {
			throw new SchemaManagementException( "Unable to execute schema management to JDBC target [" + action + "]", e );
		}
	}

	@Override
	public void release() {
		if ( statement != null ) {
			try {
				statement.close();
			}
			catch (SQLException ignore) {
			}
		}
		if ( connection != null ) {
			try {

				connectionAccess.releaseConnection( connection );
			}
			catch (SQLException ignore) {
			}
		}
	}
}
