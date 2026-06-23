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
import jakarta.annotation.Nonnull;

/**
 * A visitor able to reattach {@linkplain PersistentCollection collections}
 * to the current session.
 *
 * @author Gavin King
 */
public abstract class ProxyVisitor extends AbstractVisitor {

	public ProxyVisitor(@Nonnull EventSource session) {
		super(session);
	}

	/**
	 * Has the owner of the collection changed since the collection
	 * was snapshotted and detached?
	 */
	protected static boolean isOwnerUnchanged(
			@Nonnull CollectionPersister persister,
			@Nonnull Object id,
			@Nonnull PersistentCollection<?> snapshot) {
		return isCollectionSnapshotValid( snapshot )
			&& persister.getRole().equals( snapshot.getRole() )
			&& persister.getKeyType().isEqual( id, snapshot.getKey() );
	}

	private static boolean isCollectionSnapshotValid(@Nonnull PersistentCollection<?> snapshot) {
		assert snapshot != null;
		return snapshot.getRole() != null && snapshot.getKey() != null;
	}

	/**
	 * Reattach a detached (disassociated) initialized or uninitialized
	 * collection wrapper, using a snapshot carried with the collection
	 * wrapper
	 */
	protected void reattachCollection(@Nonnull PersistentCollection<?> collection, @Nonnull CollectionType type)
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
