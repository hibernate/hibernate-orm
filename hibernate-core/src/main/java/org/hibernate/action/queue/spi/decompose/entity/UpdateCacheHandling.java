/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.spi.decompose.entity;

import org.hibernate.CacheMode;
import org.hibernate.Incubating;
import org.hibernate.action.internal.EntityUpdateAction;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.monitor.spi.EventMonitor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.stat.internal.StatsHelper;

import static org.hibernate.cache.spi.entry.CacheEntryHelper.buildStructuredCacheEntry;
import static org.hibernate.engine.internal.CacheHelper.writingToCache;

/// Second-level cache bookkeeping for graph-based entity updates.
///
/// @author Steve Ebersole
/// @since 8.0
@Incubating
public class UpdateCacheHandling {
	public static class CacheUpdate {
		private final Object cacheKey;
		private final SoftLock lock;
		private final Object previousVersion;

		private Object cacheEntry;
		private Object nextVersion;

		public CacheUpdate(Object cacheKey, SoftLock lock, Object previousVersion) {
			this.cacheKey = cacheKey;
			this.lock = lock;
			this.previousVersion = previousVersion;
		}

		public Object cacheKey() {
			return cacheKey;
		}

		public SoftLock lock() {
			return lock;
		}

		public Object previousVersion() {
			return previousVersion;
		}

		public Object cacheEntry() {
			return cacheEntry;
		}

		public Object nextVersion() {
			return nextVersion;
		}
	}

	public static CacheUpdate lockItem(
			EntityUpdateAction action,
			Object previousVersion,
			SharedSessionContractImplementor session) {
		final var persister = action.getPersister();
		return writingToCache( persister, cache -> {
			final Object cacheKey = cache.generateCacheKey(
					action.getId(),
					persister,
					session.getFactory(),
					session.getTenantIdentifier()
			);
			final var lock = cache.lockItem( session, cacheKey, previousVersion );
			return new CacheUpdate( cacheKey, lock, previousVersion );
		}, new CacheUpdate( null, null, previousVersion ) );
	}

	public static void updateItem(
			EntityUpdateAction action,
			CacheUpdate cacheUpdate,
			Object nextVersion,
			EntityEntry entry,
			SharedSessionContractImplementor session) {
		final var persister = action.getPersister();
		writingToCache( persister, cache -> {
			if ( isCacheInvalidationRequired( persister, session ) || entry.getStatus() != Status.MANAGED ) {
				cache.remove( session, cacheUpdate.cacheKey() );
			}
			else if ( session.getCacheMode().isPutEnabled() ) {
				cacheUpdate.cacheEntry = buildStructuredCacheEntry(
						action.getInstance(),
						nextVersion,
						action.getState(),
						persister,
						session
				);
				cacheUpdate.nextVersion = nextVersion;
				final boolean put = updateCache( action, cacheUpdate, persister, cache, session );

				final var statistics = session.getFactory().getStatistics();
				if ( put && statistics.isStatisticsEnabled() ) {
					statistics.entityCachePut(
							StatsHelper.getRootEntityRole( persister ),
							cache.getRegion().getName()
					);
				}
			}
		} );
	}

	public static void afterTransactionCompletion(
			boolean success,
			EntityUpdateAction action,
			CacheUpdate cacheUpdate,
			SharedSessionContractImplementor session) {
		final var persister = action.getPersister();
		writingToCache( persister, cache -> {
			if ( cacheUpdateRequired( success, persister, cacheUpdate, session ) ) {
				cacheAfterUpdate( action, cacheUpdate, cache, session );
			}
			else {
				cache.unlockItem( session, cacheUpdate.cacheKey(), cacheUpdate.lock() );
			}
		} );
	}

	private static boolean isCacheInvalidationRequired(
			EntityPersister persister,
			SharedSessionContractImplementor session) {
		return persister.isCacheInvalidationRequired()
			|| session.getCacheMode() == CacheMode.GET
			|| session.getCacheMode() == CacheMode.IGNORE;
	}

	private static boolean updateCache(
			EntityUpdateAction action,
			CacheUpdate cacheUpdate,
			EntityPersister persister,
			EntityDataAccess cache,
			SharedSessionContractImplementor session) {
		final var eventMonitor = session.getEventMonitor();
		final var cachePutEvent = eventMonitor.beginCachePutEvent();
		final var eventListenerManager = session.getEventListenerManager();
		boolean update = false;
		try {
			eventListenerManager.cachePutStart();
			update = cache.update(
					session,
					cacheUpdate.cacheKey(),
					cacheUpdate.cacheEntry(),
					cacheUpdate.nextVersion(),
					cacheUpdate.previousVersion()
			);
			return update;
		}
		finally {
			eventMonitor.completeCachePutEvent(
					cachePutEvent,
					session,
					cache,
					persister,
					update,
					EventMonitor.CacheActionDescription.ENTITY_UPDATE
			);
			eventListenerManager.cachePutEnd();
		}
	}

	private static boolean cacheUpdateRequired(
			boolean success,
			EntityPersister persister,
			CacheUpdate cacheUpdate,
			SharedSessionContractImplementor session) {
		return success
			&& cacheUpdate.cacheEntry() != null
			&& !persister.isCacheInvalidationRequired()
			&& session.getCacheMode().isPutEnabled();
	}

	private static void cacheAfterUpdate(
			EntityUpdateAction action,
			CacheUpdate cacheUpdate,
			EntityDataAccess cache,
			SharedSessionContractImplementor session) {
		final var eventListenerManager = session.getEventListenerManager();
		final var eventMonitor = session.getEventMonitor();
		final var cachePutEvent = eventMonitor.beginCachePutEvent();
		boolean put = false;
		try {
			eventListenerManager.cachePutStart();
			put = cache.afterUpdate(
					session,
					cacheUpdate.cacheKey(),
					cacheUpdate.cacheEntry(),
					cacheUpdate.nextVersion(),
					cacheUpdate.previousVersion(),
					cacheUpdate.lock()
			);
		}
		finally {
			final var persister = action.getPersister();
			eventMonitor.completeCachePutEvent(
					cachePutEvent,
					session,
					cache,
					persister,
					put,
					EventMonitor.CacheActionDescription.ENTITY_AFTER_UPDATE
			);
			final var statistics = session.getFactory().getStatistics();
			if ( put && statistics.isStatisticsEnabled() ) {
				statistics.entityCachePut(
						StatsHelper.getRootEntityRole( persister ),
						cache.getRegion().getName()
				);
			}
			eventListenerManager.cachePutEnd();
		}
	}
}
