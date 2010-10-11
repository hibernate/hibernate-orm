/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.service.jdbc.internal;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.HibernateException;
import org.hibernate.service.jdbc.spi.SQLExceptionHelper;
import org.hibernate.service.jdbc.spi.JdbcResourceRegistry;
import org.hibernate.service.jdbc.spi.InvalidatableWrapper;

/**
 * Standard implementation of the {@link org.hibernate.engine.jdbc.spi.JdbcResourceRegistry} contract
 *
 * @author Steve Ebersole
 */
public class JdbcResourceRegistryImpl implements JdbcResourceRegistry {
	private static final Logger log = LoggerFactory.getLogger( JdbcResourceRegistryImpl.class );

	private final HashMap<Statement,Set<ResultSet>> xref = new HashMap<Statement,Set<ResultSet>>();
	private final Set<ResultSet> unassociatedResultSets = new HashSet<ResultSet>();
	private final SQLExceptionHelper exceptionHelper;

	public JdbcResourceRegistryImpl(SQLExceptionHelper exceptionHelper) {
		this.exceptionHelper = exceptionHelper;
	}

	public void register(Statement statement) {
		log.trace( "registering statement [" + statement + "]" );
		if ( xref.containsKey( statement ) ) {
			throw new HibernateException( "statement already registered with JDBCContainer" );
		}
		xref.put( statement, null );
	}

	public void release(Statement statement) {
		log.trace( "releasing statement [" + statement + "]" );
		Set<ResultSet> resultSets = xref.get( statement );
		if ( resultSets != null ) {
			for ( ResultSet resultSet : resultSets ) {
				close( resultSet );
			}
			resultSets.clear();
		}
		xref.remove( statement );
		close( statement );
	}

	public void register(ResultSet resultSet) {
		log.trace( "registering result set [" + resultSet + "]" );
		Statement statement;
		try {
			statement = resultSet.getStatement();
		}
		catch ( SQLException e ) {
			throw exceptionHelper.convert( e, "unable to access statement from resultset" );
		}
		if ( statement != null ) {
			if ( log.isWarnEnabled() && !xref.containsKey( statement ) ) {
				log.warn( "resultset's statement was not yet registered" );
			}
			Set<ResultSet> resultSets = xref.get( statement );
			if ( resultSets == null ) {
				resultSets = new HashSet<ResultSet>();
				xref.put( statement, resultSets );
			}
			resultSets.add( resultSet );
		}
		else {
			unassociatedResultSets.add( resultSet );
		}
	}

	public void release(ResultSet resultSet) {
		log.trace( "releasing result set [{}]", resultSet );
		Statement statement;
		try {
			statement = resultSet.getStatement();
		}
		catch ( SQLException e ) {
			throw exceptionHelper.convert( e, "unable to access statement from resultset" );
		}
		if ( statement != null ) {
			if ( log.isWarnEnabled() && !xref.containsKey( statement ) ) {
				log.warn( "resultset's statement was not registered" );
			}
			Set<ResultSet> resultSets = xref.get( statement );
			if ( resultSets != null ) {
				resultSets.remove( resultSet );
				if ( resultSets.isEmpty() ) {
					xref.remove( statement );
				}
			}
		}
		else {
			boolean removed = unassociatedResultSets.remove( resultSet );
			if ( !removed ) {
				log.warn( "ResultSet had no statement associated with it, but was not yet registered" );
			}
		}
		close( resultSet );
	}

	public boolean hasRegisteredResources() {
		return ! ( xref.isEmpty() && unassociatedResultSets.isEmpty() );
	}

	public void releaseResources() {
		log.trace( "releasing JDBC container resources [{}]", this );
		cleanup();
	}

	private void cleanup() {
		for ( Map.Entry<Statement,Set<ResultSet>> entry : xref.entrySet() ) {
			if ( entry.getValue() != null ) {
				for ( ResultSet resultSet : entry.getValue() ) {
					close( resultSet );
				}
				entry.getValue().clear();
			}
			close( entry.getKey() );
		}
		xref.clear();

		for ( ResultSet resultSet : unassociatedResultSets ) {
			close( resultSet );
		}
		unassociatedResultSets.clear();
	}

	public void close() {
		log.trace( "closing JDBC container [{}]", this );
		cleanup();
	}

	@SuppressWarnings({ "unchecked" })
	protected void close(Statement statement) {
		log.trace( "closing prepared statement [{}]", statement );

		if ( statement instanceof InvalidatableWrapper ) {
			InvalidatableWrapper<Statement> wrapper = ( InvalidatableWrapper<Statement> ) statement;
			close( wrapper.getWrappedObject() );
			wrapper.invalidate();
			return;
		}

		try {
			// if we are unable to "clean" the prepared statement,
			// we do not close it
			try {
				if ( statement.getMaxRows() != 0 ) {
					statement.setMaxRows( 0 );
				}
				if ( statement.getQueryTimeout() != 0 ) {
					statement.setQueryTimeout( 0 );
				}
			}
			catch( SQLException sqle ) {
				// there was a problem "cleaning" the prepared statement
				log.debug( "Exception clearing maxRows/queryTimeout [{}]", sqle.getMessage() );
				return; // EARLY EXIT!!!
			}
			statement.close();
		}
		catch( SQLException sqle ) {
			log.debug( "Unable to release statement [{}]", sqle.getMessage() );
		}
	}

	@SuppressWarnings({ "unchecked" })
	protected void close(ResultSet resultSet) {
		log.trace( "closing result set [{}]", resultSet );

		if ( resultSet instanceof InvalidatableWrapper ) {
			InvalidatableWrapper<ResultSet> wrapper = (InvalidatableWrapper<ResultSet>) resultSet;
			close( wrapper.getWrappedObject() );
			wrapper.invalidate();
		}

		try {
			resultSet.close();
		}
		catch( SQLException e ) {
			log.debug( "Unable to release result set [{}]", e.getMessage() );
		}
	}
}

