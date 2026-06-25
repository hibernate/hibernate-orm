/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.AssertionFailure;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.PostCommitDeleteEventListener;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PreDeleteEvent;
import org.hibernate.jpa.event.spi.CallbackType;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.stat.internal.StatsHelper;

/**
 * The action for performing an entity deletion.
 */
public class EntityDeleteAction extends EntityAction {
	@Nullable
	private final Object version;
	@Nullable
	private final Object[] state;

	private final boolean isCascadeDeleteEnabled;

	@Nullable
	private SoftLock lock;

	@Nullable
	private Object naturalIdValues;

	/**
	 * Constructs an EntityDeleteAction.
	 *
	 * @param id The entity identifier
	 * @param state The current (extracted) entity state
	 * @param version The current entity version
	 * @param instance The entity instance
	 * @param persister The entity persister
	 * @param isCascadeDeleteEnabled Whether cascade delete is enabled
	 * @param session The session
	 */
	public EntityDeleteAction(
			final @Nonnull Object id,
			final @Nullable Object[] state,
			final @Nullable Object version,
			final @Nonnull Object instance,
			final @Nonnull EntityPersister persister,
			final boolean isCascadeDeleteEnabled,
			final @Nonnull EventSource session) {
		super( session, id, instance, persister );
		assert id != null;
		assert instance != null;
		this.version = version;
		this.isCascadeDeleteEnabled = isCascadeDeleteEnabled;
		this.state = state;
	}

	/**
	 * Constructs an EntityDeleteAction for an unloaded proxy.
	 *
	 * @param id The entity identifier
	 * @param persister The entity persister
	 * @param session The session
	 */
	public EntityDeleteAction(
			final @Nonnull Object id,
			final @Nonnull EntityPersister persister,
			final @Nonnull EventSource session) {
		super( session, id, null, persister );
		assert id != null;
		version = null;
		isCascadeDeleteEnabled = false;
		state = null;
	}

	@Override
	@Nonnull
	public Object getId() {
		final Object id = super.getId();
		assert id != null;
		return id;
	}

	@Nullable
	public Object getVersion() {
		return version;
	}

	public boolean isCascadeDeleteEnabled() {
		return isCascadeDeleteEnabled;
	}

	@Nullable
	public Object[] getState() {
		return state;
	}

	@Nullable
	private Object getNaturalIdValues() {
		return naturalIdValues;
	}

	@Nullable
	protected SoftLock getLock() {
		return lock;
	}

	protected void setLock(@Nullable SoftLock lock) {
		this.lock = lock;
	}

	private boolean isInstanceLoaded() {
		// A null instance signals that we're deleting an unloaded proxy.
		return getInstance() != null;
	}

	/**
	 * @deprecated Legacy ActionQueue artifact.  Execution in the new queue is based on
	 * decomposition into individual SQL statements.
	 */
	@Override
	@Deprecated(since = "8.0")
	public void execute() {
		final Object id = getId();
		final Object version = getCurrentVersion();
		final var persister = getPersister();
		final var session = getSession();
		final Object instance = getInstance();
		assert instance != null;

		final boolean veto = isInstanceLoaded() && preDelete();

		handleNaturalIdLocalResolutions();

		final Object cacheKey = lockCacheItem();

		if ( !isCascadeDeleteEnabled && !veto ) {
			final var eventMonitor = session.getEventMonitor();
			final var event = eventMonitor.beginEntityDeleteEvent();
			boolean success = false;
			try {
				persister.getDeleteCoordinator().delete( instance, id, version, session );
				success = true;
			}
			finally {
				eventMonitor.completeEntityDeleteEvent( event, id, persister.getEntityName(), success, session );
			}
		}

		if ( isInstanceLoaded() ) {
			postDeleteLoaded( id, persister, session, instance, cacheKey );
		}
		else {
			// we're deleting an unloaded proxy
			postDeleteUnloaded( id, persister, session, cacheKey );
		}

		final var statistics = session.getFactory().getStatistics();
		if ( statistics.isStatisticsEnabled() && !veto ) {
			statistics.deleteEntity( persister.getEntityName() );
		}
	}

	private void handleNaturalIdLocalResolutions() {
		final var persister = getPersister();
		final var context = getSession().getPersistenceContextInternal();
		final var naturalIdMapping = persister.getNaturalIdMapping();
		if ( naturalIdMapping != null ) {
			naturalIdValues = context.getNaturalIdResolutions()
					.removeLocalResolution(
							getId(),
							naturalIdMapping.extractNaturalIdFromEntityState( state ),
							persister
					);
		}
	}

	@Nullable
	protected Object getCurrentVersion() {
		final var persister = getPersister();
		return persister.isVersionPropertyGenerated()
			// skip if we're deleting an unloaded proxy, no need for the version
			&& isInstanceLoaded()
				// we need to grab the version value from the entity, otherwise
				// we have issues with generated-version entities that may have
				// multiple actions queued during the same flush
				? persister.getVersion( getInstance() )
				: version;
	}

	protected void postDeleteLoaded(
			@Nonnull Object id,
			@Nonnull EntityPersister persister,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull Object instance,
			@Nullable Object cacheKey) {
		// After actually deleting a row, record the fact that the instance no longer
		// exists on the database (needed for identity-column key generation), and
		// remove it from the session cache
		final var persistenceContext = session.getPersistenceContextInternal();
		final var entry = persistenceContext.removeEntry( instance );
		if ( entry == null ) {
			throw new AssertionFailure( "possible non-threadsafe access to session" );
		}
		entry.postDelete();
		final var key = entry.getEntityKey();
		persistenceContext.removeEntityHolder( key );
		removeCacheItem( cacheKey );
		persistenceContext.getNaturalIdResolutions()
				.removeSharedResolution( id, naturalIdValues, persister, true );
		postDelete();
	}

	protected void postDeleteUnloaded(
			@Nonnull Object id,
			@Nonnull EntityPersister persister,
			@Nonnull SharedSessionContractImplementor session,
			@Nullable Object cacheKey) {
		final var persistenceContext = session.getPersistenceContextInternal();
		final var key = session.generateEntityKey( id, persister );
		if ( !persistenceContext.containsDeletedUnloadedEntityKey( key ) ) {
			throw new AssertionFailure( "deleted proxy should be for an unloaded entity: " + key );
		}
		persistenceContext.removeProxy( key );
		removeCacheItem( cacheKey );
	}

	private boolean preDelete() {
		if ( isInstanceLoaded() ) {
			final var callbacks = getPersister().getEntityCallbacks();
			if ( callbacks.hasRegisteredCallbacks( CallbackType.PRE_DELETE ) ) {
				final Object instance = getInstance();
				assert instance != null;
				eventSource().runEntityLifecycleCallback( () -> callbacks.preDelete( instance ) );
			}
		}

		final var listenerGroup = getEventListenerGroups().eventListenerGroup_PRE_DELETE;
		if ( listenerGroup.isEmpty() ) {
			return false;
		}
		else {
			final Object instance = getInstance();
			assert instance != null;
			final var event = new PreDeleteEvent( instance, getId(), state, getPersister(), eventSource() );
			boolean veto = false;
			for ( var listener : listenerGroup.listeners() ) {
				veto |= listener.onPreDelete( event );
			}
			return veto;
		}
	}

	private void postDelete() {
		getEventListenerGroups().eventListenerGroup_POST_DELETE
				.fireLazyEventOnEachListener( this::newPostDeleteEvent, PostDeleteEventListener::onPostDelete );
	}

	@Nonnull
	private PostDeleteEvent newPostDeleteEvent() {
		final Object instance = getInstance();
		assert instance != null;
		return new PostDeleteEvent( instance, getId(), state, getPersister(), eventSource() );
	}

	private void postCommitDelete(boolean success) {
		final var eventListeners = getEventListenerGroups().eventListenerGroup_POST_COMMIT_DELETE;
		if ( success ) {
			eventListeners.fireLazyEventOnEachListener( this::newPostDeleteEvent, PostDeleteEventListener::onPostDelete );
		}
		else {
			eventListeners.fireLazyEventOnEachListener( this::newPostDeleteEvent, EntityDeleteAction::postCommitDeleteOnUnsuccessful );
		}
	}

	private static void postCommitDeleteOnUnsuccessful(
			@Nonnull PostDeleteEventListener listener,
			@Nonnull PostDeleteEvent event) {
		if ( listener instanceof PostCommitDeleteEventListener postCommitDeleteEventListener ) {
			postCommitDeleteEventListener.onPostDeleteCommitFailed( event );
		}
		else {
			//default to the legacy implementation that always fires the event
			listener.onPostDelete( event );
		}
	}

	@Override
	public void doAfterTransactionCompletion(boolean success, @Nonnull SharedSessionContractImplementor session) {
		unlockCacheItem();
		postCommitDelete( success );
	}

	@Override
	protected boolean hasPostCommitEventListeners() {
		for ( var listener: getEventListenerGroups().eventListenerGroup_POST_COMMIT_DELETE.listeners() ) {
			if ( listener.requiresPostCommitHandling( getPersister() ) ) {
				return true;
			}
		}
		return false;
	}

	@Nullable
	private Object lockCacheItem() {
		final var persister = getPersister();
		if ( persister.canWriteToCache() ) {
			final var cache = persister.getCacheAccessStrategy();
			assert cache != null;
			final var session = getSession();
			final Object cacheKey = cache.generateCacheKey(
					getId(),
					persister,
					session.getFactory(),
					session.getTenantIdentifier()
			);
			lock = cache.lockItem( session, cacheKey, getCurrentVersion() );
			return cacheKey;
		}
		else {
			return null;
		}
	}

	protected void unlockCacheItem() {
		final var persister = getPersister();
		if ( persister.canWriteToCache() ) {
			final var cache = persister.getCacheAccessStrategy();
			assert cache != null;
			final var session = getSession();
			final Object cacheKey = cache.generateCacheKey(
					getId(),
					persister,
					session.getFactory(),
					session.getTenantIdentifier()
			);
			cache.unlockItem( session, cacheKey, lock );
		}
	}

	protected void removeCacheItem(@Nullable Object cacheKey) {
		final var persister = getPersister();
		if ( persister.canWriteToCache() ) {
			final var cache = persister.getCacheAccessStrategy();
			assert cache != null;
			assert cacheKey != null;
			cache.remove( getSession(), cacheKey );

			final var statistics = getSession().getFactory().getStatistics();
			if ( statistics.isStatisticsEnabled() ) {
				statistics.entityCacheRemove(
						StatsHelper.getRootEntityRole( persister ),
						cache.getRegion().getName()
				);
			}
		}
	}
}
