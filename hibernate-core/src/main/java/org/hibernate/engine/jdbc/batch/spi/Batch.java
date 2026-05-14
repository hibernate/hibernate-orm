/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.batch.spi;

import org.hibernate.Incubating;

/// Represents the lifecycle of a batch of statements to be executed together.
///
/// The base contract intentionally exposes only lifecycle operations shared by
/// all batching strategies.  Concrete specializations define how rows are added
/// to the batch:
///
/// - [GroupedBatch] batches rows through a group of prepared statements.
/// - [SingleStatementBatch] batches rows for one prepared mutation statement
/// shape.
///
/// A batch is usually associated with a [org.hibernate.engine.jdbc.spi.JdbcCoordinator],
/// which owns the currently active batch for a session and coordinates execution
/// when the active batch changes.
///
/// @see org.hibernate.engine.jdbc.spi.JdbcCoordinator#getGroupedBatch(BatchKey, Integer, java.util.function.Supplier)
/// @see org.hibernate.engine.jdbc.spi.JdbcCoordinator#getSingleStatementBatch(BatchKey, Integer, org.hibernate.sql.model.PreparableMutationOperation)
///
/// @author Steve Ebersole
@Incubating
public interface Batch {
	/// Retrieve the object used to identify compatible rows for this batch.
	///
	/// The [org.hibernate.engine.jdbc.spi.JdbcCoordinator] uses the key to decide
	/// whether a requested batch can reuse the currently active batch or must first
	/// execute and release it.
	///
	/// @return The batch key.
	BatchKey getKey();

	/// Adds an observer to this batch.
	///
	/// Observers are notified when a batch is executed explicitly, for example at
	/// the end of a flush, or implicitly, for example when adding a row fills the
	/// configured JDBC batch size.
	///
	/// @param observer The batch observer.
	void addObserver(BatchObserver observer);

	/// Execute this batch.
	///
	/// Implementations should tolerate being called when there is no pending row.
	/// This method is also responsible for any observer notification associated
	/// with explicit execution.
	void execute();

	/// Used to indicate that the batch instance is no longer needed and that, therefore,
	/// it can release its resources.
	///
	/// This is called when the batch instance is no longer the active batch for the
	/// owning coordinator.  Implementations should release JDBC statements and
	/// clear transient row state, but should not execute pending work that has not
	/// already been executed by [#execute()].
	void release();
}
