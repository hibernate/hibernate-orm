/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

import org.hibernate.persister.entity.EntityPersister;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An {@link EntityKey} for a temporal (historical) snapshot of an entity,
 * loaded from an audit table at a specific changeset identifier.
 * <p>
 * The changeset identifier is included in {@code equals()}/{@code hashCode()}
 * so that the persistence context naturally isolates entities at different
 * points in time. Entities with a temporal key are always read-only.
 *
 * @author Marco Belladelli
 * @see EntityKey
 * @since 7.4
 */
public final class TemporalEntityKey extends EntityKey {
	private final Object changesetId;

	/**
	 * Construct a unique identifier for a temporal snapshot of an entity.
	 *
	 * @param id The entity id
	 * @param persister The entity persister
	 * @param changesetId The changeset identifier (must not be null)
	 */
	public TemporalEntityKey(@Nullable Object id, EntityPersister persister, Object changesetId) {
		super( id, persister, changesetId.hashCode() );
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
