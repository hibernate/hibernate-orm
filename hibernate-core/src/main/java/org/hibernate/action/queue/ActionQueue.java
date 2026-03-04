/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue;

import org.hibernate.HibernateException;
import org.hibernate.PropertyValueException;
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

/**
 * Common interface for ActionQueue implementations.
 * <p>
 * The ActionQueue is responsible for managing and executing all pending persistence actions
 * (inserts, updates, deletes, etc.) in a Hibernate session. Different implementations may
 * use different execution strategies.
 *
 * @author Steve Ebersole
 */
public interface ActionQueue extends TransactionCompletionCallbacks {

	/**
	 * Clear all pending actions.
	 */
	void clear();

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Action Registration
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Adds an entity insert action.
	 *
	 * @param action The action representing the entity insertion
	 */
	void addAction(EntityInsertAction action);

	/**
	 * Adds an identity-based entity insert action.
	 *
	 * @param action The action representing the entity insertion with identity generation
	 */
	void addAction(EntityIdentityInsertAction action);

	/**
	 * Adds an entity update action.
	 *
	 * @param action The action representing the entity update
	 */
	void addAction(EntityUpdateAction action);

	/**
	 * Adds an entity delete action.
	 *
	 * @param action The action representing the entity deletion
	 */
	void addAction(EntityDeleteAction action);

	/**
	 * Adds an orphan removal action.
	 *
	 * @param action The action representing orphan removal
	 */
	void addAction(OrphanRemovalAction action);

	/**
	 * Adds a collection recreation action.
	 *
	 * @param action The action representing the collection recreation
	 */
	void addAction(CollectionRecreateAction action);

	/**
	 * Adds a collection removal action.
	 *
	 * @param action The action representing the collection removal
	 */
	void addAction(CollectionRemoveAction action);

	/**
	 * Adds a collection update action.
	 *
	 * @param action The action representing the collection update
	 */
	void addAction(CollectionUpdateAction action);

	/**
	 * Adds a queued operation collection action.
	 *
	 * @param action The action representing the queued collection operation
	 */
	void addAction(QueuedOperationCollectionAction action);

	/**
	 * Adds a bulk operation cleanup action.
	 *
	 * @param action The action representing bulk operation cleanup
	 */
	void addAction(BulkOperationCleanupAction action);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Execution
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Execute identity insert actions.
	 *
	 * @throws HibernateException If an error occurs during execution
	 */
	void executeInserts() throws HibernateException;

	/**
	 * Execute all pending actions.
	 *
	 * @throws HibernateException If an error occurs during execution
	 */
	void executeActions() throws HibernateException;

	/**
	 * Prepare actions for execution (validation, sorting, etc.).
	 *
	 * @throws HibernateException If an error occurs during preparation
	 */
	void prepareActions() throws HibernateException;

	/**
	 * Execute pending bulk operation cleanup actions.
	 */
	void executePendingBulkOperationCleanUpActions();

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// State Queries
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Check if there are unresolved entity insert actions.
	 *
	 * @return true if there are unresolved entity insert actions
	 */
	boolean hasUnresolvedEntityInsertActions();

	/**
	 * Check if there are any queued actions.
	 *
	 * @return true if there are any queued actions
	 */
	boolean hasAnyQueuedActions();

	/**
	 * Check if there are before-transaction actions.
	 *
	 * @return true if there are before-transaction actions
	 */
	boolean hasBeforeTransactionActions();

	/**
	 * Check if there are after-transaction actions.
	 *
	 * @return true if there are after-transaction actions
	 */
	boolean hasAfterTransactionActions();

	/**
	 * Check if there are insertions or deletions queued.
	 *
	 * @return true if there are insertions or deletions queued
	 */
	boolean areInsertionsOrDeletionsQueued();

	/**
	 * Check if any of the specified tables are scheduled for update.
	 *
	 * @param tables The set of table names to check
	 * @return true if any of the tables are scheduled for update
	 */
	boolean areTablesToBeUpdated(Set<? extends Serializable> tables);

	/**
	 * Check that there are no unresolved actions after an operation.
	 *
	 * @throws PropertyValueException If there are unresolved actions
	 */
	void checkNoUnresolvedActionsAfterOperation() throws PropertyValueException;

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Statistics
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Get the number of insertions.
	 *
	 * @return The number of insertions
	 */
	int numberOfInsertions();

	/**
	 * Get the number of updates.
	 *
	 * @return The number of updates
	 */
	int numberOfUpdates();

	/**
	 * Get the number of deletions.
	 *
	 * @return The number of deletions
	 */
	int numberOfDeletions();

	/**
	 * Get the number of collection creations.
	 *
	 * @return The number of collection creations
	 */
	int numberOfCollectionCreations();

	/**
	 * Get the number of collection updates.
	 *
	 * @return The number of collection updates
	 */
	int numberOfCollectionUpdates();

	/**
	 * Get the number of collection removals.
	 *
	 * @return The number of collection removals
	 */
	int numberOfCollectionRemovals();

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Transaction Completion
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Get the transaction completion callbacks.
	 *
	 * @return The transaction completion callbacks implementor
	 */
	org.hibernate.engine.spi.TransactionCompletionCallbacksImplementor getTransactionCompletionCallbacks();

	/**
	 * Set the transaction completion callbacks.
	 *
	 * @param callbacks The transaction completion callbacks
	 * @param isTransactionCoordinatorShared Whether the transaction coordinator is shared
	 *
	 * @deprecated This method is not used by {@link GraphBasedActionQueue} and is only
	 *             needed for {@link org.hibernate.engine.spi.ActionQueueLegacy}.
	 *             It will be removed when the legacy implementation is removed.
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	void setTransactionCompletionCallbacks(
			org.hibernate.engine.spi.TransactionCompletionCallbacksImplementor callbacks,
			boolean isTransactionCoordinatorShared);

	/**
	 * Execute actions before transaction completion.
	 */
	void beforeTransactionCompletion();

	/**
	 * Execute actions after transaction completion.
	 *
	 * @param success Whether the transaction completed successfully
	 */
	void afterTransactionCompletion(boolean success);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Action Scheduling/Unscheduling
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Sort entity actions if ordering is enabled.
	 *
	 * @deprecated This method is not used by {@link GraphBasedActionQueue}, which uses
	 *             graph-based ordering instead. It is only needed for
	 *             {@link org.hibernate.engine.spi.ActionQueueLegacy}.
	 *             It will be removed when the legacy implementation is removed.
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	void sortActions();

	/**
	 * Sort collection actions if ordering is enabled.
	 *
	 * @deprecated This method is not used by {@link GraphBasedActionQueue}, which uses
	 *             graph-based ordering instead. It is only needed for
	 *             {@link org.hibernate.engine.spi.ActionQueueLegacy}.
	 *             It will be removed when the legacy implementation is removed.
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	void sortCollectionActions();

	/**
	 * Un-schedule a deletion for an unloaded entity.
	 *
	 * @param newEntity The entity being persisted
	 */
	void unScheduleUnloadedDeletion(Object newEntity);

	/**
	 * Un-schedule a deletion for an entity.
	 *
	 * @param entry The entity entry
	 * @param rescuedEntity The entity being rescued from deletion
	 */
	void unScheduleDeletion(org.hibernate.engine.spi.EntityEntry entry, Object rescuedEntity);

	/**
	 * Clear actions that were added during a flush needed check.
	 *
	 * @param previousCollectionRemovalSize The previous collection removal size
	 */
	void clearFromFlushNeededCheck(int previousCollectionRemovalSize);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Serialization
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Serialize the action queue.
	 *
	 * @param oos The object output stream
	 * @throws IOException If an I/O error occurs
	 */
	void serialize(ObjectOutputStream oos) throws IOException;
}
