/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.internal;

import java.util.EnumSet;
import java.util.IdentityHashMap;

import org.hibernate.AssertionFailure;
import org.hibernate.action.internal.CollectionRecreateAction;
import org.hibernate.action.internal.CollectionRemoveAction;
import org.hibernate.action.internal.CollectionUpdateAction;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionFlushActionTracker;
import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.collection.CollectionPersister;

import static org.hibernate.engine.internal.Collections.skipRemoval;
import static org.hibernate.event.internal.EventListenerLogging.EVENT_LISTENER_LOGGER;

/// Flush-local state for collection reachability and logical collection actions.
///
/// A context is created at the beginning of a flush and installed on the persistence context for
/// the duration of that flush. It owns the temporary collection-processing state that is needed
/// while walking the object graph and preparing collection work, including reachability,
/// duplicate-processing detection, and the logical collection actions queued for each collection.
///
/// The context queues collection actions as soon as collection dirty-checking determines that work
/// is needed. This keeps the action decision close to the collection-processing code while still
/// exposing a read-only {@link CollectionFlushActionTracker} view to later phases such as action
/// execution, result processing, and post-flush cleanup.
///
/// Collection state is keyed by collection instance identity.
///
/// @since 8.0
/// @author Steve Ebersole
public final class FlushProcessingContext implements CollectionFlushActionTracker {
	private enum CollectionActionKind {
		RECREATE,
		REMOVE,
		UPDATE
	}

	private static final class CollectionState {
		private boolean reached;
		private boolean processed;
		private EnumSet<CollectionActionKind> actions;
	}

	private final EventSource session;
	private final IdentityHashMap<PersistentCollection<?>, CollectionState> collectionStates = new IdentityHashMap<>();

	/// Creates a context for a single flush of the given session.
	///
	/// @param session The event source currently being flushed
	public FlushProcessingContext(EventSource session) {
		this.session = session;
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

	/// Records and queues a logical collection recreate action.
	///
	/// The associated interceptor callback is invoked before the action is added to the session
	/// action queue.
	///
	/// @param collection The collection instance
	/// @param persister The collection persister
	/// @param key The collection key
	public void queueCollectionRecreate(PersistentCollection<?> collection, CollectionPersister persister, Object key) {
		markAction( collection, CollectionActionKind.RECREATE );
		EVENT_LISTENER_LOGGER.debugf( "Creating CollectionRecreateAction for role=%s, key=%s", persister.getRole(), key );
		session.runInterceptorCallback(	() -> session.getInterceptor().onCollectionRecreate( collection, key ) );
		session.getActionQueue().addAction(	new CollectionRecreateAction(
				collection,
				persister,
				key,
				session
		) );
	}

	/// Records and queues a logical collection remove action.
	///
	/// The action is recorded even when {@link Collections#skipRemoval(EventSource, CollectionPersister, Object)}
	/// suppresses the physical remove action, since later flush phases still need to know that remove
	/// semantics were selected for the collection.
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
		markAction( collection, CollectionActionKind.REMOVE );
		session.runInterceptorCallback(	() -> session.getInterceptor().onCollectionRemove( collection, key ) );
		if ( !skipRemoval( session, persister, key ) ) {
			session.getActionQueue().addAction(	new CollectionRemoveAction(
					collection,
					persister,
					key,
					emptySnapshot,
					session
			) );
		}
	}

	/// Records and queues a logical collection update action.
	///
	/// The associated interceptor callback is invoked before the action is added to the session
	/// action queue.
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
		markAction( collection, CollectionActionKind.UPDATE );
		session.runInterceptorCallback( () -> session.getInterceptor().onCollectionUpdate( collection, key ) );
		session.getActionQueue().addAction( new CollectionUpdateAction(
				collection,
				persister,
				key,
				emptySnapshot,
				session
		) );
	}

	private void markAction(PersistentCollection<?> collection, CollectionActionKind actionKind) {
		final var state = state( collection );
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
