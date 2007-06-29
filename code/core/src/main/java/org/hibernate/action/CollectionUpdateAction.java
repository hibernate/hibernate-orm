//$Id: CollectionUpdateAction.java 7631 2005-07-24 21:26:21Z oneovthafew $
package org.hibernate.action;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
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

		if ( getSession().getFactory().getStatistics().isStatisticsEnabled() ) {
			getSession().getFactory().getStatisticsImplementor().
					updateCollection( getPersister().getRole() );
		}
	}

}







