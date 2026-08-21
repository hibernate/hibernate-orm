/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal;

import jakarta.annotation.Nullable;

import org.hibernate.action.queue.spi.ActionQueue;
import org.hibernate.action.queue.spi.ActionQueueCheckpoint;
import org.hibernate.action.queue.spi.CollectionMutationInput;
import org.hibernate.action.queue.spi.CollectionTransition;
import org.hibernate.action.queue.spi.PlanningOptions;

import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.Internal;
import org.hibernate.PropertyValueException;
import org.hibernate.action.internal.AbstractEntityInsertAction;
import org.hibernate.action.internal.BulkOperationCleanupAction;
import org.hibernate.action.internal.EntityActionVetoException;
import org.hibernate.action.internal.EntityDeleteAction;
import org.hibernate.action.internal.EntityIdentityInsertAction;
import org.hibernate.action.internal.EntityInsertAction;
import org.hibernate.action.internal.EntityUpdateAction;
import org.hibernate.action.internal.OrphanRemovalAction;
import org.hibernate.action.queue.internal.audit.GraphAuditMutationCollector;
import org.hibernate.action.queue.internal.constraint.ConstraintModel;
import org.hibernate.action.queue.internal.constraint.DeferrableConstraintMode;
import org.hibernate.action.queue.internal.plan.FlushPlan;
import org.hibernate.action.queue.internal.support.GraphBasedActionQueueFactory;
import org.hibernate.action.spi.Executable;
import org.hibernate.engine.internal.TransactionCompletionCallbacksImpl;
import org.hibernate.engine.internal.FlushProcessingContext;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.TransactionCompletionCallbacksImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.persister.entity.EntityPersister;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hibernate.action.internal.ActionLogging.ACTION_LOGGER;
import static org.hibernate.proxy.HibernateProxy.extractLazyInitializer;

/// ActionQueue implementation using FlushCoordinator for graph-based flush scheduling.
///
/// This implementation replaces the traditional action queue execution with graph-based
/// dependency ordering and cycle breaking.
///
/// See [FlushCoordinator].
///
/// @author Steve Ebersole
@Incubating
public class GraphBasedActionQueue implements ActionQueue {
	private final SessionImplementor session;
	private final FlushCoordinator flushCoordinator;
	private final GraphAuditMutationCollector auditMutationCollector;
	private final boolean deferIdentityInserts;

	// Phase lists maintain the legacy phase order while collections use graph-native mutations.
	private final List<PreparedCollectionMutation> orphanCollectionRemovals;
	private final List<OrphanRemovalAction> orphanRemovals;
	private final List<AbstractEntityInsertAction> insertions;
	private final List<EntityUpdateAction> updates;
	private final List<PreparedCollectionMutation> collectionQueuedOps;
	private final List<PreparedCollectionMutation> collectionRemovals;
	private final List<PreparedCollectionMutation> collectionUpdates;
	private final List<PreparedCollectionMutation> collectionCreations;
	private final List<EntityDeleteAction> deletions;
	private final List<CollectionMutationInput> collectionMutationInputs;
	private final List<CollectionMutationInput> durableCollectionMutationInputs;
	private List<CollectionCacheCleanupProcess> collectionCacheCleanupProcesses;

	private boolean isTransactionCoordinatorShared;
	private TransactionCompletionCallbacksImplementor transactionCompletionCallbacks;

	/// Construct a GraphBasedActionQueue for the given session.
	///
	/// @param constraintModel Details about foreign-key and unique constraints defined in the model.
	/// @param planningOptions Options for graph building and planning.
	/// @param deferIdentityInserts Whether non-delayed IDENTITY inserts should be planned instead of executed immediately.
	/// @param session The session
	public GraphBasedActionQueue(
			ConstraintModel constraintModel,
			PlanningOptions planningOptions,
			Map<String, EntityPersister> entityPersistersByTable,
			boolean deferIdentityInserts,
			SessionImplementor session) {
		this.session = session;
		this.flushCoordinator = new FlushCoordinator(
				constraintModel,
				planningOptions,
				entityPersistersByTable,
				session
		);
		this.auditMutationCollector = new GraphAuditMutationCollector();
		this.deferIdentityInserts = deferIdentityInserts;

		this.orphanCollectionRemovals = new ArrayList<>();
		this.orphanRemovals = new ArrayList<>();
		this.insertions = new ArrayList<>();
		this.updates = new ArrayList<>();
		this.collectionQueuedOps = new ArrayList<>();
		this.collectionRemovals = new ArrayList<>();
		this.collectionUpdates = new ArrayList<>();
		this.collectionCreations = new ArrayList<>();
		this.deletions = new ArrayList<>();
		this.collectionMutationInputs = new ArrayList<>();
		this.durableCollectionMutationInputs = new ArrayList<>();

		this.transactionCompletionCallbacks = new TransactionCompletionCallbacksImpl(session);
		this.isTransactionCoordinatorShared = false;
	}

	/// Deserialization constructor.
	/// See [#deserialize(ObjectInputStream, GraphBasedActionQueueFactory, SessionImplementor)].
	public GraphBasedActionQueue(
			FlushCoordinator flushCoordinator,
			List<PreparedCollectionMutation> orphanCollectionRemovals,
			List<OrphanRemovalAction> orphanRemovals,
			List<AbstractEntityInsertAction> insertions,
			List<EntityUpdateAction> updates,
			List<PreparedCollectionMutation> collectionQueuedOps,
			List<PreparedCollectionMutation> collectionRemovals,
			List<PreparedCollectionMutation> collectionUpdates,
			List<PreparedCollectionMutation> collectionCreations,
			List<EntityDeleteAction> deletions,
			boolean deferIdentityInserts,
			SessionImplementor session) {
		this.session = session;
		this.flushCoordinator = flushCoordinator;
		this.auditMutationCollector = new GraphAuditMutationCollector();
		this.deferIdentityInserts = deferIdentityInserts;

		this.orphanCollectionRemovals = orphanCollectionRemovals;
		this.orphanRemovals = orphanRemovals;
		this.insertions = insertions;
		this.updates = updates;
		this.collectionQueuedOps = collectionQueuedOps;
		this.collectionRemovals = collectionRemovals;
		this.collectionUpdates = collectionUpdates;
		this.collectionCreations = collectionCreations;
		this.deletions = deletions;
		this.collectionMutationInputs = new ArrayList<>();
		this.durableCollectionMutationInputs = new ArrayList<>();

		this.transactionCompletionCallbacks = new TransactionCompletionCallbacksImpl(session);
		this.isTransactionCoordinatorShared = false;
	}

	/// Clear all pending actions.
	public void clear() {
		collectionMutationInputs.clear();
		durableCollectionMutationInputs.clear();
		orphanCollectionRemovals.clear();
		orphanRemovals.clear();
		insertions.clear();
		updates.clear();
		collectionQueuedOps.clear();
		collectionRemovals.clear();
		collectionUpdates.clear();
		collectionCreations.clear();
		if ( collectionCacheCleanupProcesses != null ) {
			collectionCacheCleanupProcesses.clear();
		}
		deletions.clear();
		flushCoordinator.getDecomposer().clear();
	}

	public DeferrableConstraintMode getDeferrableConstraintMode() {
		return flushCoordinator.getDeferrableConstraintMode();
	}

	public void setDeferrableConstraintMode(DeferrableConstraintMode deferrableConstraintMode) {
		flushCoordinator.setDeferrableConstraintMode( deferrableConstraintMode );
	}

	public GraphAuditMutationCollector getAuditMutationCollector() {
		return auditMutationCollector;
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
		addInsertAction(
				deferIdentityInserts && action.isEarlyInsert()
						? EntityIdentityInsertAction.delayedCopy( action )
						: action
		);
	}

	private void addInsertAction(AbstractEntityInsertAction insert) {
		if ( insert.isEarlyInsert() && !deferIdentityInserts ) {
			addImmediateIdentityInsertAction( insert );
			return;
		}

		// Check for unresolved transient dependencies before making entity managed
		// This prevents PropertyValueException in circular cascade scenarios
		final var transientDeps = insert.findNonNullableTransientEntities();
			if ( transientDeps == null || transientDeps.isEmpty() ) {
			addResolvedNonEarlyInsertAction( insert );
		}
		else {
			flushCoordinator.getDecomposer().trackUnresolvedInsert( insert, transientDeps );
		}
	}

	private void addImmediateIdentityInsertAction(AbstractEntityInsertAction insert) {
		ACTION_LOGGER.executingInsertsBeforeFindingNonNullableTransientEntitiesForEarlyInsert( insert );
		executePendingInserts();

		final var nonNullableTransientDeps = insert.findNonNullableTransientEntities();
		if ( nonNullableTransientDeps != null ) {
			flushCoordinator.getDecomposer().trackUnresolvedInsert( insert, nonNullableTransientDeps );
			return;
		}

		ACTION_LOGGER.executingIdentityInsertImmediately();
		insert.execute();
		if ( !insert.isVeto() ) {
			insert.makeEntityManaged();
			executePendingInserts();
			for ( var resolvedAction : flushCoordinator.getDecomposer().resolveDependentActions( insert.getInstance() ) ) {
				addInsertAction( resolvedAction );
			}
		}
		else {
			throw new EntityActionVetoException( insert );
		}
		registerCleanupActions( insert );
	}

	private void addResolvedNonEarlyInsertAction(AbstractEntityInsertAction insert) {
		ACTION_LOGGER.addingResolvedNonEarlyInsertAction();
			if ( !insertions.contains( insert ) ) {
				insertions.add( insert );
		}
		makeEntityManagedAndResolveDependentActions(insert);
	}

	private void makeEntityManagedAndResolveDependentActions(AbstractEntityInsertAction insert) {
		if ( !insert.isVeto() ) {
			insert.makeEntityManaged();
			for ( var resolvedAction : flushCoordinator.getDecomposer().resolveDependentActions( insert.getInstance() ) ) {
				addInsertAction( resolvedAction );
			}
		}
		else {
			throw new EntityActionVetoException( insert );
		}
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

		// Execute these inserts via FlushCoordinator
		final List<AbstractEntityInsertAction> executedInserts = new ArrayList<>(insertions);
		executeInsertFlushAndRegisterCleanup(executedInserts);

		// Clear executed actions from pending list
		insertions.clear();
	}

	/// Adds an entity update action.
	///
	/// @param action The action representing the entity update
	public void addAction(EntityUpdateAction action) {
		updates.add(action);
	}

	/// Adds an entity delete action.
	///
	/// @param action The action representing the entity deletion
	public void addAction(EntityDeleteAction action) {
		deletions.add(action);
	}

	/// Adds an orphan removal action.
	///
	/// @param action The action representing the orphan removal
	public void addAction(OrphanRemovalAction action) {
		orphanRemovals.add(action);
	}

	@Override
	public void addCollectionMutation(CollectionMutationInput input) {
		if ( isDurableCollectionMutation( input ) ) {
			durableCollectionMutationInputs.add( input );
		}
		else {
			collectionMutationInputs.add( input );
		}
	}

	private boolean isDurableCollectionMutation(CollectionMutationInput input) {
		return !(session.getPersistenceContextInternal().getCollectionFlushActionTracker()
				instanceof FlushProcessingContext)
				|| isOrphanCollectionRemoval( input );
	}

	private boolean isOrphanCollectionRemoval(CollectionMutationInput input) {
		if ( input.transition() != CollectionTransition.REMOVE
				&& input.transition() != CollectionTransition.REMOVE_AND_CREATE ) {
			return false;
		}
		Object affectedOwner = input.affectedOwner();
		if ( affectedOwner == null && input.collection() != null ) {
			affectedOwner = session.getPersistenceContextInternal()
					.getLoadedCollectionOwnerOrNull( input.collection() );
		}
		if ( affectedOwner == null ) {
			return false;
		}
		final var entry = session.getPersistenceContextInternal().getEntry( affectedOwner );
		if ( entry == null || !entry.getStatus().isDeletedOrGone() ) {
			return false;
		}
		for ( var orphanRemoval : orphanRemovals ) {
			if ( orphanRemoval.getInstance() == affectedOwner ) {
				return true;
			}
		}
		return false;
	}

	private void addPreparedCollectionMutation(PreparedCollectionMutation mutation) {
		switch ( mutation.kind() ) {
			case CREATE -> collectionCreations.add( mutation );
			case UPDATE -> collectionUpdates.add( mutation );
			case QUEUED_OPERATIONS -> collectionQueuedOps.add( mutation );
			case REMOVE -> addPreparedCollectionRemoval( mutation );
		}
	}

	private void addPreparedCollectionRemoval(PreparedCollectionMutation mutation) {
		if ( !orphanRemovals.isEmpty() && mutation.affectedOwner() != null ) {
			final EntityEntry entry = session.getPersistenceContextInternal().getEntry( mutation.affectedOwner() );
			if ( entry != null && entry.getStatus().isDeletedOrGone() ) {
				for ( OrphanRemovalAction orphanAction : orphanRemovals ) {
					if ( orphanAction.getInstance() == mutation.affectedOwner() ) {
						orphanCollectionRemovals.add( mutation );
						return;
					}
				}
			}
		}
		collectionRemovals.add( mutation );
	}

	/// Adds an action defining a cleanup relating to a bulk operation.
	///
	/// @param action The action representing the bulk operation cleanup
	public void addAction(BulkOperationCleanupAction action) {
		registerCleanupActions(action);
	}

	private void registerCleanupActions(Executable executable) {
		registerCleanupActions( executable, false );
	}

	private void registerGraphExecutedCleanupActions(Executable executable) {
		registerCleanupActions( executable, true );
	}

	private void registerCleanupActions(Executable executable, boolean graphExecuted) {
		final var beforeCompletionCallback = executable.getBeforeTransactionCompletionProcess();
		if (beforeCompletionCallback != null) {
			transactionCompletionCallbacks.registerCallback(beforeCompletionCallback);
		}
		if (session.getFactory().getSessionFactoryOptions().isQueryCacheEnabled()) {
			invalidateSpaces(executable.getPropertySpaces());
		}
		if ( !graphExecuted || !hasGraphOwnedAfterTransactionCompletion( executable ) ) {
			final var afterCompletionCallback = executable.getAfterTransactionCompletionProcess();
			if (afterCompletionCallback != null) {
				transactionCompletionCallbacks.registerCallback(afterCompletionCallback);
			}
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
			final List<AbstractEntityInsertAction> executedInserts = new ArrayList<>(insertions);
			executeInsertFlushAndRegisterCleanup(executedInserts);

			insertions.clear();

			session.getJdbcCoordinator().executeBatch();
		}
	}

	private void executeInsertFlushAndRegisterCleanup(List<AbstractEntityInsertAction> executedInserts) {
		RuntimeException runtimeException = null;
		Error error = null;
		try {
			flushCoordinator.executeInsertFlush(executedInserts);
		}
		catch (RuntimeException e) {
			runtimeException = e;
			throw e;
		}
		catch (Error e) {
			error = e;
			throw e;
		}
		finally {
			try {
				// Register cleanup actions for executed inserts (matches legacy queue pattern)
				for (AbstractEntityInsertAction action : executedInserts) {
					registerGraphExecutedCleanupActions(action);
				}
			}
			catch (RuntimeException | Error cleanupFailure) {
				if ( runtimeException != null ) {
					runtimeException.addSuppressed( cleanupFailure );
				}
				else if ( error != null ) {
					error.addSuppressed( cleanupFailure );
				}
				else {
					throw cleanupFailure;
				}
			}
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

		executeFlushAndPrepareForTransactionCompletion();

		// clear all pending actions
		clear();

		// Execute any pending JDBC batch
		session.getJdbcCoordinator().executeBatch();
	}

	private void executeFlushAndPrepareForTransactionCompletion() {
		RuntimeException runtimeException = null;
		Error error = null;
		try {
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
		}
		catch (RuntimeException e) {
			runtimeException = e;
			throw e;
		}
		catch (Error e) {
			error = e;
			throw e;
		}
		finally {
			try {
				prepareForTransactionCompletion();
			}
			catch (RuntimeException | Error cleanupFailure) {
				if ( runtimeException != null ) {
					runtimeException.addSuppressed( cleanupFailure );
				}
				else if ( error != null ) {
					error.addSuppressed( cleanupFailure );
				}
				else {
					throw cleanupFailure;
				}
			}
		}
	}

	private void prepareForTransactionCompletion() {
		final List<String> allSpaces = new ArrayList<>();
		prepareCollectionForTransactionCompletion( orphanCollectionRemovals, allSpaces );
		prepareForTransactionCompletion( orphanRemovals, allSpaces );
		prepareForTransactionCompletion( insertions, allSpaces );
		prepareForTransactionCompletion( updates, allSpaces );
		prepareCollectionForTransactionCompletion( collectionQueuedOps, allSpaces );
		prepareCollectionForTransactionCompletion( collectionRemovals, allSpaces );
		prepareCollectionForTransactionCompletion( collectionUpdates, allSpaces );
		prepareCollectionForTransactionCompletion( collectionCreations, allSpaces );
		prepareForTransactionCompletion( deletions, allSpaces );
		if ( collectionCacheCleanupProcesses != null ) {
			for ( var cacheCleanupProcess : collectionCacheCleanupProcesses ) {
				transactionCompletionCallbacks.registerCallback( cacheCleanupProcess );
			}
		}

		if (!allSpaces.isEmpty()) {
			invalidateSpaces(allSpaces.toArray(new String[0]));
		}
	}

	private void prepareCollectionForTransactionCompletion(
			List<PreparedCollectionMutation> mutations,
			List<String> allSpaces) {
		if ( !session.getFactory().getSessionFactoryOptions().isQueryCacheEnabled() ) {
			return;
		}
		for ( var mutation : mutations ) {
			for ( var space : mutation.getPersister().getCollectionSpaces() ) {
				if ( !allSpaces.contains( space ) ) {
					allSpaces.add( space );
				}
			}
		}
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
			if ( !hasGraphOwnedAfterTransactionCompletion( action ) ) {
				final var afterCompletionCallback = action.getAfterTransactionCompletionProcess();
				if (afterCompletionCallback != null) {
					transactionCompletionCallbacks.registerCallback(afterCompletionCallback);
				}
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

	private boolean hasGraphOwnedAfterTransactionCompletion(Executable action) {
		return action instanceof AbstractEntityInsertAction
			|| action instanceof EntityUpdateAction
			|| action instanceof EntityDeleteAction;
	}

	/// Prepares the internal action queues for execution.
	///
	/// Note: With FlushCoordinator, most preparation happens during decomposition,
	/// but we maintain this method for API compatibility.
	///
	/// @throws HibernateException error preparing actions
	public void prepareActions() throws HibernateException {
		prepareDeferredOwnerPreUpdates();
		prepareCollectionMutationInputs();
		prepareOwnerUpdateCallbacks();
		prepareCollectionMutations( collectionRemovals );
		prepareCollectionMutations( collectionUpdates );
		prepareCollectionMutations( collectionCreations );
		prepareCollectionMutations( collectionQueuedOps );
	}

	private void prepareDeferredOwnerPreUpdates() {
		boolean collectionRefreshRequired = false;
		for ( var action : updates ) {
			collectionRefreshRequired |= action.prepareDeferredOwnerPreUpdate();
		}
		if ( collectionRefreshRequired ) {
			collectionMutationInputs.clear();
			final var tracker = session.getPersistenceContextInternal().getCollectionFlushActionTracker();
			if ( tracker instanceof FlushProcessingContext flushProcessingContext ) {
				flushProcessingContext.refreshCollectionMutationInputs();
			}
		}
	}

	private void prepareCollectionMutationInputs() {
		if ( collectionMutationInputs.isEmpty() && durableCollectionMutationInputs.isEmpty() ) {
			return;
		}
		CollectionMutationPreparer.prepareAll(
				durableCollectionMutationInputs,
				collectionMutationInputs,
				(EventSource) session,
				this::addPreparedCollectionMutation
		);
		collectionMutationInputs.clear();
		durableCollectionMutationInputs.clear();
	}

	private void prepareOwnerUpdateCallbacks() {
		final var tracker = session.getPersistenceContextInternal().getCollectionFlushActionTracker();
		if ( !(tracker instanceof FlushProcessingContext flushProcessingContext) ) {
			return;
		}
		for ( var action : updates ) {
			flushProcessingContext.registerOwnerEntityUpdate( action.getInstance(), action.getPersister() );
		}
		for ( var action : orphanCollectionRemovals ) {
			flushProcessingContext.registerOwnerCollectionMutation(
					action.getAffectedOwner(),
					action.getPersister().isInverse()
			);
		}
		for ( var action : collectionRemovals ) {
			flushProcessingContext.registerOwnerCollectionMutation(
					action.getAffectedOwner(),
					action.getPersister().isInverse()
			);
		}
		for ( var action : collectionUpdates ) {
			flushProcessingContext.registerOwnerCollectionMutation(
					action.getAffectedOwner(),
					action.getPersister().isInverse()
			);
		}
		for ( var action : collectionCreations ) {
			flushProcessingContext.registerOwnerCollectionMutation(
					action.getAffectedOwner(),
					action.getPersister().isInverse()
			);
		}
		flushProcessingContext.sealOwnerUpdateCallbacks();
	}

	private void prepareCollectionMutations(List<PreparedCollectionMutation> mutations) {
		for ( var mutation : mutations ) {
			final var cacheCleanupProcess = CollectionCacheCleanupProcess.prepare( mutation, session );
			if ( cacheCleanupProcess != null ) {
				if ( collectionCacheCleanupProcesses == null ) {
					collectionCacheCleanupProcesses = new ArrayList<>();
				}
				collectionCacheCleanupProcesses.add( cacheCleanupProcess );
			}
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
		return insertions.size();
	}

	/// Get the number of entity updates currently queued.
	///
	/// @return count of entity updates
	public int numberOfUpdates() {
		return updates.size();
	}

	/// Get the number of entity deletions currently queued.
	///
	/// @return count of entity deletions
	public int numberOfDeletions() {
		return deletions.size() + orphanRemovals.size();
	}

	/// Get the number of collection creations currently queued.
	///
	/// @return count of collection creations
	public int numberOfCollectionCreations() {
		return collectionCreations.size() + countInputs( CollectionTransition.CREATE );
	}

	/// Get the number of collection updates currently queued.
	///
	/// @return count of collection updates
	public int numberOfCollectionUpdates() {
		return collectionUpdates.size() + countInputs( CollectionTransition.UPDATE );
	}

	/// Get the number of collection removals currently queued.
	///
	/// @return count of collection removals
	public int numberOfCollectionRemovals() {
		return collectionRemovals.size() + orphanCollectionRemovals.size()
				+ countInputs( CollectionTransition.REMOVE );
	}

	private int countInputs(CollectionTransition kind) {
		return countInputs( collectionMutationInputs, kind )
				+ countInputs( durableCollectionMutationInputs, kind );
	}

	private static int countInputs(List<CollectionMutationInput> inputs, CollectionTransition kind) {
		int count = 0;
		for ( var input : inputs ) {
			if ( input.transition() == kind
					|| input.transition() == CollectionTransition.REMOVE_AND_CREATE
						&& (kind == CollectionTransition.REMOVE || kind == CollectionTransition.CREATE) ) {
				count++;
			}
		}
		return count;
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

		return collectionInputsAffect( tables )
				|| collectionMutationsAffect(orphanCollectionRemovals, tables)
				|| areTablesToBeUpdated(orphanRemovals, tables)
				|| areTablesToBeUpdated(insertions, tables)
				|| areTablesToBeUpdated(updates, tables)
				|| collectionMutationsAffect(collectionQueuedOps, tables)
				|| collectionMutationsAffect(collectionRemovals, tables)
				|| collectionMutationsAffect(collectionUpdates, tables)
				|| collectionMutationsAffect(collectionCreations, tables)
				|| areTablesToBeUpdated(deletions, tables);
	}

	private boolean collectionInputsAffect(Set<? extends Serializable> tables) {
		return collectionInputsAffect( collectionMutationInputs, tables )
				|| collectionInputsAffect( durableCollectionMutationInputs, tables );
	}

	private boolean collectionInputsAffect(
			List<CollectionMutationInput> inputs,
			Set<? extends Serializable> tables) {
		for ( var input : inputs ) {
			final var affectedSpace = input.findAffectedQuerySpace( tables );
			if ( affectedSpace != null ) {
				ACTION_LOGGER.changesMustBeFlushedToSpace( affectedSpace );
				return true;
			}
		}
		return false;
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

	private boolean collectionMutationsAffect(
			List<PreparedCollectionMutation> mutations,
			Set<? extends Serializable> tables) {
		for ( var mutation : mutations ) {
			for ( var space : mutation.getPersister().getCollectionSpaces() ) {
				if ( tables.contains( space ) ) {
					ACTION_LOGGER.changesMustBeFlushedToSpace( space );
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
		return !collectionMutationInputs.isEmpty()
				|| !durableCollectionMutationInputs.isEmpty()
				|| !orphanCollectionRemovals.isEmpty()
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

	@Override
	public ActionQueueCheckpoint checkpoint() {
		return new GraphCheckpoint(
				this,
				updates.size(),
				collectionMutationInputs.size(),
				collectionQueuedOps.size(),
				collectionRemovals.size(),
				collectionUpdates.size(),
				collectionCreations.size()
		);
	}

	@Override
	public void restore(ActionQueueCheckpoint checkpoint) {
		if ( !( checkpoint instanceof GraphCheckpoint graphCheckpoint )
				|| graphCheckpoint.owner() != this ) {
			throw new IllegalArgumentException( "Checkpoint was not created by this action queue" );
		}
		trimToSize( updates, graphCheckpoint.updatesSize() );
		trimToSize( collectionMutationInputs, graphCheckpoint.collectionMutationInputsSize() );
		trimToSize( collectionQueuedOps, graphCheckpoint.collectionQueuedOpsSize() );
		trimToSize( collectionRemovals, graphCheckpoint.collectionRemovalsSize() );
		trimToSize( collectionUpdates, graphCheckpoint.collectionUpdatesSize() );
		trimToSize( collectionCreations, graphCheckpoint.collectionCreationsSize() );
	}

	private static void trimToSize(List<?> actions, int checkpointSize) {
		if ( actions.size() > checkpointSize ) {
			actions.subList( checkpointSize, actions.size() ).clear();
		}
	}

	private record GraphCheckpoint(
			GraphBasedActionQueue owner,
			int updatesSize,
			int collectionMutationInputsSize,
			int collectionQueuedOpsSize,
			int collectionRemovalsSize,
			int collectionUpdatesSize,
			int collectionCreationsSize) implements ActionQueueCheckpoint {
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
	@Override
	public void setTransactionCompletionCallbacks(
			TransactionCompletionCallbacksImplementor callbacks,
			boolean isTransactionCoordinatorShared) {
		this.transactionCompletionCallbacks = callbacks;
		this.isTransactionCoordinatorShared = isTransactionCoordinatorShared;
	}

	/// Get the transaction completion callbacks.
	///
	/// @return the transaction completion callbacks
	public TransactionCompletionCallbacksImplementor getTransactionCompletionCallbacks() {
		return transactionCompletionCallbacks.forSharing();
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
			auditMutationCollector.executeAuditMutations( session );
			transactionCompletionCallbacks.beforeTransactionCompletion();
			session.getJdbcCoordinator().executeBatch();
		}
	}

	@Override
	public void setAuditChangesetContext(Object changelog, org.hibernate.Session changesetSession) {
		// Graph audit execution resolves changelog context directly from the session.
	}

	/// Performs cleanup of any held cache soft locks.
	///
	/// @param success Was the transaction successful
	public void afterTransactionCompletion(boolean success) {
		if (!isTransactionCoordinatorShared) {
			auditMutationCollector.clear();
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

	/// Get all pending executable actions (for debugging/testing).
	/// Graph-native prepared collection mutations are intentionally not exposed as executables.
	///
	/// @return list of pending actions
	public List<Executable> getPendingActions() {
		var list = new ArrayList<Executable>();
		list.addAll(orphanRemovals);
		list.addAll(insertions);
		list.addAll(updates);
		list.addAll(deletions);
		return list;
	}

	/// Get the FlushCoordinator (for testing/debugging).
	///
	/// @return the flush coordinator
	public FlushCoordinator getFlushCoordinator() {
		return flushCoordinator;
	}

	/// Builds the currently queued flush plan without executing it.
	///
	/// Intended for internal diagnostics and benchmarks which discard the session
	/// immediately after inspecting the plan.
	@Internal
	public @Nullable FlushPlan buildFlushPlan() {
		return flushCoordinator.buildFlushPlan(
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
	}

	@Override
	public String toString() {
		int collectionCount = collectionCreations.size() + collectionUpdates.size()
				+ collectionRemovals.size() + collectionQueuedOps.size()
				+ orphanCollectionRemovals.size();
		return "GraphBasedActionQueue[insertions=" + numberOfInsertions()
			+ " updates=" + numberOfUpdates()
			+ " deletions=" + numberOfDeletions()
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

	private void serializeList(ObjectOutputStream oos, List<?> values) throws IOException {
		oos.writeInt(values.size());
		for (var value : values) {
			oos.writeObject(value);
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

		var orphanCollectionRemovals = deserializeList(ois, PreparedCollectionMutation.class, session);
		var orphanRemovals = deserializeList(ois, OrphanRemovalAction.class, session );
		var insertions = deserializeList(ois, AbstractEntityInsertAction.class, session );
		var updates = deserializeList(ois, EntityUpdateAction.class, session );
		var collectionQueuedOps = deserializeList(ois, PreparedCollectionMutation.class, session );
		var collectionRemovals = deserializeList(ois, PreparedCollectionMutation.class, session );
		var collectionUpdates = deserializeList(ois, PreparedCollectionMutation.class, session );
		var collectionCreations = deserializeList(ois, PreparedCollectionMutation.class, session );
		var deletions = deserializeList(ois, EntityDeleteAction.class, session );

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
				actionQueueFactory.deferIdentityInserts(),
				session
		);
	}

	@SuppressWarnings("unchecked")
	private static <T> List<T> deserializeList(
			ObjectInputStream ois,
			Class<T> actionClass,
			SessionImplementor session)
			throws IOException, ClassNotFoundException {
		int count = ois.readInt();
		var list = CollectionHelper.<T>arrayList(count);
		for (int i = 0; i < count; i++) {
			var action = (T) ois.readObject();
			if ( action instanceof Executable executable ) {
				executable.afterDeserialize( (EventSource) session );
			}
			list.add(action);
		}
		return list;
	}
}
