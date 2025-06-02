/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.internal;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.ComparableExecutable;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.monitor.spi.EventMonitor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.monitor.spi.DiagnosticEvent;
import org.hibernate.event.spi.PostCollectionUpdateEvent;
import org.hibernate.event.spi.PostCollectionUpdateEventListener;
import org.hibernate.event.spi.PreCollectionUpdateEvent;
import org.hibernate.event.spi.PreCollectionUpdateEventListener;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.stat.spi.StatisticsImplementor;

import static org.hibernate.pretty.MessageHelper.collectionInfoString;

/**
 * The action for updating a collection
 */
public final class CollectionUpdateAction extends CollectionAction {

	private final boolean emptySnapshot;

	/**
	 * Constructs a CollectionUpdateAction
	 * @param collection The collection to update
	 * @param persister The collection persister
	 * @param id The collection key
	 * @param emptySnapshot Indicates if the snapshot is empty
	 * @param session The session
	 */
	public CollectionUpdateAction(
				final PersistentCollection<?> collection,
				final CollectionPersister persister,
				final Object id,
				final boolean emptySnapshot,
				final EventSource session) {
		super( persister, collection, id, session );
		this.emptySnapshot = emptySnapshot;
	}

	@Override
	public void execute() throws HibernateException {
		final Object key = getKey();
		final SharedSessionContractImplementor session = getSession();
		final CollectionPersister persister = getPersister();
		final PersistentCollection<?> collection = getCollection();
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
			final EventMonitor eventMonitor = session.getEventMonitor();
			final DiagnosticEvent event = eventMonitor.beginCollectionUpdateEvent();
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

		final StatisticsImplementor statistics = session.getFactory().getStatistics();
		if ( statistics.isStatisticsEnabled() ) {
			statistics.updateCollection( persister.getRole() );
		}
	}

	@Override
	public int compareTo(ComparableExecutable o) {
		if ( o instanceof CollectionUpdateAction that
				&& getPrimarySortClassifier().equals( o.getPrimarySortClassifier() ) ) {
			final CollectionPersister persister = getPersister();
			boolean hasDeletes = this.getCollection().getDeletes( persister, false ).hasNext();
			boolean otherHasDeletes = that.getCollection().getDeletes( persister, false ).hasNext();
			if ( hasDeletes && !otherHasDeletes ) {
				return -1;
			}
			if ( otherHasDeletes && !hasDeletes ) {
				return 1;
			}
		}
		return super.compareTo( o );
	}

	private void preUpdate() {
		getEventListenerGroups().eventListenerGroup_PRE_COLLECTION_UPDATE
				.fireLazyEventOnEachListener( this::newPreCollectionUpdateEvent,
						PreCollectionUpdateEventListener::onPreUpdateCollection );
	}

	private PreCollectionUpdateEvent newPreCollectionUpdateEvent() {
		return new PreCollectionUpdateEvent( getPersister(), getCollection(), eventSource() );
	}

	private void postUpdate() {
		getEventListenerGroups().eventListenerGroup_POST_COLLECTION_UPDATE
				.fireLazyEventOnEachListener( this::newPostCollectionUpdateEvent,
						PostCollectionUpdateEventListener::onPostUpdateCollection );
	}

	private PostCollectionUpdateEvent newPostCollectionUpdateEvent() {
		return new PostCollectionUpdateEvent( getPersister(), getCollection(), eventSource() );
	}

}
