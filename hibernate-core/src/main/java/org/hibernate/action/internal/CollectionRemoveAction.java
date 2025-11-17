/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.internal;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
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

	private final Object affectedOwner;
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
				final PersistentCollection<?> collection,
				final CollectionPersister persister,
				final Object id,
				final boolean emptySnapshot,
				final EventSource session) {
		super( persister, collection, id, session );
		if ( collection == null ) {
			throw new AssertionFailure("collection == null");
		}
		this.emptySnapshot = emptySnapshot;
		// the loaded owner will be set to null after the collection is removed,
		// so capture its value as the affected owner so it is accessible to
		// both pre- and post- events
		this.affectedOwner = session.getPersistenceContextInternal().getLoadedCollectionOwnerOrNull( collection );
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
				final Object affectedOwner,
				final CollectionPersister persister,
				final Object id,
				final boolean emptySnapshot,
				final EventSource session) {
		super( persister, null, id, session );
		if ( affectedOwner == null ) {
			throw new AssertionFailure("affectedOwner == null");
		}
		this.emptySnapshot = emptySnapshot;
		this.affectedOwner = affectedOwner;
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
			final CollectionPersister persister,
			final Object id,
			final EventSource session) {
		super( persister, null, id, session );
		emptySnapshot = false;
		affectedOwner = null;
	}

	@Override
	public void execute() throws HibernateException {
		preRemove();
		final var session = getSession();
		if ( !emptySnapshot ) {
			// an existing collection that was either nonempty or uninitialized
			// is replaced by null or a different collection
			// (if the collection is uninitialized, Hibernate has no way of
			// knowing if the collection is actually empty without querying the db)
			final var persister = getPersister();
			final Object key = getKey();
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

		final PersistentCollection<?> collection = getCollection();
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

	private PreCollectionRemoveEvent newPreCollectionRemoveEvent() {
		return new PreCollectionRemoveEvent( getPersister(), getCollection(), eventSource(), affectedOwner );
	}

	private void postRemove() {
		getEventListenerGroups().eventListenerGroup_POST_COLLECTION_REMOVE
				.fireLazyEventOnEachListener( this::newPostCollectionRemoveEvent,
						PostCollectionRemoveEventListener::onPostRemoveCollection );
	}

	private PostCollectionRemoveEvent newPostCollectionRemoveEvent() {
		return new PostCollectionRemoveEvent( getPersister(), getCollection(), eventSource(), affectedOwner );
	}

	public Object getAffectedOwner() {
		return affectedOwner;
	}
}
