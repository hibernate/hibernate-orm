/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.spi.entry;

import jakarta.annotation.Nonnull;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * Structured CacheEntry format for persistent collections (other than Maps, see {@link StructuredMapCacheEntry}).
 *
 * @author Gavin King
 */
public class StructuredCollectionCacheEntry implements CacheEntryStructure {
	/**
	 * Access to the singleton reference.
	 */
	public static final StructuredCollectionCacheEntry INSTANCE = new StructuredCollectionCacheEntry();

	@Override
	@Nonnull
	public Object structure(@Nonnull Object item) {
		final var entry = (CollectionCacheEntry) item;
		return Arrays.asList( entry.getState() );
	}

	@Override
	@Nonnull
	public Object destructure(@Nonnull Object structured, @Nonnull SessionFactoryImplementor factory) {
		final var list = (List<?>) structured;
		return new CollectionCacheEntry( list.toArray( Serializable[]::new ) );
	}

	private StructuredCollectionCacheEntry() {
	}
}
