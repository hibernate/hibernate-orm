/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.PostCollectionRecreateEvent;
import org.hibernate.event.spi.PostCollectionRecreateEventListener;
import org.hibernate.event.spi.PreCollectionRecreateEvent;
import org.hibernate.event.spi.PreCollectionRecreateEventListener;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * The action for recreating a collection
 */
public final class CollectionRecreateAction extends CollectionAction {

	@Nullable
	private final Object affectedOwner;
	@Nullable
	private final Object affectedOwnerId;

	/**
	 * Constructs a CollectionRecreateAction
	 *  @param collection The collection being recreated
	 * @param persister The collection persister
	 * @param id The collection key
	 * @param session The session
	 */
	public CollectionRecreateAction(
			final @Nonnull PersistentCollection<?> collection,
			final @Nonnull CollectionPersister persister,
			final @Nullable Object id,
			final @Nonnull EventSource session) {
		super( persister, collection, id, session );
		assert collection != null;
		// Capture the owner at action creation time so it's available when the post-event
		// fires (which may be after the collection owner reference has been cleared)
		this.affectedOwner = collection.getOwner();
		// Also capture the owner ID from the entity entry at action creation time
		final var ownerEntry = session.getPersistenceContextInternal().getEntry( affectedOwner );
		this.affectedOwnerId = ownerEntry != null ? ownerEntry.getId() : null;
	}

	@Override
	@Nonnull
	public PersistentCollection<?> getCollection() {
		final var collection = super.getCollection();
		assert collection != null;
		return collection;
	}

	@Override
	public void execute() {
		// this method is called when a new non-null collection is persisted
		// or when an existing (non-null) collection is moved to a new owner
		final var collection = getCollection();
		preRecreate();
		final var session = getSession();
		final var persister = getPersister();
		final Object key = getKey();
		assert key != null;
		final var eventMonitor = session.getEventMonitor();
		final var event = eventMonitor.beginCollectionRecreateEvent();
		boolean success = false;
		try {
			persister.recreate( collection, key, session );
			success = true;
		}
		finally {
			eventMonitor.completeCollectionRecreateEvent( event, key, persister.getRole(), success, session );
		}

		session.getPersistenceContextInternal().getCollectionEntry( collection ).afterAction( collection );
		evict();
		postRecreate();

		final var statistics = session.getFactory().getStatistics();
		if ( statistics.isStatisticsEnabled() ) {
			statistics.recreateCollection( persister.getRole() );
		}
	}

	private void preRecreate() {
		getEventListenerGroups().eventListenerGroup_PRE_COLLECTION_RECREATE
				.fireLazyEventOnEachListener( this::newPreCollectionRecreateEvent,
						PreCollectionRecreateEventListener::onPreRecreateCollection );
	}

	@Nonnull
	private PreCollectionRecreateEvent newPreCollectionRecreateEvent() {
		return new PreCollectionRecreateEvent( getPersister(), getCollection(), eventSource() );
	}

	private void postRecreate() {
		getEventListenerGroups().eventListenerGroup_POST_COLLECTION_RECREATE
				.fireLazyEventOnEachListener( this::newPostCollectionRecreateEvent,
						PostCollectionRecreateEventListener::onPostRecreateCollection );
	}


	@Nonnull
	private PostCollectionRecreateEvent newPostCollectionRecreateEvent() {
		return new PostCollectionRecreateEvent( getPersister(), getCollection(), eventSource() );
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
