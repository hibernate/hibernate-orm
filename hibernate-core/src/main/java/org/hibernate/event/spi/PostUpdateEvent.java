/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Occurs after the datastore is updated
 *
 * @author Gavin King
 */
public class PostUpdateEvent extends AbstractPostDatabaseOperationEvent {
	private final Object[] state;
	private final Object[] oldState;
	// list of dirty properties as computed during a FlushEntityEvent
	private final int[] dirtyProperties;

	public PostUpdateEvent(
			@Nonnull Object entity,
			@Nonnull Object id,
			@Nonnull Object[] state,
			@Nullable Object[] oldState,
			@Nullable int[] dirtyProperties,
			@Nonnull EntityPersister persister,
			@Nonnull SharedSessionContractImplementor source) {
		super( source, entity, id, persister );
		this.state = state;
		this.oldState = oldState;
		this.dirtyProperties = dirtyProperties;
	}

	@Nullable
	public Object[] getOldState() {
		return oldState;
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
