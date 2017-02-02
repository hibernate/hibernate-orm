/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.jdbc.internal;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.resource.jdbc.ResourceRegistry;

/**
 * @author Steve Ebersole
 */
public class ResourceRegistryStandardImpl implements ResourceRegistry {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( ResourceRegistryStandardImpl.class );

	private final Map<Statement, Set<ResultSet>> xref = new HashMap<Statement, Set<ResultSet>>();
	private final Set<ResultSet> unassociatedResultSets = new HashSet<ResultSet>();

	private List<Blob> blobs;
	private List<Clob> clobs;
	private List<NClob> nclobs;

	private Statement lastQuery;

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
		if ( xref.containsKey( statement ) ) {
			throw new HibernateException( "JDBC Statement already registered" );
		}
		xref.put( statement, null );

		if ( cancelable ) {
			lastQuery = statement;
		}
	}

	@Override
	public void release(Statement statement) {
		log.tracev( "Releasing statement [{0}]", statement );

		// Keep this at DEBUG level, rather than warn.  Numerous connection pool implementations can return a
		// proxy/wrapper around the JDBC Statement, causing excessive logging here.  See HHH-8210.
		if ( log.isDebugEnabled() && !xref.containsKey( statement ) ) {
			log.unregisteredStatement();
		}
		else {
			final Set<ResultSet> resultSets = xref.get( statement );
			if ( resultSets != null ) {
				closeAll( resultSets );
			}
			xref.remove( statement );
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
			final Set<ResultSet> resultSets = xref.get( statement );
			if ( resultSets == null ) {
				log.unregisteredStatement();
			}
			else {
				resultSets.remove( resultSet );
				if ( resultSets.isEmpty() ) {
					xref.remove( statement );
				}
			}
		}
		else {
			final boolean removed = unassociatedResultSets.remove( resultSet );
			if ( !removed ) {
				log.unregisteredResultSetWithoutStatement();
			}

		}
		close( resultSet );
	}

	protected void closeAll(Set<ResultSet> resultSets) {
		for ( ResultSet resultSet : resultSets ) {
			close( resultSet );
		}
		resultSets.clear();
	}

	@SuppressWarnings({"unchecked"})
	public static void close(ResultSet resultSet) {
		log.tracef( "Closing result set [%s]", resultSet );

		try {
			resultSet.close();
		}
		catch (SQLException e) {
			log.debugf( "Unable to release JDBC result set [%s]", e.getMessage() );
		}
		catch (Exception e) {
			// try to handle general errors more elegantly
			log.debugf( "Unable to release JDBC result set [%s]", e.getMessage() );
		}
	}

	@SuppressWarnings({"unchecked"})
	public static void close(Statement statement) {
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
			// Keep this at DEBUG level, rather than warn.  Numerous connection pool implementations can return a
			// proxy/wrapper around the JDBC Statement, causing excessive logging here.  See HHH-8210.
			if ( log.isDebugEnabled() && !xref.containsKey( statement ) ) {
				log.debug( "ResultSet statement was not registered (on register)" );
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

	private JDBCException convert(SQLException e, String s) {
		// todo : implement
		return null;
	}

	@Override
	public void register(Blob blob) {
		if ( blobs == null ) {
			blobs = new ArrayList<Blob>();
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
			clobs = new ArrayList<Clob>();
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
			nclobs = new ArrayList<NClob>();
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

		for ( Map.Entry<Statement, Set<ResultSet>> entry : xref.entrySet() ) {
			if ( entry.getValue() != null ) {
				closeAll( entry.getValue() );
			}
			close( entry.getKey() );
		}
		xref.clear();

		closeAll( unassociatedResultSets );

		if ( blobs != null ) {
			for ( Blob blob : blobs ) {
				try {
					blob.free();
				}
				catch (SQLException e) {
					log.debugf( "Unable to free JDBC Blob reference [%s]", e.getMessage() );
				}
			}
			blobs.clear();
		}

		if ( clobs != null ) {
			for ( Clob clob : clobs ) {
				try {
					clob.free();
				}
				catch (SQLException e) {
					log.debugf( "Unable to free JDBC Clob reference [%s]", e.getMessage() );
				}
			}
			clobs.clear();
		}

		if ( nclobs != null ) {
			for ( NClob nclob : nclobs ) {
				try {
					nclob.free();
				}
				catch (SQLException e) {
					log.debugf( "Unable to free JDBC NClob reference [%s]", e.getMessage() );
				}
			}
			nclobs.clear();
		}

	}

	private boolean hasRegistered(Map resource) {
		return resource != null && !resource.isEmpty();
	}

	private boolean hasRegistered(Collection resource) {
		return resource != null && !resource.isEmpty();
	}
}
