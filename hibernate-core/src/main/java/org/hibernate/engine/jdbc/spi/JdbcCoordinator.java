/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.spi;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.function.Supplier;

import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementGroup;
import org.hibernate.jdbc.WorkExecutorVisitable;
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

	/**
	 * Retrieves the logical connection associated with this JDBC coordinator.
	 *
	 * @return The logical connection
	 */
	LogicalConnectionImplementor getLogicalConnection();

	/**
	 * The builder of prepared and callable JDBC statements for
	 * mutation operations (insert, update and delete) originating
	 * from persistent context events, as opposed to Query handling
	 */
	MutationStatementPreparer getMutationStatementPreparer();

	/**
	 * Get the {@linkplain Batch batch} for the supplied key, creating one
	 * if needed using the supplied {@linkplain PreparedStatementGroup statementGroupSupplier}.
	 *
	 * @implNote Any previous Batch is executed and released prior to returning
	 */
	Batch getBatch(
			BatchKey key,
			Integer batchSize,
			Supplier<PreparedStatementGroup> statementGroupSupplier);

	/**
	 * Execute the currently managed batch (if any)
	 */
	void executeBatch();

	/**
	 * Conditionally execute the currently managed batch (if any), if the
	 * keys do not match
	 */
	void conditionallyExecuteBatch(BatchKey key);

	/**
	 * Abort the currently managed batch (if any)
	 */
	void abortBatch();

	/**
	 * Obtain the statement preparer associated with this JDBC coordinator.
	 *
	 * @return This coordinator's statement preparer
	 */
	StatementPreparer getStatementPreparer();

	/**
	 * Obtain the {@link ResultSet} extractor associated with this JDBC coordinator.
	 *
	 * @return This coordinator's resultset extractor
	 */
	ResultSetReturn getResultSetReturn();

	/**
	 * Callback to let us know that a flush is beginning.  We use this fact
	 * to temporarily circumvent aggressive connection releasing until after
	 * the flush cycle is complete {@link #flushEnding()}
	 */
	void flushBeginning();

	/**
	 * Callback to let us know that a flush is ending.  We use this fact to
	 * stop circumventing aggressive releasing connections.
	 */
	void flushEnding();

	/**
	 * Close this coordinator and release and resources.
	 *
	 * @return The {@link Connection} associated with the managed {@link #getLogicalConnection() logical connection}
	 *
	 * @see LogicalConnectionImplementor#close
	 */
	Connection close();

	/**
	 * Signals the end of transaction.
	 * <p>
	 * Intended for use from the transaction coordinator, after local transaction completion.  Used to conditionally
	 * release the JDBC connection aggressively if the configured release mode indicates.
	 */
	void afterTransaction();

	/**
	 * Used to signify that a statement has completed execution which may
	 * indicate that this logical connection need to perform an
	 * aggressive release of its physical connection.
	 */
	void afterStatementExecution();

	/**
	 * Perform the requested work handling exceptions, coordinating and handling return processing.
	 *
	 * @param work The work to be performed.
	 * @param <T> The result type.
	 * @return The work result.
	 */
	<T> T coordinateWork(WorkExecutorVisitable<T> work);

	/**
	 * Attempt to cancel the last query sent to the JDBC driver.
	 */
	void cancelLastQuery();

	/**
	 * Calculate the amount of time, in seconds, still remaining before transaction timeout occurs.
	 *
	 * @return The number of seconds remaining until a transaction timeout occurs.  A negative value indicates
	 * no timeout was requested.
	 *
	 * @throws org.hibernate.TransactionException Indicates the timeout period has already been exceeded.
	 */
	int determineRemainingTransactionTimeOutPeriod();

	/**
	 * Enable connection releases
	 */
	void enableReleases();

	/**
	 * Disable connection releases
	 */
	void disableReleases();

	/**
	 * Register a query statement as being able to be cancelled.
	 *
	 * @param statement The cancel-able query statement.
	 */
	void registerLastQuery(Statement statement);

	/**
	 * Can this coordinator be serialized?
	 *
	 * @return {@code true} indicates the coordinator can be serialized.
	 */
	boolean isReadyForSerialization();

	void serialize(ObjectOutputStream objectOutputStream) throws IOException;

}
