/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.spi;

import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.PropertyValueException;
import org.hibernate.Session;
import org.hibernate.action.internal.BulkOperationCleanupAction;
import org.hibernate.action.internal.CollectionRecreateAction;
import org.hibernate.action.internal.CollectionRemoveAction;
import org.hibernate.action.internal.CollectionUpdateAction;
import org.hibernate.action.internal.EntityDeleteAction;
import org.hibernate.action.internal.EntityIdentityInsertAction;
import org.hibernate.action.internal.EntityInsertAction;
import org.hibernate.action.internal.EntityUpdateAction;
import org.hibernate.action.internal.OrphanRemovalAction;
import org.hibernate.action.internal.QueuedOperationCollectionAction;
import org.hibernate.engine.spi.TransactionCompletionCallbacks;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Set;

/// Common interface for ActionQueue implementations.
///
/// The ActionQueue is responsible for managing and executing all pending persistence actions
/// (inserts, updates, deletes, etc.) in a Hibernate session. Different implementations may
/// use different execution strategies.
///
/// @author Steve Ebersole
/// @since 8.0
@Incubating
public interface ActionQueue extends TransactionCompletionCallbacks {

	/// Clear all pending actions.
	void clear();

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Action Registration
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/// Adds an entity insert action.
	///
	/// @param action The action representing the entity insertion
	void addAction(EntityInsertAction action);

	/// Adds an identity-based entity insert action.
	///
	/// @param action The action representing the entity insertion with identity generation
	void addAction(EntityIdentityInsertAction action);

	/// Adds an entity update action.
	///
	/// @param action The action representing the entity update
	void addAction(EntityUpdateAction action);

	/// Adds an entity delete action.
	///
	/// @param action The action representing the entity deletion
	void addAction(EntityDeleteAction action);

	/// Adds an orphan removal action.
	///
	/// @param action The action representing orphan removal
	void addAction(OrphanRemovalAction action);

	/// Adds a collection recreation action.
	///
	/// @param action The action representing the collection recreation
	void addAction(CollectionRecreateAction action);

	/// Adds a collection removal action.
	///
	/// @param action The action representing the collection removal
	void addAction(CollectionRemoveAction action);

	/// Adds a collection update action.
	///
	/// @param action The action representing the collection update
	void addAction(CollectionUpdateAction action);

	/// Adds a queued operation collection action.
	///
	/// @param action The action representing the queued collection operation
	void addAction(QueuedOperationCollectionAction action);

	/// Adds a bulk operation cleanup action.
	///
	/// @param action The action representing bulk operation cleanup
	void addAction(BulkOperationCleanupAction action);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Execution
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/// Execute identity insert actions.
	///
	/// @throws HibernateException If an error occurs during execution
	void executeInserts() throws HibernateException;

	/// Execute all pending actions.
	///
	/// @throws HibernateException If an error occurs during execution
	void executeActions() throws HibernateException;

	/// Prepare actions for execution (validation, sorting, etc.).
	///
	/// @throws HibernateException If an error occurs during preparation
	void prepareActions() throws HibernateException;

	/// Execute pending bulk operation cleanup actions.
	void executePendingBulkOperationCleanUpActions();

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// State Queries
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/// Check if there are unresolved entity insert actions.
	///
	/// @return true if there are unresolved entity insert actions
	boolean hasUnresolvedEntityInsertActions();

	/// Check if there are any queued actions.
	///
	/// @return true if there are any queued actions
	boolean hasAnyQueuedActions();

	/// Check if there are before-transaction actions.
	///
	/// @return true if there are before-transaction actions
	boolean hasBeforeTransactionActions();

	/// Check if there are after-transaction actions.
	///
	/// @return true if there are after-transaction actions
	boolean hasAfterTransactionActions();

	/// Check if there are insertions or deletions queued.
	///
	/// @return true if there are insertions or deletions queued
	boolean areInsertionsOrDeletionsQueued();

	/// Check if any of the specified tables are scheduled for update.
	///
	/// @param tables The set of table names to check
	/// @return true if any of the tables are scheduled for update
	boolean areTablesToBeUpdated(Set<? extends Serializable> tables);

	/// Check that there are no unresolved actions after an operation.
	///
	/// @throws PropertyValueException If there are unresolved actions
	void checkNoUnresolvedActionsAfterOperation() throws PropertyValueException;

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Statistics
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/// Get the number of insertions.
	///
	/// @return The number of insertions
	int numberOfInsertions();

	/// Get the number of updates.
	///
	/// @return The number of updates
	int numberOfUpdates();

	/// Get the number of deletions.
	///
	/// @return The number of deletions
	int numberOfDeletions();

	/// Get the number of collection creations.
	///
	/// @return The number of collection creations
	int numberOfCollectionCreations();

	/// Get the number of collection updates.
	///
	/// @return The number of collection updates
	int numberOfCollectionUpdates();

	/// Get the number of collection removals.
	///
	/// @return The number of collection removals
	int numberOfCollectionRemovals();

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Transaction Completion
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/// Get the transaction completion callbacks.
	///
	/// @return The transaction completion callbacks implementor
	org.hibernate.engine.spi.TransactionCompletionCallbacksImplementor getTransactionCompletionCallbacks();

	/// Set the transaction completion callbacks.
	///
	/// @param callbacks The transaction completion callbacks
	/// @param isTransactionCoordinatorShared Whether the transaction coordinator is shared
	///
	/// @deprecated This method is not used by [org.hibernate.action.queue.internal.GraphBasedActionQueue]
	/// and is only needed for [org.hibernate.engine.spi.ActionQueueLegacy].  It will be removed when the
	/// legacy implementation is removed.
	@Deprecated(since = "7.0", forRemoval = true)
	void setTransactionCompletionCallbacks(
			org.hibernate.engine.spi.TransactionCompletionCallbacksImplementor callbacks,
			boolean isTransactionCoordinatorShared);

	/// Execute actions before transaction completion.
	void beforeTransactionCompletion();

	/// Record the changelog context generated while binding legacy audit mutations.
	///
	/// @apiNote This hook exists for [org.hibernate.engine.spi.ActionQueueLegacy], whose audit work queue
	/// receives changelog context as a side effect of resolving the current changeset id. The graph
	/// queue resolves changelog context directly from the session and ignores this callback.
	///
	/// @param changelog The changelog entity instance
	/// @param changesetSession The child session used to persist the changelog entity
	void setAuditChangesetContext(Object changelog, Session changesetSession);

	/// Execute actions after transaction completion.
	///
	/// @param success Whether the transaction completed successfully
	void afterTransactionCompletion(boolean success);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Action Scheduling/Unscheduling
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/// Sort entity actions if ordering is enabled.
	///
	/// @deprecated This method is not used by [org.hibernate.action.queue.internal.GraphBasedActionQueue]
	/// and is only needed for [org.hibernate.engine.spi.ActionQueueLegacy].  It will be removed when the
	/// legacy implementation is removed.
	@Deprecated(since = "7.0", forRemoval = true)
	default void sortActions() {
	}

	/// Sort collection actions if ordering is enabled.
	///
	/// @deprecated This method is not used by [org.hibernate.action.queue.internal.GraphBasedActionQueue]
	/// and is only needed for [org.hibernate.engine.spi.ActionQueueLegacy].  It will be removed when the
	/// legacy implementation is removed.
	@Deprecated(since = "7.0", forRemoval = true)
	default void sortCollectionActions() {
	}

	/// Un-schedule a deletion for an unloaded entity.
	///
	/// @param newEntity The entity being persisted
	void unScheduleUnloadedDeletion(Object newEntity);

	/// Un-schedule a deletion for an entity.
	///
	/// @param entry The entity entry
	/// @param rescuedEntity The entity being rescued from deletion
	void unScheduleDeletion(org.hibernate.engine.spi.EntityEntry entry, Object rescuedEntity);

	/// Clear actions that were added during a flush needed check.
	///
	/// @param previousCollectionRemovalSize The previous collection removal size
	void clearFromFlushNeededCheck(int previousCollectionRemovalSize);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Serialization
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/// Serialize the action queue.
	///
	/// @param oos The object output stream
	/// @throws IOException If an I/O error occurs
	///
	/// @see ActionQueueFactory#deserialize
	void serialize(ObjectOutputStream oos) throws IOException;
}
