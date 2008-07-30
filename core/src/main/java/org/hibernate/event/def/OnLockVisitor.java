/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.event.def;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.event.EventSource;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.CollectionType;

/**
 * When a transient entity is passed to lock(), we must inspect all its collections and
 * 1. associate any uninitialized PersistentCollections with this session
 * 2. associate any initialized PersistentCollections with this session, using the
 * existing snapshot
 * 3. throw an exception for each "new" collection
 *
 * @author Gavin King
 */
public class OnLockVisitor extends ReattachVisitor {

	public OnLockVisitor(EventSource session, Serializable key, Object owner) {
		super( session, key, owner );
	}

	Object processCollection(Object collection, CollectionType type) throws HibernateException {

		SessionImplementor session = getSession();
		CollectionPersister persister = session.getFactory().getCollectionPersister( type.getRole() );

		if ( collection == null ) {
			//do nothing
		}
		else if ( collection instanceof PersistentCollection ) {
			PersistentCollection persistentCollection = ( PersistentCollection ) collection;
			if ( persistentCollection.setCurrentSession( session ) ) {
				if ( isOwnerUnchanged( persistentCollection, persister, extractCollectionKeyFromOwner( persister ) ) ) {
					// a "detached" collection that originally belonged to the same entity
					if ( persistentCollection.isDirty() ) {
						throw new HibernateException( "reassociated object has dirty collection" );
					}
					reattachCollection( persistentCollection, type );
				}
				else {
					// a "detached" collection that belonged to a different entity
					throw new HibernateException( "reassociated object has dirty collection reference" );
				}
			}
			else {
				// a collection loaded in the current session
				// can not possibly be the collection belonging
				// to the entity passed to update()
				throw new HibernateException( "reassociated object has dirty collection reference" );
			}
		}
		else {
			// brand new collection
			//TODO: or an array!! we can't lock objects with arrays now??
			throw new HibernateException( "reassociated object has dirty collection reference (or an array)" );
		}

		return null;

	}

}
