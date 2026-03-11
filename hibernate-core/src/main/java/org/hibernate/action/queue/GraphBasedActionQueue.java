/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue;

import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.PropertyValueException;
import org.hibernate.action.internal.AbstractEntityInsertAction;
import org.hibernate.action.internal.BulkOperationCleanupAction;
import org.hibernate.action.internal.CollectionRecreateAction;
import org.hibernate.action.internal.CollectionRemoveAction;
import org.hibernate.action.internal.CollectionUpdateAction;
import org.hibernate.action.internal.EntityActionVetoException;
import org.hibernate.action.internal.EntityDeleteAction;
import org.hibernate.action.internal.EntityIdentityInsertAction;
import org.hibernate.action.internal.EntityInsertAction;
import org.hibernate.action.internal.EntityUpdateAction;
import org.hibernate.action.internal.OrphanRemovalAction;
import org.hibernate.action.internal.QueuedOperationCollectionAction;
import org.hibernate.action.queue.constraint.ConstraintModel;
import org.hibernate.action.spi.Executable;
import org.hibernate.engine.internal.TransactionCompletionCallbacksImpl;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.TransactionCompletionCallbacksImplementor;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.hibernate.action.internal.ActionLogging.ACTION_LOGGER;
import static org.hibernate.proxy.HibernateProxy.extractLazyInitializer;

/// ActionQueue implementation using FlushCoordinator for graph-based flush scheduling.
///
/// This implementation replaces the traditional action queue execution with graph-based
/// dependency ordering and cycle breaking.
///
/// @see FlushCoordinator
///
/// @author Steve Ebersole
@Incubating
public class GraphBasedActionQueue implements ActionQueue {
	private final SessionImplementor session;
	private final FlushCoordinator flushCoordinator;
	private final List<Executable> pendingActions;

	private final boolean isTransactionCoordinatorShared;
	private final TransactionCompletionCallbacksImplementor transactionCompletionCallbacks;

	// counters for stats/logging
	private int insertCount;
	private int updateCount;
	private int deleteCount;
	private int collectionCreationCount;
	private int collectionUpdateCount;
	private int collectionRemovalCount;

	/// Construct a GraphBasedActionQueue for the given session.
	///
	/// @param constraintModel Details about foreign-key and unique constraints defined in the model.
	/// @param planningOptions Options for graph building and planning.
	/// @param session The session
	public GraphBasedActionQueue(ConstraintModel constraintModel, PlanningOptions planningOptions, SessionImplementor session) {
		ACTION_LOGGER.usingActionQueue( getClass().getName() );
		this.session = session;
		this.flushCoordinator = new FlushCoordinator( constraintModel, planningOptions, session );
		this.pendingActions = new ArrayList<>();
		this.transactionCompletionCallbacks = new TransactionCompletionCallbacksImpl(session);
		this.isTransactionCoordinatorShared = false;
	}

	/**
	 * Clear all pending actions.
	 */
	public void clear() {
		pendingActions.clear();
		flushCoordinator.getDecomposer().clear();

		insertCount = 0;
		updateCount = 0;
		deleteCount = 0;
		collectionCreationCount = 0;
		collectionUpdateCount = 0;
		collectionRemovalCount = 0;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Action Registration
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Adds an entity insert action.
	 *
	 * @param action The action representing the entity insertion
	 */
	public void addAction(EntityInsertAction action) {
		ACTION_LOGGER.addingEntityInsertAction(action.getEntityName());
		addInsertAction(action);
	}

	/**
	 * Adds an entity (IDENTITY) insert action.
	 *
	 * @param action The action representing the entity insertion
	 */
	public void addAction(EntityIdentityInsertAction action) {
		ACTION_LOGGER.addingEntityIdentityInsertAction(action.getEntityName());
		addInsertAction(action);
	}

	private void addInsertAction(AbstractEntityInsertAction insert) {
		// IDENTITY inserts must execute immediately to generate IDs
		// Delegate execution to FlushCoordinator but trigger it now, not at flush time
		if (insert.isEarlyInsert()) {
			// IMPORTANT: Check for transient dependencies FIRST, before flushing pending inserts
			// This prevents a cascade deadlock where:
			// 1. Entity A (IDENTITY) deferred with transient dep on B
			// 2. Entity B (IDENTITY) tries to insert
			// 3. executePendingInserts tries to execute A
			// 4. But B is still transient (being processed) → fail!
			final var nonNullableTransientDeps = insert.findNonNullableTransientEntities();
			if (nonNullableTransientDeps != null) {
				// Defer this IDENTITY insert - DON'T add to pendingActions
				// Only track in Decomposer's unresolvedInserts
				// It will be resolved and executed via executeIdentityInsert when dependencies are satisfied
				// NOTE: Don't call makeEntityManaged() here - the entity stays transient until resolved
				// Track in Decomposer manually since we're not going through decompose()
				flushCoordinator.getDecomposer().trackUnresolvedInsert(insert, nonNullableTransientDeps);
				insertCount++;
				return;
			}

			// Execute IDENTITY insert immediately via FlushCoordinator
			// NOTE: executePendingInserts is called INSIDE executeIdentityInsert
			// AFTER makeEntityManaged() so this entity is available for FK references
			flushCoordinator.executeIdentityInsert(insert, this::executePendingInserts);

			// Still increment counter for stats
			insertCount++;

			// Don't add to pendingActions - already executed
			return;
		}

		// Regular inserts are queued for later execution
		ACTION_LOGGER.addingResolvedNonEarlyInsertAction();
		pendingActions.add(insert);
		insertCount++;

		// Check for unresolved transient dependencies before making entity managed
		// This prevents PropertyValueException in circular cascade scenarios
		final var transientDeps = insert.findNonNullableTransientEntities();
		if (transientDeps == null || transientDeps.isEmpty()) {
			// Safe to make entity managed - no unresolved dependencies
			if (!insert.isVeto()) {
				insert.makeEntityManaged();
			}
			else {
				throw new EntityActionVetoException(
						"The EntityInsertAction was vetoed.",
						insert
				);
			}
		}
		// Else: entity has unresolved dependencies, leave transient for now
		// Decomposer will track and resolve it later
	}

	/**
	 * Executes all pending insert actions.
	 * <p>
	 * This is necessary before executing IDENTITY inserts to ensure parent entities
	 * with assigned IDs are in the database before children with IDENTITY generation
	 * try to insert with foreign keys referencing those parents.
	 * <p>
	 * Mirrors the behavior of ActionQueueLegacy.executeInserts() which is called
	 * before processing early (IDENTITY) inserts.
	 */
	private void executePendingInserts() {
		if (pendingActions.isEmpty()) {
			return;
		}

		// Collect all insert actions from pending list
		final List<Executable> insertsToExecute = new ArrayList<>();
		for (Executable action : pendingActions) {
			if (action instanceof AbstractEntityInsertAction) {
				insertsToExecute.add(action);
			}
		}

		if (insertsToExecute.isEmpty()) {
			return;
		}

		// Make insert entities managed before execution
		for (Executable action : insertsToExecute) {
			if (action instanceof AbstractEntityInsertAction) {
				AbstractEntityInsertAction insert = (AbstractEntityInsertAction) action;
				if (!insert.isVeto()) {
					insert.makeEntityManaged();
				}
			}
		}

		// Execute these inserts via FlushCoordinator
		flushCoordinator.executeFlush(insertsToExecute);

		// Remove executed actions from pending list
		pendingActions.removeAll(insertsToExecute);
	}

	/// Adds an entity update action.
	///
	/// @param action The action representing the entity update
	public void addAction(EntityUpdateAction action) {
		updateCount++;
		pendingActions.add(action);
	}

	/// Adds an entity delete action.
	///
	/// @param action The action representing the entity deletion
	public void addAction(EntityDeleteAction action) {
		deleteCount++;
		pendingActions.add(action);
	}

	/**
	 * Adds an orphan removal action.
	 *
	 * @param action The action representing the orphan removal
	 */
	public void addAction(OrphanRemovalAction action) {
		pendingActions.add(action);
	}

	/**
	 * Adds a collection (re)create action.
	 *
	 * @param action The action representing the (re)creation of a collection
	 */
	public void addAction(CollectionRecreateAction action) {
		collectionCreationCount++;
		pendingActions.add(action);
	}

	/**
	 * Adds a collection remove action.
	 *
	 * @param action The action representing the removal of a collection
	 */
	public void addAction(CollectionRemoveAction action) {
		collectionRemovalCount++;
		// Check if this should be an orphan collection removal
		// (handled by FlushCoordinator's ordering instead of special list)
		pendingActions.add(action);
	}

	/**
	 * Adds a collection update action.
	 *
	 * @param action The action representing the update of a collection
	 */
	public void addAction(CollectionUpdateAction action) {
		collectionUpdateCount++;
		pendingActions.add(action);
	}

	/**
	 * Adds an action relating to a collection queued operation (extra lazy).
	 *
	 * @param action The action representing the queued operation
	 */
	public void addAction(QueuedOperationCollectionAction action) {
		pendingActions.add(action);
	}

	/**
	 * Adds an action defining a cleanup relating to a bulk operation.
	 *
	 * @param action The action representing the bulk operation cleanup
	 */
	public void addAction(BulkOperationCleanupAction action) {
		registerCleanupActions(action);
	}

	private void registerCleanupActions(Executable executable) {
		final var beforeCompletionCallback = executable.getBeforeTransactionCompletionProcess();
		if (beforeCompletionCallback != null) {
			transactionCompletionCallbacks.registerCallback(beforeCompletionCallback);
		}
		if (session.getFactory().getSessionFactoryOptions().isQueryCacheEnabled()) {
			invalidateSpaces(executable.getPropertySpaces());
		}
		final var afterCompletionCallback = executable.getAfterTransactionCompletionProcess();
		if (afterCompletionCallback != null) {
			transactionCompletionCallbacks.registerCallback(afterCompletionCallback);
		}
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Execution
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Perform all currently queued entity-insertion actions.
	 *
	 * @throws HibernateException error executing queued insertion actions
	 */
	public void executeInserts() throws HibernateException {
		List<Executable> insertActions = new ArrayList<>();
		for (Executable action : pendingActions) {
			if (action instanceof AbstractEntityInsertAction) {
				insertActions.add(action);
			}
		}

		if (!insertActions.isEmpty()) {
			// Make insert entities managed before execution
			for (Executable action : insertActions) {
				if (action instanceof AbstractEntityInsertAction) {
					AbstractEntityInsertAction insert = (AbstractEntityInsertAction) action;
					if (!insert.isVeto()) {
						insert.makeEntityManaged();
					}
				}
			}

			flushCoordinator.executeFlush(insertActions);
			pendingActions.removeAll(insertActions);
			session.getJdbcCoordinator().executeBatch();
		}
	}

	/**
	 * Perform all currently queued actions.
	 *
	 * @throws HibernateException error executing queued actions
	 */
	public void executeActions() throws HibernateException {
		if (!pendingActions.isEmpty()) {
			// Delegate to FlushCoordinator for graph-based execution
			flushCoordinator.executeFlush(pendingActions);

			// Register transaction completion callbacks for executed actions
			for (Executable action : pendingActions) {
				registerCleanupActions(action);
			}

			// Clear pending actions
			pendingActions.clear();

			// Reset counters after execution
			insertCount = 0;
			updateCount = 0;
			deleteCount = 0;
			collectionCreationCount = 0;
			collectionUpdateCount = 0;
			collectionRemovalCount = 0;

			// Execute any pending JDBC batch
			session.getJdbcCoordinator().executeBatch();

			// Invalidate query cache regions
			if (session.getFactory().getSessionFactoryOptions().isQueryCacheEnabled()) {
				// FlushCoordinator already tracked affected spaces
				// TODO: Could optimize by collecting spaces during execution
			}
		}
		else {
			ACTION_LOGGER.debugf(
				"[GRAPH-AQ-DEBUG] executeActions: no pending actions to execute"
			);
		}
	}

	/**
	 * Prepares the internal action queues for execution.
	 * <p>
	 * Note: With FlushCoordinator, most preparation happens during decomposition,
	 * but we maintain this method for API compatibility.
	 *
	 * @throws HibernateException error preparing actions
	 */
	public void prepareActions() throws HibernateException {
		for (Executable action : pendingActions) {
			if (action instanceof CollectionRecreateAction
					|| action instanceof CollectionUpdateAction
					|| action instanceof CollectionRemoveAction
					|| action instanceof QueuedOperationCollectionAction) {
				action.beforeExecutions();
			}
		}
	}

	/**
	 * Sort entity actions.
	 * <p>
	 * Note: With GraphBasedActionQueue, sorting is handled by FlushCoordinator's graph-based
	 * ordering, so this is a no-op for API compatibility.
	 *
	 * @deprecated This method is not used by GraphBasedActionQueue. It exists only for
	 *             API compatibility with {@link org.hibernate.engine.spi.ActionQueueLegacy}.
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	@Override
	public void sortActions() {
		// No-op: FlushCoordinator handles ordering via graph
	}

	/**
	 * Sort collection actions.
	 * <p>
	 * Note: With GraphBasedActionQueue, sorting is handled by FlushCoordinator's graph-based
	 * ordering, so this is a no-op for API compatibility.
	 *
	 * @deprecated This method is not used by GraphBasedActionQueue. It exists only for
	 *             API compatibility with {@link org.hibernate.engine.spi.ActionQueueLegacy}.
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	@Override
	public void sortCollectionActions() {
		// No-op: FlushCoordinator handles ordering via graph
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Query Methods
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Are there unresolved entity insert actions?
	 *
	 * @return true if there are unresolved entity insert actions
	 */
	public boolean hasUnresolvedEntityInsertActions() {
		return flushCoordinator.getDecomposer().hasUnresolvedInserts();
	}

	/**
	 * Get the number of entity insertions currently queued.
	 *
	 * @return count of entity insertions
	 */
	public int numberOfInsertions() {
		return insertCount;
	}

	/**
	 * Get the number of entity updates currently queued.
	 *
	 * @return count of entity updates
	 */
	public int numberOfUpdates() {
		return updateCount;
	}

	/**
	 * Get the number of entity deletions currently queued.
	 *
	 * @return count of entity deletions
	 */
	public int numberOfDeletions() {
		return deleteCount;
	}

	/**
	 * Get the number of collection creations currently queued.
	 *
	 * @return count of collection creations
	 */
	public int numberOfCollectionCreations() {
		return collectionCreationCount;
	}

	/**
	 * Get the number of collection updates currently queued.
	 *
	 * @return count of collection updates
	 */
	public int numberOfCollectionUpdates() {
		return collectionUpdateCount;
	}

	/**
	 * Get the number of collection removals currently queued.
	 *
	 * @return count of collection removals
	 */
	public int numberOfCollectionRemovals() {
		return collectionRemovalCount;
	}

	/**
	 * Are there before transaction completion actions registered?
	 *
	 * @return true if there are before transaction actions
	 */
	public boolean hasBeforeTransactionActions() {
		return !isTransactionCoordinatorShared
				&& transactionCompletionCallbacks.hasBeforeCompletionCallbacks();
	}

	/**
	 * Are there after transaction completion actions registered?
	 *
	 * @return true if there are after transaction actions
	 */
	public boolean hasAfterTransactionActions() {
		return !isTransactionCoordinatorShared
				&& transactionCompletionCallbacks.hasAfterCompletionCallbacks();
	}

	/**
	 * Check whether any insertion or deletion actions are currently queued.
	 *
	 * @return true if insertions or deletions are currently queued
	 */
	public boolean areInsertionsOrDeletionsQueued() {
		for (Executable action : pendingActions) {
			if (action instanceof AbstractEntityInsertAction
					|| action instanceof EntityDeleteAction
					|| action instanceof OrphanRemovalAction) {
				return true;
			}
		}
		return hasUnresolvedEntityInsertActions();
	}

	/**
	 * Check whether the given tables/query-spaces are to be updated.
	 *
	 * @param tables The table/query-spaces to check
	 * @return true if we contain pending actions against any of the given tables
	 */
	public boolean areTablesToBeUpdated(Set<? extends Serializable> tables) {
		if (tables.isEmpty()) {
			return false;
		}

		for (Executable action : pendingActions) {
			for (Serializable space : action.getPropertySpaces()) {
				if (tables.contains(space)) {
					ACTION_LOGGER.changesMustBeFlushedToSpace(space);
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Check if there are any queued actions.
	 *
	 * @return true if there are any queued actions
	 */
	public boolean hasAnyQueuedActions() {
		return !pendingActions.isEmpty() || hasUnresolvedEntityInsertActions();
	}

	/**
	 * Validate that there are no unresolved entity insert actions.
	 *
	 * @throws PropertyValueException if there are unresolved inserts
	 */
	public void checkNoUnresolvedActionsAfterOperation() throws PropertyValueException {
		flushCoordinator.getDecomposer().validateNoUnresolvedInserts();
	}

	/**
	 * Clear queued actions that were added during a flush-needed check.
	 * This is used when auto-flush determines that a flush is not actually needed.
	 *
	 * @param previousCollectionRemovalSize the size of collection removals before the check
	 */
	public void clearFromFlushNeededCheck(int previousCollectionRemovalSize) {
		// Remove all actions except:
		// - Inserts (keep them)
		// - Deletes (keep them)
		// - Collection removals that existed before the check

		int collectionRemovalCountLocal = 0;
		List<Executable> toKeep = new ArrayList<>();

		for (Executable action : pendingActions) {
			if (action instanceof AbstractEntityInsertAction
					|| action instanceof EntityDeleteAction
					|| action instanceof OrphanRemovalAction) {
				toKeep.add(action);
			}
			else if (action instanceof CollectionRemoveAction) {
				if (collectionRemovalCountLocal < previousCollectionRemovalSize) {
					toKeep.add(action);
				}
				collectionRemovalCountLocal++;
			}
			// Skip: CollectionRecreateAction, CollectionUpdateAction, EntityUpdateAction, QueuedOperationCollectionAction
		}

		pendingActions.clear();
		pendingActions.addAll(toKeep);

		// Recalculate counters after clearing actions
		recalculateCounters();
	}

	/**
	 * Recalculate action counters by scanning pending actions.
	 * Used after clearing actions during auto-flush checks.
	 */
	private void recalculateCounters() {
		insertCount = 0;
		updateCount = 0;
		deleteCount = 0;
		collectionCreationCount = 0;
		collectionUpdateCount = 0;
		collectionRemovalCount = 0;

		for (Executable action : pendingActions) {
			if (action instanceof EntityInsertAction || action instanceof EntityIdentityInsertAction) {
				insertCount++;
			}
			else if (action instanceof EntityUpdateAction) {
				updateCount++;
			}
			else if (action instanceof EntityDeleteAction || action instanceof OrphanRemovalAction) {
				deleteCount++;
			}
			else if (action instanceof CollectionRecreateAction) {
				collectionCreationCount++;
			}
			else if (action instanceof CollectionUpdateAction) {
				collectionUpdateCount++;
			}
			else if (action instanceof CollectionRemoveAction) {
				collectionRemovalCount++;
			}
		}
	}

	/**
	 * Remove a scheduled deletion for an entity.
	 * Used when an entity is rescued from deletion (e.g., during merge).
	 *
	 * @param entry the entity entry
	 * @param rescuedEntity the entity being rescued
	 */
	public void unScheduleDeletion(EntityEntry entry, Object rescuedEntity) {
		final var lazyInitializer = extractLazyInitializer(rescuedEntity);
		if (lazyInitializer != null && !lazyInitializer.isUninitialized()) {
			rescuedEntity = lazyInitializer.getImplementation(session);
		}

		final Object entityToMatch = rescuedEntity;
		pendingActions.removeIf(action -> {
			if (action instanceof EntityDeleteAction delete) {
				return delete.getInstance() == entityToMatch;
			}
			else if (action instanceof OrphanRemovalAction orphan) {
				return orphan.getInstance() == entityToMatch;
			}
			return false;
		});
	}

	/**
	 * Remove a scheduled deletion for an unloaded entity.
	 * Used when an entity instance is being merged/saved but was previously scheduled for deletion.
	 *
	 * @param newEntity the new entity instance
	 */
	public void unScheduleUnloadedDeletion(Object newEntity) {
		final var entityPersister = session.getEntityPersister(null, newEntity);
		final Object identifier = entityPersister.getIdentifier(newEntity, session);
		final String entityName = entityPersister.getEntityName();

		pendingActions.removeIf(action -> {
			if (action instanceof EntityDeleteAction delete) {
				if (delete.getInstance() == null
						&& delete.getEntityName().equals(entityName)
						&& entityPersister.getIdentifierMapping().areEqual(delete.getId(), identifier, session)) {
					session.getPersistenceContextInternal()
							.removeDeletedUnloadedEntityKey(session.generateEntityKey(identifier, entityPersister));
					return true;
				}
			}
			return false;
		});
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Transaction Completion Callbacks
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Set the transaction completion callbacks.
	 * Used when a session shares a transaction coordinator.
	 *
	 * @param callbacks the callbacks to use
	 * @param isTransactionCoordinatorShared whether the transaction coordinator is shared
	 *
	 * @deprecated This method is not used by GraphBasedActionQueue, which manages its own
	 *             callbacks internally. It exists only for API compatibility with
	 *             {@link org.hibernate.engine.spi.ActionQueueLegacy}.
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	@Override
	public void setTransactionCompletionCallbacks(
			TransactionCompletionCallbacksImplementor callbacks,
			boolean isTransactionCoordinatorShared) {
		// No-op: GraphBasedActionQueue manages its own callbacks internally
	}

	/**
	 * Get the transaction completion callbacks.
	 *
	 * @return the transaction completion callbacks
	 */
	public TransactionCompletionCallbacksImplementor getTransactionCompletionCallbacks() {
		return transactionCompletionCallbacks;
	}

	@Override
	public void registerCallback(BeforeCompletionCallback process) {
		transactionCompletionCallbacks.registerCallback(process);
	}

	@Override
	public void registerCallback(AfterCompletionCallback process) {
		transactionCompletionCallbacks.registerCallback(process);
	}

	/**
	 * Execute any registered {@link org.hibernate.action.spi.BeforeTransactionCompletionProcess}.
	 */
	public void beforeTransactionCompletion() {
		if (!isTransactionCoordinatorShared) {
			transactionCompletionCallbacks.beforeTransactionCompletion();
			session.getJdbcCoordinator().executeBatch();
		}
	}

	/**
	 * Performs cleanup of any held cache soft locks.
	 *
	 * @param success Was the transaction successful
	 */
	public void afterTransactionCompletion(boolean success) {
		if (!isTransactionCoordinatorShared) {
			transactionCompletionCallbacks.afterTransactionCompletion(success);
		}
	}

	/**
	 * Execute pending bulk operation cleanup actions.
	 */
	public void executePendingBulkOperationCleanUpActions() {
		if (!isTransactionCoordinatorShared && transactionCompletionCallbacks != null) {
			transactionCompletionCallbacks.executePendingBulkOperationCleanUpActions();
		}
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Internal Helpers
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private void invalidateSpaces(String[] spaces) {
		if (spaces != null && spaces.length > 0) {
			for (String space : spaces) {
				transactionCompletionCallbacks.addSpaceToInvalidate(space);
			}
			session.getFactory().getCache().getTimestampsCache().preInvalidate(spaces, session);
		}
	}

	/**
	 * Get all pending actions (for debugging/testing).
	 *
	 * @return list of pending actions
	 */
	public List<Executable> getPendingActions() {
		return new ArrayList<>(pendingActions);
	}

	/**
	 * Get the FlushCoordinator (for testing/debugging).
	 *
	 * @return the flush coordinator
	 */
	public FlushCoordinator getFlushCoordinator() {
		return flushCoordinator;
	}

	/**
	 * Serialize the action queue.
	 * <p>
	 * Note: Serialization is not yet fully implemented for GraphBasedActionQueue.
	 * This method is a stub for API compatibility.
	 *
	 * @param oos the output stream
	 * @throws IOException if serialization fails
	 */
	public void serialize(ObjectOutputStream oos) throws IOException {
		// TODO: Implement proper serialization for GraphBasedActionQueue
		// For now, just write minimal state
		oos.writeInt(0); // Placeholder version number
	}

	@Override
	public String toString() {
		int insertCount = 0;
		int updateCount = 0;
		int deleteCount = 0;
		int collectionCount = 0;

		for (Executable action : pendingActions) {
			if (action instanceof AbstractEntityInsertAction) {
				insertCount++;
			}
			else if (action instanceof EntityUpdateAction) {
				updateCount++;
			}
			else if (action instanceof EntityDeleteAction || action instanceof OrphanRemovalAction) {
				deleteCount++;
			}
			else if (action instanceof CollectionRecreateAction
					|| action instanceof CollectionUpdateAction
					|| action instanceof CollectionRemoveAction) {
				collectionCount++;
			}
		}

		return "GraphBasedActionQueue[insertions=" + insertCount
				+ " updates=" + updateCount
				+ " deletions=" + deleteCount
				+ " collections=" + collectionCount
				+ " unresolved=" + (hasUnresolvedEntityInsertActions() ? "yes" : "no")
				+ "]";
	}
}
