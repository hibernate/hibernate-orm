//$Id: CollectionRecreateAction.java 7147 2005-06-15 13:20:13Z oneovthafew $
package org.hibernate.action;

import org.hibernate.HibernateException;
import org.hibernate.event.PostCollectionRecreateEventListener;
import org.hibernate.event.PostCollectionRecreateEvent;
import org.hibernate.event.EventSource;
import org.hibernate.event.PreCollectionRecreateEvent;
import org.hibernate.event.PreCollectionRecreateEventListener;
import org.hibernate.cache.CacheException;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;

import java.io.Serializable;

public final class CollectionRecreateAction extends CollectionAction {

	public CollectionRecreateAction(
				final PersistentCollection collection, 
				final CollectionPersister persister, 
				final Serializable id, 
				final SessionImplementor session)
			throws CacheException {
		super( persister, collection, id, session );
	}

	public void execute() throws HibernateException {
		// this method is called when a new non-null collection is persisted
		// or when an existing (non-null) collection is moved to a new owner
		final PersistentCollection collection = getCollection();
		
		preRecreate();

		getPersister().recreate( collection, getKey(), getSession() );
		
		getSession().getPersistenceContext()
				.getCollectionEntry(collection)
				.afterAction(collection);
		
		evict();

		postRecreate();

		if ( getSession().getFactory().getStatistics().isStatisticsEnabled() ) {
			getSession().getFactory().getStatisticsImplementor()
					.recreateCollection( getPersister().getRole() );
		}
	}

	private void preRecreate() {
		PreCollectionRecreateEventListener[] preListeners = getSession().getListeners()
				.getPreCollectionRecreateEventListeners();
		if (preListeners.length > 0) {
			PreCollectionRecreateEvent preEvent = new PreCollectionRecreateEvent(
					getCollection(), ( EventSource ) getSession() );
			for ( int i = 0; i < preListeners.length; i++ ) {
				preListeners[i].onPreRecreateCollection( preEvent );
			}
		}
	}

	private void postRecreate() {
		PostCollectionRecreateEventListener[] postListeners = getSession().getListeners()
				.getPostCollectionRecreateEventListeners();
		if (postListeners.length > 0) {
			PostCollectionRecreateEvent postEvent = new PostCollectionRecreateEvent(
					getCollection(), ( EventSource ) getSession() );
			for ( int i = 0; i < postListeners.length; i++ ) {
				postListeners[i].onPostRecreateCollection( postEvent );
			}
		}
	}
}







