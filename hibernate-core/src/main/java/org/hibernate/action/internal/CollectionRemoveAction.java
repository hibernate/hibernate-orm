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
import org.hibernate.event.spi.PostCollectionRemoveEvent;
import org.hibernate.event.spi.PostCollectionRemoveEventListener;
import org.hibernate.event.spi.PreCollectionRemoveEvent;
import org.hibernate.event.spi.PreCollectionRemoveEventListener;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.stat.spi.StatisticsImplementor;

/**
 * The action for removing a collection
 */
public final class CollectionRemoveAction extends CollectionAction {
	private final Object affectedOwner;
	private boolean emptySnapshot;

	/**
	 * Removes a persistent collection from its loaded owner.
	 *
	 * Use this constructor when the collection is non-null.
	 *
	 * @param collection The collection to to remove; must be non-null
	 * @param persister  The collection's persister
	 * @param id The collection key
	 * @param emptySnapshot Indicates if the snapshot is empty
	 * @param session The session
	 *
	 * @throws AssertionFailure if collection is null.
	 */
	public CollectionRemoveAction(
				final PersistentCollection collection,
				final CollectionPersister persister,
				final Serializable id,
				final boolean emptySnapshot,
				final SharedSessionContractImplementor session) {
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
	 *
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
				final Serializable id,
				final boolean emptySnapshot,
				final SharedSessionContractImplementor session) {
		super( persister, null, id, session );
		if ( affectedOwner == null ) {
			throw new AssertionFailure("affectedOwner == null");
		}
		this.emptySnapshot = emptySnapshot;
		this.affectedOwner = affectedOwner;
	}

	@Override
	public void execute() throws HibernateException {
		preRemove();

		final SharedSessionContractImplementor session = getSession();

		if ( !emptySnapshot ) {
			// an existing collection that was either non-empty or uninitialized
			// is replaced by null or a different collection
			// (if the collection is uninitialized, hibernate has no way of
			// knowing if the collection is actually empty without querying the db)
			getPersister().remove( getKey(), session);
		}
		
		final PersistentCollection collection = getCollection();
		if ( collection != null ) {
			session.getPersistenceContextInternal().getCollectionEntry( collection ).afterAction( collection );
		}

		evict();
		postRemove();

		final StatisticsImplementor statistics = session.getFactory().getStatistics();
		if ( statistics.isStatisticsEnabled() ) {
			statistics.removeCollection( getPersister().getRole() );
		}
	}

	private void preRemove() {
		final EventListenerGroup<PreCollectionRemoveEventListener> listenerGroup = listenerGroup( EventType.PRE_COLLECTION_REMOVE );
		if ( listenerGroup.isEmpty() ) {
			return;
		}
		final PreCollectionRemoveEvent event = new PreCollectionRemoveEvent(
				getPersister(),
				getCollection(),
				eventSource(),
				affectedOwner
		);
		for ( PreCollectionRemoveEventListener listener : listenerGroup.listeners() ) {
			listener.onPreRemoveCollection( event );
		}
	}

	private void postRemove() {
		final EventListenerGroup<PostCollectionRemoveEventListener> listenerGroup = listenerGroup( EventType.POST_COLLECTION_REMOVE );
		if ( listenerGroup.isEmpty() ) {
			return;
		}
		final PostCollectionRemoveEvent event = new PostCollectionRemoveEvent(
				getPersister(),
				getCollection(),
				eventSource(),
				affectedOwner
		);
		for ( PostCollectionRemoveEventListener listener : listenerGroup.listeners() ) {
			listener.onPostRemoveCollection( event );
		}
	}
}
