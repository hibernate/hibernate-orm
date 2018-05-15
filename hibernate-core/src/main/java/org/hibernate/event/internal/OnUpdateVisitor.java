/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.event.spi.EventSource;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;

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

	OnUpdateVisitor(EventSource session, Object key, Object owner) {
		super( session, key, owner );
	}

	@Override
	Object processCollection(Object collection, PluralPersistentAttribute collectionAttribute) throws HibernateException {

		if ( collection == PersistentCollectionDescriptor.UNFETCHED_COLLECTION ) {
			return null;
		}

		final EventSource session = getSession();
		final PersistentCollectionDescriptor descriptor = session.getFactory()
				.getMetamodel()
				.findCollectionDescriptor( collectionAttribute.getNavigableName() );

		final Serializable collectionKey = extractCollectionKeyFromOwner( descriptor );
		if ( collection != null && ( collection instanceof PersistentCollection ) ) {
			PersistentCollection wrapper = (PersistentCollection) collection;
			if ( wrapper.setCurrentSession(session) ) {
				//a "detached" collection!
				if ( !isOwnerUnchanged( wrapper, descriptor, collectionKey ) ) {
					// if the collection belonged to a different entity,
					// clean up the existing state of the collection
					removeCollection( descriptor, collectionKey, session );
				}
				reattachCollection(wrapper, descriptor.getNavigableRole());
			}
			else {
				// a collection loaded in the current session
				// can not possibly be the collection belonging
				// to the entity passed to update()
				removeCollection(descriptor, collectionKey, session);
			}
		}
		else {
			// null or brand new collection
			// this will also (inefficiently) handle arrays, which have
			// no snapshot, so we can't do any better
			removeCollection(descriptor, collectionKey, session);
		}

		return null;
	}

}
