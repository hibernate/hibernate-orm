/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

import java.io.Serializable;

import jakarta.persistence.AttributeNode;
import jakarta.persistence.BatchSize;
import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;

import org.hibernate.CacheMode;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * {@linkplain jakarta.persistence.FetchOption Fetch options}
 * applied to a particular association fetch.
 *
 * @since 6.0
 * @author Gavin King
 */
public record FetchOptions(
		@Nullable CacheStoreMode cacheStoreMode,
		@Nullable CacheRetrieveMode cacheRetrieveMode,
		@Nullable Integer batchSize)
				implements Serializable {

	public static final FetchOptions NONE = new FetchOptions( null, null, null );

	public static FetchOptions of(
			@Nullable CacheStoreMode cacheStoreMode,
			@Nullable CacheRetrieveMode cacheRetrieveMode,
			@Nullable Integer batchSize) {
		return cacheStoreMode == null
			&& cacheRetrieveMode == null
			&& batchSize == null
				? NONE
				: new FetchOptions( cacheStoreMode, cacheRetrieveMode, batchSize );
	}

	public static FetchOptions of(AttributeNode<?> node) {
		return node == null
				? NONE
				: of( cacheStoreMode( node ), cacheRetrieveMode( node ), batchSize( node ) );
	}

	private static @Nullable CacheStoreMode cacheStoreMode(AttributeNode<?> node) {
		for ( var option : node.getOptions() ) {
			if ( option instanceof CacheStoreMode cacheStoreMode ) {
				return cacheStoreMode;
			}
		}
		return null;
	}

	private static @Nullable CacheRetrieveMode cacheRetrieveMode(AttributeNode<?> node) {
		for ( var option : node.getOptions() ) {
			if ( option instanceof CacheRetrieveMode cacheRetrieveMode ) {
				return cacheRetrieveMode;
			}
		}
		return null;
	}

	private static @Nullable Integer batchSize(AttributeNode<?> node) {
		for ( var option : node.getOptions() ) {
			if ( option instanceof BatchSize batchSize
					&& batchSize.batchSize() >= 0 ) {
				return batchSize.batchSize();
			}
		}
		return null;
	}

	public boolean hasOptions() {
		return this != NONE
			&& ( cacheStoreMode != null || cacheRetrieveMode != null || batchSize != null );
	}

	public boolean hasCacheModes() {
		return cacheStoreMode != null || cacheRetrieveMode != null;
	}

	public boolean hasBatchSize() {
		return batchSize != null;
	}

	public CacheMode overrideCacheMode(CacheMode previousCacheMode) {
		return CacheMode.fromJpaModes(
				cacheRetrieveMode == null
						? previousCacheMode.getJpaRetrieveMode()
						: cacheRetrieveMode,
				cacheStoreMode == null
						? previousCacheMode.getJpaStoreMode()
						: cacheStoreMode
		);
	}

}
