/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.internal;

import org.hibernate.HibernateException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.CollectionType;

/**
 * When a transient entity is passed to lock(), we must inspect all its collections and
 * <ol>
 * <li> associate any uninitialized PersistentCollections with this session
 * <li> associate any initialized PersistentCollections with this session, using the
 *      existing snapshot
 * <li> throw an exception for each "new" collection
 * </ol>
 *
 * @author Gavin King
 */
public class OnLockVisitor extends ReattachVisitor {

	public OnLockVisitor(EventSource session, Object key, Object owner) {
		super( session, key, owner );
	}

	@Override
	public Object processCollection(Object entity, Object collection, CollectionType type) throws HibernateException {
		if ( collection == null ) {
			return null;
		}

		final SessionImplementor session = getSession();
		final CollectionPersister persister =
				session.getFactory().getMappingMetamodel()
						.getCollectionDescriptor( type.getRole() );
		if ( collection instanceof PersistentCollection<?> persistentCollection ) {
			if ( persistentCollection.setCurrentSession( session ) ) {
				if ( isOwnerUnchanged( persister, extractCollectionKeyFromOwner( persister ), persistentCollection ) ) {
					// a "detached" collection that originally belonged to the same entity
					if ( persistentCollection.isDirty() ) {
						throw new HibernateException( "re-associated object has dirty collection" );
					}
					reattachCollection( persistentCollection, type );
				}
				else {
					// a "detached" collection that belonged to a different entity
					throw new HibernateException( "re-associated object has dirty collection reference" );
				}
			}
			else {
				// a collection loaded in the current session
				// can not possibly be the collection belonging
				// to the entity passed to update()
				throw new HibernateException( "re-associated object has dirty collection reference" );
			}
		}
		else {
			// brand new collection
			//TODO: or an array!! we can't lock objects with arrays now??
			throw new HibernateException( "re-associated object has dirty collection reference (or an array)" );
		}
		return null;
	}

}
