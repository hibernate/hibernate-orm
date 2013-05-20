/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.engine.jdbc.spi;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.transaction.spi.TransactionCoordinator;
import org.hibernate.jdbc.WorkExecutorVisitable;

/**
 * Coordinates JDBC-related activities.
 *
 * @author Steve Ebersole
 * @author Brett Meyer
 */
public interface JdbcCoordinator extends Serializable {
	/**
	 * Retrieve the transaction coordinator associated with this JDBC coordinator.
	 *
	 * @return The transaction coordinator
	 */
	public TransactionCoordinator getTransactionCoordinator();

	/**
	 * Retrieves the logical connection associated with this JDBC coordinator.
	 *
	 * @return The logical connection
	 */
	public LogicalConnectionImplementor getLogicalConnection();

	/**
	 * Get a batch instance.
	 *
	 * @param key The unique batch key.
	 *
	 * @return The batch
	 */
	public Batch getBatch(BatchKey key);

	/**
	 * Execute the currently managed batch (if any)
	 */
	public void executeBatch();

	/**
	 * Abort the currently managed batch (if any)
	 */
	public void abortBatch();

	/**
	 * Obtain the statement preparer associated with this JDBC coordinator.
	 *
	 * @return This coordinator's statement preparer
	 */
	public StatementPreparer getStatementPreparer();

	/**
	 * Obtain the resultset extractor associated with this JDBC coordinator.
	 *
	 * @return This coordinator's resultset extractor
	 */
	public ResultSetReturn getResultSetReturn();

	/**
	 * Callback to let us know that a flush is beginning.  We use this fact
	 * to temporarily circumvent aggressive connection releasing until after
	 * the flush cycle is complete {@link #flushEnding()}
	 */
	public void flushBeginning();

	/**
	 * Callback to let us know that a flush is ending.  We use this fact to
	 * stop circumventing aggressive releasing connections.
	 */
	public void flushEnding();

	/**
	 * Close this coordinator and release and resources.
	 *
	 * @return The {@link Connection} associated with the managed {@link #getLogicalConnection() logical connection}
	 *
	 * @see LogicalConnection#close
	 */
	public Connection close();

	/**
	 * Signals the end of transaction.
	 * <p/>
	 * Intended for use from the transaction coordinator, after local transaction completion.  Used to conditionally
	 * release the JDBC connection aggressively if the configured release mode indicates.
	 */
	public void afterTransaction();

	/**
	 * Used to signify that a statement has completed execution which may
	 * indicate that this logical connection need to perform an
	 * aggressive release of its physical connection.
	 */
	public void afterStatementExecution();

	/**
	 * Perform the requested work handling exceptions, coordinating and handling return processing.
	 *
	 * @param work The work to be performed.
	 * @param <T> The result type.
	 * @return The work result.
	 */
	public <T> T coordinateWork(WorkExecutorVisitable<T> work);

	/**
	 * Attempt to cancel the last query sent to the JDBC driver.
	 */
	public void cancelLastQuery();

	/**
	 * Set the effective transaction timeout period for the current transaction, in seconds.
	 *
	 * @param seconds The number of seconds before a time out should occur.
	 */
	public void setTransactionTimeOut(int seconds);

    /**
	 * Calculate the amount of time, in seconds, still remaining before transaction timeout occurs.
	 *
	 * @return The number of seconds remaining until until a transaction timeout occurs.  A negative value indicates
	 * no timeout was requested.
	 *
	 * @throws org.hibernate.TransactionException Indicates the time out period has already been exceeded.
	 */
	public int determineRemainingTransactionTimeOutPeriod();

	/**
	 * Register a JDBC statement.
	 *
	 * @param statement The statement to register.
	 */
	public void register(Statement statement);
	
	/**
	 * Release a previously registered statement.
	 *
	 * @param statement The statement to release.
	 */
	public void release(Statement statement);

	/**
	 * Register a JDBC result set.
	 * <p/>
	 * Implementation note: Second parameter has been introduced to prevent
	 * multiple registrations of the same statement in case {@link ResultSet#getStatement()}
	 * does not return original {@link Statement} object.
	 *
	 * @param resultSet The result set to register.
	 * @param statement Statement from which {@link ResultSet} has been generated.
	 */
	public void register(ResultSet resultSet, Statement statement);

	/**
	 * Release a previously registered result set.
	 *
	 * @param resultSet The result set to release.
	 * @param statement Statement from which {@link ResultSet} has been generated.
	 */
	public void release(ResultSet resultSet, Statement statement);

	/**
	 * Does this registry currently have any registered resources?
	 *
	 * @return True if the registry does have registered resources; false otherwise.
	 */
	public boolean hasRegisteredResources();

	/**
	 * Release all registered resources.
	 */
	public void releaseResources();

	/**
	 * Enable connection releases
	 */
	public void enableReleases();

	/**
	 * Disable connection releases
	 */
	public void disableReleases();

	/**
	 * Register a query statement as being able to be cancelled.
	 * 
	 * @param statement The cancel-able query statement.
	 */
	public void registerLastQuery(Statement statement);

	/**
	 * Can this coordinator be serialized?
	 *
	 * @return {@code true} indicates the coordinator can be serialized.
	 */
	public boolean isReadyForSerialization();
}
