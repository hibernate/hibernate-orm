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

import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.transaction.spi.TransactionCoordinator;
import org.hibernate.jdbc.WorkExecutorVisitable;

/**
 * Coordinates JDBC-related activities.
 *
 * @author Steve Ebersole
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
}
