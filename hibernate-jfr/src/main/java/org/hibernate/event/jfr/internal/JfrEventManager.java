/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.jfr.internal;

import org.hibernate.cache.spi.Region;
import org.hibernate.cache.spi.access.CachedDomainDataAccess;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.EventManager;
import org.hibernate.event.spi.HibernateEvent;
import org.hibernate.event.spi.AutoFlushEvent;
import org.hibernate.event.spi.EventSource;
import org.hibernate.internal.build.AllowNonPortable;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.stat.internal.StatsHelper;

import jdk.jfr.EventType;

import static java.util.concurrent.TimeUnit.NANOSECONDS;


@AllowNonPortable
public class JfrEventManager implements EventManager {

	private static final EventType eventType = EventType.getEventType( SessionOpenEvent.class );

	@Override
	public SessionOpenEvent beginSessionOpenEvent() {
		if ( eventType.isEnabled() ) {
			final SessionOpenEvent sessionOpenEvent = new SessionOpenEvent();
			sessionOpenEvent.begin();
			return sessionOpenEvent;
		}
		else {
			return null;
		}
	}

	@Override
	public void completeSessionOpenEvent(
			HibernateEvent event,
			SharedSessionContractImplementor session) {
		if ( event != null ) {
			final SessionOpenEvent sessionOpenEvent = (SessionOpenEvent) event;
			sessionOpenEvent.end();
			if ( sessionOpenEvent.shouldCommit() ) {
				sessionOpenEvent.sessionIdentifier = getSessionIdentifier( session );
				sessionOpenEvent.commit();
			}
		}
	}

	@Override
	public SessionClosedEvent beginSessionClosedEvent() {
		final SessionClosedEvent sessionClosedEvent = new SessionClosedEvent();
		if ( sessionClosedEvent.isEnabled() ) {
			sessionClosedEvent.begin();
		}
		return sessionClosedEvent;
	}

	@Override
	public void completeSessionClosedEvent(
			HibernateEvent event,
			SharedSessionContractImplementor session) {
		final SessionClosedEvent sessionClosedEvent = (SessionClosedEvent) event;
		if ( sessionClosedEvent.isEnabled() ) {
			sessionClosedEvent.end();
			if ( sessionClosedEvent.shouldCommit() ) {
				sessionClosedEvent.sessionIdentifier = getSessionIdentifier( session );
				sessionClosedEvent.commit();
			}
		}
	}

	@Override
	public JdbcConnectionAcquisitionEvent beginJdbcConnectionAcquisitionEvent() {
		final JdbcConnectionAcquisitionEvent jdbcConnectionAcquisitionEvent = new JdbcConnectionAcquisitionEvent();
		if ( jdbcConnectionAcquisitionEvent.isEnabled() ) {
			jdbcConnectionAcquisitionEvent.begin();
			jdbcConnectionAcquisitionEvent.startedAt = System.nanoTime();
		}
		return jdbcConnectionAcquisitionEvent;
	}

	@Override
	public void completeJdbcConnectionAcquisitionEvent(
			HibernateEvent event,
			SharedSessionContractImplementor session,
			Object tenantId) {
		final JdbcConnectionAcquisitionEvent jdbcConnectionAcquisitionEvent = (JdbcConnectionAcquisitionEvent) event;
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

	@Override
	public JdbcConnectionReleaseEvent beginJdbcConnectionReleaseEvent() {
		final JdbcConnectionReleaseEvent jdbcConnectionReleaseEvent = new JdbcConnectionReleaseEvent();
		if ( jdbcConnectionReleaseEvent.isEnabled() ) {
			jdbcConnectionReleaseEvent.begin();
			jdbcConnectionReleaseEvent.startedAt = System.nanoTime();
		}
		return jdbcConnectionReleaseEvent;
	}

	@Override
	public void completeJdbcConnectionReleaseEvent(
			HibernateEvent event,
			SharedSessionContractImplementor session,
			Object tenantId) {
		final JdbcConnectionReleaseEvent jdbcConnectionReleaseEvent = (JdbcConnectionReleaseEvent) event;
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

	@Override
	public JdbcPreparedStatementCreationEvent beginJdbcPreparedStatementCreationEvent() {
		final JdbcPreparedStatementCreationEvent jdbcPreparedStatementCreation = new JdbcPreparedStatementCreationEvent();
		if ( jdbcPreparedStatementCreation.isEnabled() ) {
			jdbcPreparedStatementCreation.begin();
			jdbcPreparedStatementCreation.startedAt = System.nanoTime();
		}
		return jdbcPreparedStatementCreation;
	}

	@Override
	public void completeJdbcPreparedStatementCreationEvent(
			HibernateEvent event,
			String preparedStatementSql) {
		final JdbcPreparedStatementCreationEvent jdbcPreparedStatementCreation = (JdbcPreparedStatementCreationEvent) event;
		if ( jdbcPreparedStatementCreation.isEnabled() ) {
			jdbcPreparedStatementCreation.end();
			if ( jdbcPreparedStatementCreation.shouldCommit() ) {
				jdbcPreparedStatementCreation.executionTime = getExecutionTime( jdbcPreparedStatementCreation.startedAt );
				jdbcPreparedStatementCreation.sql = preparedStatementSql;
				jdbcPreparedStatementCreation.commit();
			}
		}
	}

	@Override
	public JdbcPreparedStatementExecutionEvent beginJdbcPreparedStatementExecutionEvent() {
		final JdbcPreparedStatementExecutionEvent jdbcPreparedStatementExecutionEvent = new JdbcPreparedStatementExecutionEvent();
		if ( jdbcPreparedStatementExecutionEvent.isEnabled() ) {
			jdbcPreparedStatementExecutionEvent.begin();
			jdbcPreparedStatementExecutionEvent.startedAt = System.nanoTime();
		}
		return jdbcPreparedStatementExecutionEvent;
	}

	@Override
	public void completeJdbcPreparedStatementExecutionEvent(
			HibernateEvent event,
			String preparedStatementSql) {
		final JdbcPreparedStatementExecutionEvent jdbcPreparedStatementExecutionEvent = (JdbcPreparedStatementExecutionEvent) event;
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

	@Override
	public JdbcBatchExecutionEvent beginJdbcBatchExecutionEvent() {
		final JdbcBatchExecutionEvent jdbcBatchExecutionEvent = new JdbcBatchExecutionEvent();
		if ( jdbcBatchExecutionEvent.isEnabled() ) {
			jdbcBatchExecutionEvent.begin();
			jdbcBatchExecutionEvent.startedAt = System.nanoTime();
		}
		return jdbcBatchExecutionEvent;
	}

	@Override
	public void completeJdbcBatchExecutionEvent(
			HibernateEvent event,
			String statementSql) {
		final JdbcBatchExecutionEvent jdbcBatchExecutionEvent = (JdbcBatchExecutionEvent) event;
		if ( jdbcBatchExecutionEvent.isEnabled() ) {
			jdbcBatchExecutionEvent.end();
			if ( jdbcBatchExecutionEvent.shouldCommit() ) {
				jdbcBatchExecutionEvent.executionTime = getExecutionTime( jdbcBatchExecutionEvent.startedAt );
				jdbcBatchExecutionEvent.sql = statementSql;
				jdbcBatchExecutionEvent.commit();
			}
		}
	}

	@Override
	public HibernateEvent beginCachePutEvent() {
		final CachePutEvent cachePutEvent = new CachePutEvent();
		if ( cachePutEvent.isEnabled() ) {
			cachePutEvent.begin();
			cachePutEvent.startedAt = System.nanoTime();
		}
		return cachePutEvent;
	}

	@Override
	public void completeCachePutEvent(
			HibernateEvent event,
			SharedSessionContractImplementor session,
			Region region,
			boolean cacheContentChanged,
			CacheActionDescription description) {
		final CachePutEvent cachePutEvent = (CachePutEvent) event;
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

	@Override
	public void completeCachePutEvent(
			HibernateEvent event,
			SharedSessionContractImplementor session,
			CachedDomainDataAccess cachedDomainDataAccess,
			EntityPersister persister,
			boolean cacheContentChanged,
			CacheActionDescription description) {
		completeCachePutEvent(
				event,
				session,
				cachedDomainDataAccess,
				persister,
				cacheContentChanged,
				false,
				description
		);
	}

	@Override
	public void completeCachePutEvent(
			HibernateEvent event,
			SharedSessionContractImplementor session,
			CachedDomainDataAccess cachedDomainDataAccess,
			EntityPersister persister,
			boolean cacheContentChanged,
			boolean isNatualId,
			CacheActionDescription description) {
		final CachePutEvent cachePutEvent = (CachePutEvent) event;
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

	@Override
	public void completeCachePutEvent(
			HibernateEvent event,
			SharedSessionContractImplementor session,
			CachedDomainDataAccess cachedDomainDataAccess,
			CollectionPersister persister,
			boolean cacheContentChanged,
			CacheActionDescription description) {
		final CachePutEvent cachePutEvent = (CachePutEvent) event;
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

	@Override
	public HibernateEvent beginCacheGetEvent() {
		final CacheGetEvent cacheGetEvent = new CacheGetEvent();
		if ( cacheGetEvent.isEnabled() ) {
			cacheGetEvent.begin();
			cacheGetEvent.startedAt = System.nanoTime();
		}
		return cacheGetEvent;
	}

	@Override
	public void completeCacheGetEvent(
			HibernateEvent event,
			SharedSessionContractImplementor session,
			Region region,
			boolean hit) {
		final CacheGetEvent cacheGetEvent = (CacheGetEvent) event;
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

	@Override
	public void completeCacheGetEvent(
			HibernateEvent event,
			SharedSessionContractImplementor session,
			Region region,
			EntityPersister persister,
			boolean isNaturalKey,
			boolean hit) {
		final CacheGetEvent cacheGetEvent = (CacheGetEvent) event;
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

	@Override
	public void completeCacheGetEvent(
			HibernateEvent event,
			SharedSessionContractImplementor session,
			Region region,
			CollectionPersister persister,
			boolean hit) {
		final CacheGetEvent cacheGetEvent = (CacheGetEvent) event;
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

	@Override
	public FlushEvent beginFlushEvent() {
		final FlushEvent flushEvent = new FlushEvent();
		if ( flushEvent.isEnabled() ) {
			flushEvent.begin();
			flushEvent.startedAt = System.nanoTime();
		}
		return flushEvent;
	}

	@Override
	public void completeFlushEvent(
			HibernateEvent flushEvent,
			org.hibernate.event.spi.FlushEvent event) {
		completeFlushEvent( flushEvent, event, false );
	}

	@Override
	public void completeFlushEvent(
			HibernateEvent hibernateEvent,
			org.hibernate.event.spi.FlushEvent event,
			boolean autoFlush) {
		final FlushEvent flushEvent = (FlushEvent) hibernateEvent;
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

	@Override
	public PartialFlushEvent beginPartialFlushEvent() {
		final PartialFlushEvent partialFlushEvent = new PartialFlushEvent();
		if ( partialFlushEvent.isEnabled() ) {
			partialFlushEvent.startedAt = System.nanoTime();
			partialFlushEvent.begin();
		}
		return partialFlushEvent;
	}

	@Override
	public void completePartialFlushEvent(
			HibernateEvent hibernateEvent,
			AutoFlushEvent event) {
		final PartialFlushEvent flushEvent = (PartialFlushEvent) hibernateEvent;
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

	@Override
	public DirtyCalculationEvent beginDirtyCalculationEvent() {
		final DirtyCalculationEvent dirtyCalculationEvent = new DirtyCalculationEvent();
		if ( dirtyCalculationEvent.isEnabled() ) {
			dirtyCalculationEvent.startedAt = System.nanoTime();
			dirtyCalculationEvent.begin();
		}
		return dirtyCalculationEvent;
	}

	@Override
	public void completeDirtyCalculationEvent(
			HibernateEvent event,
			SharedSessionContractImplementor session,
			EntityPersister persister,
			EntityEntry entry,
			int[] dirtyProperties) {
		final DirtyCalculationEvent dirtyCalculationEvent = (DirtyCalculationEvent) event;
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

	private long getExecutionTime(Long startTime) {
		return NANOSECONDS.convert( System.nanoTime() - startTime, NANOSECONDS );
	}

	private String getSessionIdentifier(SharedSessionContractImplementor session) {
		if ( session == null ) {
			return null;
		}
		return session.getSessionIdentifier().toString();
	}

	private String getEntityName(EntityPersister persister) {
		return StatsHelper.INSTANCE.getRootEntityRole( persister ).getFullPath();
	}
}
