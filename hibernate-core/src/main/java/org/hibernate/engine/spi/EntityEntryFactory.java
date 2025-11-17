/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

import java.io.Serializable;

import org.hibernate.LockMode;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Contract to build {@link EntityEntry}
 *
 * @author Emmanuel Bernard
 *
 * @deprecated No longer used
 */
@Deprecated(since = "7", forRemoval = true)
public interface EntityEntryFactory extends Serializable {

	/**
	 * Creates {@link EntityEntry}.
	 */
	EntityEntry createEntityEntry(
			final Status status,
			final Object[] loadedState,
			final Object rowId,
			final Object id,
			final Object version,
			final LockMode lockMode,
			final boolean existsInDatabase,
			final EntityPersister persister,
			final boolean disableVersionIncrement,
			final PersistenceContext persistenceContext);
}
