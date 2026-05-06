/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.Type;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A {@link CollectionKey} for a temporal (historical) snapshot of a collection,
 * loaded from an audit table at a specific transaction identifier.
 * <p>
 * The transaction identifier is included in {@code equals()}/{@code hashCode()}
 * so that the persistence context naturally isolates collections at different
 * points in time.
 *
 * @author Marco Belladelli
 * @see CollectionKey
 * @since 7.4
 */
public final class TemporalCollectionKey extends CollectionKey {
	private final Object txId;

	/**
	 * Construct a unique identifier for a temporal snapshot of a collection.
	 *
	 * @param persister The collection persister
	 * @param key The collection key (owner FK)
	 * @param txId The audit transaction identifier (must not be null)
	 */
	public TemporalCollectionKey(CollectionPersister persister, Object key, Object txId) {
		super(
				persister.getRole(),
				key,
				persister.getKeyType().getTypeForEqualsHashCode(),
				persister.getFactory(),
				txId.hashCode()
		);
		this.txId = txId;
	}

	TemporalCollectionKey(
			String role,
			@Nullable Object key,
			@Nullable Type keyType,
			SessionFactoryImplementor factory,
			Object txId) {
		super( role, key, keyType, factory, txId.hashCode() );
		this.txId = txId;
	}

	@Override
	public Object getChangesetId() {
		return txId;
	}

	@Override
	public boolean isTemporal() {
		return true;
	}

	@Override
	public String toString() {
		return super.toString() + "@tx" + txId;
	}
}
