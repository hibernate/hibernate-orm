/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.spi;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;

/**
 * Loader for loading a single entity by primary or unique key
 *
 * @author Steve Ebersole
 */
public interface SingleEntityLoader<T> extends EntityLoader {
	@Nonnull
	@Override
	EntityMappingType getLoadable();

	/**
	 * Load an entity by a primary or unique key value.
	 */
	@Nullable
	T load(@Nonnull Object key, @Nonnull LockOptions lockOptions, @Nullable Boolean readOnly, @Nonnull SharedSessionContractImplementor session);
}
