/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.internal;

import org.hibernate.HibernateException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.event.spi.EventSource;
import org.hibernate.type.CollectionType;

/**
 * When an entity is passed to replicate(), and there is an existing row, we must
 * inspect all its collections and
 * <ol>
 * <li> associate any uninitialized PersistentCollections with this session
 * <li> associate any initialized PersistentCollections with this session, using the
 *      existing snapshot
 * <li> execute a collection removal (SQL DELETE) for each null collection property
 *      or "new" collection
 *</ol>
 * @author Gavin King
 */
public class OnReplicateVisitor extends ReattachVisitor {

	private final boolean isUpdate;

	public OnReplicateVisitor(EventSource session, Object key, Object owner, boolean isUpdate) {
		super( session, key, owner );
		this.isUpdate = isUpdate;
	}

	@Override
	public Object processCollection(Object collection, CollectionType type) throws HibernateException {
		if ( collection == CollectionType.UNFETCHED_COLLECTION ) {
			return null;
		}

		final var session = getSession();
		final var persister =
				session.getFactory().getMappingMetamodel()
						.getCollectionDescriptor( type.getRole() );
		if ( isUpdate ) {
			removeCollection( persister, extractCollectionKeyFromOwner( persister ), session );
		}
		if ( collection instanceof PersistentCollection<?> persistentCollection ) {
			persistentCollection.setCurrentSession( session );
			if ( persistentCollection.wasInitialized() ) {
				session.getPersistenceContextInternal().addNewCollection( persister, persistentCollection );
			}
			else {
				reattachCollection( persistentCollection, type );
			}
		}
//		else {
			// otherwise a null or brand-new collection
			// this will also (inefficiently) handle arrays, which
			// have no snapshot, so we can't do any better
			//processArrayOrNewCollection(collection, type);
//		}
		return null;
	}

}
