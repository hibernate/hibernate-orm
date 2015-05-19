/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.internal.schemagen;

import java.sql.SQLException;
import java.sql.Statement;
import javax.persistence.PersistenceException;

import org.hibernate.jpa.SchemaGenAction;

import org.jboss.logging.Logger;

/**
 * GenerationTarget implementation for handling generation directly to the database
 *
 * @author Steve Ebersole
 */
class GenerationTargetToDatabase implements GenerationTarget {
	private static final Logger log = Logger.getLogger( GenerationTargetToDatabase.class );

	private final JdbcConnectionContext jdbcConnectionContext;
	private final SchemaGenAction databaseAction;

	private Statement jdbcStatement;

	GenerationTargetToDatabase(JdbcConnectionContext jdbcConnectionContext, SchemaGenAction databaseAction) {
		this.jdbcConnectionContext = jdbcConnectionContext;
		this.databaseAction = databaseAction;
	}

	@Override
	public void acceptCreateCommands(Iterable<String> commands) {
		if ( !databaseAction.includesCreate() ) {
			return;
		}

		for ( String command : commands ) {
			try {
				jdbcConnectionContext.logSqlStatement( command );
				jdbcStatement().execute( command );
			}
			catch (SQLException e) {
				throw new PersistenceException(
						"Unable to execute JPA schema generation create command [" + command + "]",
						e
				);
			}
		}
	}

	private Statement jdbcStatement() {
		if ( jdbcStatement == null ) {
			try {
				jdbcStatement = jdbcConnectionContext.getJdbcConnection().createStatement();
			}
			catch (SQLException e) {
				throw new PersistenceException( "Unable to generate JDBC Statement object for schema generation" );
			}
		}
		return jdbcStatement;
	}

	@Override
	public void acceptDropCommands(Iterable<String> commands) {
		if ( !databaseAction.includesDrop() ) {
			return;
		}

		for ( String command : commands ) {
			try {
				jdbcConnectionContext.logSqlStatement( command );
				jdbcStatement().execute( command );
			}
			catch (SQLException e) {
				// Just log the error because drop commands are often unsuccessful because the tables do not yet exist...
				log.warnf( "Unable to execute JPA schema generation drop command [" + command + "]", e );
			}
		}
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
	}
}
