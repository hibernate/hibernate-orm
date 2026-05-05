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
 * loaded from an audit table at a specific changeset identifier.
 * <p>
 * The changeset identifier is included in {@code equals()}/{@code hashCode()}
 * so that the persistence context naturally isolates collections at different
 * points in time.
 *
 * @author Marco Belladelli
 * @see CollectionKey
 * @since 7.4
 */
public final class TemporalCollectionKey extends CollectionKey {
	private final Object changesetId;

	/**
	 * Construct a unique identifier for a temporal snapshot of a collection.
	 *
	 * @param persister The collection persister
	 * @param key The collection key (owner FK)
	 * @param changesetId The changeset identifier (must not be null)
	 */
	public TemporalCollectionKey(CollectionPersister persister, Object key, Object changesetId) {
		super(
				persister.getRole(),
				key,
				persister.getKeyType().getTypeForEqualsHashCode(),
				persister.getFactory(),
				changesetId.hashCode()
		);
		this.changesetId = changesetId;
	}

	TemporalCollectionKey(
			String role,
			@Nullable Object key,
			@Nullable Type keyType,
			SessionFactoryImplementor factory,
			Object changesetId) {
		super( role, key, keyType, factory, changesetId.hashCode() );
		this.changesetId = changesetId;
	}

	@Override
	public Object getChangesetId() {
		return changesetId;
	}

	@Override
	public boolean isTemporal() {
		return true;
	}

	@Override
	public String toString() {
		return super.toString() + "@" + changesetId;
	}
}
