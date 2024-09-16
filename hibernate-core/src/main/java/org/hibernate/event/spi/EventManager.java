/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import org.hibernate.Incubating;
import org.hibernate.cache.spi.Region;
import org.hibernate.cache.spi.access.CachedDomainDataAccess;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.JavaServiceLoadable;

/**
 * Defines the contract for monitoring low-level events
 * involving interactions between the {@linkplain org.hibernate.Session}
 * and the database or second-level cache.
 *
 * @apiNote This an incubating API, subject to change.
 */
@JavaServiceLoadable
@Incubating
public interface EventManager {
	HibernateMonitoringEvent beginSessionOpenEvent();

	void completeSessionOpenEvent(
			HibernateMonitoringEvent sessionOpenEvent,
			SharedSessionContractImplementor session);

	HibernateMonitoringEvent beginSessionClosedEvent();

	void completeSessionClosedEvent(
			HibernateMonitoringEvent sessionClosedEvent,
			SharedSessionContractImplementor session);

	HibernateMonitoringEvent beginJdbcConnectionAcquisitionEvent();

	void completeJdbcConnectionAcquisitionEvent(
			HibernateMonitoringEvent jdbcConnectionAcquisitionEvent,
			SharedSessionContractImplementor session,
			Object tenantId);

	HibernateMonitoringEvent beginJdbcConnectionReleaseEvent();

	void completeJdbcConnectionReleaseEvent(
			HibernateMonitoringEvent jdbcConnectionReleaseEvent,
			SharedSessionContractImplementor session,
			Object tenantId);

	HibernateMonitoringEvent beginJdbcPreparedStatementCreationEvent();

	void completeJdbcPreparedStatementCreationEvent(
			HibernateMonitoringEvent jdbcPreparedStatementCreation,
			String preparedStatementSql);

	HibernateMonitoringEvent beginJdbcPreparedStatementExecutionEvent();

	void completeJdbcPreparedStatementExecutionEvent(
			HibernateMonitoringEvent jdbcPreparedStatementExecutionEvent,
			String preparedStatementSql);

	HibernateMonitoringEvent beginJdbcBatchExecutionEvent();

	void completeJdbcBatchExecutionEvent(
			HibernateMonitoringEvent jdbcBatchExecutionEvent,
			String statementSql);

	HibernateMonitoringEvent beginCachePutEvent();

	void completeCachePutEvent(
			HibernateMonitoringEvent cachePutEvent,
			SharedSessionContractImplementor session,
			Region region,
			boolean cacheContentChanged,
			CacheActionDescription description);

	void completeCachePutEvent(
			HibernateMonitoringEvent cachePutEvent,
			SharedSessionContractImplementor session,
			CachedDomainDataAccess cachedDomainDataAccess,
			EntityPersister persister,
			boolean cacheContentChanged,
			CacheActionDescription description);

	void completeCachePutEvent(
			HibernateMonitoringEvent cachePutEvent,
			SharedSessionContractImplementor session,
			CachedDomainDataAccess cachedDomainDataAccess,
			EntityPersister persister,
			boolean cacheContentChanged,
			boolean isNatualId,
			CacheActionDescription description);

	void completeCachePutEvent(
			HibernateMonitoringEvent cachePutEvent,
			SharedSessionContractImplementor session,
			CachedDomainDataAccess cachedDomainDataAccess,
			CollectionPersister persister,
			boolean cacheContentChanged,
			CacheActionDescription description);

	HibernateMonitoringEvent beginCacheGetEvent();

	void completeCacheGetEvent(
			HibernateMonitoringEvent cacheGetEvent,
			SharedSessionContractImplementor session,
			Region region,
			boolean hit);

	void completeCacheGetEvent(
			HibernateMonitoringEvent cacheGetEvent,
			SharedSessionContractImplementor session,
			Region region,
			EntityPersister persister,
			boolean isNaturalKey,
			boolean hit);

	void completeCacheGetEvent(
			HibernateMonitoringEvent cacheGetEvent,
			SharedSessionContractImplementor session,
			Region region,
			CollectionPersister persister,
			boolean hit);

	HibernateMonitoringEvent beginFlushEvent();

	void completeFlushEvent(
			HibernateMonitoringEvent flushEvent,
			org.hibernate.event.spi.FlushEvent event);

	void completeFlushEvent(
			HibernateMonitoringEvent flushEvent,
			org.hibernate.event.spi.FlushEvent event,
			boolean autoFlush);

	HibernateMonitoringEvent beginPartialFlushEvent();

	void completePartialFlushEvent(
			HibernateMonitoringEvent flushEvent,
			AutoFlushEvent event);

	HibernateMonitoringEvent beginDirtyCalculationEvent();

	void completeDirtyCalculationEvent(
			HibernateMonitoringEvent dirtyCalculationEvent,
			SharedSessionContractImplementor session,
			EntityPersister persister,
			EntityEntry entry,
			int[] dirtyProperties);

	HibernateMonitoringEvent beginPrePartialFlush();

	void completePrePartialFlush(
			HibernateMonitoringEvent prePartialFlush,
			SharedSessionContractImplementor session
	);

	enum CacheActionDescription {
		ENTITY_INSERT( "Entity Insert" ),
		ENTITY_AFTER_INSERT( "Entity After Insert" ),
		ENTITY_UPDATE( "Entity Update" ),
		ENTITY_LOAD( "Entity Load" ),
		ENTITY_AFTER_UPDATE( "Entity After Update" ),
		TIMESTAMP_PRE_INVALIDATE( "Timestamp Pre Invalidate" ),
		TIMESTAMP_INVALIDATE( "Timestamp Invalidate" ),
		COLLECTION_INSERT( "Collection Insert" ),
		QUERY_RESULT( "Query Result" );


		private final String text;

		CacheActionDescription(String text) {
			this.text = text;
		}

		@Override
		public String toString() {
			return text;
		}

		public String getText() {
			return text;
		}
	}
}
