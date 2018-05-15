/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.action.internal;

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
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.pretty.MessageHelper;

/**
 * The action for updating a collection
 */
public final class CollectionUpdateAction extends CollectionAction {
	private final boolean emptySnapshot;

	/**
	 * Constructs a CollectionUpdateAction
	 *  @param collection The collection to update
	 * @param collectionDescriptor The collection collectionDescriptor
	 * @param collectionKey The collection key
	 * @param emptySnapshot Indicates if the snapshot is empty
	 * @param session The session
	 */
	public CollectionUpdateAction(
				final PersistentCollection collection,
				final PersistentCollectionDescriptor collectionDescriptor,
				final Object collectionKey,
				final boolean emptySnapshot,
				final SharedSessionContractImplementor session) {
		super( collectionDescriptor, collection, collectionKey, session );
		this.emptySnapshot = emptySnapshot;
	}

	@Override
	public void execute() throws HibernateException {
		final Object id = getKey();
		final SharedSessionContractImplementor session = getSession();
		final PersistentCollectionDescriptor collectionDescriptor = getPersistentCollectionDescriptor();
		final PersistentCollection collection = getCollection();
		final boolean affectedByFilters = collectionDescriptor.isAffectedByEnabledFilters( session );

		preUpdate();

		if ( !collection.wasInitialized() ) {
			// If there were queued operations, they would have been processed
			// and cleared by now.
			// The collection should still be dirty.
			if ( !collection.isDirty() ) {
				throw new AssertionFailure( "collection is not dirty" );
			}
			//do nothing - we only need to notify the cache... 
		}
		else if ( !affectedByFilters && collection.empty() ) {
			if ( !emptySnapshot ) {
				collectionDescriptor.remove( id, session );
			}
		}
		else if ( collection.needsRecreate( collectionDescriptor ) ) {
			if ( affectedByFilters ) {
				throw new HibernateException(
						"cannot recreate collection while filter is enabled: " +
								MessageHelper.collectionInfoString( collectionDescriptor, collection, id, session )
				);
			}
			if ( !emptySnapshot ) {
				collectionDescriptor.remove( id, session );
			}
			collectionDescriptor.recreate( collection, id, session );
		}
		else {
			collectionDescriptor.deleteRows( collection, id, session );
			collectionDescriptor.updateRows( collection, id, session );
			collectionDescriptor.insertRows( collection, id, session );
		}

		getSession().getPersistenceContext().getCollectionEntry( collection ).afterAction( collection );
		evict();
		postUpdate();

		if ( getSession().getFactory().getStatistics().isStatisticsEnabled() ) {
			getSession().getFactory().getStatistics().updateCollection( getPersistentCollectionDescriptor().getNavigableRole().getFullPath() );
		}
	}
	
	private void preUpdate() {
		final EventListenerGroup<PreCollectionUpdateEventListener> listenerGroup = listenerGroup( EventType.PRE_COLLECTION_UPDATE );
		if ( listenerGroup.isEmpty() ) {
			return;
		}
		final PreCollectionUpdateEvent event = new PreCollectionUpdateEvent(
				getPersistentCollectionDescriptor(),
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
				getPersistentCollectionDescriptor(),
				getCollection(),
				eventSource()
		);
		for ( PostCollectionUpdateEventListener listener : listenerGroup.listeners() ) {
			listener.onPostUpdateCollection( event );
		}
	}
}
