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
