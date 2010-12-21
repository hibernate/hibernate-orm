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
package org.hibernate.engine.jdbc.internal;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.TRACE;
import static org.jboss.logging.Logger.Level.WARN;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.spi.InvalidatableWrapper;
import org.hibernate.engine.jdbc.spi.JdbcResourceRegistry;
import org.hibernate.engine.jdbc.spi.JdbcWrapper;
import org.hibernate.engine.jdbc.spi.SQLExceptionHelper;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Logger.Level;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

/**
 * Standard implementation of the {@link org.hibernate.engine.jdbc.spi.JdbcResourceRegistry} contract
 *
 * @author Steve Ebersole
 */
public class JdbcResourceRegistryImpl implements JdbcResourceRegistry {

    private static final Logger LOG = org.jboss.logging.Logger.getMessageLogger(Logger.class,
                                                                                JdbcResourceRegistryImpl.class.getPackage().getName());

	private final HashMap<Statement,Set<ResultSet>> xref = new HashMap<Statement,Set<ResultSet>>();
	private final Set<ResultSet> unassociatedResultSets = new HashSet<ResultSet>();
	private final SQLExceptionHelper exceptionHelper;

	private Statement lastQuery;

	public JdbcResourceRegistryImpl(SQLExceptionHelper exceptionHelper) {
		this.exceptionHelper = exceptionHelper;
	}

	public void register(Statement statement) {
        LOG.registeringStatement(statement);
		if ( xref.containsKey( statement ) ) {
			throw new HibernateException( "statement already registered with JDBCContainer" );
		}
		xref.put( statement, null );
	}

	@SuppressWarnings({ "unchecked" })
	public void registerLastQuery(Statement statement) {
		log.trace( "registering last query statement [{}]", statement );		
		if ( statement instanceof JdbcWrapper ) {
			JdbcWrapper<Statement> wrapper = ( JdbcWrapper<Statement> ) statement;
			registerLastQuery( wrapper.getWrappedObject() );
			return;
		}
		lastQuery = statement;
	}

	public void cancelLastQuery() {
		try {
			if (lastQuery != null) {
				lastQuery.cancel();
			}
		}
		catch (SQLException sqle) {
			throw exceptionHelper.convert(
			        sqle,
			        "Cannot cancel query"
				);
		}
		finally {
			lastQuery = null;
		}
	}

	public void release(Statement statement) {
        LOG.releasingStatement(statement);
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
        LOG.registeringResultSet(resultSet);
		Statement statement;
		try {
			statement = resultSet.getStatement();
		}
		catch ( SQLException e ) {
			throw exceptionHelper.convert( e, "unable to access statement from resultset" );
		}
		if ( statement != null ) {
            if (LOG.isEnabled(Level.WARN) && !xref.containsKey(statement)) LOG.unregisteredStatement();
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
        LOG.releasingResultSet(resultSet);
		Statement statement;
		try {
			statement = resultSet.getStatement();
		}
		catch ( SQLException e ) {
			throw exceptionHelper.convert( e, "unable to access statement from resultset" );
		}
		if ( statement != null ) {
            if (LOG.isEnabled(Level.WARN) && !xref.containsKey(statement)) LOG.unregisteredStatement();
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
            if (!removed) LOG.unregisteredResultSetWithoutStatement();
		}
		close( resultSet );
	}

	public boolean hasRegisteredResources() {
		return ! xref.isEmpty() || ! unassociatedResultSets.isEmpty();
	}

	public void releaseResources() {
        LOG.releasingJdbcContainerResources(this);
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

		// TODO: can ConcurrentModificationException still happen???
		// Following is from old AbstractBatcher...
		/*
		Iterator iter = resultSetsToClose.iterator();
		while ( iter.hasNext() ) {
			try {
				logCloseResults();
				( ( ResultSet ) iter.next() ).close();
			}
			catch ( SQLException e ) {
				// no big deal
				log.warn( "Could not close a JDBC result set", e );
			}
			catch ( ConcurrentModificationException e ) {
				// this has been shown to happen occasionally in rare cases
				// when using a transaction manager + transaction-timeout
				// where the timeout calls back through Hibernate's
				// registered transaction synchronization on a separate
				// "reaping" thread.  In cases where that reaping thread
				// executes through this block at the same time the main
				// application thread does we can get into situations where
				// these CMEs occur.  And though it is not "allowed" per-se,
				// the end result without handling it specifically is infinite
				// looping.  So here, we simply break the loop
				log.info( "encountered CME attempting to release batcher; assuming cause is tx-timeout scenario and ignoring" );
				break;
			}
			catch ( Throwable e ) {
				// sybase driver (jConnect) throwing NPE here in certain
				// cases, but we'll just handle the general "unexpected" case
				log.warn( "Could not close a JDBC result set", e );
			}
		}
		resultSetsToClose.clear();

		iter = statementsToClose.iterator();
		while ( iter.hasNext() ) {
			try {
				closeQueryStatement( ( PreparedStatement ) iter.next() );
			}
			catch ( ConcurrentModificationException e ) {
				// see explanation above...
				log.info( "encountered CME attempting to release batcher; assuming cause is tx-timeout scenario and ignoring" );
				break;
			}
			catch ( SQLException e ) {
				// no big deal
				log.warn( "Could not close a JDBC statement", e );
			}
		}
		statementsToClose.clear();
        */
	}

	public void close() {
        LOG.closingJdbcContainer(this);
		cleanup();
	}

	@SuppressWarnings({ "unchecked" })
	protected void close(Statement statement) {
        LOG.closingPreparedStatement(statement);

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
                LOG.unableToClearMaxRowsQueryTimeout(sqle.getMessage());
				return; // EARLY EXIT!!!
			}
			statement.close();
			if ( lastQuery == statement ) {
				lastQuery = null;
			}
		}
		catch( SQLException sqle ) {
            LOG.unableToReleaseStatement(sqle.getMessage());
		}
	}

	@SuppressWarnings({ "unchecked" })
	protected void close(ResultSet resultSet) {
        LOG.closingResultSet(resultSet);

		if ( resultSet instanceof InvalidatableWrapper ) {
			InvalidatableWrapper<ResultSet> wrapper = (InvalidatableWrapper<ResultSet>) resultSet;
			close( wrapper.getWrappedObject() );
			wrapper.invalidate();
		}

		try {
			resultSet.close();
		}
		catch( SQLException e ) {
            LOG.unableToReleaseResultSet(e.getMessage());
		}
	}

    /**
     * Interface defining messages that may be logged by the outer class
     */
    @MessageLogger
    interface Logger extends BasicLogger {

        @LogMessage( level = TRACE )
        @Message( value = "Closing JDBC container [%s]" )
        void closingJdbcContainer( JdbcResourceRegistryImpl jdbcResourceRegistryImpl );

        @LogMessage( level = TRACE )
        @Message( value = "Closing prepared statement [%s]" )
        void closingPreparedStatement( Statement statement );

        @LogMessage( level = TRACE )
        @Message( value = "Closing result set [%s]" )
        void closingResultSet( ResultSet resultSet );

        @LogMessage( level = TRACE )
        @Message( value = "Registering result set [%s]" )
        void registeringResultSet( ResultSet resultSet );

        @LogMessage( level = TRACE )
        @Message( value = "Registering statement [%s]" )
        void registeringStatement( Statement statement );

        @LogMessage( level = TRACE )
        @Message( value = "Releasing JDBC container resources [%s]" )
        void releasingJdbcContainerResources( JdbcResourceRegistryImpl jdbcResourceRegistryImpl );

        @LogMessage( level = TRACE )
        @Message( value = "Releasing result set [%s]" )
        void releasingResultSet( ResultSet resultSet );

        @LogMessage( level = TRACE )
        @Message( value = "Releasing statement [%s]" )
        void releasingStatement( Statement statement );

        @LogMessage( level = DEBUG )
        @Message( value = "Exception clearing maxRows/queryTimeout [%s]" )
        void unableToClearMaxRowsQueryTimeout( String message );

        @LogMessage( level = DEBUG )
        @Message( value = "Unable to release result set [%s]" )
        void unableToReleaseResultSet( String message );

        @LogMessage( level = DEBUG )
        @Message( value = "Unable to release statement [%s]" )
        void unableToReleaseStatement( String message );

        @LogMessage( level = WARN )
        @Message( value = "ResultSet's statement was not registered" )
        void unregisteredStatement();

        @LogMessage( level = WARN )
        @Message( value = "ResultSet had no statement associated with it, but was not yet registered" )
        void unregisteredResultSetWithoutStatement();
    }
}
