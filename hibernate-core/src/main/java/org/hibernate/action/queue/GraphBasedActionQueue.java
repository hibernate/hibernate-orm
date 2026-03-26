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
import org.hibernate.action.internal.CollectionAction;
import org.hibernate.action.internal.CollectionRecreateAction;
import org.hibernate.action.internal.CollectionRemoveAction;
import org.hibernate.action.internal.CollectionUpdateAction;
import org.hibernate.action.internal.EntityAction;
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

	private final List<EntityAction> entityActions;
	private final List<CollectionAction> collectionActions;

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

		this.entityActions = new ArrayList<>();
		this.collectionActions = new ArrayList<>();

		this.transactionCompletionCallbacks = new TransactionCompletionCallbacksImpl(session);
		this.isTransactionCoordinatorShared = false;
	}

	/// Deserialization constructor.
	/// @see #deserialize(ObjectInputStream, GraphBasedActionQueueFactory, SessionImplementor)
	public GraphBasedActionQueue(
			FlushCoordinator flushCoordinator,
			ArrayList<EntityAction> entityActions,
			ArrayList<CollectionAction> collectionActions,
			SessionImplementor session) {
		this.session = session;
		this.flushCoordinator = flushCoordinator;

		this.entityActions = entityActions;
		this.collectionActions = collectionActions;

		this.transactionCompletionCallbacks = new TransactionCompletionCallbacksImpl(session);
		this.isTransactionCoordinatorShared = false;
	}

	/// Clear all pending actions.
	public void clear() {
		entityActions.clear();
		collectionActions.clear();
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

			// Still increment counter for stats
			insertCount++;

			// Don't add to pendingActions - already executed
			return;
		}

		// Regular inserts are queued for later execution
		ACTION_LOGGER.addingResolvedNonEarlyInsertAction();
		addEntityAction(insert);
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
		if (entityActions.isEmpty()) {
			return;
		}

		// Collect all insert actions from pending list
		final List<AbstractEntityInsertAction> insertsToExecute = new ArrayList<>();
		for (EntityAction action : entityActions) {
			if (action instanceof AbstractEntityInsertAction eia) {
				insertsToExecute.add(eia);
			}
		}

		if (insertsToExecute.isEmpty()) {
			return;
		}

		// Make insert entities managed before execution
		for (AbstractEntityInsertAction action : insertsToExecute) {
			if (!action.isVeto()) {
				action.makeEntityManaged();
			}
		}

		// Execute these inserts via FlushCoordinator
		flushCoordinator.executeFlush(insertsToExecute);

		// Remove executed actions from pending list
		entityActions.removeAll(insertsToExecute);
	}

	/// Adds an entity update action.
	///
	/// @param action The action representing the entity update
	public void addAction(EntityUpdateAction action) {
		updateCount++;
		addEntityAction(action);
	}

	/// Adds an entity delete action.
	///
	/// @param action The action representing the entity deletion
	public void addAction(EntityDeleteAction action) {
		deleteCount++;
		addEntityAction(action);
	}

	/// Adds an orphan removal action.
	///
	/// @param action The action representing the orphan removal
	public void addAction(OrphanRemovalAction action) {
		addEntityAction(action);
	}

	/// Adds a collection (re)create action.
	///
	/// @param action The action representing the (re)creation of a collection
	public void addAction(CollectionRecreateAction action) {
		ACTION_LOGGER.debugf( "GraphBasedActionQueue.addAction(CollectionRecreateAction) - role=%s, key=%s", action.getPersister().getRole(), action.getKey() );
		collectionCreationCount++;
		addCollectionAction(action);
	}

	/// Adds a collection remove action.
	///
	/// @param action The action representing the removal of a collection
	public void addAction(CollectionRemoveAction action) {
		collectionRemovalCount++;
		// Check if this should be an orphan collection removal
		// (handled by FlushCoordinator's ordering instead of special list)
		addCollectionAction(action);
	}

	/// Adds a collection update action.
	///
	/// @param action The action representing the update of a collection
	public void addAction(CollectionUpdateAction action) {
		collectionUpdateCount++;
		addCollectionAction(action);
	}

	/// Adds an action relating to a collection queued operation (extra lazy).
	///
	/// @param action The action representing the queued operation
	public void addAction(QueuedOperationCollectionAction action) {
		addCollectionAction(action);
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

	/// Helper to add an entity action while maintaining entity/collection separation.
	/// Entity actions are always kept before collection actions in pendingActions.
	private void addEntityAction(EntityAction action) {
		entityActions.add( action );
	}

	/// Helper to add a collection action while maintaining entity/collection separation.
	/// Collection actions are always kept after entity actions in pendingActions.
	private void addCollectionAction(CollectionAction action) {
		collectionActions.add( action );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Execution
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/// Perform all currently queued entity-insertion actions.
	///
	/// @throws HibernateException error executing queued insertion actions
	public void executeInserts() throws HibernateException {
		List<AbstractEntityInsertAction> insertActions = new ArrayList<>();
		for (Executable action : entityActions) {
			if (action instanceof AbstractEntityInsertAction eia) {
				insertActions.add(eia);
			}
		}

		if (!insertActions.isEmpty()) {
			// Make insert entities managed before execution
			for (AbstractEntityInsertAction action : insertActions) {
				if (!action.isVeto()) {
					action.makeEntityManaged();
				}
			}

			flushCoordinator.executeFlush(insertActions);
			entityActions.removeAll(insertActions);

			session.getJdbcCoordinator().executeBatch();
		}
	}

	/// Perform all currently queued actions.
	///
	/// @throws HibernateException error executing queued actions
	public void executeActions() throws HibernateException {
		if ( ACTION_LOGGER.isDebugEnabled() ) {
			ACTION_LOGGER.debugf( "GraphBasedActionQueue.executeActions() - %d entityActions", entityActions.size() );
			for (EntityAction action : entityActions) {
				ACTION_LOGGER.debugf( "  - pending action: %s", action.getLoggableDetails() );
			}
			ACTION_LOGGER.debugf( "GraphBasedActionQueue.executeActions() - %d collectionActions", collectionActions.size() );
			for (CollectionAction action : collectionActions) {
				ACTION_LOGGER.debugf( "  - pending action: %s", action.getLoggableDetails() );
			}
		}

		if ( entityActions.isEmpty() && collectionActions.isEmpty() ) {
			ACTION_LOGGER.debugf("executeActions: no pending actions to execute" );
			// EARLY EXIT!!
		}

		List<Executable> combinedActions = new ArrayList<>();
		combinedActions.addAll(entityActions);
		combinedActions.addAll(collectionActions);

		// Delegate to FlushCoordinator for graph-based execution
		flushCoordinator.executeFlush(combinedActions);

		// Register transaction completion callbacks for executed actions
		for (Executable action : combinedActions) {
			registerCleanupActions(action);
		}

		// clear all pending actions
		entityActions.clear();
		collectionActions.clear();

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

	/// Prepares the internal action queues for execution.
	///
	/// Note: With FlushCoordinator, most preparation happens during decomposition,
	/// but we maintain this method for API compatibility.
	///
	/// @throws HibernateException error preparing actions
	public void prepareActions() throws HibernateException {
		prepareActions(entityActions);
		prepareActions(collectionActions);
	}

	private void prepareActions(List<? extends Executable> pendingActions) throws HibernateException {
		for (Executable action : pendingActions) {
			if (action instanceof CollectionRecreateAction
					|| action instanceof CollectionUpdateAction
					|| action instanceof CollectionRemoveAction
					|| action instanceof QueuedOperationCollectionAction) {
				action.beforeExecutions();
			}
		}
	}

	/// Sort entity actions.
	///
	/// Note: With GraphBasedActionQueue, sorting is handled by FlushCoordinator's graph-based
	/// ordering, so this is a no-op for API compatibility.
	///
	/// @deprecated This method is not used by GraphBasedActionQueue. It exists only for
	///             API compatibility with [org.hibernate.engine.spi.ActionQueueLegacy].
	@Deprecated(since = "7.0", forRemoval = true)
	@Override
	public void sortActions() {
		// No-op: FlushCoordinator handles ordering via graph
	}

	/// Sort collection actions.
	///
	/// Note: With GraphBasedActionQueue, sorting is handled by FlushCoordinator's graph-based
	/// ordering, so this is a no-op for API compatibility.
	///
	/// @deprecated This method is not used by GraphBasedActionQueue. It exists only for
	///             API compatibility with [org.hibernate.engine.spi.ActionQueueLegacy].
	@Deprecated(since = "7.0", forRemoval = true)
	@Override
	public void sortCollectionActions() {
		// No-op: FlushCoordinator handles ordering via graph
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
		for (EntityAction action : entityActions) {
			if (action instanceof AbstractEntityInsertAction
					|| action instanceof EntityDeleteAction) {
				return true;
			}
		}
		return hasUnresolvedEntityInsertActions();
	}

	/// Check whether the given tables/query-spaces are to be updated.
	///
	/// @param tables The table/query-spaces to check
	/// @return true if we contain pending actions against any of the given tables
	public boolean areTablesToBeUpdated(Set<? extends Serializable> tables) {
		if (tables.isEmpty()) {
			return false;
		}

		var entityActionsMatched = areTablesToBeUpdated(entityActions, tables);
		if ( entityActionsMatched ) {
			return true;
		}

		return areTablesToBeUpdated(collectionActions, tables);
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
		return !entityActions.isEmpty() || !collectionActions.isEmpty() || hasUnresolvedEntityInsertActions();
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
		// Remove all actions except:
		// - Inserts (keep them)
		// - Deletes (keep them)
		// - Collection removals that existed before the check

		int collectionRemovalCountLocal = 0;
		List<EntityAction> entityActionsToKeep = new ArrayList<>();
		List<CollectionAction> collectionActionsToKeep = new ArrayList<>();

		for ( EntityAction entityAction : entityActions ) {
			if (entityAction instanceof AbstractEntityInsertAction
				|| entityAction instanceof EntityDeleteAction) {
				entityActionsToKeep.add(entityAction);
			}
			// Skip: EntityUpdateAction
		}

		for (CollectionAction collectionAction : collectionActions) {
			if (collectionAction instanceof CollectionRemoveAction) {
				if (collectionRemovalCountLocal < previousCollectionRemovalSize) {
					collectionActionsToKeep.add(collectionAction);
				}
				collectionRemovalCountLocal++;
			}
			// Skip: CollectionRecreateAction, CollectionUpdateAction, QueuedOperationCollectionAction
		}

		entityActions.clear();
		if ( !entityActionsToKeep.isEmpty() ) {
			entityActions.addAll(entityActionsToKeep);
		}

		collectionActions.clear();
		if ( !collectionActionsToKeep.isEmpty() ) {
			collectionActions.addAll(collectionActionsToKeep);
		}

		// Recalculate counters after clearing actions
		recalculateCounters();
	}

	/// Recalculate action counters by scanning pending actions.
	/// Used after clearing actions during auto-flush checks.
	private void recalculateCounters() {
		insertCount = 0;
		updateCount = 0;
		deleteCount = 0;
		collectionCreationCount = 0;
		collectionUpdateCount = 0;
		collectionRemovalCount = 0;

		for ( EntityAction entityAction : entityActions ) {
			if ( entityAction instanceof AbstractEntityInsertAction ) {
				insertCount++;
			}
			else if (entityAction instanceof EntityUpdateAction) {
				updateCount++;
			}
			else if ( entityAction instanceof EntityDeleteAction ) {
				deleteCount++;
			}
		}

		for (CollectionAction action : collectionActions) {
			if (action instanceof CollectionRecreateAction) {
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
		boolean removed = entityActions.removeIf(action -> {
			if (action instanceof EntityDeleteAction delete) {
				return delete.getInstance() == entityToMatch;
			}
			return false;
		});

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

		boolean removed = entityActions.removeIf(action -> {
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
		list.addAll(entityActions);
		list.addAll(collectionActions);
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
		return "GraphBasedActionQueue[insertions=" + insertCount
			+ " updates=" + updateCount
			+ " deletions=" + deleteCount
			+ " collections=" + collectionActions.size()
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

		oos.writeInt(entityActions.size());
		for ( var action : entityActions ) {
			oos.writeObject(action);
		}

		oos.writeInt(collectionActions.size());
		for ( var action : collectionActions ) {
			oos.writeObject( action );
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

		var flushCoordinator = FlushCoordinator.deserialize(  ois, actionQueueFactory, session );

		var entityActionCount = ois.readInt();
		var entityActions = CollectionHelper.<EntityAction>arrayList(entityActionCount);
		for ( int i = 0; i < entityActionCount; i++ ) {
			entityActions.add( (EntityAction) ois.readObject() );
		}

		var collectionActionCount = ois.readInt();
		var collectionActions = CollectionHelper.<CollectionAction>arrayList(collectionActionCount);
		for ( int i = 0; i < collectionActionCount; i++ ) {
			collectionActions.add( (CollectionAction) ois.readObject() );
		}

		return new GraphBasedActionQueue( flushCoordinator, entityActions, collectionActions, session );
	}
}
