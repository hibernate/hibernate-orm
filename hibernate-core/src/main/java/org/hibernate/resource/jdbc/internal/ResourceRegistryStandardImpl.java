/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.jdbc.internal;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;

import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.resource.jdbc.ResourceRegistry;
import org.hibernate.resource.jdbc.spi.JdbcEventHandler;

/**
 * Helps to track {@link Statement}s and {@link ResultSet}s which need to be closed.
 * This class is not threadsafe.
 * <p>
 * Note regarding performance: we had evidence that allocating {@code Iterator}s
 * to implement the cleanup on each element recursively was the dominant
 * resource cost, so we decided to use "for each" and lambdas in this case.
 * However, the "for each"/lambda combination is able to dodge allocating
 * {@code Iterator}s on {@code HashMap} and {@code ArrayList}, but not on {@code HashSet} (at least on JDK8 and 11).
 * Therefore some types which should ideally be modelled as a {@code Set} have
 * been implemented using {@code HashMap}.
 *
 * @author Steve Ebersole
 * @author Sanne Grinovero
 */
public final class ResourceRegistryStandardImpl implements ResourceRegistry {

	private static final CoreMessageLogger log = CoreLogging.messageLogger( ResourceRegistryStandardImpl.class );

	// Dummy value to associate with an Object in the backing Map when we use it as a set:
	private static final Object PRESENT = new Object();

	//Used instead of Collections.EMPTY_SET to avoid polymorphic calls on xref;
	//Also, uses an HashMap as it were an HashSet, as technically we just need the Set semantics
	//but in this case the overhead of HashSet is not negligible.
	private static final HashMap<ResultSet,Object> EMPTY = new HashMap<>( 1, 0.2f );

	private final JdbcEventHandler jdbcEventHandler;

	private final HashMap<Statement, HashMap<ResultSet,Object>> xref = new HashMap<>();
	private HashMap<ResultSet,Object> unassociatedResultSets;

	private ArrayList<Blob> blobs;
	private ArrayList<Clob> clobs;
	private ArrayList<NClob> nclobs;

	private Statement lastQuery;

	public ResourceRegistryStandardImpl() {
		this( null );
	}

	public ResourceRegistryStandardImpl(JdbcEventHandler jdbcEventHandler) {
		this.jdbcEventHandler = jdbcEventHandler;
	}

	@Override
	public boolean hasRegisteredResources() {
		return hasRegistered( xref )
			|| hasRegistered( unassociatedResultSets )
			|| hasRegistered( blobs )
			|| hasRegistered( clobs )
			|| hasRegistered( nclobs );
	}

	@Override
	public void register(Statement statement, boolean cancelable) {
		log.tracef( "Registering statement [%s]", statement );

		HashMap<ResultSet,Object> previousValue = xref.putIfAbsent( statement, EMPTY );
		if ( previousValue != null ) {
			throw new HibernateException( "JDBC Statement already registered" );
		}

		if ( cancelable ) {
			lastQuery = statement;
		}
	}

	@Override
	public void release(Statement statement) {
		log.tracev( "Releasing statement [{0}]", statement );

		final HashMap<ResultSet,Object> resultSets = xref.remove( statement );
		if ( resultSets != null ) {
			closeAll( resultSets );
		}
		else {
			// Keep this at DEBUG level, rather than warn.  Numerous connection pool implementations can return a
			// proxy/wrapper around the JDBC Statement, causing excessive logging here.  See HHH-8210.
			log.unregisteredStatement();
		}

		close( statement );

		if ( lastQuery == statement ) {
			lastQuery = null;
		}
	}

	@Override
	public void release(ResultSet resultSet, Statement statement) {
		log.tracef( "Releasing result set [%s]", resultSet );

		if ( statement == null ) {
			try {
				statement = resultSet.getStatement();
			}
			catch (SQLException e) {
				throw convert( e, "unable to access Statement from ResultSet" );
			}
		}
		if ( statement != null ) {
			final HashMap<ResultSet,Object> resultSets = xref.get( statement );
			if ( resultSets == null ) {
				log.unregisteredStatement();
			}
			else {
				resultSets.remove( resultSet );
				if ( resultSets.isEmpty() ) {
					try {
						if ( statement.isClosed() ) {
							xref.remove( statement );
						}
					}
					catch (SQLException e) {
						log.debugf( "Unable to release JDBC statement [%s]", e.getMessage() );
					}
				}
			}
		}
		else {
			final Object removed = unassociatedResultSets == null ? null : unassociatedResultSets.remove( resultSet );
			if ( removed == null ) {
				log.unregisteredResultSetWithoutStatement();
			}
		}
		close( resultSet );
	}

	private static void closeAll(final HashMap<ResultSet,Object> resultSets) {
		if ( resultSets == null ) {
			return;
		}
		resultSets.forEach( (resultSet, o) -> close( resultSet ) );
		resultSets.clear();
	}

	private static void releaseXref(final Statement s, final HashMap<ResultSet, Object> r) {
		closeAll( r );
		close( s );
	}

	private static void close(final ResultSet resultSet) {
		log.tracef( "Closing result set [%s]", resultSet );

		try {
			if ( resultSet != null ) {
				resultSet.close();
			}
		}
		catch (SQLException e) {
			log.debugf( "Unable to release JDBC result set [%s]", e.getMessage() );
		}
		catch (Exception e) {
			// try to handle general errors more elegantly
			log.debugf( "Unable to release JDBC result set [%s]", e.getMessage() );
		}
	}

	private static void close(Statement statement) {
		log.tracef( "Closing prepared statement [%s]", statement );

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
			catch (SQLException sqle) {
				// there was a problem "cleaning" the prepared statement
				if ( log.isDebugEnabled() ) {
					log.debugf( "Exception clearing maxRows/queryTimeout [%s]", sqle.getMessage() );
				}
				// EARLY EXIT!!!
				return;
			}
			statement.close();
		}
		catch (SQLException e) {
			log.debugf( "Unable to release JDBC statement [%s]", e.getMessage() );
		}
		catch (Exception e) {
			// try to handle general errors more elegantly
			log.debugf( "Unable to release JDBC statement [%s]", e.getMessage() );
		}
	}

	@Override
	public void register(ResultSet resultSet, Statement statement) {
		log.tracef( "Registering result set [%s]", resultSet );

		if ( statement == null ) {
			try {
				statement = resultSet.getStatement();
			}
			catch (SQLException e) {
				throw convert( e, "unable to access Statement from ResultSet" );
			}
		}
		if ( statement != null ) {
			HashMap<ResultSet,Object> resultSets = xref.get( statement );

			// Keep this at DEBUG level, rather than warn.  Numerous connection pool implementations can return a
			// proxy/wrapper around the JDBC Statement, causing excessive logging here.  See HHH-8210.
			if ( resultSets == null ) {
				log.debug( "ResultSet statement was not registered (on register)" );
			}

			if ( resultSets == null || resultSets == EMPTY ) {
				resultSets = new HashMap<>();
				xref.put( statement, resultSets );
			}
			resultSets.put( resultSet, PRESENT );
		}
		else {
			if ( unassociatedResultSets == null ) {
				this.unassociatedResultSets = new HashMap<>();
			}
			unassociatedResultSets.put( resultSet, PRESENT );
		}
	}

	private JDBCException convert(SQLException e, String s) {
		return new JDBCException( s, e );
	}

	@Override
	public void register(Blob blob) {
		if ( blobs == null ) {
			blobs = new ArrayList<>();
		}

		blobs.add( blob );
	}

	@Override
	public void release(Blob blob) {
		if ( blobs == null ) {
			log.debug( "Request to release Blob, but appears no Blobs have ever been registered" );
			return;
		}
		blobs.remove( blob );
	}

	@Override
	public void register(Clob clob) {
		if ( clobs == null ) {
			clobs = new ArrayList<>();
		}
		clobs.add( clob );
	}

	@Override
	public void release(Clob clob) {
		if ( clobs == null ) {
			log.debug( "Request to release Clob, but appears no Clobs have ever been registered" );
			return;
		}
		clobs.remove( clob );
	}

	@Override
	public void register(NClob nclob) {
		// todo : just store them in clobs?
		if ( nclobs == null ) {
			nclobs = new ArrayList<>();
		}
		nclobs.add( nclob );
	}

	@Override
	public void release(NClob nclob) {
		// todo : just store them in clobs?
		if ( nclobs == null ) {
			log.debug( "Request to release NClob, but appears no NClobs have ever been registered" );
			return;
		}
		nclobs.remove( nclob );
	}

	@Override
	public void cancelLastQuery() {
		try {
			if ( lastQuery != null ) {
				lastQuery.cancel();
			}
		}
		catch (SQLException e) {
			throw convert( e, "Cannot cancel query" );
		}
		finally {
			lastQuery = null;
		}
	}

	@Override
	public void releaseResources() {
		log.trace( "Releasing JDBC resources" );

		if ( jdbcEventHandler != null ) {
			jdbcEventHandler.jdbcReleaseRegistryResourcesStart();
		}

		xref.forEach( ResourceRegistryStandardImpl::releaseXref );
		xref.clear();

		closeAll( unassociatedResultSets );

		if ( blobs != null ) {
			blobs.forEach( blob -> {
				try {
					blob.free();
				}
				catch (SQLException e) {
					log.debugf( "Unable to free JDBC Blob reference [%s]", e.getMessage() );
				}
			} );
			//for these, it seems better to null the map rather than clear it:
			blobs = null;
		}

		if ( clobs != null ) {
			clobs.forEach( clob -> {
				try {
					clob.free();
				}
				catch (SQLException e) {
					log.debugf( "Unable to free JDBC Clob reference [%s]", e.getMessage() );
				}
			} );
			clobs = null;
		}

		if ( nclobs != null ) {
			nclobs.forEach( nclob -> {
				try {
					nclob.free();
				}
				catch (SQLException e) {
					log.debugf( "Unable to free JDBC NClob reference [%s]", e.getMessage() );
				}
			} );
			nclobs = null;
		}

		if ( jdbcEventHandler != null ) {
			jdbcEventHandler.jdbcReleaseRegistryResourcesEnd();
		}
	}

	private boolean hasRegistered(final HashMap resource) {
		return resource != null && !resource.isEmpty();
	}

	private boolean hasRegistered(final ArrayList resource) {
		return resource != null && !resource.isEmpty();
	}
}
