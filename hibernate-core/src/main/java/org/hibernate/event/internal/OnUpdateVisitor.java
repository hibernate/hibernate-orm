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

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.CollectionType;

/**
 * When an entity is passed to update(), we must inspect all its collections and
 * 1. associate any uninitialized PersistentCollections with this session
 * 2. associate any initialized PersistentCollections with this session, using the
 *    existing snapshot
 * 3. execute a collection removal (SQL DELETE) for each null collection property
 *    or "new" collection
 *
 * @author Gavin King
 */
public class OnUpdateVisitor extends ReattachVisitor {

	OnUpdateVisitor(EventSource session, Serializable key, Object owner) {
		super( session, key, owner );
	}

	@Override
	Object processCollection(Object collection, CollectionType type) throws HibernateException {

		if ( collection == CollectionType.UNFETCHED_COLLECTION ) {
			return null;
		}

		EventSource session = getSession();
		CollectionPersister persister = session.getFactory().getCollectionPersister( type.getRole() );

		final Serializable collectionKey = extractCollectionKeyFromOwner( persister );
		if ( collection!=null && (collection instanceof PersistentCollection) ) {
			PersistentCollection wrapper = (PersistentCollection) collection;
			if ( wrapper.setCurrentSession(session) ) {
				//a "detached" collection!
				if ( !isOwnerUnchanged( wrapper, persister, collectionKey ) ) {
					// if the collection belonged to a different entity,
					// clean up the existing state of the collection
					removeCollection( persister, collectionKey, session );
				}
				reattachCollection(wrapper, type);
			}
			else {
				// a collection loaded in the current session
				// can not possibly be the collection belonging
				// to the entity passed to update()
				removeCollection(persister, collectionKey, session);
			}
		}
		else {
			// null or brand new collection
			// this will also (inefficiently) handle arrays, which have
			// no snapshot, so we can't do any better
			removeCollection(persister, collectionKey, session);
		}

		return null;
	}

}
