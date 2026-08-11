/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.action.spi.AfterTransactionCompletionProcess;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.CollectionPersister;

import static org.hibernate.engine.internal.CacheHelper.usingCache;

/// Holds the second-level cache lock acquired for one graph-native collection mutation.
///
/// @since 8.0
/// @author Steve Ebersole
final class CollectionCacheCleanupProcess implements AfterTransactionCompletionProcess {
	private final Object key;
	private final CollectionPersister persister;
	private final @Nullable SoftLock lock;

	private CollectionCacheCleanupProcess(
			Object key,
			CollectionPersister persister,
			@Nullable SoftLock lock) {
		this.key = key;
		this.persister = persister;
		this.lock = lock;
	}

	static @Nullable CollectionCacheCleanupProcess prepare(
			PreparedCollectionMutation mutation,
			SessionImplementor session) {
		final var resolvedMutation = mutation.resolveKey( session );
		final var persister = resolvedMutation.getPersister();
		return usingCache(
				persister,
				cache -> {
					final Object key = resolvedMutation.getKey();
					final Object cacheKey = cache.generateCacheKey(
							key,
							persister,
							session.getFactory(),
							session.getTenantIdentifier()
					);
					return new CollectionCacheCleanupProcess(
							key,
							persister,
							cache.lockItem( session, cacheKey, null )
					);
				},
				null
		);
	}

	@Override
	public void doAfterTransactionCompletion(
			boolean success,
			@Nonnull SharedSessionContractImplementor session) {
		usingCache( persister, cache -> {
			final Object cacheKey = cache.generateCacheKey(
					key,
					persister,
					session.getFactory(),
					session.getTenantIdentifier()
			);
			cache.unlockItem( session, cacheKey, lock );
		} );
	}
}
