/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.internal;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.event.spi.EventSource;
import org.hibernate.type.CollectionType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * When a detached entity is passed to {@link org.hibernate.Session#remove(Object)},
 * we must inspect all its collections and:
 * <ol>
 * <li>associate any uninitialized PersistentCollections with this session
 * <li>associate any initialized PersistentCollections with this session, using the
 *     existing snapshot
 * <li>execute a collection removal (SQL DELETE) for each null collection property
 *     or "new" collection
 *</ol>
 *
 * @author Gavin King
 */
public class OnUpdateVisitor extends ReattachVisitor {

	public OnUpdateVisitor(@Nonnull EventSource session, @Nonnull Object key, @Nonnull Object owner) {
		super( session, key, owner );
	}

	@Override
	@Nullable
	Object processCollection(@Nullable Object collection, @Nonnull CollectionType type) {
		final var session = getSession();
		final var persister =
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
