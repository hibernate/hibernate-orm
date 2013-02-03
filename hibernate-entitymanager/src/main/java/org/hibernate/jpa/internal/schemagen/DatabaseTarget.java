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

import javax.persistence.PersistenceException;
import java.sql.SQLException;
import java.sql.Statement;

import org.jboss.logging.Logger;

/**
 * GenerationTarget implementation for handling generation directly to the database
 *
 * @see org.hibernate.jpa.SchemaGenTarget#DATABASE
 * @see org.hibernate.jpa.SchemaGenTarget#BOTH
 *
 * @author Steve Ebersole
 */
class DatabaseTarget implements GenerationTarget {
	private static final Logger log = Logger.getLogger( DatabaseTarget.class );

	private final JdbcConnectionContext jdbcConnectionContext;

	private Statement jdbcStatement;

	DatabaseTarget(JdbcConnectionContext jdbcConnectionContext) {
		this.jdbcConnectionContext = jdbcConnectionContext;
	}

	@Override
	public void acceptCreateCommands(Iterable<String> commands) {
		for ( String command : commands ) {
			try {
				jdbcStatement().execute( command );
			}
			catch (SQLException e) {
				throw new PersistenceException(
						"Unable to execute JPA schema generation create command [" + command + "]"
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
		for ( String command : commands ) {
			try {
				jdbcStatement().execute( command );
			}
			catch (SQLException e) {
				throw new PersistenceException(
						"Unable to execute JPA schema generation drop command [" + command + "]"
				);
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
