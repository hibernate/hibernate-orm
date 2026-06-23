/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.spi;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Specialized {@link Region} whose data is accessed directly,
 * without the need for key/item wrapping.
 *
 * Does not define a "remove" operation because Hibernate's
 * query and timestamps caches only ever "get" and "put".
 *
 * @author Steve Ebersole
 */
public interface DirectAccessRegion extends Region {
	/**
	 * Get value by key
	 */
	@Nullable
	Object getFromCache(@Nonnull Object key, @Nonnull SharedSessionContractImplementor session);

	/**
	 * Put a value by key
	 */
	void putIntoCache(
			@Nonnull Object key,
			@Nonnull Object value,
			@Nonnull SharedSessionContractImplementor session);
}
