/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import org.hibernate.HibernateException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.event.spi.EventSource;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;

/**
 * When an entity is passed to replicate(), and there is an existing row, we must
 * inspect all its collections and
 * 1. associate any uninitialized PersistentCollections with this session
 * 2. associate any initialized PersistentCollections with this session, using the
 * existing snapshot
 * 3. execute a collection removal (SQL DELETE) for each null collection property
 * or "new" collection
 *
 * @author Gavin King
 */
public class OnReplicateVisitor extends ReattachVisitor {

	private boolean isUpdate;

	OnReplicateVisitor(EventSource session, Object key, Object owner, boolean isUpdate) {
		super( session, key, owner );
		this.isUpdate = isUpdate;
	}

	@Override
	public Object processCollection(Object collection, PluralPersistentAttribute collectionAttribute) throws HibernateException {
		if ( collection == PersistentCollectionDescriptor.UNFETCHED_COLLECTION ) {
			return null;
		}

		final EventSource session = getSession();

		final PersistentCollectionDescriptor descriptor = session.getFactory()
				.getMetamodel()
				.findCollectionDescriptor( collectionAttribute.getNavigableName() );

		if ( isUpdate ) {
			removeCollection( descriptor, extractCollectionKeyFromOwner( descriptor ), session );
		}
		if ( collection != null && collection instanceof PersistentCollection ) {
			final PersistentCollection wrapper = (PersistentCollection) collection;
			wrapper.setCurrentSession( session );
			if ( wrapper.wasInitialized() ) {
				session.getPersistenceContext().addNewCollection( descriptor, wrapper );
			}
			else {
				reattachCollection( wrapper, descriptor.getNavigableRole() );
			}
		}
		else {
			// otherwise a null or brand new collection
			// this will also (inefficiently) handle arrays, which
			// have no snapshot, so we can't do any better
			//processArrayOrNewCollection(collection, type);
		}

		return null;

	}

}
