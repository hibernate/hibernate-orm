/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.internal;

import java.util.EnumSet;
import java.util.IdentityHashMap;

import org.hibernate.AssertionFailure;
import org.hibernate.action.queue.spi.CollectionEndpoint;
import org.hibernate.action.queue.spi.CollectionMutationInput;
import org.hibernate.action.queue.spi.CollectionTransition;
import org.hibernate.action.queue.internal.FrozenCollectionDelta;
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
/// the duration of that flush. It owns the temporary collection-processing state that is needed
/// while walking the object graph and preparing collection work, including reachability,
/// duplicate-processing detection, and the logical collection actions queued for each collection.
///
/// The context records queue-neutral collection mutation inputs as collection dirty-checking
/// determines that work is needed. Lifecycle preparation and queue-native lowering remain deferred
/// until after a positive flush-needed decision. The context also exposes a read-only
/// {@link CollectionFlushActionTracker} view to later flush phases.
///
/// Collection state is keyed by collection instance identity.
///
/// @since 8.0
/// @author Steve Ebersole
public final class FlushProcessingContext implements CollectionFlushActionTracker {
	private enum CollectionActionKind {
		CREATE,
		REMOVE,
		UPDATE
	}

	private static final class CollectionState {
		private boolean reached;
		private boolean processed;
		private EnumSet<CollectionActionKind> actions;
		private CollectionEndpoint loadedEndpoint;
		private CollectionEndpoint currentEndpoint;
		private boolean emptySnapshot;
		private boolean removalSkipped;
		private boolean queuedOperations;
	}

	private final EventSource session;
	private final boolean speculative;
	private final OwnerUpdateCompletionCoordinator ownerUpdateCompletionCoordinator;
	private final IdentityHashMap<PersistentCollection<?>, CollectionState> collectionStates = new IdentityHashMap<>();
	private final IdentityHashMap<PersistentCollection<?>, FrozenCollectionDelta> frozenDeltas = new IdentityHashMap<>();

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
		ownerUpdateCompletionCoordinator = new OwnerUpdateCompletionCoordinator( session );
	}

	public boolean isSpeculative() {
		return speculative;
	}

	/// Retains the one frozen delta for a collection's current structural state.
	public void retainFrozenDelta(
			PersistentCollection<?> collection,
			FrozenCollectionDelta frozenDelta) {
		frozenDeltas.put( collection, frozenDelta );
	}

	/// Returns the retained delta only while its comparison state remains valid.
	public FrozenCollectionDelta getValidFrozenDelta(PersistentCollection<?> collection) {
		final var frozenDelta = frozenDeltas.get( collection );
		return frozenDelta != null && frozenDelta.isValid( collection ) ? frozenDelta : null;
	}

	/// Registers an entity update as the authority for owner update-callback applicability.
	public void registerOwnerEntityUpdate(Object owner, org.hibernate.persister.entity.EntityPersister persister) {
		ownerUpdateCompletionCoordinator.registerEntityMutation( owner, persister );
	}

	/// Registers non-inverse collection work with an applicable owner update lifecycle.
	public void registerOwnerCollectionMutation(Object owner, boolean inverse) {
		ownerUpdateCompletionCoordinator.registerCollectionMutation( owner, inverse );
	}

	/// Seals owner update participation after entity and collection work has been discovered.
	public void sealOwnerUpdateCallbacks() {
		ownerUpdateCompletionCoordinator.seal();
	}

	public boolean coordinatesOwnerUpdate(Object owner) {
		return ownerUpdateCompletionCoordinator.handles( owner );
	}

	public void ownerEntityUpdateCompleted(Object owner, Runnable successfulCompletionHandler) {
		if ( !ownerUpdateCompletionCoordinator.entityMutationCompleted( owner, successfulCompletionHandler ) ) {
			successfulCompletionHandler.run();
		}
	}

	public void ownerCollectionMutationCompleted(
			Object owner,
			boolean inverse,
			Runnable successfulCompletionHandler) {
		if ( inverse
				|| !ownerUpdateCompletionCoordinator.collectionMutationCompleted(
						owner,
						successfulCompletionHandler
				) ) {
			successfulCompletionHandler.run();
		}
	}

	public void ownerMutationFailed(Object owner) {
		ownerUpdateCompletionCoordinator.mutationFailed( owner );
	}

	/// Initializes flush-local state for a collection known to the persistence context before
	/// collection reachability processing begins.
	///
	/// @param collection The collection instance
	public void beginCollectionFlush(PersistentCollection<?> collection) {
		collectionStates.put( collection, new CollectionState() );
	}

	/// Was the collection already marked reachable during this flush?
	///
	/// @param collection The collection instance
	///
	/// @return {@code true} if the collection was marked reachable
	public boolean isCollectionReached(PersistentCollection<?> collection) {
		return state( collection ).reached;
	}

	/// Marks the collection as reachable from a flushed entity.
	///
	/// @param collection The collection instance
	public void markCollectionReached(PersistentCollection<?> collection) {
		state( collection ).reached = true;
	}

	/// Marks the collection as processed by collection reachability handling.
	///
	/// Duplicate processing indicates inconsistent graph traversal and results in an assertion
	/// failure, matching the previous {@link org.hibernate.engine.spi.CollectionEntry} guard.
	///
	/// @param collection The collection instance
	public void markCollectionProcessed(PersistentCollection<?> collection) {
		final var state = state( collection );
		if ( state.processed ) {
			throw new AssertionFailure( "collection was processed twice by flush()" );
		}
		state.processed = true;
	}

	/// Records a logical collection create mutation for later queue-neutral registration.
	///
	/// @param collection The collection instance
	/// @param persister The collection persister
	/// @param key The collection key
	public void queueCollectionRecreate(PersistentCollection<?> collection, CollectionPersister persister, Object key) {
		final var state = state( collection );
		markAction( state, CollectionActionKind.CREATE );
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
		final var state = state( collection );
		markAction( state, CollectionActionKind.REMOVE );
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
		final var state = state( collection );
		markAction( state, CollectionActionKind.UPDATE );
		state.loadedEndpoint = new CollectionEndpoint( persister, key );
		state.currentEndpoint = state.loadedEndpoint;
		state.emptySnapshot = emptySnapshot;
	}

	/// Records delayed operations for normalization with the collection's transition.
	public void queueCollectionQueuedOperations(
			PersistentCollection<?> collection,
			CollectionPersister persister,
			Object key) {
		final var state = state( collection );
		state.queuedOperations = true;
		if ( state.loadedEndpoint == null ) {
			state.loadedEndpoint = new CollectionEndpoint( persister, key );
		}
	}

	/// Registers normalized collection mutation inputs with the selected action queue.
	public void registerCollectionMutationInputs() {
		for ( var entry : collectionStates.entrySet() ) {
			final var state = entry.getValue();
			if ( state.actions == null && !state.queuedOperations ) {
				continue;
			}
			session.getActionQueue().addCollectionMutation( new CollectionMutationInput(
					entry.getKey(),
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
		for ( var state : collectionStates.values() ) {
			state.reached = false;
			state.processed = false;
			state.actions = null;
			state.loadedEndpoint = null;
			state.currentEndpoint = null;
			state.emptySnapshot = false;
			state.removalSkipped = false;
			state.queuedOperations = false;
		}
		final var persistenceContext = session.getPersistenceContextInternal();
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
					if ( !wasCollectionReached( collection ) && !wasCollectionProcessed( collection ) ) {
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

	private static CollectionTransition transition(CollectionState state) {
		if ( state.actions == null ) {
			return CollectionTransition.NONE;
		}
		final boolean remove = state.actions.contains( CollectionActionKind.REMOVE );
		final boolean create = state.actions.contains( CollectionActionKind.CREATE );
		if ( remove && create ) {
			return CollectionTransition.REMOVE_AND_CREATE;
		}
		if ( remove ) {
			return CollectionTransition.REMOVE;
		}
		if ( create ) {
			return CollectionTransition.CREATE;
		}
		if ( state.actions.contains( CollectionActionKind.UPDATE ) ) {
			return CollectionTransition.UPDATE;
		}
		return CollectionTransition.NONE;
	}

	private static void markAction(CollectionState state, CollectionActionKind actionKind) {
		if ( state.actions == null ) {
			state.actions = EnumSet.noneOf( CollectionActionKind.class );
		}
		state.actions.add( actionKind );
	}

	@Override
	public boolean wasCollectionReached(PersistentCollection<?> collection) {
		final var state = collectionStates.get( collection );
		return state != null && state.reached;
	}

	@Override
	public boolean wasCollectionProcessed(PersistentCollection<?> collection) {
		final var state = collectionStates.get( collection );
		return state != null && state.processed;
	}

	@Override
	public boolean hasQueuedCollectionAction(PersistentCollection<?> collection) {
		final var state = collectionStates.get( collection );
		return state != null && state.actions != null && !state.actions.isEmpty();
	}

	@Override
	public boolean hasQueuedCollectionRemove(PersistentCollection<?> collection) {
		final var state = collectionStates.get( collection );
		return state != null && state.actions != null && state.actions.contains( CollectionActionKind.REMOVE );
	}

	private CollectionState state(PersistentCollection<?> collection) {
		return collectionStates.computeIfAbsent( collection, key -> new CollectionState() );
	}
}
