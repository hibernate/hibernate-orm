//$Id: CollectionRecreateAction.java 7147 2005-06-15 13:20:13Z oneovthafew $
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
		final PersistentCollection collection = getCollection();
		
		getPersister().recreate( collection, getKey(), getSession() );
		
		getSession().getPersistenceContext()
				.getCollectionEntry(collection)
				.afterAction(collection);
		
		evict();

		if ( getSession().getFactory().getStatistics().isStatisticsEnabled() ) {
			getSession().getFactory().getStatisticsImplementor()
					.recreateCollection( getPersister().getRole() );
		}
	}

}







