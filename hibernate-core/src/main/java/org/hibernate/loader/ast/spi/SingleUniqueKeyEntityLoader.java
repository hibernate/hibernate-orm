/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.spi;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Loader subtype for loading an entity by a single unique-key value.
 *
 * @author Steve Ebersole
 */
public interface SingleUniqueKeyEntityLoader<T> extends SingleEntityLoader<T> {
	/**
	 * Load by unique key value
	 */
	@Nullable
	@Override
	T load(@Nonnull Object ukValue, @Nonnull LockOptions lockOptions, @Nullable Boolean readOnly, @Nonnull SharedSessionContractImplementor session);

	/**
	 * Resolve the matching id
	 */
	@Nullable
	Object resolveId(@Nonnull Object key, @Nonnull SharedSessionContractImplementor session);
}
