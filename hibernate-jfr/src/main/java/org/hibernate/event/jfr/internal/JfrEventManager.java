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
import org.hibernate.event.spi.AutoFlushEvent;
import org.hibernate.event.spi.EventManager;
import org.hibernate.event.spi.HibernateMonitoringEvent;
import org.hibernate.internal.build.AllowNonPortable;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.stat.internal.StatsHelper;

import jdk.jfr.EventType;


@AllowNonPortable
public class JfrEventManager implements EventManager {

	private static final EventType sessionOpenEventType = EventType.getEventType( SessionOpenEvent.class );
	private static final EventType sessionClosedEventType = EventType.getEventType( SessionClosedEvent.class );
	private static final EventType jdbcConnectionAcquisitionEventType = EventType
			.getEventType( JdbcConnectionAcquisitionEvent.class );
	private static final EventType jdbcConnectionReleaseEventType = EventType
			.getEventType( JdbcConnectionReleaseEvent.class );
	private static final EventType jdbcPreparedStatementCreationEventType = EventType
			.getEventType( JdbcPreparedStatementCreationEvent.class );
	private static final EventType jdbcPreparedStatementExecutionEventType = EventType.getEventType(
			JdbcPreparedStatementExecutionEvent.class );
	private static final EventType jdbcBatchExecutionEventType = EventType.getEventType( JdbcBatchExecutionEvent.class );
	private static final EventType cachePutEventType = EventType.getEventType( CachePutEvent.class );
	private static final EventType cacheGetEventType = EventType.getEventType( CacheGetEvent.class );
	private static final EventType flushEventType = EventType.getEventType( FlushEvent.class );
	private static final EventType partialFlushEventType = EventType.getEventType( PartialFlushEvent.class );
	private static final EventType dirtyCalculationEventType = EventType.getEventType( DirtyCalculationEvent.class );
	private static final EventType prePartialFlushEventType = EventType.getEventType( PrePartialFlushEvent.class );

	@Override
	public SessionOpenEvent beginSessionOpenEvent() {
		if ( sessionOpenEventType.isEnabled() ) {
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
			HibernateMonitoringEvent monitoringEvent,
			SharedSessionContractImplementor session) {
		if ( monitoringEvent != null ) {
			final SessionOpenEvent sessionOpenEvent = (SessionOpenEvent) monitoringEvent;
			sessionOpenEvent.end();
			if ( sessionOpenEvent.shouldCommit() ) {
				sessionOpenEvent.sessionIdentifier = getSessionIdentifier( session );
				sessionOpenEvent.commit();
			}
		}
	}

	@Override
	public SessionClosedEvent beginSessionClosedEvent() {
		if ( sessionClosedEventType.isEnabled() ) {
			final SessionClosedEvent sessionClosedEvent = new SessionClosedEvent();
			sessionClosedEvent.begin();
			return sessionClosedEvent;
		}
		else {
			return null;
		}
	}

	@Override
	public void completeSessionClosedEvent(
			HibernateMonitoringEvent monitoringEvent,
			SharedSessionContractImplementor session) {
		if ( monitoringEvent != null ) {
			final SessionClosedEvent sessionClosedEvent = (SessionClosedEvent) monitoringEvent;
			sessionClosedEvent.end();
			if ( sessionClosedEvent.shouldCommit() ) {
				sessionClosedEvent.sessionIdentifier = getSessionIdentifier( session );
				sessionClosedEvent.commit();
			}
		}
	}

	@Override
	public JdbcConnectionAcquisitionEvent beginJdbcConnectionAcquisitionEvent() {
		if ( jdbcConnectionAcquisitionEventType.isEnabled() ) {
			final JdbcConnectionAcquisitionEvent jdbcConnectionAcquisitionEvent = new JdbcConnectionAcquisitionEvent();
			jdbcConnectionAcquisitionEvent.begin();
			return jdbcConnectionAcquisitionEvent;
		}
		else {
			return null;
		}
	}

	@Override
	public void completeJdbcConnectionAcquisitionEvent(
			HibernateMonitoringEvent monitoringEvent,
			SharedSessionContractImplementor session,
			Object tenantId) {
		if ( monitoringEvent != null ) {
			final JdbcConnectionAcquisitionEvent jdbcConnectionAcquisitionEvent = (JdbcConnectionAcquisitionEvent) monitoringEvent;
			jdbcConnectionAcquisitionEvent.end();
			if ( jdbcConnectionAcquisitionEvent.shouldCommit() ) {
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
		if ( jdbcConnectionReleaseEventType.isEnabled() ) {
			final JdbcConnectionReleaseEvent jdbcConnectionReleaseEvent = new JdbcConnectionReleaseEvent();
			jdbcConnectionReleaseEvent.begin();
			return jdbcConnectionReleaseEvent;
		}
		else {
			return null;
		}
	}

	@Override
	public void completeJdbcConnectionReleaseEvent(
			HibernateMonitoringEvent monitoringEvent,
			SharedSessionContractImplementor session,
			Object tenantId) {
		if ( monitoringEvent != null ) {
			final JdbcConnectionReleaseEvent jdbcConnectionReleaseEvent = (JdbcConnectionReleaseEvent) monitoringEvent;
			jdbcConnectionReleaseEvent.end();
			if ( jdbcConnectionReleaseEvent.shouldCommit() ) {
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
		if ( jdbcPreparedStatementCreationEventType.isEnabled() ) {
			final JdbcPreparedStatementCreationEvent jdbcPreparedStatementCreation = new JdbcPreparedStatementCreationEvent();
			jdbcPreparedStatementCreation.begin();
			return jdbcPreparedStatementCreation;
		}
		else {
			return null;
		}
	}

	@Override
	public void completeJdbcPreparedStatementCreationEvent(
			HibernateMonitoringEvent monitoringEvent,
			String preparedStatementSql) {
		if ( monitoringEvent != null ) {
			final JdbcPreparedStatementCreationEvent jdbcPreparedStatementCreation = (JdbcPreparedStatementCreationEvent) monitoringEvent;
			jdbcPreparedStatementCreation.end();
			if ( jdbcPreparedStatementCreation.shouldCommit() ) {
				jdbcPreparedStatementCreation.sql = preparedStatementSql;
				jdbcPreparedStatementCreation.commit();
			}
		}
	}

	@Override
	public JdbcPreparedStatementExecutionEvent beginJdbcPreparedStatementExecutionEvent() {
		if ( jdbcPreparedStatementExecutionEventType.isEnabled() ) {
			final JdbcPreparedStatementExecutionEvent jdbcPreparedStatementExecutionEvent = new JdbcPreparedStatementExecutionEvent();
			jdbcPreparedStatementExecutionEvent.begin();
			return jdbcPreparedStatementExecutionEvent;
		}
		else {
			return null;
		}
	}

	@Override
	public void completeJdbcPreparedStatementExecutionEvent(
			HibernateMonitoringEvent monitoringEvent,
			String preparedStatementSql) {
		if ( monitoringEvent != null ) {
			final JdbcPreparedStatementExecutionEvent jdbcPreparedStatementExecutionEvent = (JdbcPreparedStatementExecutionEvent) monitoringEvent;
			jdbcPreparedStatementExecutionEvent.end();
			if ( jdbcPreparedStatementExecutionEvent.shouldCommit() ) {
				jdbcPreparedStatementExecutionEvent.sql = preparedStatementSql;
				jdbcPreparedStatementExecutionEvent.commit();
			}
		}
	}

	@Override
	public JdbcBatchExecutionEvent beginJdbcBatchExecutionEvent() {
		if ( jdbcBatchExecutionEventType.isEnabled() ) {
			final JdbcBatchExecutionEvent jdbcBatchExecutionEvent = new JdbcBatchExecutionEvent();
			jdbcBatchExecutionEvent.begin();
			return jdbcBatchExecutionEvent;
		}
		else {
			return null;
		}
	}

	@Override
	public void completeJdbcBatchExecutionEvent(
			HibernateMonitoringEvent monitoringEvent,
			String statementSql) {
		if ( monitoringEvent != null ) {
			final JdbcBatchExecutionEvent jdbcBatchExecutionEvent = (JdbcBatchExecutionEvent) monitoringEvent;
			jdbcBatchExecutionEvent.end();
			if ( jdbcBatchExecutionEvent.shouldCommit() ) {
				jdbcBatchExecutionEvent.sql = statementSql;
				jdbcBatchExecutionEvent.commit();
			}
		}
	}

	@Override
	public HibernateMonitoringEvent beginCachePutEvent() {
		if ( cachePutEventType.isEnabled() ) {
			final CachePutEvent cachePutEvent = new CachePutEvent();
			cachePutEvent.begin();
			return cachePutEvent;
		}
		else {
			return null;
		}
	}

	@Override
	public void completeCachePutEvent(
			HibernateMonitoringEvent monitoringEvent,
			SharedSessionContractImplementor session,
			Region region,
			boolean cacheContentChanged,
			CacheActionDescription description) {
		if ( monitoringEvent != null ) {
			final CachePutEvent cachePutEvent = (CachePutEvent) monitoringEvent;
			cachePutEvent.end();
			if ( cachePutEvent.shouldCommit() ) {
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
			HibernateMonitoringEvent monitoringEvent,
			SharedSessionContractImplementor session,
			CachedDomainDataAccess cachedDomainDataAccess,
			EntityPersister persister,
			boolean cacheContentChanged,
			CacheActionDescription description) {
		completeCachePutEvent(
				monitoringEvent,
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
			HibernateMonitoringEvent monitoringEvent,
			SharedSessionContractImplementor session,
			CachedDomainDataAccess cachedDomainDataAccess,
			EntityPersister persister,
			boolean cacheContentChanged,
			boolean isNatualId,
			CacheActionDescription description) {
		if ( monitoringEvent != null ) {
			final CachePutEvent cachePutEvent = (CachePutEvent) monitoringEvent;
			cachePutEvent.end();
			if ( cachePutEvent.shouldCommit() ) {
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
			HibernateMonitoringEvent monitoringEvent,
			SharedSessionContractImplementor session,
			CachedDomainDataAccess cachedDomainDataAccess,
			CollectionPersister persister,
			boolean cacheContentChanged,
			CacheActionDescription description) {
		if ( monitoringEvent != null ) {
			final CachePutEvent cachePutEvent = (CachePutEvent) monitoringEvent;
			cachePutEvent.end();
			if ( cachePutEvent.shouldCommit() ) {
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
	public HibernateMonitoringEvent beginCacheGetEvent() {
		if ( cacheGetEventType.isEnabled() ) {
			final CacheGetEvent cacheGetEvent = new CacheGetEvent();
			cacheGetEvent.begin();
			return cacheGetEvent;
		}
		else {
			return null;
		}
	}

	@Override
	public void completeCacheGetEvent(
			HibernateMonitoringEvent monitoringEvent,
			SharedSessionContractImplementor session,
			Region region,
			boolean hit) {
		if ( monitoringEvent != null ) {
			final CacheGetEvent cacheGetEvent = (CacheGetEvent) monitoringEvent;
			cacheGetEvent.end();
			if ( cacheGetEvent.shouldCommit() ) {
				cacheGetEvent.sessionIdentifier = getSessionIdentifier( session );
				cacheGetEvent.regionName = region.getName();
				cacheGetEvent.hit = hit;
				cacheGetEvent.commit();
			}
		}
	}

	@Override
	public void completeCacheGetEvent(
			HibernateMonitoringEvent monitoringEvent,
			SharedSessionContractImplementor session,
			Region region,
			EntityPersister persister,
			boolean isNaturalKey,
			boolean hit) {
		if ( monitoringEvent != null ) {
			final CacheGetEvent cacheGetEvent = (CacheGetEvent) monitoringEvent;
			cacheGetEvent.end();
			if ( cacheGetEvent.shouldCommit() ) {
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
			HibernateMonitoringEvent monitoringEvent,
			SharedSessionContractImplementor session,
			Region region,
			CollectionPersister persister,
			boolean hit) {
		if ( monitoringEvent != null ) {
			final CacheGetEvent cacheGetEvent = (CacheGetEvent) monitoringEvent;
			cacheGetEvent.end();
			if ( cacheGetEvent.shouldCommit() ) {
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
		if ( flushEventType.isEnabled() ) {
			final FlushEvent flushEvent = new FlushEvent();
			flushEvent.begin();
			return flushEvent;
		}
		else {
			return null;
		}
	}

	@Override
	public void completeFlushEvent(
			HibernateMonitoringEvent flushEvent,
			org.hibernate.event.spi.FlushEvent event) {
		completeFlushEvent( flushEvent, event, false );
	}

	@Override
	public void completeFlushEvent(
			HibernateMonitoringEvent monitoringEvent,
			org.hibernate.event.spi.FlushEvent hibernateFlushEvent,
			boolean autoFlush) {
		if ( monitoringEvent != null ) {
			final FlushEvent jfrFlushEvent = (FlushEvent) monitoringEvent;
			jfrFlushEvent.end();
			if ( jfrFlushEvent.shouldCommit() ) {
				jfrFlushEvent.sessionIdentifier = getSessionIdentifier( hibernateFlushEvent.getSession() );
				jfrFlushEvent.numberOfEntitiesProcessed = hibernateFlushEvent.getNumberOfEntitiesProcessed();
				jfrFlushEvent.numberOfCollectionsProcessed = hibernateFlushEvent.getNumberOfCollectionsProcessed();
				jfrFlushEvent.isAutoFlush = autoFlush;
				jfrFlushEvent.commit();
			}
		}
	}

	@Override
	public PartialFlushEvent beginPartialFlushEvent() {
		if ( partialFlushEventType.isEnabled() ) {
			final PartialFlushEvent partialFlushEvent = new PartialFlushEvent();
			partialFlushEvent.begin();
			return partialFlushEvent;
		}
		else {
			return null;
		}
	}

	@Override
	public void completePartialFlushEvent(
			HibernateMonitoringEvent monitoringEvent,
			AutoFlushEvent hibernateAutoFlushEvent) {
		if ( monitoringEvent != null) {
			final PartialFlushEvent jfrPartialFlushEvent = (PartialFlushEvent) monitoringEvent;
			jfrPartialFlushEvent.end();
			if ( jfrPartialFlushEvent.shouldCommit() ) {
				jfrPartialFlushEvent.sessionIdentifier = getSessionIdentifier( hibernateAutoFlushEvent.getSession() );
				jfrPartialFlushEvent.numberOfEntitiesProcessed = hibernateAutoFlushEvent.getNumberOfEntitiesProcessed();
				jfrPartialFlushEvent.numberOfCollectionsProcessed = hibernateAutoFlushEvent.getNumberOfCollectionsProcessed();
				jfrPartialFlushEvent.isAutoFlush = true;
				jfrPartialFlushEvent.commit();
			}
		}
	}

	@Override
	public DirtyCalculationEvent beginDirtyCalculationEvent() {
		if ( dirtyCalculationEventType.isEnabled() ) {
			final DirtyCalculationEvent dirtyCalculationEvent = new DirtyCalculationEvent();
			dirtyCalculationEvent.begin();
			return dirtyCalculationEvent;
		}
		else {
			return null;
		}
	}

	@Override
	public void completeDirtyCalculationEvent(
			HibernateMonitoringEvent monitoringEvent,
			SharedSessionContractImplementor session,
			EntityPersister persister,
			EntityEntry entry,
			int[] dirtyProperties) {
		if ( monitoringEvent != null ) {
			final DirtyCalculationEvent dirtyCalculationEvent = (DirtyCalculationEvent) monitoringEvent;
			dirtyCalculationEvent.end();
			if ( dirtyCalculationEvent.shouldCommit() ) {
				dirtyCalculationEvent.sessionIdentifier = getSessionIdentifier( session );
				dirtyCalculationEvent.entityName = getEntityName( persister );
				dirtyCalculationEvent.entityStatus = entry.getStatus().name();
				dirtyCalculationEvent.dirty = dirtyProperties != null;
				dirtyCalculationEvent.commit();
			}
		}
	}

	@Override
	public PrePartialFlushEvent beginPrePartialFlush() {
		if ( prePartialFlushEventType.isEnabled() ) {
			final PrePartialFlushEvent partialFlushEvent = new PrePartialFlushEvent();
			partialFlushEvent.begin();
			return partialFlushEvent;
		}
		else {
			return null;
		}
	}

	@Override
	public void completePrePartialFlush(
			HibernateMonitoringEvent monitoringEvent,
			SharedSessionContractImplementor session) {
		if ( monitoringEvent != null ) {
			final PrePartialFlushEvent prePartialFlushEvent = (PrePartialFlushEvent) monitoringEvent;
			prePartialFlushEvent.end();
			if ( prePartialFlushEvent.shouldCommit() ) {
				prePartialFlushEvent.sessionIdentifier = getSessionIdentifier( session );
				prePartialFlushEvent.commit();
			}
		}
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
