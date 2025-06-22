/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import org.hibernate.CacheMode;
import org.hibernate.action.spi.AfterTransactionCompletionProcess;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionEventListenerManager;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.monitor.spi.DiagnosticEvent;
import org.hibernate.event.monitor.spi.EventMonitor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.stat.internal.StatsHelper;
import org.hibernate.stat.spi.StatisticsImplementor;

public final class OptimisticLockHelper {

	private OptimisticLockHelper() {
		//utility class, not to be constructed
	}

	public static void forceVersionIncrement(Object object, EntityEntry entry, SharedSessionContractImplementor session) {
		final EntityPersister persister = entry.getPersister();
		final Object previousVersion = entry.getVersion();
		SoftLock lock = null;
		final Object cacheKey;
		if ( persister.canWriteToCache() ) {
			final EntityDataAccess cache = persister.getCacheAccessStrategy();
			cacheKey = cache.generateCacheKey(
					entry.getId(),
					persister,
					session.getFactory(),
					session.getTenantIdentifier()
			);
			lock = cache.lockItem( session, cacheKey, previousVersion );
		}
		else {
			cacheKey = null;
		}
		final Object nextVersion = persister.forceVersionIncrement( entry.getId(), previousVersion, session );
		entry.forceLocked( object, nextVersion );
		if ( persister.canWriteToCache() ) {
			final Object cacheEntry = updateCacheItem(
					object,
					previousVersion,
					nextVersion,
					cacheKey,
					entry,
					persister,
					session
			);
			session.registerProcess( new CacheCleanupProcess(
					cacheKey,
					persister,
					previousVersion,
					nextVersion,
					lock,
					cacheEntry
			) );
		}
	}

	private static Object updateCacheItem(Object entity, Object previousVersion, Object nextVersion, Object ck, EntityEntry entry, EntityPersister persister, SharedSessionContractImplementor session) {
		if ( isCacheInvalidationRequired( persister, session ) || entry.getStatus() != Status.MANAGED ) {
			persister.getCacheAccessStrategy().remove( session, ck );
		}
		else if ( session.getCacheMode().isPutEnabled() ) {
			//TODO: inefficient if that cache is just going to ignore the updated state!
			final CacheEntry ce = persister.buildCacheEntry( entity, entry.getLoadedState(), nextVersion, session );
			final Object cacheEntry = persister.getCacheEntryStructure().structure( ce );
			final boolean put = updateCache( persister, cacheEntry, previousVersion, nextVersion, ck, session );

			final StatisticsImplementor statistics = session.getFactory().getStatistics();
			if ( put && statistics.isStatisticsEnabled() ) {
				statistics.entityCachePut(
						StatsHelper.getRootEntityRole( persister ),
						persister.getCacheAccessStrategy().getRegion().getName()
				);
			}
			return cacheEntry;
		}
		return null;
	}

	private static boolean updateCache(EntityPersister persister, Object cacheEntry, Object previousVersion, Object nextVersion, Object ck, SharedSessionContractImplementor session) {
		final EventMonitor eventMonitor = session.getEventMonitor();
		final DiagnosticEvent cachePutEvent = eventMonitor.beginCachePutEvent();
		final EntityDataAccess cacheAccessStrategy = persister.getCacheAccessStrategy();
		boolean update = false;
		try {
			session.getEventListenerManager().cachePutStart();
			update = cacheAccessStrategy.update( session, ck, cacheEntry, nextVersion, previousVersion );
			return update;
		}
		finally {
			eventMonitor.completeCachePutEvent(
					cachePutEvent,
					session,
					cacheAccessStrategy,
					persister,
					update,
					EventMonitor.CacheActionDescription.ENTITY_UPDATE
			);
			session.getEventListenerManager().cachePutEnd();
		}
	}

	private static boolean isCacheInvalidationRequired(
			EntityPersister persister,
			SharedSessionContractImplementor session) {
		// the cache has to be invalidated when CacheMode is equal to GET or IGNORE
		return persister.isCacheInvalidationRequired()
			|| session.getCacheMode() == CacheMode.GET
			|| session.getCacheMode() == CacheMode.IGNORE;
	}

	private static class CacheCleanupProcess implements AfterTransactionCompletionProcess {
		private final Object cacheKey;
		private final EntityPersister persister;
		private final Object previousVersion;
		private final Object nextVersion;
		private final SoftLock lock;
		private final Object cacheEntry;

		private CacheCleanupProcess(Object cacheKey, EntityPersister persister, Object previousVersion, Object nextVersion, SoftLock lock, Object cacheEntry) {
			this.cacheKey = cacheKey;
			this.persister = persister;
			this.previousVersion = previousVersion;
			this.nextVersion = nextVersion;
			this.lock = lock;
			this.cacheEntry = cacheEntry;
		}

		@Override
		public void doAfterTransactionCompletion(boolean success, SharedSessionContractImplementor session) {
			final EntityDataAccess cache = persister.getCacheAccessStrategy();
			if ( cacheUpdateRequired( success, persister, session ) ) {
				cacheAfterUpdate( cache, cacheKey, session );
			}
			else {
				cache.unlockItem( session, cacheKey, lock );
			}
		}

		private static boolean cacheUpdateRequired(boolean success, EntityPersister persister, SharedSessionContractImplementor session) {
			return success
				&& !persister.isCacheInvalidationRequired()
				&& session.getCacheMode().isPutEnabled();
		}

		protected void cacheAfterUpdate(EntityDataAccess cache, Object ck, SharedSessionContractImplementor session) {
			final SessionEventListenerManager eventListenerManager = session.getEventListenerManager();
			final EventMonitor eventMonitor = session.getEventMonitor();
			final DiagnosticEvent cachePutEvent = eventMonitor.beginCachePutEvent();
			boolean put = false;
			try {
				eventListenerManager.cachePutStart();
				put = cache.afterUpdate( session, ck, cacheEntry, nextVersion, previousVersion, lock );
			}
			finally {
				eventMonitor.completeCachePutEvent(
						cachePutEvent,
						session,
						cache,
						persister,
						put,
						EventMonitor.CacheActionDescription.ENTITY_AFTER_UPDATE
				);
				final StatisticsImplementor statistics = session.getFactory().getStatistics();
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

}
