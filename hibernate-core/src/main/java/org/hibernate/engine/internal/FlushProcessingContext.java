/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.internal;

import java.util.IdentityHashMap;

import org.hibernate.action.queue.spi.CollectionEndpoint;
import org.hibernate.action.queue.spi.CollectionMutationInput;
import org.hibernate.action.queue.spi.CollectionTransition;
import org.hibernate.collection.spi.CollectionMutationInterpretation;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionFlushActionTracker;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.internal.FlushVisitor;
import org.hibernate.persister.collection.CollectionPersister;

import static org.hibernate.engine.internal.Collections.processUnreachableCollection;
import static org.hibernate.engine.internal.Collections.skipRemoval;

/// Flush-local state for collection reachability and logical collection actions.
///
/// A context is created at the beginning of a flush and installed on the persistence context for
/// the duration of that flush. It coordinates collection reachability and duplicate-processing
/// detection, whose compact traversal bits live on `CollectionEntry`, and owns the temporary
/// mutation state needed while preparing collection work.
///
/// The context records queue-neutral collection mutation inputs as collection dirty-checking
/// determines that work is needed. Lifecycle preparation and queue-native lowering remain deferred
/// until after a positive flush-needed decision. The context also exposes a read-only
/// {@link CollectionFlushActionTracker} view to later flush phases.
///
/// Mutation state is allocated only for collections which need it and is keyed by collection
/// instance identity.
///
/// @since 8.0
/// @author Steve Ebersole
public final class FlushProcessingContext implements CollectionFlushActionTracker {
	private static final int CREATE = 1;
	private static final int REMOVE = 1 << 1;
	private static final int UPDATE = 1 << 2;

	private static final class CollectionMutationState {
		private final PersistentCollection<?> collection;
		private int actions;
		private CollectionEndpoint loadedEndpoint;
		private CollectionEndpoint currentEndpoint;
		private boolean emptySnapshot;
		private boolean removalSkipped;
		private boolean queuedOperations;
		private CollectionMutationInterpretation interpretation;
		private CollectionMutationState next;

		private CollectionMutationState(PersistentCollection<?> collection) {
			this.collection = collection;
		}
	}

	private final EventSource session;
	private final boolean speculative;
	private IdentityHashMap<PersistentCollection<?>, CollectionMutationState> collectionMutationStates;
	private CollectionMutationState firstCollectionMutation;
	private CollectionMutationState lastCollectionMutation;
	private OwnerUpdateCompletionCoordinator ownerUpdateCompletionCoordinator;

	/// Creates a context for a single flush of the given session.
	///
	/// @param session The event source currently being flushed
	public FlushProcessingContext(EventSource session) {
		this( session, false );
	}

	/// Creates a context, optionally retaining work for an auto-flush-needed decision.
	public FlushProcessingContext(EventSource session, boolean speculative) {
		this.session = session;
		this.speculative = speculative;
	}

	public boolean isSpeculative() {
		return speculative;
	}

	/// Retains the frozen interpretation for a collection's current structural state.
	public void retainCollectionInterpretation(
			PersistentCollection<?> collection,
			CollectionMutationInterpretation interpretation) {
		mutationState( collection ).interpretation = interpretation;
	}

	/// Returns the retained interpretation only while its comparison state remains valid.
	public CollectionMutationInterpretation getValidCollectionInterpretation(
			PersistentCollection<?> collection) {
		final var mutationState = mutationStateOrNull( collection );
		final var interpretation = mutationState == null ? null : mutationState.interpretation;
		return interpretation != null && interpretation.isValid( collection ) ? interpretation : null;
	}

	/// Registers an entity update as the authority for owner update-callback applicability.
	public void registerOwnerEntityUpdate(Object owner, org.hibernate.persister.entity.EntityPersister persister) {
		if ( !OwnerUpdateCompletionCoordinator.isNeeded( persister ) ) {
			return;
		}
		if ( ownerUpdateCompletionCoordinator == null ) {
			ownerUpdateCompletionCoordinator = new OwnerUpdateCompletionCoordinator( session );
		}
		ownerUpdateCompletionCoordinator.registerEntityMutation( owner, persister );
	}

	/// Registers non-inverse collection work with an applicable owner update lifecycle.
	public void registerOwnerCollectionMutation(Object owner, boolean inverse) {
		if ( ownerUpdateCompletionCoordinator != null ) {
			ownerUpdateCompletionCoordinator.registerCollectionMutation( owner, inverse );
		}
	}

	/// Seals owner update participation after entity and collection work has been discovered.
	public void sealOwnerUpdateCallbacks() {
		if ( ownerUpdateCompletionCoordinator != null ) {
			ownerUpdateCompletionCoordinator.seal();
		}
	}

	public boolean coordinatesOwnerUpdate(Object owner) {
		return ownerUpdateCompletionCoordinator != null && ownerUpdateCompletionCoordinator.handles( owner );
	}

	public void ownerEntityUpdateCompleted(Object owner, Runnable successfulCompletionHandler) {
		if ( ownerUpdateCompletionCoordinator == null
				|| !ownerUpdateCompletionCoordinator.entityMutationCompleted( owner, successfulCompletionHandler ) ) {
			successfulCompletionHandler.run();
		}
	}

	public void ownerCollectionMutationCompleted(
			Object owner,
			boolean inverse,
			Runnable successfulCompletionHandler) {
		if ( inverse
				|| ownerUpdateCompletionCoordinator == null
				|| !ownerUpdateCompletionCoordinator.collectionMutationCompleted(
						owner,
						successfulCompletionHandler
				) ) {
			successfulCompletionHandler.run();
		}
	}

	public void ownerMutationFailed(Object owner) {
		if ( ownerUpdateCompletionCoordinator != null ) {
			ownerUpdateCompletionCoordinator.mutationFailed( owner );
		}
	}

	/// Was the collection already marked reachable during this flush?
	///
	/// @param collection The collection instance
	///
	/// @return {@code true} if the collection was marked reachable
	public boolean isCollectionReached(PersistentCollection<?> collection) {
		final var entry = session.getPersistenceContextInternal().getCollectionEntry( collection );
		return entry != null && entry.wasReachedDuringFlush();
	}

	/// Marks the collection as reachable from a flushed entity.
	///
	/// @param collection The collection instance
	public void markCollectionReached(PersistentCollection<?> collection) {
		final var entry = session.getPersistenceContextInternal().getCollectionEntry( collection );
		if ( entry != null ) {
			entry.markReachedDuringFlush();
		}
	}

	/// Marks the collection as processed by collection reachability handling.
	///
	/// Duplicate processing indicates inconsistent graph traversal and results in an assertion
	/// failure, matching the previous {@link org.hibernate.engine.spi.CollectionEntry} guard.
	///
	/// @param collection The collection instance
	public void markCollectionProcessed(PersistentCollection<?> collection) {
		final var entry = session.getPersistenceContextInternal().getCollectionEntry( collection );
		if ( entry != null ) {
			entry.markProcessedDuringFlush();
		}
	}

	/// Records a logical collection create mutation for later queue-neutral registration.
	///
	/// @param collection The collection instance
	/// @param persister The collection persister
	/// @param key The collection key
	public void queueCollectionRecreate(PersistentCollection<?> collection, CollectionPersister persister, Object key) {
		final var state = mutationState( collection );
		state.actions |= CREATE;
		state.currentEndpoint = new CollectionEndpoint( persister, key );
	}

	/// Records a logical collection remove mutation for later queue-neutral registration.
	///
	/// The mutation is recorded even when database cascade will later suppress physical removal,
	/// since semantic remove lifecycle still applies.
	///
	/// @param collection The collection instance
	/// @param persister The collection persister
	/// @param key The collection key
	/// @param emptySnapshot Whether the collection had an empty snapshot
	public void queueCollectionRemove(
			PersistentCollection<?> collection,
			CollectionPersister persister,
			Object key,
			boolean emptySnapshot) {
		final var state = mutationState( collection );
		state.actions |= REMOVE;
		state.loadedEndpoint = new CollectionEndpoint( persister, key );
		state.emptySnapshot = emptySnapshot;
		state.removalSkipped = skipRemoval( session, persister, key );
	}

	/// Records a logical collection update mutation for later queue-neutral registration.
	///
	/// @param collection The collection instance
	/// @param persister The collection persister
	/// @param key The collection key
	/// @param emptySnapshot Whether the collection had an empty snapshot
	public void queueCollectionUpdate(
			PersistentCollection<?> collection,
			CollectionPersister persister,
			Object key,
			boolean emptySnapshot) {
		final var state = mutationState( collection );
		state.actions |= UPDATE;
		state.loadedEndpoint = new CollectionEndpoint( persister, key );
		state.currentEndpoint = state.loadedEndpoint;
		state.emptySnapshot = emptySnapshot;
	}

	/// Records delayed operations for normalization with the collection's transition.
	public void queueCollectionQueuedOperations(
			PersistentCollection<?> collection,
			CollectionPersister persister,
			Object key) {
		final var state = mutationState( collection );
		state.queuedOperations = true;
		if ( state.loadedEndpoint == null ) {
			state.loadedEndpoint = new CollectionEndpoint( persister, key );
		}
	}

	/// Registers normalized collection mutation inputs with the selected action queue.
	public void registerCollectionMutationInputs() {
		for ( var state = firstCollectionMutation; state != null; state = state.next ) {
			session.getActionQueue().addCollectionMutation( new CollectionMutationInput(
					state.collection,
					transition( state ),
					state.loadedEndpoint,
					state.currentEndpoint,
					state.emptySnapshot,
					state.removalSkipped,
					state.queuedOperations,
					null,
					null
			) );
		}
	}

	/// Re-evaluates existing collection wrappers after deferred owner pre-update callbacks.
	public void refreshCollectionMutationInputs() {
		collectionMutationStates = null;
		firstCollectionMutation = null;
		lastCollectionMutation = null;
		final var persistenceContext = session.getPersistenceContextInternal();
		persistenceContext.forEachCollectionEntry(
				(collection, collectionEntry) -> collectionEntry.resetFlushState(),
				true
		);
		for ( var managedEntity : persistenceContext.reentrantSafeManagedEntities() ) {
			final var entityEntry = managedEntity.$$_hibernate_getEntityEntry();
			final var status = entityEntry.getStatus();
			final var persister = entityEntry.getPersister();
			if ( status != Status.LOADING
					&& status != Status.GONE
					&& status != Status.DELETED
					&& persister.hasCollections() ) {
				final Object entity = managedEntity.$$_hibernate_getEntityInstance();
				new FlushVisitor( session, entity, this ).processEntityPropertyValues(
						persister.getValues( entity ),
						persister.getPropertyTypes()
				);
			}
		}
		persistenceContext.forEachCollectionEntry(
				(collection, collectionEntry) -> {
					if ( !collectionEntry.wasReachedDuringFlush()
							&& !collectionEntry.wasProcessedDuringFlush() ) {
						processUnreachableCollection( collection, session, this );
					}
					if ( !collection.wasInitialized() && collection.hasQueuedOperations() ) {
						queueCollectionQueuedOperations(
								collection,
								collectionEntry.getLoadedPersister(),
								collectionEntry.getLoadedKey()
						);
					}
				},
				true
		);
		registerCollectionMutationInputs();
	}

	private static CollectionTransition transition(CollectionMutationState state) {
		if ( state.actions == 0 ) {
			return CollectionTransition.NONE;
		}
		final boolean remove = (state.actions & REMOVE) != 0;
		final boolean create = (state.actions & CREATE) != 0;
		if ( remove && create ) {
			return CollectionTransition.REMOVE_AND_CREATE;
		}
		if ( remove ) {
			return CollectionTransition.REMOVE;
		}
		if ( create ) {
			return CollectionTransition.CREATE;
		}
		if ( (state.actions & UPDATE) != 0 ) {
			return CollectionTransition.UPDATE;
		}
		return CollectionTransition.NONE;
	}

	@Override
	public boolean wasCollectionReached(PersistentCollection<?> collection) {
		return isCollectionReached( collection );
	}

	@Override
	public boolean wasCollectionProcessed(PersistentCollection<?> collection) {
		final var entry = session.getPersistenceContextInternal().getCollectionEntry( collection );
		return entry != null && entry.wasProcessedDuringFlush();
	}

	@Override
	public boolean hasQueuedCollectionAction(PersistentCollection<?> collection) {
		final var state = mutationStateOrNull( collection );
		return state != null && state.actions != 0;
	}

	@Override
	public boolean hasQueuedCollectionRemove(PersistentCollection<?> collection) {
		final var state = mutationStateOrNull( collection );
		return state != null && (state.actions & REMOVE) != 0;
	}

	private CollectionMutationState mutationState(PersistentCollection<?> collection) {
		if ( collectionMutationStates == null ) {
			collectionMutationStates = new IdentityHashMap<>();
		}
		final var existing = collectionMutationStates.get( collection );
		if ( existing != null ) {
			return existing;
		}
		final var state = new CollectionMutationState( collection );
		collectionMutationStates.put( collection, state );
		if ( firstCollectionMutation == null ) {
			firstCollectionMutation = state;
		}
		else {
			lastCollectionMutation.next = state;
		}
		lastCollectionMutation = state;
		return state;
	}

	private CollectionMutationState mutationStateOrNull(PersistentCollection<?> collection) {
		return collectionMutationStates == null ? null : collectionMutationStates.get( collection );
	}
}
