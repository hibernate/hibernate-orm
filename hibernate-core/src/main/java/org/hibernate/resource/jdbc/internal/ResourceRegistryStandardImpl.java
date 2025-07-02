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
	private static final boolean IS_TRACE_ENABLED = log.isTraceEnabled();

	private final JdbcEventHandler jdbcEventHandler;

	private final ResultsetsTrackingContainer xref = new ResultsetsTrackingContainer();

	private ExtendedState ext;
	private Statement lastQuery;

	public ResourceRegistryStandardImpl() {
		this( null );
	}

	public ResourceRegistryStandardImpl(JdbcEventHandler jdbcEventHandler) {
		this.jdbcEventHandler = jdbcEventHandler;
	}

	@Override
	public boolean hasRegisteredResources() {
		return xref.hasRegisteredResources()
			|| ext != null && ext.hasRegisteredResources();
	}

	@Override
	public void register(Statement statement, boolean cancelable) {
		if ( IS_TRACE_ENABLED ) log.tracef( "Registering statement [%s]", statement );

		xref.registerExpectingNew( statement );

		if ( cancelable ) {
			lastQuery = statement;
		}
	}

	@Override
	public void release(Statement statement) {
		if ( IS_TRACE_ENABLED ) log.tracev( "Releasing statement [{0}]", statement );

		final ResultSetsSet resultSets = xref.remove( statement );
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
		if ( IS_TRACE_ENABLED ) log.tracef( "Releasing result set [%s]", resultSet );

		if ( statement == null ) {
			try {
				statement = resultSet.getStatement();
			}
			catch (SQLException e) {
				throw convert( e, "unable to access Statement from ResultSet" );
			}
		}
		if ( statement != null ) {
			final ResultSetsSet resultSets = xref.getForResultSetRemoval( statement );
			if ( resultSets == null ) {
				log.unregisteredStatement();
			}
			else {
				resultSets.removeResultSet( resultSet );
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
			if ( ext != null ) {
				ext.releaseUnassociatedResult( resultSet );
			}
		}
		close( resultSet );
	}

	private static void closeAll(final ResultSetsSet resultSets) {
		if ( resultSets == null ) {
			return;
		}
		resultSets.forEachResultSet( ResourceRegistryStandardImpl::close );
	}

	private static void releaseXref(final Statement s, final ResultSetsSet r) {
		closeAll( r );
		close( s );
	}

	private static void close(final ResultSet resultSet) {
		if ( IS_TRACE_ENABLED ) log.tracef( "Closing result set [%s]", resultSet );

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
		if ( IS_TRACE_ENABLED ) log.tracef( "Closing prepared statement [%s]", statement );

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
		if ( IS_TRACE_ENABLED ) log.tracef( "Registering result set [%s]", resultSet );

		if ( statement == null ) {
			try {
				statement = resultSet.getStatement();
			}
			catch (SQLException e) {
				throw convert( e, "unable to access Statement from ResultSet" );
			}
		}
		if ( statement != null ) {
			xref.storeAssociatedResultset( statement, resultSet );
		}
		else {
			getExtendedStateForWrite().storeUnassociatedResultset( resultSet );
		}
	}

	private ExtendedState getExtendedStateForWrite() {
		if ( this.ext == null ) {
			this.ext = new ExtendedState();
		}
		return this.ext;
	}

	private JDBCException convert(SQLException e, String s) {
		return new JDBCException( s, e );
	}

	@Override
	public void register(Blob blob) {
		getExtendedStateForWrite().registerBlob( blob );
	}

	@Override
	public void release(final Blob blob) {
		if ( ext == null || ext.blobs == null ) {
			log.debug( "Request to release Blob, but appears no Blobs have ever been registered" );
			return;
		}
		ext.blobs.remove( blob );
	}

	@Override
	public void register(final Clob clob) {
		getExtendedStateForWrite().registerClob( clob );
	}

	@Override
	public void release(final Clob clob) {
		if ( ext == null || ext.clobs == null ) {
			log.debug( "Request to release Clob, but appears no Clobs have ever been registered" );
			return;
		}
		ext.clobs.remove( clob );
	}

	@Override
	public void register(final NClob nclob) {
		// todo : just store them in clobs?
		getExtendedStateForWrite().registerNClob( nclob );
	}

	@Override
	public void release(NClob nclob) {
		// todo : just store them in clobs?
		if ( ext == null || ext.nclobs == null ) {
			log.debug( "Request to release NClob, but appears no NClobs have ever been registered" );
			return;
		}
		ext.nclobs.remove( nclob );
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
		if ( IS_TRACE_ENABLED ) log.trace( "Releasing JDBC resources" );

		if ( jdbcEventHandler != null ) {
			jdbcEventHandler.jdbcReleaseRegistryResourcesStart();
		}

		xref.forEach( ResourceRegistryStandardImpl::releaseXref );
		xref.clear();

		if ( ext != null ) {
			ext.releaseResources();
		}

		if ( jdbcEventHandler != null ) {
			jdbcEventHandler.jdbcReleaseRegistryResourcesEnd();
		}
	}

	private static boolean hasRegistered(final ResultSetsSet resource) {
		return resource != null && !resource.isEmpty();
	}

	private static boolean hasRegistered(final ArrayList resource) {
		return resource != null && !resource.isEmpty();
	}

	/**
	 * Keeping this state separate from the main instance state as these are less-so commonly
	 * used: this keeps memory pressure low and the code a bit more organized.
	 * This implies that instances of ExtendedState should be lazily initialized, and access
	 * carefully guarded against null.
	 */
	private static class ExtendedState {
		//All fields lazily initialized as well:
		private ResultSetsSet unassociatedResultSets;
		private ArrayList<Blob> blobs;
		private ArrayList<Clob> clobs;
		private ArrayList<NClob> nclobs;

		public boolean hasRegisteredResources() {
			return hasRegistered( unassociatedResultSets )
				|| hasRegistered( blobs )
				|| hasRegistered( clobs )
				|| hasRegistered( nclobs );
		}

		public void releaseUnassociatedResult(final ResultSet resultSet) {
			final Object removed = unassociatedResultSets == null ? null : unassociatedResultSets.removeResultSet( resultSet );
			if ( removed == null ) {
				log.unregisteredResultSetWithoutStatement();
			}
		}

		public void storeUnassociatedResultset(ResultSet resultSet) {
			if ( unassociatedResultSets == null ) {
				this.unassociatedResultSets = new ResultSetsSet();
			}
			unassociatedResultSets.storeResultSet( resultSet );
		}

		public void registerBlob(final Blob blob) {
			if ( blobs == null ) {
				blobs = new ArrayList<>();
			}

			blobs.add( blob );
		}

		public void registerClob(final Clob clob) {
			if ( clobs == null ) {
				clobs = new ArrayList<>();
			}
			clobs.add( clob );
		}

		public void registerNClob(final NClob nclob) {
			if ( nclobs == null ) {
				nclobs = new ArrayList<>();
			}
			nclobs.add( nclob );
		}

		public void releaseResources() {
			closeAll( unassociatedResultSets );
			unassociatedResultSets.clear();

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
		}
	}
}
