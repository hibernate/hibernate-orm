/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.spi.support;

import jakarta.annotation.Nonnull;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Specialization of {@link StorageAccess} for domain data regions.
 *
 * @author Steve Ebersole
 */
public interface DomainDataStorageAccess extends StorageAccess {
	/**
	 * Specialized form of putting something into the cache
	 * in cases where the put is coming from a load (read) from
	 * the database
	 *
	 * @implNote the method default is to call {@link #putIntoCache}
	 */
	default void putFromLoad(
			@Nonnull Object key,
			@Nonnull Object value,
			@Nonnull SharedSessionContractImplementor session) {
		putIntoCache( key, value, session );
	}
}
