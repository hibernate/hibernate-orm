/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.jfr.internal;

import java.sql.PreparedStatement;

import org.hibernate.cache.spi.Region;
import org.hibernate.cache.spi.access.CachedDomainDataAccess;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.jfr.CacheGetEvent;
import org.hibernate.event.jfr.CachePutEvent;
import org.hibernate.event.jfr.DirtyCalculationEvent;
import org.hibernate.event.jfr.FlushEvent;
import org.hibernate.event.jfr.JdbcBatchExecutionEvent;
import org.hibernate.event.jfr.JdbcConnectionAcquisitionEvent;
import org.hibernate.event.jfr.JdbcConnectionReleaseEvent;
import org.hibernate.event.jfr.JdbcPreparedStatementCreationEvent;
import org.hibernate.event.jfr.JdbcPreparedStatementExecutionEvent;
import org.hibernate.event.jfr.PartialFlushEvent;
import org.hibernate.event.jfr.SessionClosedEvent;
import org.hibernate.event.jfr.SessionOpenEvent;
import org.hibernate.event.spi.AutoFlushEvent;
import org.hibernate.event.spi.EventSource;
import org.hibernate.internal.build.AllowNonPortable;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.stat.internal.StatsHelper;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

@AllowNonPortable
public class JfrEventManager {

	public static SessionOpenEvent beginSessionOpenEvent() {
		final SessionOpenEvent sessionOpenEvent = new SessionOpenEvent();
		if ( sessionOpenEvent.isEnabled() ) {
			sessionOpenEvent.begin();
		}
		return sessionOpenEvent;
	}

	public static void completeSessionOpenEvent(
			SessionOpenEvent sessionOpenEvent,
			SharedSessionContractImplementor session) {
		if ( sessionOpenEvent.isEnabled() ) {
			sessionOpenEvent.end();
			if ( sessionOpenEvent.shouldCommit() ) {
				sessionOpenEvent.sessionIdentifier = getSessionIdentifier( session );
				sessionOpenEvent.commit();
			}
		}
	}

	public static SessionClosedEvent beginSessionClosedEvent() {
		final SessionClosedEvent sessionClosedEvent = new SessionClosedEvent();
		if ( sessionClosedEvent.isEnabled() ) {
			sessionClosedEvent.begin();
		}
		return sessionClosedEvent;
	}

	public static void completeSessionClosedEvent(
			SessionClosedEvent sessionClosedEvent,
			SharedSessionContractImplementor session) {
		if ( sessionClosedEvent.isEnabled() ) {
			sessionClosedEvent.end();
			if ( sessionClosedEvent.shouldCommit() ) {
				sessionClosedEvent.sessionIdentifier = getSessionIdentifier( session );
				sessionClosedEvent.commit();
			}
		}
	}

	public static JdbcConnectionAcquisitionEvent beginJdbcConnectionAcquisitionEvent() {
		final JdbcConnectionAcquisitionEvent jdbcConnectionAcquisitionEvent = new JdbcConnectionAcquisitionEvent();
		if ( jdbcConnectionAcquisitionEvent.isEnabled() ) {
			jdbcConnectionAcquisitionEvent.begin();
			jdbcConnectionAcquisitionEvent.startedAt = System.nanoTime();
		}
		return jdbcConnectionAcquisitionEvent;
	}

	public static void completeJdbcConnectionAcquisitionEvent(
			JdbcConnectionAcquisitionEvent jdbcConnectionAcquisitionEvent,
			SharedSessionContractImplementor session,
			Object tenantId) {
		if ( jdbcConnectionAcquisitionEvent.isEnabled() ) {
			jdbcConnectionAcquisitionEvent.end();
			if ( jdbcConnectionAcquisitionEvent.shouldCommit() ) {
				jdbcConnectionAcquisitionEvent.executionTime = getExecutionTime( jdbcConnectionAcquisitionEvent.startedAt );
				jdbcConnectionAcquisitionEvent.sessionIdentifier = getSessionIdentifier( session );
				jdbcConnectionAcquisitionEvent.tenantIdentifier = tenantId == null ? null : session.getFactory()
						.getTenantIdentifierJavaType()
						.toString( tenantId );
				jdbcConnectionAcquisitionEvent.commit();
			}
		}
	}

	public static JdbcConnectionReleaseEvent beginJdbcConnectionReleaseEvent() {
		final JdbcConnectionReleaseEvent jdbcConnectionReleaseEvent = new JdbcConnectionReleaseEvent();
		if ( jdbcConnectionReleaseEvent.isEnabled() ) {
			jdbcConnectionReleaseEvent.begin();
			jdbcConnectionReleaseEvent.startedAt = System.nanoTime();
		}
		return jdbcConnectionReleaseEvent;
	}

	public static void completeJdbcConnectionReleaseEvent(
			JdbcConnectionReleaseEvent jdbcConnectionReleaseEvent,
			SharedSessionContractImplementor session,
			Object tenantId) {
		if ( jdbcConnectionReleaseEvent.isEnabled() ) {
			jdbcConnectionReleaseEvent.end();
			if ( jdbcConnectionReleaseEvent.shouldCommit() ) {
				jdbcConnectionReleaseEvent.executionTime = getExecutionTime( jdbcConnectionReleaseEvent.startedAt );
				jdbcConnectionReleaseEvent.sessionIdentifier = getSessionIdentifier( session );
				jdbcConnectionReleaseEvent.tenantIdentifier = tenantId == null ? null : session.getFactory()
						.getTenantIdentifierJavaType()
						.toString( tenantId );
				jdbcConnectionReleaseEvent.commit();
			}
		}
	}

	public static JdbcPreparedStatementCreationEvent beginJdbcPreparedStatementCreationEvent() {
		final JdbcPreparedStatementCreationEvent jdbcPreparedStatementCreation = new JdbcPreparedStatementCreationEvent();
		if ( jdbcPreparedStatementCreation.isEnabled() ) {
			jdbcPreparedStatementCreation.begin();
			jdbcPreparedStatementCreation.startedAt = System.nanoTime();
		}
		return jdbcPreparedStatementCreation;
	}

	public static void completeJdbcPreparedStatementCreationEvent(
			JdbcPreparedStatementCreationEvent jdbcPreparedStatementCreation,
			String preparedStatementSql) {
		if ( jdbcPreparedStatementCreation.isEnabled() ) {
			jdbcPreparedStatementCreation.end();
			if ( jdbcPreparedStatementCreation.shouldCommit() ) {
				jdbcPreparedStatementCreation.executionTime = getExecutionTime( jdbcPreparedStatementCreation.startedAt );
				jdbcPreparedStatementCreation.sql = preparedStatementSql;
				jdbcPreparedStatementCreation.commit();
			}
		}
	}

	public static JdbcPreparedStatementExecutionEvent beginJdbcPreparedStatementExecutionEvent() {
		final JdbcPreparedStatementExecutionEvent jdbcPreparedStatementExecutionEvent = new JdbcPreparedStatementExecutionEvent();
		if ( jdbcPreparedStatementExecutionEvent.isEnabled() ) {
			jdbcPreparedStatementExecutionEvent.begin();
			jdbcPreparedStatementExecutionEvent.startedAt = System.nanoTime();
		}
		return jdbcPreparedStatementExecutionEvent;
	}

	public static void completeJdbcPreparedStatementExecutionEvent(
			JdbcPreparedStatementExecutionEvent jdbcPreparedStatementExecutionEvent,
			String preparedStatementSql) {
		if ( jdbcPreparedStatementExecutionEvent.isEnabled() ) {
			jdbcPreparedStatementExecutionEvent.end();
			if ( jdbcPreparedStatementExecutionEvent.shouldCommit() ) {
				jdbcPreparedStatementExecutionEvent.executionTime = getExecutionTime(
						jdbcPreparedStatementExecutionEvent.startedAt );
				jdbcPreparedStatementExecutionEvent.sql = preparedStatementSql;
				jdbcPreparedStatementExecutionEvent.commit();
			}
		}
	}

	public static JdbcBatchExecutionEvent beginJdbcBatchExecutionEvent() {
		final JdbcBatchExecutionEvent jdbcBatchExecutionEvent = new JdbcBatchExecutionEvent();
		if ( jdbcBatchExecutionEvent.isEnabled() ) {
			jdbcBatchExecutionEvent.begin();
			jdbcBatchExecutionEvent.startedAt = System.nanoTime();
		}
		return jdbcBatchExecutionEvent;
	}

	public static void completeJdbcBatchExecutionEvent(
			JdbcBatchExecutionEvent jdbcBatchExecutionEvent,
			String statementSql) {
		if ( jdbcBatchExecutionEvent.isEnabled() ) {
			jdbcBatchExecutionEvent.end();
			if ( jdbcBatchExecutionEvent.shouldCommit() ) {
				jdbcBatchExecutionEvent.executionTime = getExecutionTime( jdbcBatchExecutionEvent.startedAt );
				jdbcBatchExecutionEvent.sql = statementSql;
				jdbcBatchExecutionEvent.commit();
			}
		}
	}

	public static CachePutEvent beginCachePutEvent() {
		final CachePutEvent cachePutEvent = new CachePutEvent();
		if ( cachePutEvent.isEnabled() ) {
			cachePutEvent.begin();
			cachePutEvent.startedAt = System.nanoTime();
		}
		return cachePutEvent;
	}

	public static void completeCachePutEvent(
			CachePutEvent cachePutEvent,
			SharedSessionContractImplementor session,
			Region region,
			boolean cacheContentChanged,
			CacheActionDescription description) {
		if ( cachePutEvent.isEnabled() ) {
			cachePutEvent.end();
			if ( cachePutEvent.shouldCommit() ) {
				cachePutEvent.executionTime = getExecutionTime( cachePutEvent.startedAt );
				cachePutEvent.sessionIdentifier = getSessionIdentifier( session );
				cachePutEvent.regionName = region.getName();
				cachePutEvent.description = description.getText();
				cachePutEvent.cacheChanged = cacheContentChanged;
				cachePutEvent.commit();
			}
		}
	}

	public static void completeCachePutEvent(
			CachePutEvent cachePutEvent,
			SharedSessionContractImplementor session,
			CachedDomainDataAccess cachedDomainDataAccess,
			EntityPersister persister,
			boolean cacheContentChanged,
			CacheActionDescription description) {
		completeCachePutEvent(
				cachePutEvent,
				session,
				cachedDomainDataAccess,
				persister,
				cacheContentChanged,
				false,
				description
		);
	}

	public static void completeCachePutEvent(
			CachePutEvent cachePutEvent,
			SharedSessionContractImplementor session,
			CachedDomainDataAccess cachedDomainDataAccess,
			EntityPersister persister,
			boolean cacheContentChanged,
			boolean isNatualId,
			CacheActionDescription description) {
		if ( cachePutEvent.isEnabled() ) {
			cachePutEvent.end();
			if ( cachePutEvent.shouldCommit() ) {
				cachePutEvent.executionTime = getExecutionTime( cachePutEvent.startedAt );
				cachePutEvent.sessionIdentifier = getSessionIdentifier( session );
				cachePutEvent.regionName = cachedDomainDataAccess.getRegion().getName();
				cachePutEvent.entityName = getEntityName( persister );
				cachePutEvent.description = description.getText();
				cachePutEvent.isNaturalId = isNatualId;
				cachePutEvent.cacheChanged = cacheContentChanged;
				cachePutEvent.commit();
			}
		}
	}

	public static void completeCachePutEvent(
			CachePutEvent cachePutEvent,
			SharedSessionContractImplementor session,
			CachedDomainDataAccess cachedDomainDataAccess,
			CollectionPersister persister,
			boolean cacheContentChanged,
			CacheActionDescription description) {
		if ( cachePutEvent.isEnabled() ) {
			cachePutEvent.end();
			if ( cachePutEvent.shouldCommit() ) {
				cachePutEvent.executionTime = getExecutionTime( cachePutEvent.startedAt );
				cachePutEvent.sessionIdentifier = getSessionIdentifier( session );
				cachePutEvent.regionName = cachedDomainDataAccess.getRegion().getName();
				cachePutEvent.collectionName = persister.getNavigableRole().getFullPath();
				cachePutEvent.description = description.getText();
				cachePutEvent.cacheChanged = cacheContentChanged;
				cachePutEvent.commit();
			}
		}
	}

	public static CacheGetEvent beginCacheGetEvent() {
		final CacheGetEvent cacheGetEvent = new CacheGetEvent();
		if ( cacheGetEvent.isEnabled() ) {
			cacheGetEvent.begin();
			cacheGetEvent.startedAt = System.nanoTime();
		}
		return cacheGetEvent;
	}

	public static void completeCacheGetEvent(
			CacheGetEvent cacheGetEvent,
			SharedSessionContractImplementor session,
			Region region,
			boolean hit) {
		if ( cacheGetEvent.isEnabled() ) {
			cacheGetEvent.end();
			if ( cacheGetEvent.shouldCommit() ) {
				cacheGetEvent.executionTime = getExecutionTime( cacheGetEvent.startedAt );
				cacheGetEvent.sessionIdentifier = getSessionIdentifier( session );
				cacheGetEvent.regionName = region.getName();
				cacheGetEvent.hit = hit;
				cacheGetEvent.commit();
			}
		}
	}

	public static void completeCacheGetEvent(
			CacheGetEvent cacheGetEvent,
			SharedSessionContractImplementor session,
			Region region,
			EntityPersister persister,
			boolean isNaturalKey,
			boolean hit) {
		if ( cacheGetEvent.isEnabled() ) {
			cacheGetEvent.end();
			if ( cacheGetEvent.shouldCommit() ) {
				cacheGetEvent.executionTime = getExecutionTime( cacheGetEvent.startedAt );
				cacheGetEvent.sessionIdentifier = getSessionIdentifier( session );
				cacheGetEvent.entityName = getEntityName( persister );
				cacheGetEvent.regionName = region.getName();
				cacheGetEvent.isNaturalId = isNaturalKey;
				cacheGetEvent.hit = hit;
				cacheGetEvent.commit();
			}
		}
	}

	public static void completeCacheGetEvent(
			CacheGetEvent cacheGetEvent,
			SharedSessionContractImplementor session,
			Region region,
			CollectionPersister persister,
			boolean hit) {
		if ( cacheGetEvent.isEnabled() ) {
			cacheGetEvent.end();
			if ( cacheGetEvent.shouldCommit() ) {
				cacheGetEvent.executionTime = getExecutionTime( cacheGetEvent.startedAt );
				cacheGetEvent.sessionIdentifier = getSessionIdentifier( session );
				cacheGetEvent.collectionName = persister.getNavigableRole().getFullPath();
				cacheGetEvent.regionName = region.getName();
				cacheGetEvent.hit = hit;
				cacheGetEvent.commit();
			}
		}
	}

	public static FlushEvent beginFlushEvent() {
		final FlushEvent flushEvent = new FlushEvent();
		if ( flushEvent.isEnabled() ) {
			flushEvent.begin();
			flushEvent.startedAt = System.nanoTime();
		}
		return flushEvent;
	}

	public static void completeFlushEvent(
			FlushEvent flushEvent,
			org.hibernate.event.spi.FlushEvent event) {
		completeFlushEvent( flushEvent, event, false );
	}

	public static void completeFlushEvent(
			FlushEvent flushEvent,
			org.hibernate.event.spi.FlushEvent event,
			boolean autoFlush) {
		if ( flushEvent.isEnabled() ) {
			flushEvent.end();
			if ( flushEvent.shouldCommit() ) {
				flushEvent.executionTime = getExecutionTime( flushEvent.startedAt );
				EventSource session = event.getSession();
				flushEvent.sessionIdentifier = getSessionIdentifier( session );
				flushEvent.numberOfEntitiesProcessed = event.getNumberOfEntitiesProcessed();
				flushEvent.numberOfCollectionsProcessed = event.getNumberOfCollectionsProcessed();
				flushEvent.isAutoFlush = autoFlush;
				flushEvent.commit();
			}
		}
	}

	public static PartialFlushEvent beginPartialFlushEvent() {
		final PartialFlushEvent partialFlushEvent = new PartialFlushEvent();
		if ( partialFlushEvent.isEnabled() ) {
			partialFlushEvent.startedAt = System.nanoTime();
			partialFlushEvent.begin();
		}
		return partialFlushEvent;
	}

	public static void completePartialFlushEvent(
			PartialFlushEvent flushEvent,
			AutoFlushEvent event) {
		if ( flushEvent.isEnabled() ) {
			flushEvent.end();
			if ( flushEvent.shouldCommit() ) {
				flushEvent.executionTime = getExecutionTime( flushEvent.startedAt );
				EventSource session = event.getSession();
				flushEvent.sessionIdentifier = getSessionIdentifier( session );
				flushEvent.numberOfEntitiesProcessed = event.getNumberOfEntitiesProcessed();
				flushEvent.numberOfCollectionsProcessed = event.getNumberOfCollectionsProcessed();
				flushEvent.isAutoFlush = true;
				flushEvent.commit();
			}
		}
	}

	public static DirtyCalculationEvent beginDirtyCalculationEvent() {
		final DirtyCalculationEvent dirtyCalculationEvent = new DirtyCalculationEvent();
		if ( dirtyCalculationEvent.isEnabled() ) {
			dirtyCalculationEvent.startedAt = System.nanoTime();
			dirtyCalculationEvent.begin();
		}
		return dirtyCalculationEvent;
	}

	public static void completeDirtyCalculationEvent(
			DirtyCalculationEvent dirtyCalculationEvent,
			SharedSessionContractImplementor session,
			EntityPersister persister,
			EntityEntry entry,
			int[] dirtyProperties) {
		if ( dirtyCalculationEvent.isEnabled() ) {
			dirtyCalculationEvent.end();
			if ( dirtyCalculationEvent.shouldCommit() ) {
				dirtyCalculationEvent.executionTime = getExecutionTime( dirtyCalculationEvent.startedAt );
				dirtyCalculationEvent.sessionIdentifier = getSessionIdentifier( session );
				dirtyCalculationEvent.entityName = getEntityName( persister );
				dirtyCalculationEvent.entityStatus = entry.getStatus().name();
				dirtyCalculationEvent.dirty = dirtyProperties != null;
				dirtyCalculationEvent.commit();
			}
		}
	}

	public enum CacheActionDescription {
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

	private static long getExecutionTime(Long startTime) {
		return NANOSECONDS.convert( System.nanoTime() - startTime, NANOSECONDS );
	}

	private static String getSessionIdentifier(SharedSessionContractImplementor session) {
		if ( session == null ) {
			return null;
		}
		return session.getSessionIdentifier().toString();
	}

	private static String getEntityName(EntityPersister persister) {
		return StatsHelper.INSTANCE.getRootEntityRole( persister ).getFullPath();
	}
}
