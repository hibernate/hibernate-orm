/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.spi;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Loader for loading an entity by a single identifier value.
 *
 * @author Steve Ebersole
 */
public interface SingleIdEntityLoader<T> extends SingleEntityLoader<T> {
	/**
	 * Load by primary key value
	 */
	@Override
	T load(Object pkValue, LockOptions lockOptions, Boolean readOnly, SharedSessionContractImplementor session);


	T load(Object pkValue, Object entityInstance, LockOptions lockOptions, Boolean readOnly, SharedSessionContractImplementor session);

	/**
	 * Load by primary key value, populating the passed entity instance.  Used to initialize an uninitialized
	 * bytecode-proxy or {@link org.hibernate.event.spi.LoadEvent} handling.
	 * The passed instance is the enhanced proxy or the entity to be loaded.
	 */
	default T load(Object pkValue, Object entityInstance, LockOptions lockOptions, SharedSessionContractImplementor session) {
		return load( pkValue, entityInstance, lockOptions, null, session );
	}

	/**
	 * Load database snapshot by primary key value
	 */
	Object[] loadDatabaseSnapshot(Object id, SharedSessionContractImplementor session);
}
