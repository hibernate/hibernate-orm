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
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.CollectionType;

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

	public OnReplicateVisitor(EventSource session, Serializable key, Object owner, boolean isUpdate) {
		super( session, key, owner );
		this.isUpdate = isUpdate;
	}

	@Override
	public Object processCollection(Object collection, CollectionType type) throws HibernateException {
		if ( collection == CollectionType.UNFETCHED_COLLECTION ) {
			return null;
		}

		final EventSource session = getSession();
		final CollectionPersister persister = session.getFactory().getMetamodel().collectionPersister( type.getRole() );

		if ( isUpdate ) {
			removeCollection( persister, extractCollectionKeyFromOwner( persister ), session );
		}
		if ( collection != null && collection instanceof PersistentCollection ) {
			final PersistentCollection wrapper = (PersistentCollection) collection;
			wrapper.setCurrentSession( (SessionImplementor) session );
			if ( wrapper.wasInitialized() ) {
				session.getPersistenceContextInternal().addNewCollection( persister, wrapper );
			}
			else {
				reattachCollection( wrapper, type );
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
