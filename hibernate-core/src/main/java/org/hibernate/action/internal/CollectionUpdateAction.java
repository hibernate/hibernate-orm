/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.action.internal;

import java.io.Serializable;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostCollectionUpdateEvent;
import org.hibernate.event.spi.PostCollectionUpdateEventListener;
import org.hibernate.event.spi.PreCollectionUpdateEvent;
import org.hibernate.event.spi.PreCollectionUpdateEventListener;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.pretty.MessageHelper;

/**
 * The action for updating a collection
 */
public final class CollectionUpdateAction extends CollectionAction {
	private final boolean emptySnapshot;

	/**
	 * Constructs a CollectionUpdateAction
	 *
	 * @param collection The collection to update
	 * @param persister The collection persister
	 * @param id The collection key
	 * @param emptySnapshot Indicates if the snapshot is empty
	 * @param session The session
	 */
	public CollectionUpdateAction(
				final PersistentCollection collection,
				final CollectionPersister persister,
				final Serializable id,
				final boolean emptySnapshot,
				final SharedSessionContractImplementor session) {
		super( persister, collection, id, session );
		this.emptySnapshot = emptySnapshot;
	}

	@Override
	public void execute() throws HibernateException {
		final Serializable id = getKey();
		final SharedSessionContractImplementor session = getSession();
		final CollectionPersister persister = getPersister();
		final PersistentCollection collection = getCollection();
		final boolean affectedByFilters = persister.isAffectedByEnabledFilters( session );

		preUpdate();

		if ( !collection.wasInitialized() ) {
			if ( !collection.hasQueuedOperations() ) {
				throw new AssertionFailure( "no queued adds" );
			}
			//do nothing - we only need to notify the cache... 
		}
		else if ( !affectedByFilters && collection.empty() ) {
			if ( !emptySnapshot ) {
				persister.remove( id, session );
			}
		}
		else if ( collection.needsRecreate( persister ) ) {
			if ( affectedByFilters ) {
				throw new HibernateException(
						"cannot recreate collection while filter is enabled: " +
								MessageHelper.collectionInfoString( persister, collection, id, session )
				);
			}
			if ( !emptySnapshot ) {
				persister.remove( id, session );
			}
			persister.recreate( collection, id, session );
		}
		else {
			persister.deleteRows( collection, id, session );
			persister.updateRows( collection, id, session );
			persister.insertRows( collection, id, session );
		}

		getSession().getPersistenceContext().getCollectionEntry( collection ).afterAction( collection );
		evict();
		postUpdate();

		if ( getSession().getFactory().getStatistics().isStatisticsEnabled() ) {
			getSession().getFactory().getStatistics().updateCollection( getPersister().getRole() );
		}
	}
	
	private void preUpdate() {
		final EventListenerGroup<PreCollectionUpdateEventListener> listenerGroup = listenerGroup( EventType.PRE_COLLECTION_UPDATE );
		if ( listenerGroup.isEmpty() ) {
			return;
		}
		final PreCollectionUpdateEvent event = new PreCollectionUpdateEvent(
				getPersister(),
				getCollection(),
				eventSource()
		);
		for ( PreCollectionUpdateEventListener listener : listenerGroup.listeners() ) {
			listener.onPreUpdateCollection( event );
		}
	}

	private void postUpdate() {
		final EventListenerGroup<PostCollectionUpdateEventListener> listenerGroup = listenerGroup( EventType.POST_COLLECTION_UPDATE );
		if ( listenerGroup.isEmpty() ) {
			return;
		}
		final PostCollectionUpdateEvent event = new PostCollectionUpdateEvent(
				getPersister(),
				getCollection(),
				eventSource()
		);
		for ( PostCollectionUpdateEventListener listener : listenerGroup.listeners() ) {
			listener.onPostUpdateCollection( event );
		}
	}
}
