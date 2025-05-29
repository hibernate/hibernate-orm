/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.internal;

import org.hibernate.HibernateException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.CollectionType;

/**
 * When an entity is passed to update(), we must inspect all its collections and
 * 1. associate any uninitialized PersistentCollections with this session
 * 2. associate any initialized PersistentCollections with this session, using the
 *	existing snapshot
 * 3. execute a collection removal (SQL DELETE) for each null collection property
 *	or "new" collection
 *
 * @author Gavin King
 */
public class OnUpdateVisitor extends ReattachVisitor {

	public OnUpdateVisitor(EventSource session, Object key, Object owner) {
		super( session, key, owner );
	}

	@Override
	Object processCollection(Object entity, Object collection, CollectionType type) throws HibernateException {
		if ( collection == CollectionType.UNFETCHED_COLLECTION ) {
			return null;
		}

		final EventSource session = getSession();
		final CollectionPersister persister =
				session.getFactory().getMappingMetamodel()
						.getCollectionDescriptor( type.getRole() );
		final Object collectionKey = extractCollectionKeyFromOwner( persister );
		if ( collection instanceof PersistentCollection<?> persistentCollection ) {
			if ( persistentCollection.setCurrentSession( session ) ) {
				//a "detached" collection!
				if ( !isOwnerUnchanged( persister, collectionKey, persistentCollection ) ) {
					// if the collection belonged to a different entity,
					// clean up the existing state of the collection
					removeCollection( persister, collectionKey, session );
				}
				reattachCollection( persistentCollection, type );
			}
			else {
				// a collection loaded in the current session
				// can not possibly be the collection belonging
				// to the entity passed to update()
				removeCollection( persister, collectionKey, session );
			}
		}
		else {
			// null or brand-new collection
			// this will also (inefficiently) handle arrays, which have
			// no snapshot, so we can't do any better
			removeCollection( persister, collectionKey, session );
		}

		return null;
	}

}
