/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import org.hibernate.HibernateException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.type.CollectionType;

/**
 * Do we have a dirty collection here?
 * 1. if it is a new application-instantiated collection, return true (does not occur anymore!)
 * 2. if it is a component, recurse
 * 3. if it is a wrappered collection, ask the collection entry
 *
 * @author Gavin King
 */
public class DirtyCollectionSearchVisitor extends AbstractVisitor {

	private boolean dirty;
	private boolean[] propertyVersionability;

	DirtyCollectionSearchVisitor(EventSource session, boolean[] propertyVersionability) {
		super( session );
		this.propertyVersionability = propertyVersionability;
	}

	boolean wasDirtyCollectionFound() {
		return dirty;
	}

	Object processCollection(Object collection, CollectionType type) throws HibernateException {
		if ( collection != null ) {
			final SessionImplementor session = getSession();
			final PersistentCollection persistentCollection;
			if ( type.isArrayType() ) {
				persistentCollection = session.getPersistenceContext().getCollectionHolder( collection );
				// if no array holder we found an unwrappered array (this can't occur,
				// because we now always call wrap() before getting to here)
				// return (ah==null) ? true : searchForDirtyCollections(ah, type);
			}
			else {
				// if not wrappered yet, its dirty (this can't occur, because
				// we now always call wrap() before getting to here)
				// return ( ! (obj instanceof PersistentCollection) ) ?
				//true : searchForDirtyCollections( (PersistentCollection) obj, type );
				persistentCollection = (PersistentCollection) collection;
			}

			if ( persistentCollection.isDirty() ) { //we need to check even if it was not initialized, because of delayed adds!
				dirty = true;
				return null; //NOTE: EARLY EXIT!
			}
		}

		return null;
	}

	boolean includeEntityProperty(Object[] values, int i) {
		return propertyVersionability[i] && super.includeEntityProperty( values, i );
	}
}
