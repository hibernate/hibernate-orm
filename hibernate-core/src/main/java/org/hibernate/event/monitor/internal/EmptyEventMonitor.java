/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.monitor.internal;

import org.hibernate.LockMode;
import org.hibernate.cache.spi.Region;
import org.hibernate.cache.spi.access.CachedDomainDataAccess;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.AutoFlushEvent;
import org.hibernate.event.monitor.spi.EventMonitor;
import org.hibernate.event.monitor.spi.DiagnosticEvent;
import org.hibernate.event.spi.FlushEvent;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * An {@link EventMonitor} that ignores all events.
 */
public final class EmptyEventMonitor implements EventMonitor {

	@Override
	public @Nullable DiagnosticEvent beginSessionOpenEvent() {
		return null;
	}

	@Override
	public void completeSessionOpenEvent(@Nullable DiagnosticEvent sessionOpenEvent, @Nonnull SharedSessionContractImplementor session) {

	}

	@Override
	public @Nullable DiagnosticEvent beginSessionClosedEvent() {
		return null;
	}

	@Override
	public void completeSessionClosedEvent(
			@Nullable DiagnosticEvent sessionClosedEvent,
			@Nonnull SharedSessionContractImplementor session) {

	}

	@Override
	public @Nullable DiagnosticEvent beginJdbcConnectionAcquisitionEvent() {
		return null;
	}

	@Override
	public void completeJdbcConnectionAcquisitionEvent(
			@Nullable DiagnosticEvent jdbcConnectionAcquisitionEvent,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull Object tenantId) {

	}

	@Override
	public @Nullable DiagnosticEvent beginJdbcConnectionReleaseEvent() {
		return null;
	}

	@Override
	public void completeJdbcConnectionReleaseEvent(
			@Nullable DiagnosticEvent jdbcConnectionReleaseEvent,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull Object tenantId) {

	}

	@Override
	public @Nullable DiagnosticEvent beginJdbcPreparedStatementCreationEvent() {
		return null;
	}

	@Override
	public void completeJdbcPreparedStatementCreationEvent(
			@Nullable DiagnosticEvent jdbcPreparedStatementCreation,
			@Nonnull String preparedStatementSql) {

	}

	@Override
	public @Nullable DiagnosticEvent beginJdbcPreparedStatementExecutionEvent() {
		return null;
	}

	@Override
	public void completeJdbcPreparedStatementExecutionEvent(
			@Nullable DiagnosticEvent jdbcPreparedStatementExecutionEvent,
			@Nonnull String preparedStatementSql) {

	}

	@Override
	public @Nullable DiagnosticEvent beginJdbcBatchExecutionEvent() {
		return null;
	}

	@Override
	public void completeJdbcBatchExecutionEvent(@Nullable DiagnosticEvent jdbcBatchExecutionEvent, @Nonnull String statementSql) {

	}

	@Override
	public @Nullable DiagnosticEvent beginCachePutEvent() {
		return null;
	}

	@Override
	public void completeCachePutEvent(
			@Nullable DiagnosticEvent cachePutEvent,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull Region region,
			boolean cacheContentChanged,
			@Nonnull CacheActionDescription description) {

	}

	@Override
	public void completeCachePutEvent(
			@Nullable DiagnosticEvent cachePutEvent,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull CachedDomainDataAccess cachedDomainDataAccess,
			@Nonnull EntityPersister persister,
			boolean cacheContentChanged,
			@Nonnull CacheActionDescription description) {

	}

	@Override
	public void completeCachePutEvent(
			@Nullable DiagnosticEvent cachePutEvent,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull CachedDomainDataAccess cachedDomainDataAccess,
			@Nonnull EntityPersister persister,
			boolean cacheContentChanged,
			boolean isNatualId,
			@Nonnull CacheActionDescription description) {

	}

	@Override
	public void completeCachePutEvent(
			@Nullable DiagnosticEvent cachePutEvent,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull CachedDomainDataAccess cachedDomainDataAccess,
			@Nonnull CollectionPersister persister,
			boolean cacheContentChanged,
			@Nonnull CacheActionDescription description) {

	}

	@Override
	public @Nullable DiagnosticEvent beginCacheGetEvent() {
		return null;
	}

	@Override
	public void completeCacheGetEvent(
			@Nullable DiagnosticEvent cacheGetEvent,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull Region region,
			boolean hit) {

	}

	@Override
	public void completeCacheGetEvent(
			@Nullable DiagnosticEvent cacheGetEvent,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull Region region,
			@Nonnull EntityPersister persister,
			boolean isNaturalKey,
			boolean hit) {

	}

	@Override
	public void completeCacheGetEvent(
			@Nullable DiagnosticEvent cacheGetEvent,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull Region region,
			@Nonnull CollectionPersister persister,
			boolean hit) {

	}

	@Override
	public @Nullable DiagnosticEvent beginFlushEvent() {
		return null;
	}

	@Override
	public void completeFlushEvent(@Nullable DiagnosticEvent flushEvent, @Nonnull FlushEvent event) {

	}

	@Override
	public void completeFlushEvent(@Nullable DiagnosticEvent flushEvent, @Nonnull FlushEvent event, boolean autoFlush) {

	}

	@Override
	public @Nullable DiagnosticEvent beginPartialFlushEvent() {
		return null;
	}

	@Override
	public void completePartialFlushEvent(@Nullable DiagnosticEvent flushEvent, @Nonnull AutoFlushEvent event) {

	}

	@Override
	public @Nullable DiagnosticEvent beginDirtyCalculationEvent() {
		return null;
	}

	@Override
	public void completeDirtyCalculationEvent(
			@Nullable DiagnosticEvent dirtyCalculationEvent,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull EntityPersister persister,
			@Nonnull EntityEntry entry,
			@Nullable int[] dirtyProperties) {

	}

	@Override
	public @Nullable DiagnosticEvent beginPrePartialFlush() {
		return null;
	}

	@Override
	public void completePrePartialFlush(
			@Nullable DiagnosticEvent prePartialFlush,
			@Nonnull SharedSessionContractImplementor session) {

	}

	@Override
	public @Nullable DiagnosticEvent beginEntityInsertEvent() {
		return null;
	}

	@Override
	public void completeEntityInsertEvent(
			@Nullable DiagnosticEvent event,
			@Nonnull Object id, @Nonnull String entityName,
			boolean success,
			@Nonnull SharedSessionContractImplementor session) {

	}

	@Override
	public @Nullable DiagnosticEvent beginEntityUpdateEvent() {
		return null;
	}

	@Override
	public void completeEntityUpdateEvent(
			@Nullable DiagnosticEvent event,
			@Nonnull Object id, @Nonnull String entityName,
			boolean success,
			@Nonnull SharedSessionContractImplementor session) {

	}

	@Override
	public @Nullable DiagnosticEvent beginEntityUpsertEvent() {
		return null;
	}

	@Override
	public void completeEntityUpsertEvent(@Nullable DiagnosticEvent event, @Nonnull Object id, @Nonnull String entityName, boolean success, @Nonnull SharedSessionContractImplementor session) {

	}

	@Override
	public @Nullable DiagnosticEvent beginEntityDeleteEvent() {
		return null;
	}

	@Override
	public void completeEntityDeleteEvent(
			@Nullable DiagnosticEvent event,
			@Nonnull Object id, @Nonnull String entityName,
			boolean success,
			@Nonnull SharedSessionContractImplementor session) {

	}

	@Override
	public @Nullable DiagnosticEvent beginEntityLockEvent() {
		return null;
	}

	@Override
	public void completeEntityLockEvent(@Nullable DiagnosticEvent event, @Nonnull Object id, @Nonnull String entityName, @Nonnull LockMode lockMode, boolean success, @Nonnull SharedSessionContractImplementor session) {

	}

	@Override
	public @Nullable DiagnosticEvent beginCollectionRecreateEvent() {
		return null;
	}

	@Override
	public void completeCollectionRecreateEvent(@Nullable DiagnosticEvent event, @Nonnull Object id, @Nonnull String role, boolean success, @Nonnull SharedSessionContractImplementor session) {

	}

	@Override
	public @Nullable DiagnosticEvent beginCollectionUpdateEvent() {
		return null;
	}

	@Override
	public void completeCollectionUpdateEvent(@Nullable DiagnosticEvent event, @Nonnull Object id, @Nonnull String role, boolean success, @Nonnull SharedSessionContractImplementor session) {

	}

	@Override
	public @Nullable DiagnosticEvent beginCollectionRemoveEvent() {
		return null;
	}

	@Override
	public void completeCollectionRemoveEvent(@Nullable DiagnosticEvent event, @Nonnull Object id, @Nonnull String role, boolean success, @Nonnull SharedSessionContractImplementor session) {

	}
}
