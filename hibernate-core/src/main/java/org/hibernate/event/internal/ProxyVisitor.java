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
		final EventSource session = getSession();
		if ( collection.wasInitialized() ) {
			CollectionPersister collectionPersister = session.getFactory()
			.getCollectionPersister( type.getRole() );
			session.getPersistenceContext()
				.addInitializedDetachedCollection( collectionPersister, collection );
		}
		else {
			if ( !isCollectionSnapshotValid( collection ) ) {
				throw new HibernateException( "could not reassociate uninitialized transient collection" );
			}
			CollectionPersister collectionPersister = session.getFactory()
					.getCollectionPersister( collection.getRole() );
			session.getPersistenceContext()
				.addUninitializedDetachedCollection( collectionPersister, collection );
		}
	}

}
