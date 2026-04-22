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
import org.hibernate.action.queue.support.GraphBasedActionQueueFactory;
import org.hibernate.action.spi.Executable;
import org.hibernate.engine.internal.TransactionCompletionCallbacksImpl;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.TransactionCompletionCallbacksImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;

import java.io.IOException;
import java.io.ObjectInputStream;
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

	// Action lists - maintain same order as legacy ActionQueue for proper dependency resolution
	private final List<CollectionRemoveAction> orphanCollectionRemovals;
	private final List<OrphanRemovalAction> orphanRemovals;
	private final List<AbstractEntityInsertAction> insertions;
	private final List<EntityUpdateAction> updates;
	private final List<QueuedOperationCollectionAction> collectionQueuedOps;
	private final List<CollectionRemoveAction> collectionRemovals;
	private final List<CollectionUpdateAction> collectionUpdates;
	private final List<CollectionRecreateAction> collectionCreations;
	private final List<EntityDeleteAction> deletions;

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
		this.session = session;
		this.flushCoordinator = new FlushCoordinator( constraintModel, planningOptions, session );

		this.orphanCollectionRemovals = new ArrayList<>();
		this.orphanRemovals = new ArrayList<>();
		this.insertions = new ArrayList<>();
		this.updates = new ArrayList<>();
		this.collectionQueuedOps = new ArrayList<>();
		this.collectionRemovals = new ArrayList<>();
		this.collectionUpdates = new ArrayList<>();
		this.collectionCreations = new ArrayList<>();
		this.deletions = new ArrayList<>();

		this.transactionCompletionCallbacks = new TransactionCompletionCallbacksImpl(session);
		this.isTransactionCoordinatorShared = false;
	}

	/// Deserialization constructor.
	/// @see #deserialize(ObjectInputStream, GraphBasedActionQueueFactory, SessionImplementor)
	public GraphBasedActionQueue(
			FlushCoordinator flushCoordinator,
			List<CollectionRemoveAction> orphanCollectionRemovals,
			List<OrphanRemovalAction> orphanRemovals,
			List<AbstractEntityInsertAction> insertions,
			List<EntityUpdateAction> updates,
			List<QueuedOperationCollectionAction> collectionQueuedOps,
			List<CollectionRemoveAction> collectionRemovals,
			List<CollectionUpdateAction> collectionUpdates,
			List<CollectionRecreateAction> collectionCreations,
			List<EntityDeleteAction> deletions,
			SessionImplementor session) {
		this.session = session;
		this.flushCoordinator = flushCoordinator;

		this.orphanCollectionRemovals = orphanCollectionRemovals;
		this.orphanRemovals = orphanRemovals;
		this.insertions = insertions;
		this.updates = updates;
		this.collectionQueuedOps = collectionQueuedOps;
		this.collectionRemovals = collectionRemovals;
		this.collectionUpdates = collectionUpdates;
		this.collectionCreations = collectionCreations;
		this.deletions = deletions;

		this.transactionCompletionCallbacks = new TransactionCompletionCallbacksImpl(session);
		this.isTransactionCoordinatorShared = false;
	}

	/// Clear all pending actions.
	public void clear() {
		orphanCollectionRemovals.clear();
		orphanRemovals.clear();
		insertions.clear();
		updates.clear();
		collectionQueuedOps.clear();
		collectionRemovals.clear();
		collectionUpdates.clear();
		collectionCreations.clear();
		deletions.clear();
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

	/// Adds an entity insert action.
	///
	/// @param action The action representing the entity insertion
	public void addAction(EntityInsertAction action) {
		ACTION_LOGGER.addingEntityInsertAction(action.getEntityName());
		addInsertAction(action);
	}

	/// Adds an entity (IDENTITY) insert action.
	///
	/// @param action The action representing the entity insertion
	public void addAction(EntityIdentityInsertAction action) {
		ACTION_LOGGER.addingEntityIdentityInsertAction(action.getEntityName());
		addInsertAction(action);
	}

	private void addInsertAction(AbstractEntityInsertAction insert) {
		// IDENTITY inserts must execute immediately to generate IDs
		// Delegate execution to FlushCoordinator but trigger it now, not at flush time
		if (insert.isEarlyInsert()) {
			// For early inserts, must execute pending inserts BEFORE checking for transient dependencies
			// This allows cascaded persist operations to complete, preventing circular reference issues
			// For example: persist(item1) cascades to category, which cascades back to item1 (exampleItem)
			// If we check transient deps first, both would be deferred causing deadlock
			ACTION_LOGGER.executingInsertsBeforeFindingNonNullableTransientEntitiesForEarlyInsert(insert);
			executePendingInserts();

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

			// Register cleanup actions for this early insert (matches legacy queue pattern)
			registerCleanupActions(insert);

			// Still increment counter for stats
			insertCount++;

			// Don't add to pendingActions - already executed
			return;
		}

		// Regular inserts are queued for later execution
		ACTION_LOGGER.addingResolvedNonEarlyInsertAction();
		insertions.add(insert);
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

	/// Executes all pending insert actions.
	///
	/// This is necessary before executing IDENTITY inserts to ensure parent entities
	/// with assigned IDs are in the database before children with IDENTITY generation
	/// try to insert with foreign keys referencing those parents.
	///
	/// Mirrors the behavior of ActionQueueLegacy.executeInserts() which is called
	/// before processing early (IDENTITY) inserts.
	private void executePendingInserts() {
		if (insertions.isEmpty()) {
			return;
		}

		// Make insert entities managed before execution
		for (AbstractEntityInsertAction action : insertions) {
			if (!action.isVeto()) {
				action.makeEntityManaged();
			}
		}

		// Execute these inserts via FlushCoordinator
		final List<AbstractEntityInsertAction> executedInserts = new ArrayList<>(insertions);
		flushCoordinator.executeInsertFlush(executedInserts);

		// Register cleanup actions for executed inserts (matches legacy queue pattern)
		for (AbstractEntityInsertAction action : executedInserts) {
			registerCleanupActions(action);
		}

		// Clear executed actions from pending list
		insertions.clear();
	}

	/// Adds an entity update action.
	///
	/// @param action The action representing the entity update
	public void addAction(EntityUpdateAction action) {
		updateCount++;
		updates.add(action);
	}

	/// Adds an entity delete action.
	///
	/// @param action The action representing the entity deletion
	public void addAction(EntityDeleteAction action) {
		deleteCount++;
		deletions.add(action);
	}

	/// Adds an orphan removal action.
	///
	/// @param action The action representing the orphan removal
	public void addAction(OrphanRemovalAction action) {
		orphanRemovals.add(action);
	}

	/// Adds a collection (re)create action.
	///
	/// @param action The action representing the (re)creation of a collection
	public void addAction(CollectionRecreateAction action) {
		ACTION_LOGGER.tracef( "GraphBasedActionQueue.addAction(CollectionRecreateAction) - role=%s, key=%s", action.getPersister().getRole(), action.getKey() );
		collectionCreationCount++;
		collectionCreations.add(action);
	}

	/// Adds a collection remove action.
	///
	/// @param action The action representing the removal of a collection
	public void addAction(CollectionRemoveAction action) {
		collectionRemovalCount++;

		// Check if this is an orphan collection removal (owner is being orphan-removed)
		// If so, add to orphanCollectionRemovals to execute before orphan removals
		if (!orphanRemovals.isEmpty() && action.getAffectedOwner() != null) {
			final EntityEntry entry = session.getPersistenceContextInternal()
					.getEntry(action.getAffectedOwner());
			if (entry != null && entry.getStatus().isDeletedOrGone()) {
				// Check if any orphan removal action exists for this owner
				for (OrphanRemovalAction orphanAction : orphanRemovals) {
					if (orphanAction.getInstance() == action.getAffectedOwner()) {
						orphanCollectionRemovals.add(action);
						return;
					}
				}
			}
		}

		collectionRemovals.add(action);
	}

	/// Adds a collection update action.
	///
	/// @param action The action representing the update of a collection
	public void addAction(CollectionUpdateAction action) {
		collectionUpdateCount++;
		collectionUpdates.add(action);
	}

	/// Adds an action relating to a collection queued operation (extra lazy).
	///
	/// @param action The action representing the queued operation
	public void addAction(QueuedOperationCollectionAction action) {
		collectionQueuedOps.add(action);
	}

	/// Adds an action defining a cleanup relating to a bulk operation.
	///
	/// @param action The action representing the bulk operation cleanup
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

	/// Perform all currently queued entity-insertion actions.
	///
	/// @throws HibernateException error executing queued insertion actions
	public void executeInserts() throws HibernateException {
		if (!insertions.isEmpty()) {
			// Make insert entities managed before execution
			for (AbstractEntityInsertAction action : insertions) {
				if (!action.isVeto()) {
					action.makeEntityManaged();
				}
			}

			final List<AbstractEntityInsertAction> executedInserts = new ArrayList<>(insertions);
			flushCoordinator.executeInsertFlush(executedInserts);

			// Register cleanup actions for executed inserts (matches legacy queue pattern)
			for (AbstractEntityInsertAction action : executedInserts) {
				registerCleanupActions(action);
			}

			insertions.clear();

			session.getJdbcCoordinator().executeBatch();
		}
	}

	/// Perform all currently queued actions.
	///
	/// @throws HibernateException error executing queued actions
	public void executeActions() throws HibernateException {
		if ( ACTION_LOGGER.isTraceEnabled() ) {
			int totalActions = orphanCollectionRemovals.size() + orphanRemovals.size() + insertions.size()
					+ updates.size() + collectionQueuedOps.size() + collectionRemovals.size()
					+ collectionUpdates.size() + collectionCreations.size() + deletions.size();

			if ( totalActions == 0 ) {
				ACTION_LOGGER.tracef("executeActions: no pending actions to execute" );
				// EARLY EXIT!!
			}

			ACTION_LOGGER.tracef( "GraphBasedActionQueue.executeActions() - %d total actions", totalActions );
		}

		// Delegate to FlushCoordinator for graph-based execution
		// Pass separate action lists to preserve phase boundaries and cascade metadata
		flushCoordinator.executeFlush(
				orphanCollectionRemovals,
				orphanRemovals,
				insertions,
				updates,
				collectionQueuedOps,
				collectionRemovals,
				collectionUpdates,
				collectionCreations,
				deletions
		);

		// Collect before and after transaction-completion callbacks, and
		// collect all unique spaces from all actions
		final List<String> allSpaces = new ArrayList<>();
		prepareForTransactionCompletion( orphanCollectionRemovals, allSpaces );
		prepareForTransactionCompletion( orphanRemovals, allSpaces );
		prepareForTransactionCompletion( insertions, allSpaces );
		prepareForTransactionCompletion( updates, allSpaces );
		prepareForTransactionCompletion( collectionQueuedOps, allSpaces );
		prepareForTransactionCompletion( collectionRemovals, allSpaces );
		prepareForTransactionCompletion( collectionUpdates, allSpaces );
		prepareForTransactionCompletion( collectionCreations, allSpaces );
		prepareForTransactionCompletion( deletions, allSpaces );

		if (!allSpaces.isEmpty()) {
			invalidateSpaces(allSpaces.toArray(new String[0]));
		}

		// clear all pending actions
		clear();

		// Execute any pending JDBC batch
		session.getJdbcCoordinator().executeBatch();
	}

	private void prepareForTransactionCompletion(
			List<? extends Executable> actions,
			List<String> allSpaces) {
		var isQueryCacheEnabled = session.getFactory().getSessionFactoryOptions().isQueryCacheEnabled();
		for ( Executable action : actions ) {
			final var beforeCompletionCallback = action.getBeforeTransactionCompletionProcess();
			if (beforeCompletionCallback != null) {
				transactionCompletionCallbacks.registerCallback(beforeCompletionCallback);
			}
			final var afterCompletionCallback = action.getAfterTransactionCompletionProcess();
			if (afterCompletionCallback != null) {
				transactionCompletionCallbacks.registerCallback(afterCompletionCallback);
			}

			if ( isQueryCacheEnabled ) {
				final String[] spaces = action.getPropertySpaces();
				if ( CollectionHelper.isNotEmpty( spaces ) ) {
					for ( String space : spaces) {
						if ( !allSpaces.contains(space) ) {
							allSpaces.add( space );
						}
					}
				}
			}
		}
	}

	/// Prepares the internal action queues for execution.
	///
	/// Note: With FlushCoordinator, most preparation happens during decomposition,
	/// but we maintain this method for API compatibility.
	///
	/// @throws HibernateException error preparing actions
	public void prepareActions() throws HibernateException {
		prepareCollectionActions(collectionRemovals);
		prepareCollectionActions(collectionUpdates);
		prepareCollectionActions(collectionCreations);
		prepareCollectionActions(collectionQueuedOps);
	}

	private void prepareCollectionActions(List<? extends Executable> actions) throws HibernateException {
		for (Executable action : actions) {
			action.beforeExecutions();
		}
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Query Methods
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/// Are there unresolved entity insert actions?
	///
	/// @return true if there are unresolved entity insert actions
	public boolean hasUnresolvedEntityInsertActions() {
		return flushCoordinator.getDecomposer().hasUnresolvedInserts();
	}

	/// Get the number of entity insertions currently queued.
	///
	/// @return count of entity insertions
	public int numberOfInsertions() {
		return insertCount;
	}

	/// Get the number of entity updates currently queued.
	///
	/// @return count of entity updates
	public int numberOfUpdates() {
		return updateCount;
	}

	/// Get the number of entity deletions currently queued.
	///
	/// @return count of entity deletions
	public int numberOfDeletions() {
		return deleteCount;
	}

	/// Get the number of collection creations currently queued.
	///
	/// @return count of collection creations
	public int numberOfCollectionCreations() {
		return collectionCreationCount;
	}

	/// Get the number of collection updates currently queued.
	///
	/// @return count of collection updates
	public int numberOfCollectionUpdates() {
		return collectionUpdateCount;
	}

	/// Get the number of collection removals currently queued.
	///
	/// @return count of collection removals
	public int numberOfCollectionRemovals() {
		return collectionRemovalCount;
	}

	/// Are there before transaction completion actions registered?
	///
	/// @return true if there are before transaction actions
	public boolean hasBeforeTransactionActions() {
		return !isTransactionCoordinatorShared
				&& transactionCompletionCallbacks.hasBeforeCompletionCallbacks();
	}

	/// Are there after transaction completion actions registered?
	///
	/// @return true if there are after transaction actions
	public boolean hasAfterTransactionActions() {
		return !isTransactionCoordinatorShared
				&& transactionCompletionCallbacks.hasAfterCompletionCallbacks();
	}

	/// Check whether any insertion or deletion actions are currently queued.
	///
	/// @return true if insertions or deletions are currently queued
	public boolean areInsertionsOrDeletionsQueued() {
		return !insertions.isEmpty()
				|| !deletions.isEmpty()
				|| !orphanRemovals.isEmpty()
				|| hasUnresolvedEntityInsertActions();
	}

	/// Check whether the given tables/query-spaces are to be updated.
	///
	/// @param tables The table/query-spaces to check
	/// @return true if we contain pending actions against any of the given tables
	public boolean areTablesToBeUpdated(Set<? extends Serializable> tables) {
		if (tables.isEmpty()) {
			return false;
		}

		return areTablesToBeUpdated(orphanCollectionRemovals, tables)
				|| areTablesToBeUpdated(orphanRemovals, tables)
				|| areTablesToBeUpdated(insertions, tables)
				|| areTablesToBeUpdated(updates, tables)
				|| areTablesToBeUpdated(collectionQueuedOps, tables)
				|| areTablesToBeUpdated(collectionRemovals, tables)
				|| areTablesToBeUpdated(collectionUpdates, tables)
				|| areTablesToBeUpdated(collectionCreations, tables)
				|| areTablesToBeUpdated(deletions, tables);
	}

	private boolean areTablesToBeUpdated(
			List<? extends Executable> actions,
			Set<? extends Serializable> tables) {
		for (Executable action : actions) {
			for (Serializable space : action.getPropertySpaces()) {
				if (tables.contains(space)) {
					ACTION_LOGGER.changesMustBeFlushedToSpace(space);
					return true;
				}
			}
		}
		return false;
	}

	/// Check if there are any queued actions.
	///
	/// @return true if there are any queued actions
	public boolean hasAnyQueuedActions() {
		return !orphanCollectionRemovals.isEmpty()
				|| !orphanRemovals.isEmpty()
				|| !insertions.isEmpty()
				|| !updates.isEmpty()
				|| !collectionQueuedOps.isEmpty()
				|| !collectionRemovals.isEmpty()
				|| !collectionUpdates.isEmpty()
				|| !collectionCreations.isEmpty()
				|| !deletions.isEmpty()
				|| hasUnresolvedEntityInsertActions();
	}

	/// Validate that there are no unresolved entity insert actions.
	///
	/// @throws PropertyValueException if there are unresolved inserts
	public void checkNoUnresolvedActionsAfterOperation() throws PropertyValueException {
		flushCoordinator.getDecomposer().validateNoUnresolvedInserts();
	}

	/// Clear queued actions that were added during a flush-needed check.
	/// This is used when auto-flush determines that a flush is not actually needed.
	///
	/// @param previousCollectionRemovalSize the size of collection removals before the check
	public void clearFromFlushNeededCheck(int previousCollectionRemovalSize) {
		// Clear all actions except:
		// - Inserts (keep them)
		// - Orphan removals (keep them)
		// - Deletes (keep them)
		// - Collection removals that existed before the check

		// Keep orphan collection removals
		// Keep orphan removals (already in list)
		// Keep insertions (already in list)
		updates.clear();
		collectionQueuedOps.clear();

		// Keep only the first N collection removals
		if (collectionRemovals.size() > previousCollectionRemovalSize) {
			collectionRemovals.subList(previousCollectionRemovalSize, collectionRemovals.size()).clear();
		}

		collectionUpdates.clear();
		collectionCreations.clear();
		// Keep deletions (already in list)

		// Recalculate counters after clearing actions
		updateCount = 0;
		collectionUpdateCount = 0;
		collectionCreationCount = 0;
		collectionRemovalCount = collectionRemovals.size() + orphanCollectionRemovals.size();
	}

	/// Remove a scheduled deletion for an entity.
	/// Used when an entity is rescued from deletion (e.g., during merge).
	///
	/// @param entry the entity entry
	/// @param rescuedEntity the entity being rescued
	public void unScheduleDeletion(EntityEntry entry, Object rescuedEntity) {
		final var lazyInitializer = extractLazyInitializer(rescuedEntity);
		if (lazyInitializer != null && !lazyInitializer.isUninitialized()) {
			rescuedEntity = lazyInitializer.getImplementation(session);
		}

		final Object entityToMatch = rescuedEntity;

		// Check deletions list
		boolean removed = deletions.removeIf(delete -> delete.getInstance() == entityToMatch);

		// Also check orphan removals
		if (!removed) {
			removed = orphanRemovals.removeIf(orphan -> orphan.getInstance() == entityToMatch);
		}

		if (removed && ACTION_LOGGER.isDebugEnabled()) {
			ACTION_LOGGER.debugf("Unschedule deletion for entity %s", entityToMatch);
		}
	}

	/// Remove a scheduled deletion for an unloaded entity.
	/// Used when an entity instance is being merged/saved but was previously scheduled for deletion.
	///
	/// @param newEntity the new entity instance
	public void unScheduleUnloadedDeletion(Object newEntity) {
		final var entityPersister = session.getEntityPersister(null, newEntity);
		final Object identifier = entityPersister.getIdentifier(newEntity, session);
		final String entityName = entityPersister.getEntityName();

		boolean removed = deletions.removeIf(delete -> {
			if (delete.getInstance() == null
					&& delete.getEntityName().equals(entityName)
					&& entityPersister.getIdentifierMapping().areEqual(delete.getId(), identifier, session)) {
				session.getPersistenceContextInternal()
						.removeDeletedUnloadedEntityKey(session.generateEntityKey(identifier, entityPersister));
				return true;
			}
			return false;
		});

		if (removed && ACTION_LOGGER.isDebugEnabled()) {
			ACTION_LOGGER.debugf("Unschedule deletion for entity %s", newEntity);
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Transaction Completion Callbacks
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/// Set the transaction completion callbacks.
	/// Used when a session shares a transaction coordinator.
	///
	/// @param callbacks the callbacks to use
	/// @param isTransactionCoordinatorShared whether the transaction coordinator is shared
	///
	/// @deprecated This method is not used by GraphBasedActionQueue, which manages its own
	///             callbacks internally. It exists only for API compatibility with
	///             [org.hibernate.engine.spi.ActionQueueLegacy].
	@Deprecated(since = "7.0", forRemoval = true)
	@Override
	public void setTransactionCompletionCallbacks(
			TransactionCompletionCallbacksImplementor callbacks,
			boolean isTransactionCoordinatorShared) {
		// No-op: GraphBasedActionQueue manages its own callbacks internally
	}

	/// Get the transaction completion callbacks.
	///
	/// @return the transaction completion callbacks
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

	/// Execute any registered [org.hibernate.action.spi.BeforeTransactionCompletionProcess].
	public void beforeTransactionCompletion() {
		if (!isTransactionCoordinatorShared) {
			transactionCompletionCallbacks.beforeTransactionCompletion();
			session.getJdbcCoordinator().executeBatch();
		}
	}

	/// Performs cleanup of any held cache soft locks.
	///
	/// @param success Was the transaction successful
	public void afterTransactionCompletion(boolean success) {
		if (!isTransactionCoordinatorShared) {
			transactionCompletionCallbacks.afterTransactionCompletion(success);
		}
	}

	/// Execute pending bulk operation cleanup actions.
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


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Testing/debugging
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/// Get all pending actions (for debugging/testing).
	///
	/// @return list of pending actions
	public List<Executable> getPendingActions() {
		var list = new ArrayList<Executable>();
		list.addAll(orphanCollectionRemovals);
		list.addAll(orphanRemovals);
		list.addAll(insertions);
		list.addAll(updates);
		list.addAll(collectionQueuedOps);
		list.addAll(collectionRemovals);
		list.addAll(collectionUpdates);
		list.addAll(collectionCreations);
		list.addAll(deletions);
		return list;
	}

	/// Get the FlushCoordinator (for testing/debugging).
	///
	/// @return the flush coordinator
	public FlushCoordinator getFlushCoordinator() {
		return flushCoordinator;
	}

	@Override
	public String toString() {
		int collectionCount = collectionCreations.size() + collectionUpdates.size()
				+ collectionRemovals.size() + collectionQueuedOps.size()
				+ orphanCollectionRemovals.size();
		return "GraphBasedActionQueue[insertions=" + insertCount
			+ " updates=" + updateCount
			+ " deletions=" + deleteCount
			+ " orphanRemovals=" + orphanRemovals.size()
			+ " collections=" + collectionCount
			+ " unresolved=" + (hasUnresolvedEntityInsertActions() ? "yes" : "no")
			+ "]";
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Serialization support
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/// Serialize the action queue.
	///
	/// Note: Serialization is not yet fully implemented for GraphBasedActionQueue.
	/// This method is a stub for API compatibility.
	///
	/// @param oos the output stream
	/// @throws IOException if serialization fails
	public void serialize(ObjectOutputStream oos) throws IOException {
		ACTION_LOGGER.serializingActionQueue();
		flushCoordinator.getDecomposer().serialize(oos);

		serializeList(oos, orphanCollectionRemovals);
		serializeList(oos, orphanRemovals);
		serializeList(oos, insertions);
		serializeList(oos, updates);
		serializeList(oos, collectionQueuedOps);
		serializeList(oos, collectionRemovals);
		serializeList(oos, collectionUpdates);
		serializeList(oos, collectionCreations);
		serializeList(oos, deletions);
	}

	private void serializeList(ObjectOutputStream oos, List<? extends Executable> actions) throws IOException {
		oos.writeInt(actions.size());
		for (var action : actions) {
			oos.writeObject(action);
		}
	}

	public static GraphBasedActionQueue deserialize(
			ObjectInputStream ois,
			GraphBasedActionQueueFactory actionQueueFactory,
			SessionImplementor session) throws IOException, ClassNotFoundException {
		final boolean traceEnabled = ACTION_LOGGER.isTraceEnabled();
		if ( traceEnabled ) {
			ACTION_LOGGER.deserializingActionQueue();
		}

		var flushCoordinator = FlushCoordinator.deserialize(ois, actionQueueFactory, session);

		var orphanCollectionRemovals = deserializeList(ois, CollectionRemoveAction.class);
		var orphanRemovals = deserializeList(ois, OrphanRemovalAction.class);
		var insertions = deserializeList(ois, AbstractEntityInsertAction.class);
		var updates = deserializeList(ois, EntityUpdateAction.class);
		var collectionQueuedOps = deserializeList(ois, QueuedOperationCollectionAction.class);
		var collectionRemovals = deserializeList(ois, CollectionRemoveAction.class);
		var collectionUpdates = deserializeList(ois, CollectionUpdateAction.class);
		var collectionCreations = deserializeList(ois, CollectionRecreateAction.class);
		var deletions = deserializeList(ois, EntityDeleteAction.class);

		return new GraphBasedActionQueue(
				flushCoordinator,
				orphanCollectionRemovals,
				orphanRemovals,
				insertions,
				updates,
				collectionQueuedOps,
				collectionRemovals,
				collectionUpdates,
				collectionCreations,
				deletions,
				session
		);
	}

	@SuppressWarnings("unchecked")
	private static <T> List<T> deserializeList(ObjectInputStream ois, Class<T> actionClass)
			throws IOException, ClassNotFoundException {
		int count = ois.readInt();
		var list = CollectionHelper.<T>arrayList(count);
		for (int i = 0; i < count; i++) {
			list.add((T) ois.readObject());
		}
		return list;
	}
}
