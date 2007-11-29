//$Id$
package org.hibernate.action;

import org.hibernate.HibernateException;
import org.hibernate.cache.CacheException;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;

import java.io.Serializable;

public final class CollectionRemoveAction extends CollectionAction {

	private boolean emptySnapshot;

	public CollectionRemoveAction(
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
        final boolean stats = getSession().getFactory().getStatistics().isStatisticsEnabled();
        long startTime = 0;
        if ( stats ) startTime = System.currentTimeMillis();

        if ( !emptySnapshot ) getPersister().remove( getKey(), getSession() );
		
		final PersistentCollection collection = getCollection();
		if (collection!=null) {
			getSession().getPersistenceContext()
				.getCollectionEntry(collection)
				.afterAction(collection);
		}
		
		evict();

		if ( stats ) {
			getSession().getFactory().getStatisticsImplementor()
					.removeCollection( getPersister().getRole(), System.currentTimeMillis() - startTime);
		}
	}


}







