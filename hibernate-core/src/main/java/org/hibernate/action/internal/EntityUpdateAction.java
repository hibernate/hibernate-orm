/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.AssertionFailure;
import org.hibernate.CacheMode;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.CachedNaturalIdValueSource;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.monitor.spi.EventMonitor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.PostCommitUpdateEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.metamodel.mapping.NaturalIdMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.stat.internal.StatsHelper;
import org.hibernate.type.TypeHelper;

import static org.hibernate.cache.spi.entry.CacheEntryHelper.buildStructuredCacheEntry;
import static org.hibernate.engine.internal.CacheHelper.writingToCache;
import static org.hibernate.engine.internal.Versioning.getVersion;

/**
 * The action for performing entity updates.
 */
public class EntityUpdateAction extends EntityAction {

	private final Object[] state;
	@Nullable
	private final Object[] previousState;
	@Nullable
	private final Object previousVersion;
	@Nullable
	private final int[] dirtyFields;
	private final boolean hasDirtyCollection;
	@Nullable
	private final Object rowId;

	@Nullable
	private final Object previousNaturalIdValues;

	@Nullable
	private Object nextVersion;
	@Nullable
	private Object cacheEntry;
	@Nullable
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
			final @Nonnull Object id,
			final @Nonnull Object[] state,
			final @Nullable int[] dirtyProperties,
			final boolean hasDirtyCollection,
			final @Nullable Object[] previousState,
			final @Nullable Object previousVersion,
			final @Nullable Object nextVersion,
			final @Nonnull Object instance,
			final @Nullable Object rowId,
			final @Nonnull EntityPersister persister,
			final @Nonnull EventSource session) {
		super( session, id, instance, persister );
		assert state != null;
		assert instance != null;
		assert id != null;
		this.state = state;
		this.previousState = previousState;
		this.previousVersion = previousVersion;
		this.nextVersion = nextVersion;
		this.dirtyFields = dirtyProperties;
		this.hasDirtyCollection = hasDirtyCollection;
		this.rowId = rowId;

		final var naturalIdMapping = persister.getNaturalIdMapping();
		this.previousNaturalIdValues =
				naturalIdMapping == null
						? null
						: determinePreviousNaturalIdValues( persister, naturalIdMapping, id, previousState, session );
	}

	@Nullable
	private static Object determinePreviousNaturalIdValues(
			@Nonnull EntityPersister persister,
			@Nonnull NaturalIdMapping naturalIdMapping,
			@Nonnull Object id,
			@Nullable Object[] previousState,
			@Nonnull SharedSessionContractImplementor session) {
		return previousState == null
				? session.getPersistenceContextInternal().getNaturalIdSnapshot( id, persister )
				: naturalIdMapping.extractNaturalIdFromEntityState( previousState );
	}

	@Override
	@Nonnull
	public Object getInstance() {
		final Object instance = super.getInstance();
		assert instance != null;
		return instance;
	}

	@Override
	@Nonnull
	public Object getId() {
		final Object id = super.getId();
		assert id != null;
		return id;
	}

	@Nonnull
	public Object[] getState() {
		return state;
	}

	@Nullable
	public Object[] getPreviousState() {
		return previousState;
	}

	@Nullable
	public Object getNextVersion() {
		return nextVersion;
	}

	@Nullable
	public int[] getDirtyFields() {
		return dirtyFields;
	}
	public boolean hasDirtyCollection() {
		return hasDirtyCollection;
	}

	@Nullable
	protected NaturalIdMapping getNaturalIdMapping() {
		return getPersister().getNaturalIdMapping();
	}

	@Nullable
	protected Object getPreviousNaturalIdValues() {
		return previousNaturalIdValues;
	}

	@Nullable
	public Object getRowId() {
		return rowId;
	}

	protected void setLock(@Nullable SoftLock lock) {
		this.lock = lock;
	}

	protected void setCacheEntry(@Nullable Object cacheEntry) {
		this.cacheEntry = cacheEntry;
	}

	@Override
	public void execute() {
		if ( !preUpdate() ) {
			final var persister = getPersister();
			final var session = getSession();
			final var persistenceContext = session.getPersistenceContextInternal();
			final Object id = getId();
			final Object instance = getInstance();

			handleNaturalIdLocalResolutions( id, persister, persistenceContext );

			final Object previousVersion = getPreviousVersion();
			final Object cacheKey = lockCacheItem( previousVersion );
			final var eventMonitor = session.getEventMonitor();
			final var event = eventMonitor.beginEntityUpdateEvent();
			boolean success = false;
			final GeneratedValues generatedValues;
			try {
				generatedValues = persister.getUpdateCoordinator().update(
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
				success = true;
			}
			finally {
				eventMonitor.completeEntityUpdateEvent( event, id, persister.getEntityName(), success, session );
			}
			final var entry = session.getPersistenceContextInternal().getEntry( instance );
			if ( entry == null ) {
				throw new AssertionFailure( "possible non thread safe access to session" );
			}
			handleGeneratedProperties( entry, generatedValues );
			handleDeleted( entry );
			updateCacheItem( previousVersion, cacheKey, entry );
			handleNaturalIdSharedResolutions( id, persister, persistenceContext );
			postUpdate();

			final var statistics = session.getFactory().getStatistics();
			if ( statistics.isStatisticsEnabled() ) {
				statistics.updateEntity( getPersister().getEntityName() );
			}
		}
	}

	public void handleNaturalIdLocalResolutions(
			@Nonnull Object id,
			@Nonnull EntityPersister persister,
			@Nonnull PersistenceContext context) {
		final var naturalIdMapping = persister.getNaturalIdMapping();
		if ( naturalIdMapping != null) {
			context.getNaturalIdResolutions().manageLocalResolution(
					id,
					naturalIdMapping.extractNaturalIdFromEntityState( state ),
					persister,
					CachedNaturalIdValueSource.UPDATE
			);
		}
	}

	public void handleNaturalIdSharedResolutions(
			@Nonnull Object id,
			@Nonnull EntityPersister persister,
			@Nonnull PersistenceContext context) {
		final var naturalIdMapping = persister.getNaturalIdMapping();
		if ( naturalIdMapping != null ) {
			context.getNaturalIdResolutions().manageSharedResolution(
					id,
					naturalIdMapping.extractNaturalIdFromEntityState( state ),
					previousNaturalIdValues,
					persister,
					CachedNaturalIdValueSource.UPDATE
			);
		}
	}

	public void updateCacheItem(
			@Nullable Object previousVersion,
			@Nullable Object cacheKey,
			@Nonnull EntityEntry entry) {
		final var persister = getPersister();
		writingToCache( persister, cache -> {
			final var session = getSession();
			assert cacheKey != null;
			if ( isCacheInvalidationRequired( persister, session ) || entry.getStatus() != Status.MANAGED ) {
				cache.remove( session, cacheKey );
			}
			else if ( session.getCacheMode().isPutEnabled() ) {
				//TODO: inefficient if that cache is just going to ignore the updated state!
				cacheEntry = buildStructuredCacheEntry( getInstance(), nextVersion, state, persister, session );
				final boolean put = updateCache( cache, persister, previousVersion, cacheKey );

				final var statistics = session.getFactory().getStatistics();
				if ( put && statistics.isStatisticsEnabled() ) {
					statistics.entityCachePut(
							StatsHelper.getRootEntityRole( persister ),
							cache.getRegion().getName()
					);
				}
			}
		} );
	}

	private static boolean isCacheInvalidationRequired(
			@Nonnull EntityPersister persister,
			@Nonnull SharedSessionContractImplementor session) {
		// the cache has to be invalidated when CacheMode is equal to GET or IGNORE
		return persister.isCacheInvalidationRequired()
			|| session.getCacheMode() == CacheMode.GET
			|| session.getCacheMode() == CacheMode.IGNORE;
	}

	public void handleGeneratedProperties(@Nonnull EntityEntry entry, @Nullable GeneratedValues generatedValues) {
		final var persister = getPersister();
		if ( entry.getStatus() == Status.MANAGED || persister.isVersionPropertyGenerated() ) {
			final var session = getSession();
			final Object instance = getInstance();
			final Object id = getId();
			// get the updated snapshot of the entity state by cloning current state;
			// it is safe to copy in place, since by this time no-one else should
			// have a reference to the array
			TypeHelper.deepCopy(
					state,
					persister.getPropertyTypes(),
					persister.getPropertyCheckability(),
					state,
					session
			);
			if ( persister.hasUpdateGeneratedProperties() ) {
				// this entity defines property generation, so process those generated values
				persister.processUpdateGeneratedProperties( id, instance, state, generatedValues, session );
			}
			// have the entity entry doAfterTransactionCompletion post-update processing,
			// passing it the update state and the new version (if there is one)
			if ( persister.isVersionPropertyGenerated() ) {
				nextVersion = getVersion( state, persister );
			}
			entry.postUpdate( instance, state, nextVersion );
			entry.setMaybeLazySet( null );
		}
	}

	/*
	 * Used by Hibernate Reactive
	 */
	public void handleDeleted(@Nonnull EntityEntry entry) {
		if ( entry.getStatus() == Status.DELETED ) {
			final var entityMetamodel = getPersister();
			final boolean isImpliedOptimisticLocking = !entityMetamodel.isVersioned()
					&& entityMetamodel.optimisticLockStyle().isAllOrDirty();
			if ( isImpliedOptimisticLocking && entry.getLoadedState() != null ) {
				// The entity will be deleted, and because we are going to create a delete statement
				// that uses all the state values in the where clause, the entry state needs to be
				// updated. Otherwise, the statement execution will not delete any row (see HHH-15218).
				entry.postUpdate( getInstance(), state, nextVersion );
			}
		}
	}

	@Nullable
	public Object getPreviousVersion() {
		final var persister = getPersister();
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

	@Nullable
	public Object lockCacheItem(@Nullable Object previousVersion) {
		final var persister = getPersister();
		return writingToCache( persister, cache -> {
			final var session = getSession();
			final Object cacheKey = cache.generateCacheKey(
					getId(),
					persister,
					session.getFactory(),
					session.getTenantIdentifier()
			);
			lock = cache.lockItem( session, cacheKey, previousVersion );
			return cacheKey;
		}, null );
	}

	protected boolean updateCache(
			@Nonnull EntityDataAccess cache,
			@Nonnull EntityPersister persister,
			@Nullable Object previousVersion,
			@Nonnull Object cacheKey) {
		final var session = getSession();
		final var eventMonitor = session.getEventMonitor();
		final var cachePutEvent = eventMonitor.beginCachePutEvent();
		final var eventListenerManager = session.getEventListenerManager();
		boolean update = false;
		try {
			eventListenerManager.cachePutStart();
			assert cacheEntry != null;
			update = cache.update( session, cacheKey, cacheEntry, nextVersion, previousVersion );
			return update;
		}
		finally {
			eventMonitor.completeCachePutEvent(
					cachePutEvent,
					session,
					cache,
					persister,
					update,
					EventMonitor.CacheActionDescription.ENTITY_UPDATE
			);
			eventListenerManager.cachePutEnd();
		}
	}

	protected boolean preUpdate() {
		final var listenerGroup = getEventListenerGroups().eventListenerGroup_PRE_UPDATE;
		if ( listenerGroup.isEmpty() ) {
			return false;
		}
		else {
			final var event = new PreUpdateEvent( getInstance(), getId(), state, previousState, getPersister(), eventSource() );
			boolean veto = false;
			for ( var listener : listenerGroup.listeners() ) {
				veto |= listener.onPreUpdate( event );
			}
			return veto;
		}
	}

	public void postUpdate() {
		getEventListenerGroups().eventListenerGroup_POST_UPDATE
				.fireLazyEventOnEachListener( this::newPostUpdateEvent, PostUpdateEventListener::onPostUpdate );
	}

	@Nonnull
	private PostUpdateEvent newPostUpdateEvent() {
		return new PostUpdateEvent( getInstance(), getId(), state, previousState, dirtyFields, getPersister(), eventSource() );
	}

	protected void postCommitUpdate(boolean success) {
		getEventListenerGroups().eventListenerGroup_POST_COMMIT_UPDATE
				.fireLazyEventOnEachListener( this::newPostUpdateEvent,
						success ? PostUpdateEventListener::onPostUpdate : this::onPostCommitFailure );
	}

	private void onPostCommitFailure(@Nonnull PostUpdateEventListener listener, @Nonnull PostUpdateEvent event) {
		if ( listener instanceof PostCommitUpdateEventListener postCommitUpdateEventListener ) {
			postCommitUpdateEventListener.onPostUpdateCommitFailed( event );
		}
		else {
			//default to the legacy implementation that always fires the event
			listener.onPostUpdate( event );
		}
	}

	@Override
	protected boolean hasPostCommitEventListeners() {
		final var group = getEventListenerGroups().eventListenerGroup_POST_COMMIT_UPDATE;
		for ( var listener : group.listeners() ) {
			if ( listener.requiresPostCommitHandling( getPersister() ) ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void doAfterTransactionCompletion(boolean success, @Nonnull SharedSessionContractImplementor session)
			throws CacheException {
		updateCacheIfNecessary( success, session );
		postCommitUpdate( success );
	}

	private void updateCacheIfNecessary(boolean success, @Nonnull SharedSessionContractImplementor session) {
		final var persister = getPersister();
		writingToCache( persister, cache -> {
			final var factory = session.getFactory();
			final Object cacheKey = cache.generateCacheKey(
					getId(),
					persister,
					factory,
					session.getTenantIdentifier()

			);
			if ( cacheUpdateRequired( success, persister, session ) ) {
				cacheAfterUpdate( cache, cacheKey, session );
			}
			else {
				cache.unlockItem( session, cacheKey, lock );
			}
		} );
	}

	private boolean cacheUpdateRequired(
			boolean success,
			@Nonnull EntityPersister persister,
			@Nonnull SharedSessionContractImplementor session) {
		return success
			&& cacheEntry != null
			&& !persister.isCacheInvalidationRequired()
			&& session.getCacheMode().isPutEnabled();
	}

	protected void cacheAfterUpdate(
			@Nonnull EntityDataAccess cache,
			@Nonnull Object cacheKey,
			@Nonnull SharedSessionContractImplementor session) {
		final var eventListenerManager = session.getEventListenerManager();
		final var eventMonitor = session.getEventMonitor();
		final var cachePutEvent = eventMonitor.beginCachePutEvent();
		boolean put = false;
		try {
			eventListenerManager.cachePutStart();
			assert cacheEntry != null;
			put = cache.afterUpdate( session, cacheKey, cacheEntry, nextVersion, previousVersion, lock );
		}
		finally {
			final var persister = getPersister();
			eventMonitor.completeCachePutEvent(
					cachePutEvent,
					session,
					cache,
					persister,
					put,
					EventMonitor.CacheActionDescription.ENTITY_AFTER_UPDATE
			);
			final var statistics = session.getFactory().getStatistics();
			if ( put && statistics.isStatisticsEnabled() ) {
				statistics.entityCachePut(
						StatsHelper.getRootEntityRole( persister ),
						cache.getRegion().getName()
				);
			}
			eventListenerManager.cachePutEnd();
		}
	}

}
