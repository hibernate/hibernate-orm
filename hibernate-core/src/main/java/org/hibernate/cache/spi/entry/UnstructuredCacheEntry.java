/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.spi.entry;

import jakarta.annotation.Nonnull;

import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * Unstructured CacheEntry format (used to store entities and collections).
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class UnstructuredCacheEntry implements CacheEntryStructure {
	/**
	 * Access to the singleton instance.
	 */
	public static final UnstructuredCacheEntry INSTANCE = new UnstructuredCacheEntry();

	@Override
	@Nonnull
	public Object structure(@Nonnull Object item) {
		return item;
	}

	@Override
	@Nonnull
	public Object destructure(@Nonnull Object structured, @Nonnull SessionFactoryImplementor factory) {
		return structured;
	}

	private UnstructuredCacheEntry() {
	}
}
