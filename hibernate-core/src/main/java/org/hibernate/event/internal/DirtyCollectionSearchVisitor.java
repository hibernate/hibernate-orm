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

	private boolean dirty = false;
	private boolean[] propertyVersionability;

	DirtyCollectionSearchVisitor(EventSource session, boolean[] propertyVersionability) {
		super(session);
		this.propertyVersionability = propertyVersionability;
	}

	boolean wasDirtyCollectionFound() {
		return dirty;
	}

	Object processCollection(Object collection, CollectionType type)
		throws HibernateException {

		if (collection!=null) {

			SessionImplementor session = getSession();

			final PersistentCollection persistentCollection;
			if ( type.isArrayType() ) {
				 persistentCollection = session.getPersistenceContext().getCollectionHolder(collection);
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
				dirty=true;
				return null; //NOTE: EARLY EXIT!
			}
		}

		return null;
	}

	boolean includeEntityProperty(Object[] values, int i) {
		return propertyVersionability[i] && super.includeEntityProperty(values, i);
	}
}
