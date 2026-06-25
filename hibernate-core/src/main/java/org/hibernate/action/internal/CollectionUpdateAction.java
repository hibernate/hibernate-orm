/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.ComparableExecutable;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.PostCollectionUpdateEvent;
import org.hibernate.event.spi.PostCollectionUpdateEventListener;
import org.hibernate.event.spi.PreCollectionUpdateEvent;
import org.hibernate.event.spi.PreCollectionUpdateEventListener;
import org.hibernate.persister.collection.CollectionPersister;

import static org.hibernate.pretty.MessageHelper.collectionInfoString;

/**
 * The action for updating a collection
 */
public final class CollectionUpdateAction extends CollectionAction {

	private final boolean emptySnapshot;
	@Nullable
	private final Object affectedOwner;
	@Nullable
	private final Object affectedOwnerId;

	/**
	 * Constructs a CollectionUpdateAction
	 * @param collection The collection to update
	 * @param persister The collection persister
	 * @param id The collection key
	 * @param emptySnapshot Indicates if the snapshot is empty
	 * @param session The session
	 */
	public CollectionUpdateAction(
				final @Nonnull PersistentCollection<?> collection,
				final @Nonnull CollectionPersister persister,
				final @Nonnull Object id,
				final boolean emptySnapshot,
				final @Nonnull EventSource session) {
		super( persister, collection, id, session );
		assert collection != null;
		assert persister != null;
		assert id != null;
		this.emptySnapshot = emptySnapshot;
		// Capture the owner at action creation time so it's available when the post-event
		// fires (which may be after the collection owner reference has been cleared)
		affectedOwner = collection.getOwner();
		// Also capture the owner ID from the entity entry at action creation time
		final var ownerEntry = session.getPersistenceContextInternal().getEntry( affectedOwner );
		affectedOwnerId = ownerEntry != null ? ownerEntry.getId() : null;
	}

	@Override
	@Nonnull
	public PersistentCollection<?> getCollection() {
		final var collection = super.getCollection();
		assert collection != null;
		return collection;
	}

	@Override
	@Nonnull
	public Object getKey() {
		final Object key = super.getKey();
		assert key != null;
		return key;
	}

	public boolean isEmptySnapshot() {
		return emptySnapshot;
	}

	@Override
	public void execute() {
		final Object key = getKey();
		final var session = getSession();
		final var persister = getPersister();
		final var collection = getCollection();
		final boolean affectedByFilters = persister.isAffectedByEnabledFilters( session );

		preUpdate();

		if ( !collection.wasInitialized() ) {
			// If there were queued operations, they would have
			// been processed and cleared by now.
			if ( !collection.isDirty() ) {
				// The collection should still be dirty.
				throw new AssertionFailure( "collection is not dirty" );
			}
			// Do nothing - we only need to notify the cache
		}
		else {
			final var eventMonitor = session.getEventMonitor();
			final var event = eventMonitor.beginCollectionUpdateEvent();
			boolean success = false;
			try {
				if ( !affectedByFilters && collection.empty() ) {
					if ( !emptySnapshot ) {
						persister.remove( key, session );
					}
					//TODO: else we really shouldn't have sent an update event to JFR
				}
				else if ( collection.needsRecreate( persister ) ) {
					if ( affectedByFilters ) {
						throw new HibernateException( "cannot recreate collection while filter is enabled: "
												+ collectionInfoString( persister, collection, key, session ) );
					}
					if ( !emptySnapshot ) {
						persister.remove( key, session );
					}
					persister.recreate( collection, key, session );
				}
				else {
					persister.deleteRows( collection, key, session );
					persister.updateRows( collection, key, session );
					persister.insertRows( collection, key, session );
				}
				success = true;
			}
			finally {
				eventMonitor.completeCollectionUpdateEvent( event, key, persister.getRole(), success, session );
			}
		}

		session.getPersistenceContextInternal().getCollectionEntry( collection ).afterAction( collection );
		evict();
		postUpdate();

		final var statistics = session.getFactory().getStatistics();
		if ( statistics.isStatisticsEnabled() ) {
			statistics.updateCollection( persister.getRole() );
		}
	}

	/**
	 * Sort update actions with deletions to the start of the line
	 * in order to limit the chance of a unique key violation.
	 */
	@Override
	public int compareTo(@Nonnull ComparableExecutable executable) {
		if ( executable instanceof CollectionUpdateAction that
				&& getPrimarySortClassifier().equals( executable.getPrimarySortClassifier() ) ) {
			final var persister = getPersister();
			final boolean hasDeletes = this.getCollection().hasDeletes( persister );
			final boolean otherHasDeletes = that.getCollection().hasDeletes( persister );
			if ( hasDeletes && !otherHasDeletes ) {
				return -1;
			}
			if ( otherHasDeletes && !hasDeletes ) {
				return 1;
			}
		}
		return super.compareTo( executable );
	}

	public void preUpdate() {
		getEventListenerGroups().eventListenerGroup_PRE_COLLECTION_UPDATE
				.fireLazyEventOnEachListener( this::newPreCollectionUpdateEvent,
						PreCollectionUpdateEventListener::onPreUpdateCollection );
	}

	@Nonnull
	private PreCollectionUpdateEvent newPreCollectionUpdateEvent() {
		return new PreCollectionUpdateEvent( getPersister(), getCollection(), eventSource() );
	}

	public void postUpdate() {
		getEventListenerGroups().eventListenerGroup_POST_COLLECTION_UPDATE
				.fireLazyEventOnEachListener( this::newPostCollectionUpdateEvent,
						PostCollectionUpdateEventListener::onPostUpdateCollection );
	}

	@Nonnull
	private PostCollectionUpdateEvent newPostCollectionUpdateEvent() {
		return new PostCollectionUpdateEvent( getPersister(), getCollection(), eventSource() );
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
