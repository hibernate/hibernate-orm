/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.decompose.entity;

import org.hibernate.action.internal.EntityDeleteAction;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;

/// Second-level cache bookkeeping for graph-based entity deletes.
///
/// @author Steve Ebersole
public class DeleteCacheHandling {
	public record CacheLock(Object cacheKey, SoftLock lock) {
	}

	public static CacheLock lockItem(
			EntityDeleteAction action,
			SharedSessionContractImplementor session) {
		final var persister = action.getPersister();
		if ( !persister.canWriteToCache() ) {
			return new CacheLock( null, null );
		}

		final var cache = persister.getCacheAccessStrategy();
		final Object cacheKey = cache.generateCacheKey(
				action.getId(),
				persister,
				session.getFactory(),
				session.getTenantIdentifier()
		);
		return new CacheLock( cacheKey, cache.lockItem( session, cacheKey, currentVersion( action, persister ) ) );
	}

	public static void unlockItem(
			EntityDeleteAction action,
			CacheLock cacheLock,
			SharedSessionContractImplementor session) {
		final var persister = action.getPersister();
		if ( persister.canWriteToCache() ) {
			final var cache = persister.getCacheAccessStrategy();
			cache.unlockItem( session, cacheLock.cacheKey(), cacheLock.lock() );
		}
	}

	private static Object currentVersion(EntityDeleteAction action, EntityPersister persister) {
		return persister.isVersionPropertyGenerated() && action.getInstance() != null
				? persister.getVersion( action.getInstance() )
				: action.getVersion();
	}
}
