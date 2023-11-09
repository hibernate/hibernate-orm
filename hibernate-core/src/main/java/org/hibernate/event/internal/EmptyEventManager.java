/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import org.hibernate.cache.spi.Region;
import org.hibernate.cache.spi.access.CachedDomainDataAccess;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.AutoFlushEvent;
import org.hibernate.event.spi.EventManager;
import org.hibernate.event.spi.HibernateEvent;
import org.hibernate.event.spi.FlushEvent;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;

public class EmptyEventManager implements EventManager {

	public static final EmptyEventManager INSTANCE = new EmptyEventManager();

	private EmptyEventManager(){

	}
	@Override
	public HibernateEvent beginSessionOpenEvent() {
		return null;
	}

	@Override
	public void completeSessionOpenEvent(HibernateEvent sessionOpenEvent, SharedSessionContractImplementor session) {

	}

	@Override
	public HibernateEvent beginSessionClosedEvent() {
		return null;
	}

	@Override
	public void completeSessionClosedEvent(
			HibernateEvent sessionClosedEvent,
			SharedSessionContractImplementor session) {

	}

	@Override
	public HibernateEvent beginJdbcConnectionAcquisitionEvent() {
		return null;
	}

	@Override
	public void completeJdbcConnectionAcquisitionEvent(
			HibernateEvent jdbcConnectionAcquisitionEvent,
			SharedSessionContractImplementor session,
			Object tenantId) {

	}

	@Override
	public HibernateEvent beginJdbcConnectionReleaseEvent() {
		return null;
	}

	@Override
	public void completeJdbcConnectionReleaseEvent(
			HibernateEvent jdbcConnectionReleaseEvent,
			SharedSessionContractImplementor session,
			Object tenantId) {

	}

	@Override
	public HibernateEvent beginJdbcPreparedStatementCreationEvent() {
		return null;
	}

	@Override
	public void completeJdbcPreparedStatementCreationEvent(
			HibernateEvent jdbcPreparedStatementCreation,
			String preparedStatementSql) {

	}

	@Override
	public HibernateEvent beginJdbcPreparedStatementExecutionEvent() {
		return null;
	}

	@Override
	public void completeJdbcPreparedStatementExecutionEvent(
			HibernateEvent jdbcPreparedStatementExecutionEvent,
			String preparedStatementSql) {

	}

	@Override
	public HibernateEvent beginJdbcBatchExecutionEvent() {
		return null;
	}

	@Override
	public void completeJdbcBatchExecutionEvent(HibernateEvent jdbcBatchExecutionEvent, String statementSql) {

	}

	@Override
	public HibernateEvent beginCachePutEvent() {
		return null;
	}

	@Override
	public void completeCachePutEvent(
			HibernateEvent cachePutEvent,
			SharedSessionContractImplementor session,
			Region region,
			boolean cacheContentChanged,
			CacheActionDescription description) {

	}

	@Override
	public void completeCachePutEvent(
			HibernateEvent cachePutEvent,
			SharedSessionContractImplementor session,
			CachedDomainDataAccess cachedDomainDataAccess,
			EntityPersister persister,
			boolean cacheContentChanged,
			CacheActionDescription description) {

	}

	@Override
	public void completeCachePutEvent(
			HibernateEvent cachePutEvent,
			SharedSessionContractImplementor session,
			CachedDomainDataAccess cachedDomainDataAccess,
			EntityPersister persister,
			boolean cacheContentChanged,
			boolean isNatualId,
			CacheActionDescription description) {

	}

	@Override
	public void completeCachePutEvent(
			HibernateEvent cachePutEvent,
			SharedSessionContractImplementor session,
			CachedDomainDataAccess cachedDomainDataAccess,
			CollectionPersister persister,
			boolean cacheContentChanged,
			CacheActionDescription description) {

	}

	@Override
	public HibernateEvent beginCacheGetEvent() {
		return null;
	}

	@Override
	public void completeCacheGetEvent(
			HibernateEvent cacheGetEvent,
			SharedSessionContractImplementor session,
			Region region,
			boolean hit) {

	}

	@Override
	public void completeCacheGetEvent(
			HibernateEvent cacheGetEvent,
			SharedSessionContractImplementor session,
			Region region,
			EntityPersister persister,
			boolean isNaturalKey,
			boolean hit) {

	}

	@Override
	public void completeCacheGetEvent(
			HibernateEvent cacheGetEvent,
			SharedSessionContractImplementor session,
			Region region,
			CollectionPersister persister,
			boolean hit) {

	}

	@Override
	public HibernateEvent beginFlushEvent() {
		return null;
	}

	@Override
	public void completeFlushEvent(HibernateEvent flushEvent, FlushEvent event) {

	}

	@Override
	public void completeFlushEvent(HibernateEvent flushEvent, FlushEvent event, boolean autoFlush) {

	}

	@Override
	public HibernateEvent beginPartialFlushEvent() {
		return null;
	}

	@Override
	public void completePartialFlushEvent(HibernateEvent flushEvent, AutoFlushEvent event) {

	}

	@Override
	public HibernateEvent beginDirtyCalculationEvent() {
		return null;
	}

	@Override
	public void completeDirtyCalculationEvent(
			HibernateEvent dirtyCalculationEvent,
			SharedSessionContractImplementor session,
			EntityPersister persister,
			EntityEntry entry,
			int[] dirtyProperties) {

	}
}
