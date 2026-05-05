/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.spi.decompose.entity;

import org.hibernate.Incubating;
import org.hibernate.action.internal.AbstractEntityInsertAction;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.monitor.spi.EventMonitor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.stat.internal.StatsHelper;

import static org.hibernate.cache.spi.entry.CacheEntryHelper.buildStructuredCacheEntry;

/// Second-level cache bookkeeping for graph-based entity inserts.
///
/// @author Steve Ebersole
/// @since 8.0
@Incubating
public class InsertCacheHandling {
	public static class CacheInsert {
		private Object cacheEntry;
		private Object version;
		private Object id;

		public Object cacheEntry() {
			return cacheEntry;
		}

		public Object version() {
			return version;
		}

		public Object id() {
			return id;
		}
	}

	public static void putIfNecessary(
			AbstractEntityInsertAction action,
			CacheInsert cacheInsert,
			Object id,
			Object version,
			SharedSessionContractImplementor session) {
		final var persister = action.getPersister();
		if ( isCachePutEnabled( persister, session ) ) {
			cacheInsert.id = id;
			cacheInsert.version = version;
			cacheInsert.cacheEntry = buildStructuredCacheEntry(
					action.getInstance(),
					version,
					action.getState(),
					persister,
					session
			);

			final var factory = session.getFactory();
			final var cache = persister.getCacheAccessStrategy();
			final Object cacheKey = cache.generateCacheKey( id, persister, factory, session.getTenantIdentifier() );
			final boolean put = cacheInsert( action, cacheInsert, persister, cacheKey, session );

			final var statistics = factory.getStatistics();
			if ( put && statistics.isStatisticsEnabled() ) {
				statistics.entityCachePut(
						StatsHelper.getRootEntityRole( persister ),
						cache.getRegion().getName()
				);
			}
		}
	}

	public static void afterTransactionCompletion(
			boolean success,
			AbstractEntityInsertAction action,
			CacheInsert cacheInsert,
			SharedSessionContractImplementor session) {
		final var persister = action.getPersister();
		if ( success && isCachePutEnabled( persister, session ) && cacheInsert.cacheEntry() != null ) {
			final var cache = persister.getCacheAccessStrategy();
			final var factory = session.getFactory();
			final Object cacheKey = cache.generateCacheKey(
					cacheInsert.id(),
					persister,
					factory,
					session.getTenantIdentifier()
			);
			final boolean put = cacheAfterInsert( action, cacheInsert, cache, cacheKey, session );

			final var statistics = factory.getStatistics();
			if ( put && statistics.isStatisticsEnabled() ) {
				statistics.entityCachePut(
						StatsHelper.getRootEntityRole( persister ),
						cache.getRegion().getName()
				);
			}
		}
	}

	private static boolean isCachePutEnabled(
			EntityPersister persister,
			SharedSessionContractImplementor session) {
		return persister.canWriteToCache()
			&& !persister.isCacheInvalidationRequired()
			&& session.getCacheMode().isPutEnabled();
	}

	private static boolean cacheInsert(
			AbstractEntityInsertAction action,
			CacheInsert cacheInsert,
			EntityPersister persister,
			Object cacheKey,
			SharedSessionContractImplementor session) {
		final var eventMonitor = session.getEventMonitor();
		final var cachePutEvent = eventMonitor.beginCachePutEvent();
		final var cacheAccessStrategy = persister.getCacheAccessStrategy();
		final var eventListenerManager = session.getEventListenerManager();
		boolean insert = false;
		try {
			eventListenerManager.cachePutStart();
			insert = cacheAccessStrategy.insert(
					session,
					cacheKey,
					cacheInsert.cacheEntry(),
					cacheInsert.version()
			);
			return insert;
		}
		finally {
			eventMonitor.completeCachePutEvent(
					cachePutEvent,
					session,
					cacheAccessStrategy,
					action.getPersister(),
					insert,
					EventMonitor.CacheActionDescription.ENTITY_INSERT
			);
			eventListenerManager.cachePutEnd();
		}
	}

	private static boolean cacheAfterInsert(
			AbstractEntityInsertAction action,
			CacheInsert cacheInsert,
			EntityDataAccess cache,
			Object cacheKey,
			SharedSessionContractImplementor session) {
		final var eventListenerManager = session.getEventListenerManager();
		final var eventMonitor = session.getEventMonitor();
		final var cachePutEvent = eventMonitor.beginCachePutEvent();
		boolean afterInsert = false;
		try {
			eventListenerManager.cachePutStart();
			afterInsert = cache.afterInsert(
					session,
					cacheKey,
					cacheInsert.cacheEntry(),
					cacheInsert.version()
			);
			return afterInsert;
		}
		finally {
			eventMonitor.completeCachePutEvent(
					cachePutEvent,
					session,
					cache,
					action.getPersister(),
					afterInsert,
					EventMonitor.CacheActionDescription.ENTITY_AFTER_INSERT
			);
			eventListenerManager.cachePutEnd();
		}
	}
}
