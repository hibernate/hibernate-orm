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

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.event.PostCollectionUpdateEvent;
import org.hibernate.event.PreCollectionUpdateEvent;
import org.hibernate.event.PreCollectionUpdateEventListener;
import org.hibernate.event.EventSource;
import org.hibernate.event.PostCollectionUpdateEventListener;
import org.hibernate.cache.CacheException;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.pretty.MessageHelper;

import java.io.Serializable;

public final class CollectionUpdateAction extends CollectionAction {

	private final boolean emptySnapshot;

	public CollectionUpdateAction(
				final PersistentCollection collection,
				final CollectionPersister persister,
				final Serializable id,
				final boolean emptySnapshot,
				final SessionImplementor session)
			throws CacheException {
		super( persister, collection, id, session );
		this.emptySnapshot = emptySnapshot;
	}

	public void execute() throws HibernateException {
		final Serializable id = getKey();
		final SessionImplementor session = getSession();
		final CollectionPersister persister = getPersister();
		final PersistentCollection collection = getCollection();
		boolean affectedByFilters = persister.isAffectedByEnabledFilters(session);

		preUpdate();

		if ( !collection.wasInitialized() ) {
			if ( !collection.hasQueuedOperations() ) throw new AssertionFailure( "no queued adds" );
			//do nothing - we only need to notify the cache...
		}
		else if ( !affectedByFilters && collection.empty() ) {
			if ( !emptySnapshot ) persister.remove( id, session );
		}
		else if ( collection.needsRecreate(persister) ) {
			if (affectedByFilters) {
				throw new HibernateException(
					"cannot recreate collection while filter is enabled: " + 
					MessageHelper.collectionInfoString( persister, id, persister.getFactory() )
				);
			}
			if ( !emptySnapshot ) persister.remove( id, session );
			persister.recreate( collection, id, session );
		}
		else {
			persister.deleteRows( collection, id, session );
			persister.updateRows( collection, id, session );
			persister.insertRows( collection, id, session );
		}

		getSession().getPersistenceContext()
			.getCollectionEntry(collection)
			.afterAction(collection);

		evict();

		postUpdate();

		if ( getSession().getFactory().getStatistics().isStatisticsEnabled() ) {
			getSession().getFactory().getStatisticsImplementor().
					updateCollection( getPersister().getRole() );
		}
	}
	
	private void preUpdate() {
		PreCollectionUpdateEventListener[] preListeners = getSession().getListeners()
				.getPreCollectionUpdateEventListeners();
		if (preListeners.length > 0) {
			PreCollectionUpdateEvent preEvent = new PreCollectionUpdateEvent(
					getPersister(), getCollection(), ( EventSource ) getSession() );
			for ( int i = 0; i < preListeners.length; i++ ) {
				preListeners[i].onPreUpdateCollection( preEvent );
			}
		}
	}

	private void postUpdate() {
		PostCollectionUpdateEventListener[] postListeners = getSession().getListeners()
				.getPostCollectionUpdateEventListeners();
		if (postListeners.length > 0) {
			PostCollectionUpdateEvent postEvent = new PostCollectionUpdateEvent(
					getPersister(), getCollection(), ( EventSource ) getSession() );
			for ( int i = 0; i < postListeners.length; i++ ) {
				postListeners[i].onPostUpdateCollection( postEvent );
			}
		}
	}
}







