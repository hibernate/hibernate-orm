/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.internal;

import org.hibernate.LockMode;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityEntryFactory;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.Status;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Factory for the safe approach implementation of {@link EntityEntry}.
 * <p>
 * Smarter implementations could store less state.
 *
 * @author Emmanuel Bernard
 */
@Deprecated(since = "7", forRemoval = true)
public final class MutableEntityEntryFactory implements EntityEntryFactory {
	/**
	 * Singleton access
	 */
	public static final MutableEntityEntryFactory INSTANCE = new MutableEntityEntryFactory();

	private MutableEntityEntryFactory() {
	}

	@Override
	public EntityEntry createEntityEntry(
			Status status,
			Object[] loadedState,
			Object rowId,
			Object id,
			Object version,
			LockMode lockMode,
			boolean existsInDatabase,
			EntityPersister persister,
			boolean disableVersionIncrement,
			PersistenceContext persistenceContext) {
		return new MutableEntityEntry(
				status,
				loadedState,
				rowId,
				id,
				version,
				lockMode,
				existsInDatabase,
				persister,
				disableVersionIncrement,
				persistenceContext
		);
	}
}
