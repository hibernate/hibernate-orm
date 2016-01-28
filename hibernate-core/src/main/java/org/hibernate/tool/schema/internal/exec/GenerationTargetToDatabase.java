/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal.exec;

import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.tool.schema.spi.CommandAcceptanceException;
import org.hibernate.tool.schema.spi.SchemaManagementException;

/**
 * GenerationTarget implementation for handling generation directly to the database
 *
 * @author Steve Ebersole
 */
public class GenerationTargetToDatabase implements GenerationTarget {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( GenerationTargetToDatabase.class );

	private final SqlExceptionHelper sqlExceptionHelper;
	private final JdbcConnectionContext jdbcConnectionContext;

	private Statement jdbcStatement;

	public GenerationTargetToDatabase(JdbcConnectionContext jdbcConnectionContext) {
		this( jdbcConnectionContext, new SqlExceptionHelper( true ) );
	}

	public GenerationTargetToDatabase(JdbcConnectionContext jdbcConnectionContext, SqlExceptionHelper sqlExceptionHelper) {
		this.jdbcConnectionContext = jdbcConnectionContext;
		this.sqlExceptionHelper = sqlExceptionHelper;
	}

	@Override
	public void prepare() {
	}

	@Override
	public void accept(String command) {
		try {
			jdbcConnectionContext.logSqlStatement( command );

			final Statement jdbcStatement = jdbcStatement();
			jdbcStatement.execute( command );
			try {
				SQLWarning warnings = jdbcStatement.getWarnings();
				if ( warnings != null) {
					sqlExceptionHelper.logAndClearWarnings( jdbcStatement );
				}
			}
			catch( SQLException e ) {
				log.unableToLogSqlWarnings( e );
			}
		}
		catch (SQLException e) {
			throw new CommandAcceptanceException(
					"Unable to execute command [" + command + "]",
					e
			);
		}
	}

	protected Statement jdbcStatement() {
		if ( jdbcStatement == null ) {
			try {
				jdbcStatement = jdbcConnectionContext.getConnection().createStatement();
			}
			catch (SQLException e) {
				throw new SchemaManagementException(
						"Unable to create JDBC Statement for schema management target",
						e
				);
			}
		}

		return jdbcStatement;
	}

	@Override
	public void release() {
		if ( jdbcStatement != null ) {
			try {
				jdbcStatement.close();
			}
			catch (SQLException e) {
				log.debug( "Unable to close JDBC statement after JPA schema generation : " + e.toString() );
			}
		}
		jdbcStatement = null;

		jdbcConnectionContext.release();
	}
}
