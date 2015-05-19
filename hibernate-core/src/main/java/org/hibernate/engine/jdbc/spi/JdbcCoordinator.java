/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.spi;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.Statement;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.jdbc.WorkExecutorVisitable;
import org.hibernate.resource.jdbc.ResourceRegistry;
import org.hibernate.resource.jdbc.spi.LogicalConnectionImplementor;
import org.hibernate.resource.transaction.backend.jdbc.spi.JdbcResourceTransactionAccess;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorOwner;

/**
 * Coordinates JDBC-related activities.
 *
 * @author Steve Ebersole
 * @author Brett Meyer
 */
public interface JdbcCoordinator extends Serializable, TransactionCoordinatorOwner, JdbcResourceTransactionAccess {
//	/**
//	 * Retrieve the transaction coordinator associated with this JDBC coordinator.
//	 *
//	 * @return The transaction coordinator
//	 */
//	public TransactionCoordinator getTransactionCoordinator();

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
	 * @see org.hibernate.resource.jdbc.spi.LogicalConnectionImplementor#close
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
	 * Calculate the amount of time, in seconds, still remaining before transaction timeout occurs.
	 *
	 * @return The number of seconds remaining until until a transaction timeout occurs.  A negative value indicates
	 * no timeout was requested.
	 *
	 * @throws org.hibernate.TransactionException Indicates the time out period has already been exceeded.
	 */
	public int determineRemainingTransactionTimeOutPeriod();

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

	/**
	 * The release mode under which this logical connection is operating.
	 *
	 * @return the release mode.
	 */
	public ConnectionReleaseMode getConnectionReleaseMode();

	public ResourceRegistry getResourceRegistry();
}
