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

/**
 * A visitor able to reattach {@linkplain PersistentCollection collections}
 * to the current session.
 *
 * @author Gavin King
 */
public abstract class ProxyVisitor extends AbstractVisitor {

	public ProxyVisitor(EventSource session) {
		super(session);
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
				throw new HibernateException( "Could not reassociate uninitialized transient collection" );
			}
			final var persister = metamodel.getCollectionDescriptor( collection.getRole() );
			context.addUninitializedDetachedCollection( persister, collection );
		}
	}

}
