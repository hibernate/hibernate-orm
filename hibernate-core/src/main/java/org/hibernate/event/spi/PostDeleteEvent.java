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
 * Occurs after deleting an item from the datastore
 *
 * @author Gavin King
 */
public class PostDeleteEvent extends AbstractPostDatabaseOperationEvent {
	private final Object[] deletedState;

	public PostDeleteEvent(
			@Nonnull Object entity,
			@Nonnull Object id,
			@Nullable Object[] deletedState,
			@Nonnull EntityPersister persister,
			@Nonnull SharedSessionContractImplementor source) {
		super( source, entity, id, persister );
		this.deletedState = deletedState;
	}

	@Nullable
	public Object[] getDeletedState() {
		return deletedState;
	}
}
