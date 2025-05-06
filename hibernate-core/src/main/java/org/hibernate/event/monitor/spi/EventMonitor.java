/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.monitor.spi;

import org.hibernate.Incubating;
import org.hibernate.LockMode;
import org.hibernate.cache.spi.Region;
import org.hibernate.cache.spi.access.CachedDomainDataAccess;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.AutoFlushEvent;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.JavaServiceLoadable;

/**
 * Contract implemented by services which collect, report, or monitor
 * {@linkplain DiagnosticEvent diagnostic events} involving interactions
 * between the {@linkplain org.hibernate.Session session} and the database
 * or second-level cache.
 * <p>
 * For example, this interface is implemented by Hibernate JFR to report
 * events to Java Flight Recorder.
 * <p>
 * Note that event reporting is different to aggregate <em>metrics</em>,
 * which Hibernate exposes via the {@link org.hibernate.stat.Statistics}
 * interface.
 *
 * @apiNote This an incubating API, subject to change.
 *
 * @since 7.0
 */
@JavaServiceLoadable
@Incubating
public interface EventMonitor {
	DiagnosticEvent beginSessionOpenEvent();

	void completeSessionOpenEvent(
			DiagnosticEvent sessionOpenEvent,
			SharedSessionContractImplementor session);

	DiagnosticEvent beginSessionClosedEvent();

	void completeSessionClosedEvent(
			DiagnosticEvent sessionClosedEvent,
			SharedSessionContractImplementor session);

	DiagnosticEvent beginJdbcConnectionAcquisitionEvent();

	void completeJdbcConnectionAcquisitionEvent(
			DiagnosticEvent jdbcConnectionAcquisitionEvent,
			SharedSessionContractImplementor session,
			Object tenantId);

	DiagnosticEvent beginJdbcConnectionReleaseEvent();

	void completeJdbcConnectionReleaseEvent(
			DiagnosticEvent jdbcConnectionReleaseEvent,
			SharedSessionContractImplementor session,
			Object tenantId);

	DiagnosticEvent beginJdbcPreparedStatementCreationEvent();

	void completeJdbcPreparedStatementCreationEvent(
			DiagnosticEvent jdbcPreparedStatementCreation,
			String preparedStatementSql);

	DiagnosticEvent beginJdbcPreparedStatementExecutionEvent();

	void completeJdbcPreparedStatementExecutionEvent(
			DiagnosticEvent jdbcPreparedStatementExecutionEvent,
			String preparedStatementSql);

	DiagnosticEvent beginJdbcBatchExecutionEvent();

	void completeJdbcBatchExecutionEvent(
			DiagnosticEvent jdbcBatchExecutionEvent,
			String statementSql);

	DiagnosticEvent beginCachePutEvent();

	void completeCachePutEvent(
			DiagnosticEvent cachePutEvent,
			SharedSessionContractImplementor session,
			Region region,
			boolean cacheContentChanged,
			CacheActionDescription description);

	void completeCachePutEvent(
			DiagnosticEvent cachePutEvent,
			SharedSessionContractImplementor session,
			CachedDomainDataAccess cachedDomainDataAccess,
			EntityPersister persister,
			boolean cacheContentChanged,
			CacheActionDescription description);

	void completeCachePutEvent(
			DiagnosticEvent cachePutEvent,
			SharedSessionContractImplementor session,
			CachedDomainDataAccess cachedDomainDataAccess,
			EntityPersister persister,
			boolean cacheContentChanged,
			boolean isNatualId,
			CacheActionDescription description);

	void completeCachePutEvent(
			DiagnosticEvent cachePutEvent,
			SharedSessionContractImplementor session,
			CachedDomainDataAccess cachedDomainDataAccess,
			CollectionPersister persister,
			boolean cacheContentChanged,
			CacheActionDescription description);

	DiagnosticEvent beginCacheGetEvent();

	void completeCacheGetEvent(
			DiagnosticEvent cacheGetEvent,
			SharedSessionContractImplementor session,
			Region region,
			boolean hit);

	void completeCacheGetEvent(
			DiagnosticEvent cacheGetEvent,
			SharedSessionContractImplementor session,
			Region region,
			EntityPersister persister,
			boolean isNaturalKey,
			boolean hit);

	void completeCacheGetEvent(
			DiagnosticEvent cacheGetEvent,
			SharedSessionContractImplementor session,
			Region region,
			CollectionPersister persister,
			boolean hit);

	DiagnosticEvent beginFlushEvent();

	void completeFlushEvent(
			DiagnosticEvent flushEvent,
			org.hibernate.event.spi.FlushEvent event);

	void completeFlushEvent(
			DiagnosticEvent flushEvent,
			org.hibernate.event.spi.FlushEvent event,
			boolean autoFlush);

	DiagnosticEvent beginPartialFlushEvent();

	void completePartialFlushEvent(
			DiagnosticEvent flushEvent,
			AutoFlushEvent event);

	DiagnosticEvent beginDirtyCalculationEvent();

	void completeDirtyCalculationEvent(
			DiagnosticEvent dirtyCalculationEvent,
			SharedSessionContractImplementor session,
			EntityPersister persister,
			EntityEntry entry,
			int[] dirtyProperties);

	DiagnosticEvent beginPrePartialFlush();

	void completePrePartialFlush(
			DiagnosticEvent prePartialFlush,
			SharedSessionContractImplementor session
	);

	DiagnosticEvent beginEntityInsertEvent();

	void completeEntityInsertEvent(DiagnosticEvent event, Object id, String entityName, boolean success, SharedSessionContractImplementor session);

	DiagnosticEvent beginEntityUpdateEvent();

	void completeEntityUpdateEvent(DiagnosticEvent event, Object id, String entityName, boolean success, SharedSessionContractImplementor session);

	DiagnosticEvent beginEntityUpsertEvent();

	void completeEntityUpsertEvent(DiagnosticEvent event, Object id, String entityName, boolean success, SharedSessionContractImplementor session);

	DiagnosticEvent beginEntityDeleteEvent();

	void completeEntityDeleteEvent(DiagnosticEvent event, Object id, String entityName, boolean success, SharedSessionContractImplementor session);

	DiagnosticEvent beginEntityLockEvent();

	void completeEntityLockEvent(DiagnosticEvent event, Object id, String entityName, LockMode lockMode, boolean success, SharedSessionContractImplementor session);

	DiagnosticEvent beginCollectionRecreateEvent();

	void completeCollectionRecreateEvent(DiagnosticEvent event, Object id, String role, boolean success, SharedSessionContractImplementor session);

	DiagnosticEvent beginCollectionUpdateEvent();

	void completeCollectionUpdateEvent(DiagnosticEvent event, Object id, String role, boolean success, SharedSessionContractImplementor session);

	DiagnosticEvent beginCollectionRemoveEvent();

	void completeCollectionRemoveEvent(DiagnosticEvent event, Object id, String role, boolean success, SharedSessionContractImplementor session);

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
