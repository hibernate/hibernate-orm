/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.jdbc;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ConcurrentModificationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.ScrollMode;
import org.hibernate.TransactionException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.exception.JDBCExceptionHelper;
import org.hibernate.jdbc.util.FormatStyle;
import org.hibernate.util.JDBCExceptionReporter;

/**
 * Manages prepared statements and batching.
 *
 * @author Gavin King
 */
public abstract class AbstractBatcher implements Batcher {

	private static int globalOpenPreparedStatementCount;
	private static int globalOpenResultSetCount;

	private int openPreparedStatementCount;
	private int openResultSetCount;

	protected static final Logger log = LoggerFactory.getLogger( AbstractBatcher.class );

	private final ConnectionManager connectionManager;
	private final SessionFactoryImplementor factory;

	private PreparedStatement batchUpdate;
	private String batchUpdateSQL;

	private HashSet statementsToClose = new HashSet();
	private HashSet resultSetsToClose = new HashSet();
	private PreparedStatement lastQuery;

	private boolean releasing = false;
	private final Interceptor interceptor;

	private long transactionTimeout = -1;
	boolean isTransactionTimeoutSet;

	public AbstractBatcher(ConnectionManager connectionManager, Interceptor interceptor) {
		this.connectionManager = connectionManager;
		this.interceptor = interceptor;
		this.factory = connectionManager.getFactory();
	}

	public void setTransactionTimeout(int seconds) {
		isTransactionTimeoutSet = true;
		transactionTimeout = System.currentTimeMillis() / 1000 + seconds;
	}

	public void unsetTransactionTimeout() {
		isTransactionTimeoutSet = false;
	}

	protected PreparedStatement getStatement() {
		return batchUpdate;
	}

	public CallableStatement prepareCallableStatement(String sql)
	throws SQLException, HibernateException {
		executeBatch();
		logOpenPreparedStatement();
		return getCallableStatement( connectionManager.getConnection(), sql, false);
	}

	public PreparedStatement prepareStatement(String sql)
	throws SQLException, HibernateException {
		return prepareStatement( sql, false );
	}

	public PreparedStatement prepareStatement(String sql, boolean getGeneratedKeys)
			throws SQLException, HibernateException {
		executeBatch();
		logOpenPreparedStatement();
		return getPreparedStatement(
				connectionManager.getConnection(),
		        sql,
		        false,
		        getGeneratedKeys,
		        null,
		        null,
		        false
		);
	}

	public PreparedStatement prepareStatement(String sql, String[] columnNames)
			throws SQLException, HibernateException {
		executeBatch();
		logOpenPreparedStatement();
		return getPreparedStatement(
				connectionManager.getConnection(),
		        sql,
		        false,
		        false,
		        columnNames,
		        null,
		        false
		);
	}

	public PreparedStatement prepareSelectStatement(String sql)
			throws SQLException, HibernateException {
		logOpenPreparedStatement();
		return getPreparedStatement(
				connectionManager.getConnection(),
		        sql,
		        false,
		        false,
		        null,
		        null,
		        false
		);
	}

	public PreparedStatement prepareQueryStatement(
			String sql,
	        boolean scrollable,
	        ScrollMode scrollMode) throws SQLException, HibernateException {
		logOpenPreparedStatement();
		PreparedStatement ps = getPreparedStatement(
				connectionManager.getConnection(),
		        sql,
		        scrollable,
		        scrollMode
		);
		setStatementFetchSize( ps );
		statementsToClose.add( ps );
		lastQuery = ps;
		return ps;
	}

	public CallableStatement prepareCallableQueryStatement(
			String sql,
	        boolean scrollable,
	        ScrollMode scrollMode) throws SQLException, HibernateException {
		logOpenPreparedStatement();
		CallableStatement ps = ( CallableStatement ) getPreparedStatement(
				connectionManager.getConnection(),
		        sql,
		        scrollable,
		        false,
		        null,
		        scrollMode,
		        true
		);
		setStatementFetchSize( ps );
		statementsToClose.add( ps );
		lastQuery = ps;
		return ps;
	}

	public void abortBatch(SQLException sqle) {
		try {
			if (batchUpdate!=null) closeStatement(batchUpdate);
		}
		catch (SQLException e) {
			//noncritical, swallow and let the other propagate!
			JDBCExceptionReporter.logExceptions(e);
		}
		finally {
			batchUpdate=null;
			batchUpdateSQL=null;
		}
	}

	public ResultSet getResultSet(PreparedStatement ps) throws SQLException {
		ResultSet rs = ps.executeQuery();
		resultSetsToClose.add(rs);
		logOpenResults();
		return rs;
	}

	public ResultSet getResultSet(CallableStatement ps, Dialect dialect) throws SQLException {
		ResultSet rs = dialect.getResultSet(ps);
		resultSetsToClose.add(rs);
		logOpenResults();
		return rs;

	}

	public void closeQueryStatement(PreparedStatement ps, ResultSet rs) throws SQLException {
		boolean psStillThere = statementsToClose.remove( ps );
		try {
			if ( rs != null ) {
				if ( resultSetsToClose.remove( rs ) ) {
					logCloseResults();
					rs.close();
				}
			}
		}
		finally {
			if ( psStillThere ) {
				closeQueryStatement( ps );
			}
		}
	}

	public PreparedStatement prepareBatchStatement(String sql)
			throws SQLException, HibernateException {
		sql = getSQL( sql );

		if ( !sql.equals(batchUpdateSQL) ) {
			batchUpdate=prepareStatement(sql); // calls executeBatch()
			batchUpdateSQL=sql;
		}
		else {
			log.debug("reusing prepared statement");
			log(sql);
		}
		return batchUpdate;
	}

	public CallableStatement prepareBatchCallableStatement(String sql)
			throws SQLException, HibernateException {
		if ( !sql.equals(batchUpdateSQL) ) { // TODO: what if batchUpdate is a callablestatement ?
			batchUpdate=prepareCallableStatement(sql); // calls executeBatch()
			batchUpdateSQL=sql;
		}
		return (CallableStatement)batchUpdate;
	}


	public void executeBatch() throws HibernateException {
		if (batchUpdate!=null) {
			try {
				try {
					doExecuteBatch(batchUpdate);
				}
				finally {
					closeStatement(batchUpdate);
				}
			}
			catch (SQLException sqle) {
				throw JDBCExceptionHelper.convert(
				        factory.getSQLExceptionConverter(),
				        sqle,
				        "Could not execute JDBC batch update",
				        batchUpdateSQL
					);
			}
			finally {
				batchUpdate=null;
				batchUpdateSQL=null;
			}
		}
	}

	public void closeStatement(PreparedStatement ps) throws SQLException {
		logClosePreparedStatement();
		closePreparedStatement(ps);
	}

	private void closeQueryStatement(PreparedStatement ps) throws SQLException {

		try {
			//work around a bug in all known connection pools....
			if ( ps.getMaxRows()!=0 ) ps.setMaxRows(0);
			if ( ps.getQueryTimeout()!=0 ) ps.setQueryTimeout(0);
		}
		catch (Exception e) {
			log.warn("exception clearing maxRows/queryTimeout", e);
//			ps.close(); //just close it; do NOT try to return it to the pool!
			return; //NOTE: early exit!
		}
		finally {
			closeStatement(ps);
		}

		if ( lastQuery==ps ) lastQuery = null;

	}

	/**
	 * Actually releases the batcher, allowing it to cleanup internally held
	 * resources.
	 */
	public void closeStatements() {
		try {
			releasing = true;

			try {
				if ( batchUpdate != null ) {
					batchUpdate.close();
				}
			}
			catch ( SQLException sqle ) {
				//no big deal
				log.warn( "Could not close a JDBC prepared statement", sqle );
			}
			batchUpdate = null;
			batchUpdateSQL = null;

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
		}
		finally {
			releasing = false;
		}
	}

	protected abstract void doExecuteBatch(PreparedStatement ps) throws SQLException, HibernateException;

	private String preparedStatementCountsToString() {
		return
				" (open PreparedStatements: " +
				openPreparedStatementCount +
				", globally: " +
				globalOpenPreparedStatementCount +
				")";
	}

	private String resultSetCountsToString() {
		return
				" (open ResultSets: " +
				openResultSetCount +
				", globally: " +
				globalOpenResultSetCount +
				")";
	}

	private void logOpenPreparedStatement() {
		if ( log.isDebugEnabled() ) {
			log.debug( "about to open PreparedStatement" + preparedStatementCountsToString() );
			openPreparedStatementCount++;
			globalOpenPreparedStatementCount++;
		}
	}

	private void logClosePreparedStatement() {
		if ( log.isDebugEnabled() ) {
			log.debug( "about to close PreparedStatement" + preparedStatementCountsToString() );
			openPreparedStatementCount--;
			globalOpenPreparedStatementCount--;
		}
	}

	private void logOpenResults() {
		if ( log.isDebugEnabled() ) {
			log.debug( "about to open ResultSet" + resultSetCountsToString() );
			openResultSetCount++;
			globalOpenResultSetCount++;
		}
	}
	private void logCloseResults() {
		if ( log.isDebugEnabled() ) {
			log.debug( "about to close ResultSet" + resultSetCountsToString() );
			openResultSetCount--;
			globalOpenResultSetCount--;
		}
	}

	protected SessionFactoryImplementor getFactory() {
		return factory;
	}

	private void log(String sql) {
		factory.getSettings().getSqlStatementLogger().logStatement( sql, FormatStyle.BASIC );
	}

	private PreparedStatement getPreparedStatement(
			final Connection conn,
	        final String sql,
	        final boolean scrollable,
	        final ScrollMode scrollMode) throws SQLException {
		return getPreparedStatement(
				conn,
		        sql,
		        scrollable,
		        false,
		        null,
		        scrollMode,
		        false
		);
	}

	private CallableStatement getCallableStatement(
			final Connection conn,
	        String sql,
	        boolean scrollable) throws SQLException {
		if ( scrollable && !factory.getSettings().isScrollableResultSetsEnabled() ) {
			throw new AssertionFailure("scrollable result sets are not enabled");
		}

		sql = getSQL( sql );
		log( sql );

		log.trace("preparing callable statement");
		if ( scrollable ) {
			return conn.prepareCall(
					sql,
			        ResultSet.TYPE_SCROLL_INSENSITIVE,
			        ResultSet.CONCUR_READ_ONLY
			);
		}
		else {
			return conn.prepareCall( sql );
		}
	}

	private String getSQL(String sql) {
		sql = interceptor.onPrepareStatement( sql );
		if ( sql==null || sql.length() == 0 ) {
			throw new AssertionFailure( "Interceptor.onPrepareStatement() returned null or empty string." );
		}
		return sql;
	}

	private PreparedStatement getPreparedStatement(
			final Connection conn,
	        String sql,
	        boolean scrollable,
	        final boolean useGetGeneratedKeys,
	        final String[] namedGeneratedKeys,
	        final ScrollMode scrollMode,
	        final boolean callable) throws SQLException {
		if ( scrollable && !factory.getSettings().isScrollableResultSetsEnabled() ) {
			throw new AssertionFailure("scrollable result sets are not enabled");
		}
		if ( useGetGeneratedKeys && !factory.getSettings().isGetGeneratedKeysEnabled() ) {
			throw new AssertionFailure("getGeneratedKeys() support is not enabled");
		}

		sql = getSQL( sql );
		log( sql );

		log.trace( "preparing statement" );
		PreparedStatement result;
		if ( scrollable ) {
			if ( callable ) {
				result = conn.prepareCall( sql, scrollMode.toResultSetType(), ResultSet.CONCUR_READ_ONLY );
			}
			else {
				result = conn.prepareStatement( sql, scrollMode.toResultSetType(), ResultSet.CONCUR_READ_ONLY );
			}
		}
		else if ( useGetGeneratedKeys ) {
			result = conn.prepareStatement( sql, PreparedStatement.RETURN_GENERATED_KEYS );
		}
		else if ( namedGeneratedKeys != null ) {
			result = conn.prepareStatement( sql, namedGeneratedKeys );
		}
		else {
			if ( callable ) {
				result = conn.prepareCall( sql );
			}
			else {
				result = conn.prepareStatement( sql );
			}
		}

		setTimeout( result );

		if ( factory.getStatistics().isStatisticsEnabled() ) {
			factory.getStatisticsImplementor().prepareStatement();
		}

		return result;

	}

	private void setTimeout(PreparedStatement result) throws SQLException {
		if ( isTransactionTimeoutSet ) {
			int timeout = (int) ( transactionTimeout - ( System.currentTimeMillis() / 1000 ) );
			if (timeout<=0) {
				throw new TransactionException("transaction timeout expired");
			}
			else {
				result.setQueryTimeout(timeout);
			}
		}
	}

	private void closePreparedStatement(PreparedStatement ps) throws SQLException {
		try {
			log.trace("closing statement");
			ps.close();
			if ( factory.getStatistics().isStatisticsEnabled() ) {
				factory.getStatisticsImplementor().closeStatement();
			}
		}
		finally {
			if ( !releasing ) {
				// If we are in the process of releasing, no sense
				// checking for aggressive-release possibility.
				connectionManager.afterStatement();
			}
		}
	}

	private void setStatementFetchSize(PreparedStatement statement) throws SQLException {
		Integer statementFetchSize = factory.getSettings().getJdbcFetchSize();
		if ( statementFetchSize!=null ) {
			statement.setFetchSize( statementFetchSize.intValue() );
		}
	}

	public Connection openConnection() throws HibernateException {
		log.debug("opening JDBC connection");
		try {
			return factory.getConnectionProvider().getConnection();
		}
		catch (SQLException sqle) {
			throw JDBCExceptionHelper.convert(
					factory.getSQLExceptionConverter(),
			        sqle,
			        "Cannot open connection"
				);
		}
	}

	public void closeConnection(Connection conn) throws HibernateException {
		if ( conn == null ) {
			log.debug( "found null connection on AbstractBatcher#closeConnection" );
			// EARLY EXIT!!!!
			return;
		}

		if ( log.isDebugEnabled() ) {
			log.debug( "closing JDBC connection" + preparedStatementCountsToString() + resultSetCountsToString() );
		}

		try {
			if ( !conn.isClosed() ) {
				JDBCExceptionReporter.logAndClearWarnings( conn );
			}
			factory.getConnectionProvider().closeConnection( conn );
		}
		catch ( SQLException sqle ) {
			throw JDBCExceptionHelper.convert( factory.getSQLExceptionConverter(), sqle, "Cannot close connection" );
		}
	}

	public void cancelLastQuery() throws HibernateException {
		try {
			if (lastQuery!=null) lastQuery.cancel();
		}
		catch (SQLException sqle) {
			throw JDBCExceptionHelper.convert(
					factory.getSQLExceptionConverter(),
			        sqle,
			        "Cannot cancel query"
				);
		}
	}

	public boolean hasOpenResources() {
		return resultSetsToClose.size() > 0 || statementsToClose.size() > 0;
	}

	public String openResourceStatsAsString() {
		return preparedStatementCountsToString() + resultSetCountsToString();
	}

}






