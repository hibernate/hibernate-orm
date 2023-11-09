/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

@JavaServiceLoadable
@Incubating
public interface EventManager {
	HibernateEvent beginSessionOpenEvent();

	void completeSessionOpenEvent(
			HibernateEvent sessionOpenEvent,
			SharedSessionContractImplementor session);

	HibernateEvent beginSessionClosedEvent();

	void completeSessionClosedEvent(
			HibernateEvent sessionClosedEvent,
			SharedSessionContractImplementor session);

	HibernateEvent beginJdbcConnectionAcquisitionEvent();

	void completeJdbcConnectionAcquisitionEvent(
			HibernateEvent jdbcConnectionAcquisitionEvent,
			SharedSessionContractImplementor session,
			Object tenantId);

	HibernateEvent beginJdbcConnectionReleaseEvent();

	void completeJdbcConnectionReleaseEvent(
			HibernateEvent jdbcConnectionReleaseEvent,
			SharedSessionContractImplementor session,
			Object tenantId);

	HibernateEvent beginJdbcPreparedStatementCreationEvent();

	void completeJdbcPreparedStatementCreationEvent(
			HibernateEvent jdbcPreparedStatementCreation,
			String preparedStatementSql);

	HibernateEvent beginJdbcPreparedStatementExecutionEvent();

	void completeJdbcPreparedStatementExecutionEvent(
			HibernateEvent jdbcPreparedStatementExecutionEvent,
			String preparedStatementSql);

	HibernateEvent beginJdbcBatchExecutionEvent();

	void completeJdbcBatchExecutionEvent(
			HibernateEvent jdbcBatchExecutionEvent,
			String statementSql);

	HibernateEvent beginCachePutEvent();

	void completeCachePutEvent(
			HibernateEvent cachePutEvent,
			SharedSessionContractImplementor session,
			Region region,
			boolean cacheContentChanged,
			CacheActionDescription description);

	void completeCachePutEvent(
			HibernateEvent cachePutEvent,
			SharedSessionContractImplementor session,
			CachedDomainDataAccess cachedDomainDataAccess,
			EntityPersister persister,
			boolean cacheContentChanged,
			CacheActionDescription description);

	void completeCachePutEvent(
			HibernateEvent cachePutEvent,
			SharedSessionContractImplementor session,
			CachedDomainDataAccess cachedDomainDataAccess,
			EntityPersister persister,
			boolean cacheContentChanged,
			boolean isNatualId,
			CacheActionDescription description);

	void completeCachePutEvent(
			HibernateEvent cachePutEvent,
			SharedSessionContractImplementor session,
			CachedDomainDataAccess cachedDomainDataAccess,
			CollectionPersister persister,
			boolean cacheContentChanged,
			CacheActionDescription description);

	HibernateEvent beginCacheGetEvent();

	void completeCacheGetEvent(
			HibernateEvent cacheGetEvent,
			SharedSessionContractImplementor session,
			Region region,
			boolean hit);

	void completeCacheGetEvent(
			HibernateEvent cacheGetEvent,
			SharedSessionContractImplementor session,
			Region region,
			EntityPersister persister,
			boolean isNaturalKey,
			boolean hit);

	void completeCacheGetEvent(
			HibernateEvent cacheGetEvent,
			SharedSessionContractImplementor session,
			Region region,
			CollectionPersister persister,
			boolean hit);

	HibernateEvent beginFlushEvent();

	void completeFlushEvent(
			HibernateEvent flushEvent,
			org.hibernate.event.spi.FlushEvent event);

	void completeFlushEvent(
			HibernateEvent flushEvent,
			org.hibernate.event.spi.FlushEvent event,
			boolean autoFlush);

	HibernateEvent beginPartialFlushEvent();

	void completePartialFlushEvent(
			HibernateEvent flushEvent,
			AutoFlushEvent event);

	HibernateEvent beginDirtyCalculationEvent();

	void completeDirtyCalculationEvent(
			HibernateEvent dirtyCalculationEvent,
			SharedSessionContractImplementor session,
			EntityPersister persister,
			EntityEntry entry,
			int[] dirtyProperties);


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
