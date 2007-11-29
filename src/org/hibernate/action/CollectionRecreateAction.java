//$Id$
package org.hibernate.action;

import org.hibernate.HibernateException;
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
        final boolean stats = getSession().getFactory().getStatistics().isStatisticsEnabled();
        long startTime = 0;
        if ( stats ) startTime = System.currentTimeMillis();

        final PersistentCollection collection = getCollection();
		
		getPersister().recreate( collection, getKey(), getSession() );
		
		getSession().getPersistenceContext()
				.getCollectionEntry(collection)
				.afterAction(collection);
		
		evict();

		if ( stats ) {
			getSession().getFactory().getStatisticsImplementor()
					.recreateCollection( getPersister().getRole(), System.currentTimeMillis() - startTime);
		}
	}

}







