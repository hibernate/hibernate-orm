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
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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
	@Nullable DiagnosticEvent beginSessionOpenEvent();

	void completeSessionOpenEvent(
			@Nullable DiagnosticEvent sessionOpenEvent,
			@Nonnull SharedSessionContractImplementor session);

	@Nullable DiagnosticEvent beginSessionClosedEvent();

	void completeSessionClosedEvent(
			@Nullable DiagnosticEvent sessionClosedEvent,
			@Nonnull SharedSessionContractImplementor session);

	@Nullable DiagnosticEvent beginJdbcConnectionAcquisitionEvent();

	void completeJdbcConnectionAcquisitionEvent(
			@Nullable DiagnosticEvent jdbcConnectionAcquisitionEvent,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull Object tenantId);

	@Nullable DiagnosticEvent beginJdbcConnectionReleaseEvent();

	void completeJdbcConnectionReleaseEvent(
			@Nullable DiagnosticEvent jdbcConnectionReleaseEvent,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull Object tenantId);

	@Nullable DiagnosticEvent beginJdbcPreparedStatementCreationEvent();

	void completeJdbcPreparedStatementCreationEvent(
			@Nullable DiagnosticEvent jdbcPreparedStatementCreation,
			@Nonnull String preparedStatementSql);

	@Nullable DiagnosticEvent beginJdbcPreparedStatementExecutionEvent();

	void completeJdbcPreparedStatementExecutionEvent(
			@Nullable DiagnosticEvent jdbcPreparedStatementExecutionEvent,
			@Nonnull String preparedStatementSql);

	@Nullable DiagnosticEvent beginJdbcBatchExecutionEvent();

	void completeJdbcBatchExecutionEvent(
			@Nullable DiagnosticEvent jdbcBatchExecutionEvent,
			@Nonnull String statementSql);

	@Nullable DiagnosticEvent beginCachePutEvent();

	void completeCachePutEvent(
			@Nullable DiagnosticEvent cachePutEvent,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull Region region,
			boolean cacheContentChanged,
			@Nonnull CacheActionDescription description);

	void completeCachePutEvent(
			@Nullable DiagnosticEvent cachePutEvent,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull CachedDomainDataAccess cachedDomainDataAccess,
			@Nonnull EntityPersister persister,
			boolean cacheContentChanged,
			@Nonnull CacheActionDescription description);

	void completeCachePutEvent(
			@Nullable DiagnosticEvent cachePutEvent,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull CachedDomainDataAccess cachedDomainDataAccess,
			@Nonnull EntityPersister persister,
			boolean cacheContentChanged,
			boolean isNatualId,
			@Nonnull CacheActionDescription description);

	void completeCachePutEvent(
			@Nullable DiagnosticEvent cachePutEvent,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull CachedDomainDataAccess cachedDomainDataAccess,
			@Nonnull CollectionPersister persister,
			boolean cacheContentChanged,
			@Nonnull CacheActionDescription description);

	@Nullable DiagnosticEvent beginCacheGetEvent();

	void completeCacheGetEvent(
			@Nullable DiagnosticEvent cacheGetEvent,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull Region region,
			boolean hit);

	void completeCacheGetEvent(
			@Nullable DiagnosticEvent cacheGetEvent,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull Region region,
			@Nonnull EntityPersister persister,
			boolean isNaturalKey,
			boolean hit);

	void completeCacheGetEvent(
			@Nullable DiagnosticEvent cacheGetEvent,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull Region region,
			@Nonnull CollectionPersister persister,
			boolean hit);

	@Nullable DiagnosticEvent beginFlushEvent();

	void completeFlushEvent(
			@Nullable DiagnosticEvent flushEvent,
			@Nonnull org.hibernate.event.spi.FlushEvent event);

	void completeFlushEvent(
			@Nullable DiagnosticEvent flushEvent,
			@Nonnull org.hibernate.event.spi.FlushEvent event,
			boolean autoFlush);

	@Nullable DiagnosticEvent beginPartialFlushEvent();

	void completePartialFlushEvent(
			@Nullable DiagnosticEvent flushEvent,
			@Nonnull AutoFlushEvent event);

	@Nullable DiagnosticEvent beginDirtyCalculationEvent();

	void completeDirtyCalculationEvent(
			@Nullable DiagnosticEvent dirtyCalculationEvent,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull EntityPersister persister,
			@Nonnull EntityEntry entry,
			@Nullable int[] dirtyProperties);

	@Nullable DiagnosticEvent beginPrePartialFlush();

	void completePrePartialFlush(
			@Nullable DiagnosticEvent prePartialFlush,
			@Nonnull SharedSessionContractImplementor session
	);

	@Nullable DiagnosticEvent beginEntityInsertEvent();

	void completeEntityInsertEvent(@Nullable DiagnosticEvent event, @Nullable Object id, @Nonnull String entityName, boolean success, @Nonnull SharedSessionContractImplementor session);

	@Nullable DiagnosticEvent beginEntityUpdateEvent();

	void completeEntityUpdateEvent(@Nullable DiagnosticEvent event, @Nonnull Object id, @Nonnull String entityName, boolean success, @Nonnull SharedSessionContractImplementor session);

	@Nullable DiagnosticEvent beginEntityUpsertEvent();

	void completeEntityUpsertEvent(@Nullable DiagnosticEvent event, @Nonnull Object id, @Nonnull String entityName, boolean success, @Nonnull SharedSessionContractImplementor session);

	@Nullable DiagnosticEvent beginEntityDeleteEvent();

	void completeEntityDeleteEvent(@Nullable DiagnosticEvent event, @Nonnull Object id, @Nonnull String entityName, boolean success, @Nonnull SharedSessionContractImplementor session);

	@Nullable DiagnosticEvent beginEntityLockEvent();

	void completeEntityLockEvent(@Nullable DiagnosticEvent event, @Nonnull Object id, @Nonnull String entityName, @Nonnull LockMode lockMode, boolean success, @Nonnull SharedSessionContractImplementor session);

	@Nullable DiagnosticEvent beginCollectionRecreateEvent();

	void completeCollectionRecreateEvent(@Nullable DiagnosticEvent event, @Nonnull Object id, @Nonnull String role, boolean success, @Nonnull SharedSessionContractImplementor session);

	@Nullable DiagnosticEvent beginCollectionUpdateEvent();

	void completeCollectionUpdateEvent(@Nullable DiagnosticEvent event, @Nonnull Object id, @Nonnull String role, boolean success, @Nonnull SharedSessionContractImplementor session);

	@Nullable DiagnosticEvent beginCollectionRemoveEvent();

	void completeCollectionRemoveEvent(@Nullable DiagnosticEvent event, @Nonnull Object id, @Nonnull String role, boolean success, @Nonnull SharedSessionContractImplementor session);

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

		CacheActionDescription(@Nonnull String text) {
			this.text = text;
		}

		@Override
		public @Nonnull String toString() {
			return text;
		}

		public @Nonnull String getText() {
			return text;
		}
	}
}
