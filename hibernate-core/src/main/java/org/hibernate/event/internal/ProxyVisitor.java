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
import org.hibernate.type.EntityType;

/**
 * Reassociates uninitialized proxies with the session
 * @author Gavin King
 */
public abstract class ProxyVisitor extends AbstractVisitor {

	public ProxyVisitor(EventSource session) {
		super(session);
	}

	Object processEntity(Object value, EntityType entityType) {
		if ( value != null ) {
			getSession().getPersistenceContext().reassociateIfUninitializedProxy( value );
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
			CollectionPersister persister, Object id, PersistentCollection<?> snapshot) {
		return isCollectionSnapshotValid( snapshot )
			&& persister.getRole().equals( snapshot.getRole() )
			&& persister.getKeyType().isEqual( id, snapshot.getKey() );
	}

	private static boolean isCollectionSnapshotValid(PersistentCollection<?> snapshot) {
		return snapshot != null
			&& snapshot.getRole() != null
			&& snapshot.getKey() != null;
	}

	/**
	 * Reattach a detached (disassociated) initialized or uninitialized
	 * collection wrapper, using a snapshot carried with the collection
	 * wrapper
	 */
	protected void reattachCollection(PersistentCollection<?> collection, CollectionType type)
			throws HibernateException {
		final var session = getSession();
		final var metamodel = session.getFactory().getMappingMetamodel();
		final var context = session.getPersistenceContext();
		if ( collection.wasInitialized() ) {
			final var persister = metamodel.getCollectionDescriptor( type.getRole() );
			context.addInitializedDetachedCollection( persister, collection );
		}
		else {
			if ( !isCollectionSnapshotValid( collection ) ) {
				throw new HibernateException( "could not re-associate uninitialized transient collection" );
			}
			final var persister = metamodel.getCollectionDescriptor( collection.getRole() );
			context.addUninitializedDetachedCollection( persister, collection );
		}
	}

}
