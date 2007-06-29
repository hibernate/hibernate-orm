//$Id: EvictVisitor.java 6929 2005-05-27 03:54:08Z oneovthafew $
package org.hibernate.event.def;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.engine.CollectionEntry;
import org.hibernate.engine.CollectionKey;
import org.hibernate.event.EventSource;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.type.CollectionType;

/**
 * Evict any collections referenced by the object from the session cache.
 * This will NOT pick up any collections that were dereferenced, so they
 * will be deleted (suboptimal but not exactly incorrect).
 *
 * @author Gavin King
 */
public class EvictVisitor extends AbstractVisitor {
	
	private static final Log log = LogFactory.getLog(EvictVisitor.class);

	EvictVisitor(EventSource session) {
		super(session);
	}

	Object processCollection(Object collection, CollectionType type)
		throws HibernateException {

		if (collection!=null) evictCollection(collection, type);

		return null;
	}
	public void evictCollection(Object value, CollectionType type) {

		final Object pc;
		if ( type.hasHolder( getSession().getEntityMode() ) ) {
			pc = getSession().getPersistenceContext().removeCollectionHolder(value);
		}
		else if ( value instanceof PersistentCollection ) {
			pc = value;
		}
		else {
			return; //EARLY EXIT!
		}

		PersistentCollection collection = (PersistentCollection) pc;
		if ( collection.unsetSession( getSession() ) ) evictCollection(collection);
	}

	private void evictCollection(PersistentCollection collection) {
		CollectionEntry ce = (CollectionEntry) getSession().getPersistenceContext().getCollectionEntries().remove(collection);
		if ( log.isDebugEnabled() )
			log.debug(
					"evicting collection: " +
					MessageHelper.collectionInfoString( ce.getLoadedPersister(), ce.getLoadedKey(), getSession().getFactory() )
			);
		if ( ce.getLoadedPersister() != null && ce.getLoadedKey() != null ) {
			//TODO: is this 100% correct?
			getSession().getPersistenceContext().getCollectionsByKey().remove( 
					new CollectionKey( ce.getLoadedPersister(), ce.getLoadedKey(), getSession().getEntityMode() ) 
			);
		}
	}

}
