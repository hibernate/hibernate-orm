/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import jakarta.annotation.Nullable;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;
import jakarta.annotation.Nonnull;

/**
 * Occurs after the datastore is updated via a SQL {@code merge}
 *
 * @author Gavin King
 */
public class PostUpsertEvent extends AbstractPostDatabaseOperationEvent {
	private final Object[] state;
	//list of dirty properties as computed by Hibernate during a FlushEntityEvent
	private final int[] dirtyProperties;

	public PostUpsertEvent(
			@Nonnull Object entity,
			@Nonnull Object id,
			@Nonnull Object[] state,
			@Nullable int[] dirtyProperties,
			@Nonnull EntityPersister persister,
			@Nonnull SharedSessionContractImplementor source) {
		super( source, entity, id, persister );
		this.state = state;
		this.dirtyProperties = dirtyProperties;
	}

	@Nonnull
	public Object[] getState() {
		return state;
	}

	@Nullable
	public int[] getDirtyProperties() {
		return dirtyProperties;
	}
}
