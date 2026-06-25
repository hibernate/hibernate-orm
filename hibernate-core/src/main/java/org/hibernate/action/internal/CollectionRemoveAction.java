/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.AssertionFailure;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.PostCollectionRemoveEvent;
import org.hibernate.event.spi.PostCollectionRemoveEventListener;
import org.hibernate.event.spi.PreCollectionRemoveEvent;
import org.hibernate.event.spi.PreCollectionRemoveEventListener;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * The action for removing a collection
 */
public final class CollectionRemoveAction extends CollectionAction {

	@Nullable
	private final Object affectedOwner;
	@Nullable
	private final Object affectedOwnerId;
	private final boolean emptySnapshot;

	/**
	 * Removes a persistent collection from its loaded owner.
	 * <p>
	 * Use this constructor when the collection is non-null.
	 *
	 * @param collection The collection to remove; must be non-null
	 * @param persister  The collection's persister
	 * @param id The collection key
	 * @param emptySnapshot Indicates if the snapshot is empty
	 * @param session The session
	 *
	 * @throws AssertionFailure if collection is null.
	 */
	public CollectionRemoveAction(
				final @Nonnull PersistentCollection<?> collection,
				final @Nonnull CollectionPersister persister,
				final @Nullable Object id, // only null in certain deserialization tests
				final boolean emptySnapshot,
				final @Nonnull EventSource session) {
		super( persister, collection, id, session );
		assert collection != null;
		this.emptySnapshot = emptySnapshot;
		// the loaded owner will be set to null after the collection is removed,
		// so capture its value as the affected owner so it is accessible to
		// both pre- and post- events
		final var persistenceContext = session.getPersistenceContextInternal();
		affectedOwner = persistenceContext.getLoadedCollectionOwnerOrNull( collection );
		affectedOwnerId = persistenceContext.getLoadedCollectionOwnerIdOrNull( collection );
	}

	/**
	 * Removes a persistent collection from a specified owner.
	 * <p>
	 * Use this constructor when the collection to be removed has not been loaded.
	 *
	 * @param affectedOwner The collection's owner; must be non-null
	 * @param persister  The collection's persister
	 * @param id The collection key
	 * @param emptySnapshot Indicates if the snapshot is empty
	 * @param session The session
	 *
	 * @throws AssertionFailure if affectedOwner is null.
	 */
	public CollectionRemoveAction(
				final @Nonnull Object affectedOwner,
				final @Nonnull CollectionPersister persister,
				final @Nonnull Object id,
				final boolean emptySnapshot,
				final @Nonnull EventSource session) {
		super( persister, null, id, session );
		assert affectedOwner != null;
		assert id != null;
		this.emptySnapshot = emptySnapshot;
		this.affectedOwner = affectedOwner;
		// Get the owner ID from the entity entry at action creation time
		final var ownerEntry = session.getPersistenceContextInternal().getEntry( affectedOwner );
		this.affectedOwnerId = ownerEntry != null ? ownerEntry.getId() : null;
	}

	/**
	 * Removes a persistent collection for an unloaded proxy.
	 * <p>
	 * Use this constructor when the owning entity is has not been loaded.
	 *
	 * @param persister The collection's persister
	 * @param id The collection key
	 * @param session The session
	 */
	public CollectionRemoveAction(
			final @Nonnull CollectionPersister persister,
			final @Nonnull Object id,
			final @Nonnull EventSource session) {
		super( persister, null, id, session );
		assert id != null;
		emptySnapshot = false;
		affectedOwner = null;
		affectedOwnerId = null;
	}

	public boolean isEmptySnapshot() {
		return emptySnapshot;
	}

	@Override
	public void execute() {
		preRemove();
		final var session = getSession();
		if ( !emptySnapshot ) {
			// an existing collection that was either nonempty or uninitialized
			// is replaced by null or a different collection
			// (if the collection is uninitialized, Hibernate has no way of
			// knowing if the collection is actually empty without querying the db)
			final var persister = getPersister();
			final Object key = getKey();
			assert key != null;
			final var eventMonitor = session.getEventMonitor();
			final var event = eventMonitor.beginCollectionRemoveEvent();
			boolean success = false;
			try {
				persister.remove( key, session );
				success = true;
			}
			finally {
				eventMonitor.completeCollectionRemoveEvent( event, key, persister.getRole(), success, session );
			}
		}

		final var collection = getCollection();
		if ( collection != null ) {
			session.getPersistenceContextInternal().getCollectionEntry( collection ).afterAction( collection );
		}
		evict();
		postRemove();

		final var statistics = session.getFactory().getStatistics();
		if ( statistics.isStatisticsEnabled() ) {
			statistics.removeCollection( getPersister().getRole() );
		}
	}

	private void preRemove() {
		getEventListenerGroups().eventListenerGroup_PRE_COLLECTION_REMOVE
				.fireLazyEventOnEachListener( this::newPreCollectionRemoveEvent,
						PreCollectionRemoveEventListener::onPreRemoveCollection );
	}

	@Nonnull
	private PreCollectionRemoveEvent newPreCollectionRemoveEvent() {
		final var collection = getCollection();
		assert collection != null;
		return new PreCollectionRemoveEvent( getPersister(), collection, eventSource(), affectedOwner );
	}

	private void postRemove() {
		getEventListenerGroups().eventListenerGroup_POST_COLLECTION_REMOVE
				.fireLazyEventOnEachListener( this::newPostCollectionRemoveEvent,
						PostCollectionRemoveEventListener::onPostRemoveCollection );
	}

	@Nonnull
	private PostCollectionRemoveEvent newPostCollectionRemoveEvent() {
		final var collection = getCollection();
		assert collection != null;
		return new PostCollectionRemoveEvent( getPersister(), collection, eventSource(), affectedOwner );
	}

	@Nullable
	public Object getAffectedOwner() {
		return affectedOwner;
	}

	@Nullable
	public Object getAffectedOwnerId() {
		return affectedOwnerId;
	}
}
