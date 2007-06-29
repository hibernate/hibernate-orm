//$Id: FlushVisitor.java 6929 2005-05-27 03:54:08Z oneovthafew $
package org.hibernate.event.def;

import org.hibernate.HibernateException;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.engine.Collections;
import org.hibernate.event.EventSource;
import org.hibernate.type.CollectionType;

/**
 * Process collections reachable from an entity. This
 * visitor assumes that wrap was already performed for
 * the entity.
 *
 * @author Gavin King
 */
public class FlushVisitor extends AbstractVisitor {
	
	private Object owner;

	Object processCollection(Object collection, CollectionType type)
	throws HibernateException {
		
		if (collection==CollectionType.UNFETCHED_COLLECTION) {
			return null;
		}

		if (collection!=null) {
			final PersistentCollection coll;
			if ( type.hasHolder( getSession().getEntityMode() ) ) {
				coll = getSession().getPersistenceContext().getCollectionHolder(collection);
			}
			else {
				coll = (PersistentCollection) collection;
			}

			Collections.processReachableCollection( coll, type, owner, getSession() );
		}

		return null;

	}

	FlushVisitor(EventSource session, Object owner) {
		super(session);
		this.owner = owner;
	}

}
