/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.decompose.entity;


import org.hibernate.action.internal.EntityDeleteAction;
import org.hibernate.engine.internal.CacheHelper.CacheLock;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;

import static org.hibernate.engine.internal.CacheHelper.writingToCache;

/// Second-level cache bookkeeping for graph-based entity deletes.
///
/// @author Steve Ebersole
public class DeleteCacheHandling {
	public static CacheLock lockItem(
			EntityDeleteAction action,
			SharedSessionContractImplementor session) {
		final var persister = action.getPersister();
		return writingToCache( persister, cache -> {
			final Object cacheKey = cache.generateCacheKey(
					action.getId(),
					persister,
					session.getFactory(),
					session.getTenantIdentifier()
			);
			final var lock = cache.lockItem( session, cacheKey, currentVersion( action, persister ) );
			return new CacheLock( cache, cacheKey, lock );
		}, null );
	}

	public static void unlockItem(
			CacheLock cacheLock,
			SharedSessionContractImplementor session) {
		if ( cacheLock != null ) {
			cacheLock.cache().unlockItem( session, cacheLock.cacheKey(), cacheLock.lock() );
		}
	}

	private static Object currentVersion(EntityDeleteAction action, EntityPersister persister) {
		return persister.isVersionPropertyGenerated() && action.getInstance() != null
				? persister.getVersion( action.getInstance() )
				: action.getVersion();
	}
}
