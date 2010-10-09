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

import org.hibernate.HibernateException;
import org.hibernate.ScrollMode;
import org.hibernate.dialect.Dialect;

/**
 * Manages <tt>PreparedStatement</tt>s for a session. Abstracts JDBC
 * batching to maintain the illusion that a single logical batch
 * exists for the whole session, even when batching is disabled.
 * Provides transparent <tt>PreparedStatement</tt> caching.
 *
 * @see java.sql.PreparedStatement
 * @see org.hibernate.impl.SessionImpl
 * @author Gavin King
 */
public interface Batcher {
	/**
	 * Get a prepared statement for use in loading / querying. If not explicitly
	 * released by <tt>closeQueryStatement()</tt>, it will be released when the
	 * session is closed or disconnected.
	 */
	public PreparedStatement prepareQueryStatement(String sql, boolean scrollable, ScrollMode scrollMode) throws SQLException, HibernateException;
	/**
	 * Close a prepared statement opened with <tt>prepareQueryStatement()</tt>
	 */
	public void closeQueryStatement(PreparedStatement ps, ResultSet rs) throws SQLException;
	/**
	 * Get a prepared statement for use in loading / querying. If not explicitly
	 * released by <tt>closeQueryStatement()</tt>, it will be released when the
	 * session is closed or disconnected.
	 */
	public CallableStatement prepareCallableQueryStatement(String sql, boolean scrollable, ScrollMode scrollMode) throws SQLException, HibernateException;
	
	
	/**
	 * Get a non-batchable prepared statement to use for selecting. Does not
	 * result in execution of the current batch.
	 */
	public PreparedStatement prepareSelectStatement(String sql) throws SQLException, HibernateException;

	/**
	 * Get a non-batchable prepared statement to use for inserting / deleting / updating,
	 * using JDBC3 getGeneratedKeys ({@link Connection#prepareStatement(String, int)}).
	 * <p/>
	 * Must be explicitly released by {@link #closeStatement} after use.
	 */
	public PreparedStatement prepareStatement(String sql, boolean useGetGeneratedKeys) throws SQLException, HibernateException;

	/**
	 * Get a non-batchable prepared statement to use for inserting / deleting / updating.
	 * using JDBC3 getGeneratedKeys ({@link Connection#prepareStatement(String, String[])}).
	 * <p/>
	 * Must be explicitly released by {@link #closeStatement} after use.
	 */
	public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException, HibernateException;

	/**
	 * Get a non-batchable prepared statement to use for inserting / deleting / updating.
	 * <p/>
	 * Must be explicitly released by {@link #closeStatement} after use.
	 */
	public PreparedStatement prepareStatement(String sql) throws SQLException, HibernateException;

	/**
	 * Get a non-batchable callable statement to use for inserting / deleting / updating.
	 * <p/>
	 * Must be explicitly released by {@link #closeStatement} after use.
	 */
	public CallableStatement prepareCallableStatement(String sql) throws SQLException, HibernateException;

	/**
	 * Close a prepared or callable statement opened using <tt>prepareStatement()</tt> or <tt>prepareCallableStatement()</tt>
	 */
	public void closeStatement(PreparedStatement ps) throws SQLException;

	/**
	 * Get a batchable prepared statement to use for inserting / deleting / updating
	 * (might be called many times before a single call to <tt>executeBatch()</tt>).
	 * After setting parameters, call <tt>addToBatch</tt> - do not execute the
	 * statement explicitly.
	 * @see Batcher#addToBatch
	 */
	public PreparedStatement prepareBatchStatement(String sql) throws SQLException, HibernateException;

	/**
	 * Get a batchable callable statement to use for inserting / deleting / updating
	 * (might be called many times before a single call to <tt>executeBatch()</tt>).
	 * After setting parameters, call <tt>addToBatch</tt> - do not execute the
	 * statement explicitly.
	 * @see Batcher#addToBatch
	 */
	public CallableStatement prepareBatchCallableStatement(String sql) throws SQLException, HibernateException;

	/**
	 * Add an insert / delete / update to the current batch (might be called multiple times
	 * for single <tt>prepareBatchStatement()</tt>)
	 */
	public void addToBatch(Expectation expectation) throws SQLException, HibernateException;

	/**
	 * Execute the batch
	 */
	public void executeBatch() throws HibernateException;

	/**
	 * Close any query statements that were left lying around
	 */
	public void closeStatements();
	/**
	 * Execute the statement and return the result set
	 */
	public ResultSet getResultSet(PreparedStatement ps) throws SQLException;
	/**
	 * Execute the statement and return the result set from a callable statement
	 */
	public ResultSet getResultSet(CallableStatement ps, Dialect dialect) throws SQLException;

	/**
	 * Must be called when an exception occurs
	 * @param sqle the (not null) exception that is the reason for aborting
	 */
	public void abortBatch(SQLException sqle);

	/**
	 * Cancel the current query statement
	 */
	public void cancelLastQuery() throws HibernateException;

	public boolean hasOpenResources();

	public String openResourceStatsAsString();

	// TODO : remove these last two as batcher is no longer managing connections

	/**
	 * Obtain a JDBC connection
	 *
	 * @deprecated Obtain connections from {@link ConnectionProvider} instead
	 */
	public Connection openConnection() throws HibernateException;
	/**
	 * Dispose of the JDBC connection
	 *
	 * @deprecated Obtain connections from {@link ConnectionProvider} instead
	 */
	public void closeConnection(Connection conn) throws HibernateException;
	
	/**
	 * Set the transaction timeout to <tt>seconds</tt> later
	 * than the current system time.
	 */
	public void setTransactionTimeout(int seconds);
	/**
	 * Unset the transaction timeout, called after the end of a 
	 * transaction.
	 */
	public void unsetTransactionTimeout();
}

