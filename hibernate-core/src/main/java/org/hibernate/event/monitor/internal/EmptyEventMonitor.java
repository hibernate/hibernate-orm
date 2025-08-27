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

/**
 * An {@link EventMonitor} that ignores all events.
 */
public final class EmptyEventMonitor implements EventMonitor {

	@Override
	public DiagnosticEvent beginSessionOpenEvent() {
		return null;
	}

	@Override
	public void completeSessionOpenEvent(DiagnosticEvent sessionOpenEvent, SharedSessionContractImplementor session) {

	}

	@Override
	public DiagnosticEvent beginSessionClosedEvent() {
		return null;
	}

	@Override
	public void completeSessionClosedEvent(
			DiagnosticEvent sessionClosedEvent,
			SharedSessionContractImplementor session) {

	}

	@Override
	public DiagnosticEvent beginJdbcConnectionAcquisitionEvent() {
		return null;
	}

	@Override
	public void completeJdbcConnectionAcquisitionEvent(
			DiagnosticEvent jdbcConnectionAcquisitionEvent,
			SharedSessionContractImplementor session,
			Object tenantId) {

	}

	@Override
	public DiagnosticEvent beginJdbcConnectionReleaseEvent() {
		return null;
	}

	@Override
	public void completeJdbcConnectionReleaseEvent(
			DiagnosticEvent jdbcConnectionReleaseEvent,
			SharedSessionContractImplementor session,
			Object tenantId) {

	}

	@Override
	public DiagnosticEvent beginJdbcPreparedStatementCreationEvent() {
		return null;
	}

	@Override
	public void completeJdbcPreparedStatementCreationEvent(
			DiagnosticEvent jdbcPreparedStatementCreation,
			String preparedStatementSql) {

	}

	@Override
	public DiagnosticEvent beginJdbcPreparedStatementExecutionEvent() {
		return null;
	}

	@Override
	public void completeJdbcPreparedStatementExecutionEvent(
			DiagnosticEvent jdbcPreparedStatementExecutionEvent,
			String preparedStatementSql) {

	}

	@Override
	public DiagnosticEvent beginJdbcBatchExecutionEvent() {
		return null;
	}

	@Override
	public void completeJdbcBatchExecutionEvent(DiagnosticEvent jdbcBatchExecutionEvent, String statementSql) {

	}

	@Override
	public DiagnosticEvent beginCachePutEvent() {
		return null;
	}

	@Override
	public void completeCachePutEvent(
			DiagnosticEvent cachePutEvent,
			SharedSessionContractImplementor session,
			Region region,
			boolean cacheContentChanged,
			CacheActionDescription description) {

	}

	@Override
	public void completeCachePutEvent(
			DiagnosticEvent cachePutEvent,
			SharedSessionContractImplementor session,
			CachedDomainDataAccess cachedDomainDataAccess,
			EntityPersister persister,
			boolean cacheContentChanged,
			CacheActionDescription description) {

	}

	@Override
	public void completeCachePutEvent(
			DiagnosticEvent cachePutEvent,
			SharedSessionContractImplementor session,
			CachedDomainDataAccess cachedDomainDataAccess,
			EntityPersister persister,
			boolean cacheContentChanged,
			boolean isNatualId,
			CacheActionDescription description) {

	}

	@Override
	public void completeCachePutEvent(
			DiagnosticEvent cachePutEvent,
			SharedSessionContractImplementor session,
			CachedDomainDataAccess cachedDomainDataAccess,
			CollectionPersister persister,
			boolean cacheContentChanged,
			CacheActionDescription description) {

	}

	@Override
	public DiagnosticEvent beginCacheGetEvent() {
		return null;
	}

	@Override
	public void completeCacheGetEvent(
			DiagnosticEvent cacheGetEvent,
			SharedSessionContractImplementor session,
			Region region,
			boolean hit) {

	}

	@Override
	public void completeCacheGetEvent(
			DiagnosticEvent cacheGetEvent,
			SharedSessionContractImplementor session,
			Region region,
			EntityPersister persister,
			boolean isNaturalKey,
			boolean hit) {

	}

	@Override
	public void completeCacheGetEvent(
			DiagnosticEvent cacheGetEvent,
			SharedSessionContractImplementor session,
			Region region,
			CollectionPersister persister,
			boolean hit) {

	}

	@Override
	public DiagnosticEvent beginFlushEvent() {
		return null;
	}

	@Override
	public void completeFlushEvent(DiagnosticEvent flushEvent, FlushEvent event) {

	}

	@Override
	public void completeFlushEvent(DiagnosticEvent flushEvent, FlushEvent event, boolean autoFlush) {

	}

	@Override
	public DiagnosticEvent beginPartialFlushEvent() {
		return null;
	}

	@Override
	public void completePartialFlushEvent(DiagnosticEvent flushEvent, AutoFlushEvent event) {

	}

	@Override
	public DiagnosticEvent beginDirtyCalculationEvent() {
		return null;
	}

	@Override
	public void completeDirtyCalculationEvent(
			DiagnosticEvent dirtyCalculationEvent,
			SharedSessionContractImplementor session,
			EntityPersister persister,
			EntityEntry entry,
			int[] dirtyProperties) {

	}

	@Override
	public DiagnosticEvent beginPrePartialFlush() {
		return null;
	}

	@Override
	public void completePrePartialFlush(
			DiagnosticEvent prePartialFlush,
			SharedSessionContractImplementor session) {

	}

	@Override
	public DiagnosticEvent beginEntityInsertEvent() {
		return null;
	}

	@Override
	public void completeEntityInsertEvent(
			DiagnosticEvent event,
			Object id, String entityName,
			boolean success,
			SharedSessionContractImplementor session) {

	}

	@Override
	public DiagnosticEvent beginEntityUpdateEvent() {
		return null;
	}

	@Override
	public void completeEntityUpdateEvent(
			DiagnosticEvent event,
			Object id, String entityName,
			boolean success,
			SharedSessionContractImplementor session) {

	}

	@Override
	public DiagnosticEvent beginEntityUpsertEvent() {
		return null;
	}

	@Override
	public void completeEntityUpsertEvent(DiagnosticEvent event, Object id, String entityName, boolean success, SharedSessionContractImplementor session) {

	}

	@Override
	public DiagnosticEvent beginEntityDeleteEvent() {
		return null;
	}

	@Override
	public void completeEntityDeleteEvent(
			DiagnosticEvent event,
			Object id, String entityName,
			boolean success,
			SharedSessionContractImplementor session) {

	}

	@Override
	public DiagnosticEvent beginEntityLockEvent() {
		return null;
	}

	@Override
	public void completeEntityLockEvent(DiagnosticEvent event, Object id, String entityName, LockMode lockMode, boolean success, SharedSessionContractImplementor session) {

	}

	@Override
	public DiagnosticEvent beginCollectionRecreateEvent() {
		return null;
	}

	@Override
	public void completeCollectionRecreateEvent(DiagnosticEvent event, Object id, String role, boolean success, SharedSessionContractImplementor session) {

	}

	@Override
	public DiagnosticEvent beginCollectionUpdateEvent() {
		return null;
	}

	@Override
	public void completeCollectionUpdateEvent(DiagnosticEvent event, Object id, String role, boolean success, SharedSessionContractImplementor session) {

	}

	@Override
	public DiagnosticEvent beginCollectionRemoveEvent() {
		return null;
	}

	@Override
	public void completeCollectionRemoveEvent(DiagnosticEvent event, Object id, String role, boolean success, SharedSessionContractImplementor session) {

	}
}
