/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
 */
package org.hibernate.action.internal;

import java.io.Serializable;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.event.EventSource;
import org.hibernate.event.PostCollectionUpdateEvent;
import org.hibernate.event.PostCollectionUpdateEventListener;
import org.hibernate.event.PreCollectionUpdateEvent;
import org.hibernate.event.PreCollectionUpdateEventListener;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.pretty.MessageHelper;

public final class CollectionUpdateAction extends CollectionAction {

	private final boolean emptySnapshot;

	public CollectionUpdateAction(
				final PersistentCollection collection,
				final CollectionPersister persister,
				final Serializable id,
				final boolean emptySnapshot,
				final SessionImplementor session) {
		super( persister, collection, id, session );
		this.emptySnapshot = emptySnapshot;
	}

	@Override
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
			for ( PreCollectionUpdateEventListener preListener : preListeners ) {
				preListener.onPreUpdateCollection( preEvent );
			}
		}
	}

	private void postUpdate() {
		PostCollectionUpdateEventListener[] postListeners = getSession().getListeners()
				.getPostCollectionUpdateEventListeners();
		if (postListeners.length > 0) {
			PostCollectionUpdateEvent postEvent = new PostCollectionUpdateEvent(
					getPersister(),
					getCollection(),
					( EventSource ) getSession()
			);
			for ( PostCollectionUpdateEventListener postListener : postListeners ) {
				postListener.onPostUpdateCollection( postEvent );
			}
		}
	}
}







