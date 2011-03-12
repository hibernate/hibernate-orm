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
import org.hibernate.event.EventSource;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.CollectionType;
import org.hibernate.type.EntityType;

/**
 * Reassociates uninitialized proxies with the session
 * @author Gavin King
 */
public abstract class ProxyVisitor extends AbstractVisitor {


	public ProxyVisitor(EventSource session) {
		super(session);
	}

	Object processEntity(Object value, EntityType entityType) throws HibernateException {

		if (value!=null) {
			getSession().getPersistenceContext().reassociateIfUninitializedProxy(value);
			// if it is an initialized proxy, let cascade
			// handle it later on
		}

		return null;
	}

	/**
	 * Has the owner of the collection changed since the collection
	 * was snapshotted and detached?
	 */
	protected static boolean isOwnerUnchanged(
			final PersistentCollection snapshot, 
			final CollectionPersister persister, 
			final Serializable id
	) {
		return isCollectionSnapshotValid(snapshot) &&
				persister.getRole().equals( snapshot.getRole() ) &&
				id.equals( snapshot.getKey() );
	}

	private static boolean isCollectionSnapshotValid(PersistentCollection snapshot) {
		return snapshot != null &&
				snapshot.getRole() != null &&
				snapshot.getKey() != null;
	}
	
	/**
	 * Reattach a detached (disassociated) initialized or uninitialized
	 * collection wrapper, using a snapshot carried with the collection
	 * wrapper
	 */
	protected void reattachCollection(PersistentCollection collection, CollectionType type)
	throws HibernateException {
		if ( collection.wasInitialized() ) {
			CollectionPersister collectionPersister = getSession().getFactory()
			.getCollectionPersister( type.getRole() );
			getSession().getPersistenceContext()
				.addInitializedDetachedCollection( collectionPersister, collection );
		}
		else {
			if ( !isCollectionSnapshotValid(collection) ) {
				throw new HibernateException( "could not reassociate uninitialized transient collection" );
			}
			CollectionPersister collectionPersister = getSession().getFactory()
					.getCollectionPersister( collection.getRole() );
			getSession().getPersistenceContext()
				.addUninitializedDetachedCollection( collectionPersister, collection );
		}
	}

}
