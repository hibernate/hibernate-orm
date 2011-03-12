/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.action;

import org.hibernate.HibernateException;
import org.hibernate.AssertionFailure;
import org.hibernate.event.PostCollectionRemoveEvent;
import org.hibernate.event.PreCollectionRemoveEvent;
import org.hibernate.event.PreCollectionRemoveEventListener;
import org.hibernate.event.EventSource;
import org.hibernate.event.PostCollectionRemoveEventListener;
import org.hibernate.cache.CacheException;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;

import java.io.Serializable;

public final class CollectionRemoveAction extends CollectionAction {

	private boolean emptySnapshot;
	private final Object affectedOwner;
	
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
	 * @throws AssertionFailure if collection is null.
	 */
	public CollectionRemoveAction(
				final PersistentCollection collection,
				final CollectionPersister persister,
				final Serializable id,
				final boolean emptySnapshot,
				final SessionImplementor session)
			throws CacheException {
		super( persister, collection, id, session );
		if (collection == null) { throw new AssertionFailure("collection == null"); }
		this.emptySnapshot = emptySnapshot;
		// the loaded owner will be set to null after the collection is removed,
		// so capture its value as the affected owner so it is accessible to
		// both pre- and post- events
		this.affectedOwner = session.getPersistenceContext().getLoadedCollectionOwnerOrNull( collection );
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
	 * @throws AssertionFailure if affectedOwner is null.
	 */
	public CollectionRemoveAction(
				final Object affectedOwner,
				final CollectionPersister persister,
				final Serializable id,
				final boolean emptySnapshot,
				final SessionImplementor session)
			throws CacheException {
		super( persister, null, id, session );
		if (affectedOwner == null) { throw new AssertionFailure("affectedOwner == null"); }
		this.emptySnapshot = emptySnapshot;
		this.affectedOwner = affectedOwner;
	}
	
	public void execute() throws HibernateException {
		preRemove();

		if ( !emptySnapshot ) {
			// an existing collection that was either non-empty or uninitialized
			// is replaced by null or a different collection
			// (if the collection is uninitialized, hibernate has no way of
			// knowing if the collection is actually empty without querying the db)
			getPersister().remove( getKey(), getSession() );
		}
		
		final PersistentCollection collection = getCollection();
		if (collection!=null) {
			getSession().getPersistenceContext()
				.getCollectionEntry(collection)
				.afterAction(collection);
		}
		
		evict();

		postRemove();		

		if ( getSession().getFactory().getStatistics().isStatisticsEnabled() ) {
			getSession().getFactory().getStatisticsImplementor()
					.removeCollection( getPersister().getRole() );
		}
	}

	private void preRemove() {
		PreCollectionRemoveEventListener[] preListeners = getSession().getListeners()
				.getPreCollectionRemoveEventListeners();
		if (preListeners.length>0) {
			PreCollectionRemoveEvent preEvent = new PreCollectionRemoveEvent(
					getPersister(), getCollection(), ( EventSource ) getSession(), affectedOwner );
			for ( int i = 0; i < preListeners.length; i++ ) {
				preListeners[i].onPreRemoveCollection(preEvent);
			}
		}
	}

	private void postRemove() {
		PostCollectionRemoveEventListener[] postListeners = getSession().getListeners()
				.getPostCollectionRemoveEventListeners();
		if (postListeners.length>0) {
			PostCollectionRemoveEvent postEvent = new PostCollectionRemoveEvent(
					getPersister(), getCollection(), ( EventSource ) getSession(), affectedOwner );
			for ( int i = 0; i < postListeners.length; i++ ) {
				postListeners[i].onPostRemoveCollection(postEvent);
			}
		}
	}
}
