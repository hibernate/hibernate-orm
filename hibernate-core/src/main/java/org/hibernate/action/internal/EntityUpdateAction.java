/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.action.internal;

import org.hibernate.AssertionFailure;
import org.hibernate.CacheMode;
import org.hibernate.HibernateException;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.engine.spi.CachedNaturalIdValueSource;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionEventListenerManager;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.EventManager;
import org.hibernate.event.spi.HibernateMonitoringEvent;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.PostCommitUpdateEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.metamodel.mapping.NaturalIdMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.stat.internal.StatsHelper;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.type.TypeHelper;

import static org.hibernate.engine.internal.Versioning.getVersion;

/**
 * The action for performing entity updates.
 */
public class EntityUpdateAction extends EntityAction {

	private final Object[] state;
	private final Object[] previousState;
	private final Object previousVersion;
	private final int[] dirtyFields;
	private final boolean hasDirtyCollection;
	private final Object rowId;

	private final NaturalIdMapping naturalIdMapping;
	private final Object previousNaturalIdValues;

	private Object nextVersion;
	private Object cacheEntry;
	private SoftLock lock;

	/**
	 * Constructs an EntityUpdateAction
	 *  @param id The entity identifier
	 * @param state The current (extracted) entity state
	 * @param dirtyProperties The indexes (in reference to state) properties with dirty state
	 * @param hasDirtyCollection Were any collections dirty?
	 * @param previousState The previous (stored) state
	 * @param previousVersion The previous (stored) version
	 * @param nextVersion The incremented version
	 * @param instance The entity instance
	 * @param rowId The entity's row id
	 * @param persister The entity's persister
	 * @param session The session
	 */
	public EntityUpdateAction(
			final Object id,
			final Object[] state,
			final int[] dirtyProperties,
			final boolean hasDirtyCollection,
			final Object[] previousState,
			final Object previousVersion,
			final Object nextVersion,
			final Object instance,
			final Object rowId,
			final EntityPersister persister,
			final EventSource session) {
		super( session, id, instance, persister );
		this.state = state;
		this.previousState = previousState;
		this.previousVersion = previousVersion;
		this.nextVersion = nextVersion;
		this.dirtyFields = dirtyProperties;
		this.hasDirtyCollection = hasDirtyCollection;
		this.rowId = rowId;

		this.naturalIdMapping = persister.getNaturalIdMapping();
		if ( naturalIdMapping == null ) {
			previousNaturalIdValues = null;
		}
		else {
			previousNaturalIdValues = determinePreviousNaturalIdValues( persister, naturalIdMapping, id, previousState, session );
			session.getPersistenceContextInternal().getNaturalIdResolutions().manageLocalResolution(
					id,
					naturalIdMapping.extractNaturalIdFromEntityState( state ),
					persister,
					CachedNaturalIdValueSource.UPDATE
			);
		}
	}

	private static Object determinePreviousNaturalIdValues(
			EntityPersister persister,
			NaturalIdMapping naturalIdMapping,
			Object id,
			Object[] previousState,
			SharedSessionContractImplementor session) {
		return previousState == null
				? session.getPersistenceContextInternal().getNaturalIdSnapshot( id, persister )
				: naturalIdMapping.extractNaturalIdFromEntityState( previousState );
	}

	public Object[] getState() {
		return state;
	}

	protected Object[] getPreviousState() {
		return previousState;
	}

	protected Object getNextVersion() {
		return nextVersion;
	}

	public int[] getDirtyFields() {
		return dirtyFields;
	}
	protected boolean hasDirtyCollection() {
		return hasDirtyCollection;
	}

	protected NaturalIdMapping getNaturalIdMapping() {
		return naturalIdMapping;
	}

	protected Object getPreviousNaturalIdValues() {
		return previousNaturalIdValues;
	}

	public Object getRowId() {
		return rowId;
	}

	protected void setLock(SoftLock lock) {
		this.lock = lock;
	}

	protected void setCacheEntry(Object cacheEntry) {
		this.cacheEntry = cacheEntry;
	}

	@Override
	public void execute() throws HibernateException {
		if ( !preUpdate() ) {
			final EntityPersister persister = getPersister();
			final SharedSessionContractImplementor session = getSession();
			final Object id = getId();
			final Object instance = getInstance();
			final Object previousVersion = getPreviousVersion();
			final Object ck = lockCacheItem( previousVersion );
			final GeneratedValues generatedValues = persister.getUpdateCoordinator().update(
					instance,
					id,
					rowId,
					state,
					previousVersion,
					previousState,
					dirtyFields,
					hasDirtyCollection,
					session
			);
			final EntityEntry entry = session.getPersistenceContextInternal().getEntry( instance );
			if ( entry == null ) {
				throw new AssertionFailure( "possible non thread safe access to session" );
			}
			handleGeneratedProperties( entry, generatedValues );
			handleDeleted( entry );
			updateCacheItem( previousVersion, ck, entry );
			handleNaturalIdResolutions( persister, session, id );
			postUpdate();

			final StatisticsImplementor statistics = session.getFactory().getStatistics();
			if ( statistics.isStatisticsEnabled() ) {
				statistics.updateEntity( getPersister().getEntityName() );
			}
		}
	}

	protected void handleNaturalIdResolutions(EntityPersister persister, SharedSessionContractImplementor session, Object id) {
		if ( naturalIdMapping != null ) {
			session.getPersistenceContextInternal().getNaturalIdResolutions().manageSharedResolution(
					id,
					naturalIdMapping.extractNaturalIdFromEntityState( state ),
					previousNaturalIdValues,
					persister,
					CachedNaturalIdValueSource.UPDATE
			);
		}
	}

	protected void updateCacheItem(Object previousVersion, Object ck, EntityEntry entry) {
		final EntityPersister persister = getPersister();
		if ( persister.canWriteToCache() ) {
			final SharedSessionContractImplementor session = getSession();
			if ( isCacheInvalidationRequired( persister, session ) || entry.getStatus() != Status.MANAGED ) {
				persister.getCacheAccessStrategy().remove( session, ck );
			}
			else if ( session.getCacheMode().isPutEnabled() ) {
				//TODO: inefficient if that cache is just going to ignore the updated state!
				final CacheEntry ce = persister.buildCacheEntry( getInstance(), state, nextVersion, getSession() );
				cacheEntry = persister.getCacheEntryStructure().structure( ce );
				final boolean put = updateCache( persister, previousVersion, ck );

				final StatisticsImplementor statistics = session.getFactory().getStatistics();
				if ( put && statistics.isStatisticsEnabled() ) {
					statistics.entityCachePut(
							StatsHelper.INSTANCE.getRootEntityRole( persister ),
							getPersister().getCacheAccessStrategy().getRegion().getName()
					);
				}
			}
		}
	}

	private static boolean isCacheInvalidationRequired(
			EntityPersister persister,
			SharedSessionContractImplementor session) {
		// the cache has to be invalidated when CacheMode is equal to GET or IGNORE
		return persister.isCacheInvalidationRequired()
			|| session.getCacheMode() == CacheMode.GET
			|| session.getCacheMode() == CacheMode.IGNORE;
	}

	private void handleGeneratedProperties(EntityEntry entry, GeneratedValues generatedValues) {
		final EntityPersister persister = getPersister();
		if ( entry.getStatus() == Status.MANAGED || persister.isVersionPropertyGenerated() ) {
			final SharedSessionContractImplementor session = getSession();
			final Object instance = getInstance();
			final Object id = getId();
			// get the updated snapshot of the entity state by cloning current state;
			// it is safe to copy in place, since by this time no-one else (should have)
			// has a reference  to the array
			TypeHelper.deepCopy(
					state,
					persister.getPropertyTypes(),
					persister.getPropertyCheckability(),
					state,
					session
			);
			if ( persister.hasUpdateGeneratedProperties() ) {
				// this entity defines property generation, so process those generated
				// values...
				persister.processUpdateGeneratedProperties( id, instance, state, generatedValues, session );
			}
			// have the entity entry doAfterTransactionCompletion post-update processing, passing it the
			// update state and the new version (if one).
			if ( persister.isVersionPropertyGenerated() ) {
				nextVersion = getVersion( state, persister );
			}
			entry.postUpdate( instance, state, nextVersion );
		}
	}

	/*
	 * Used by Hibernate Reactive
	 */
	protected void handleDeleted(EntityEntry entry) {
		if ( entry.getStatus() == Status.DELETED ) {
			final EntityMetamodel entityMetamodel = getPersister().getEntityMetamodel();
			final boolean isImpliedOptimisticLocking = !entityMetamodel.isVersioned()
					&& entityMetamodel.getOptimisticLockStyle().isAllOrDirty();
			if ( isImpliedOptimisticLocking && entry.getLoadedState() != null ) {
				// The entity will be deleted and because we are going to create a delete statement
				// that uses all the state values in the where clause, the entry state needs to be
				// updated otherwise the statement execution will not delete any row (see HHH-15218).
				entry.postUpdate( getInstance(), state, nextVersion );
			}
		}
	}

	protected Object getPreviousVersion() {
		final EntityPersister persister = getPersister();
		if ( persister.isVersionPropertyGenerated() ) {
			// we need to grab the version value from the entity, otherwise
			// we have issues with generated-version entities that may have
			// multiple actions queued during the same flush
			return persister.getVersion( getInstance() );
		}
		else {
			return previousVersion;
		}
	}

	protected Object lockCacheItem(Object previousVersion) {
		final EntityPersister persister = getPersister();
		if ( persister.canWriteToCache() ) {
			final SharedSessionContractImplementor session = getSession();
			final EntityDataAccess cache = persister.getCacheAccessStrategy();
			final Object ck = cache.generateCacheKey(
					getId(),
					persister,
					session.getFactory(),
					session.getTenantIdentifier()
			);
			lock = cache.lockItem( session, ck, previousVersion );
			return ck;
		}
		else {
			return null;
		}
	}

	protected boolean updateCache(EntityPersister persister, Object previousVersion, Object ck) {
		final SharedSessionContractImplementor session = getSession();
		final EventManager eventManager = session.getEventManager();
		final HibernateMonitoringEvent cachePutEvent = eventManager.beginCachePutEvent();
		final EntityDataAccess cacheAccessStrategy = persister.getCacheAccessStrategy();
		boolean update = false;
		try {
			session.getEventListenerManager().cachePutStart();
			update = cacheAccessStrategy.update( session, ck, cacheEntry, nextVersion, previousVersion );
			return update;
		}
		finally {
			eventManager.completeCachePutEvent(
					cachePutEvent,
					session,
					cacheAccessStrategy,
					getPersister(),
					update,
					EventManager.CacheActionDescription.ENTITY_UPDATE
			);
			session.getEventListenerManager().cachePutEnd();
		}
	}

	protected boolean preUpdate() {
		final EventListenerGroup<PreUpdateEventListener> listenerGroup
				= getFastSessionServices().eventListenerGroup_PRE_UPDATE;
		if ( listenerGroup.isEmpty() ) {
			return false;
		}
		else {
			final PreUpdateEvent event = new PreUpdateEvent(
					getInstance(),
					getId(),
					state,
					previousState,
					getPersister(),
					eventSource()
			);
			boolean veto = false;
			for ( PreUpdateEventListener listener : listenerGroup.listeners() ) {
				veto |= listener.onPreUpdate( event );
			}
			return veto;
		}
	}

	protected void postUpdate() {
		getFastSessionServices().eventListenerGroup_POST_UPDATE
				.fireLazyEventOnEachListener( this::newPostUpdateEvent, PostUpdateEventListener::onPostUpdate );
	}

	private PostUpdateEvent newPostUpdateEvent() {
		return new PostUpdateEvent(
				getInstance(),
				getId(),
				state,
				previousState,
				dirtyFields,
				getPersister(),
				eventSource()
		);
	}

	protected void postCommitUpdate(boolean success) {
		getFastSessionServices().eventListenerGroup_POST_COMMIT_UPDATE
				.fireLazyEventOnEachListener( this::newPostUpdateEvent,
						success ? PostUpdateEventListener::onPostUpdate : this::onPostCommitFailure );
	}

	private void onPostCommitFailure(PostUpdateEventListener listener, PostUpdateEvent event) {
		if ( listener instanceof PostCommitUpdateEventListener ) {
			((PostCommitUpdateEventListener) listener).onPostUpdateCommitFailed( event );
		}
		else {
			//default to the legacy implementation that always fires the event
			listener.onPostUpdate( event );
		}
	}

	@Override
	protected boolean hasPostCommitEventListeners() {
		final EventListenerGroup<PostUpdateEventListener> group
				= getFastSessionServices().eventListenerGroup_POST_COMMIT_UPDATE;
		for ( PostUpdateEventListener listener : group.listeners() ) {
			if ( listener.requiresPostCommitHandling( getPersister() ) ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void doAfterTransactionCompletion(boolean success, SharedSessionContractImplementor session) throws CacheException {
		updateCacheIfNecessary( success, session );
		postCommitUpdate( success );
	}

	private void updateCacheIfNecessary(boolean success, SharedSessionContractImplementor session) {
		final EntityPersister persister = getPersister();
		if ( persister.canWriteToCache() ) {
			final EntityDataAccess cache = persister.getCacheAccessStrategy();
			final SessionFactoryImplementor factory = session.getFactory();
			final Object ck = cache.generateCacheKey(
					getId(),
					persister,
					factory,
					session.getTenantIdentifier()

			);
			if ( cacheUpdateRequired( success, persister, session ) ) {
				cacheAfterUpdate( cache, ck, session);
			}
			else {
				cache.unlockItem( session, ck, lock );
			}
		}
	}

	private boolean cacheUpdateRequired(boolean success, EntityPersister persister, SharedSessionContractImplementor session) {
		return success
			&& cacheEntry != null
			&& !persister.isCacheInvalidationRequired()
			&& session.getCacheMode().isPutEnabled();
	}

	protected void cacheAfterUpdate(EntityDataAccess cache, Object ck, SharedSessionContractImplementor session) {
		final SessionEventListenerManager eventListenerManager = session.getEventListenerManager();
		final EventManager eventManager = session.getEventManager();
		final HibernateMonitoringEvent cachePutEvent = eventManager.beginCachePutEvent();
		boolean put = false;
		try {
			eventListenerManager.cachePutStart();
			put = cache.afterUpdate( session, ck, cacheEntry, nextVersion, previousVersion, lock );
		}
		finally {
			eventManager.completeCachePutEvent(
					cachePutEvent,
					session,
					cache,
					getPersister(),
					put,
					EventManager.CacheActionDescription.ENTITY_AFTER_UPDATE
			);
			final StatisticsImplementor statistics = session.getFactory().getStatistics();
			if ( put && statistics.isStatisticsEnabled() ) {
				statistics.entityCachePut(
						StatsHelper.INSTANCE.getRootEntityRole( getPersister() ),
						cache.getRegion().getName()
				);
			}
			eventListenerManager.cachePutEnd();
		}

	}

}
