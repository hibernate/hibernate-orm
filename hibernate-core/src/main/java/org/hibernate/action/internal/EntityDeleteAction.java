/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.internal;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.monitor.spi.EventMonitor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.monitor.spi.DiagnosticEvent;
import org.hibernate.event.spi.PostCommitDeleteEventListener;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PreDeleteEvent;
import org.hibernate.event.spi.PreDeleteEventListener;
import org.hibernate.metamodel.mapping.NaturalIdMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.stat.spi.StatisticsImplementor;

/**
 * The action for performing an entity deletion.
 */
public class EntityDeleteAction extends EntityAction {
	private final Object version;
	private final boolean isCascadeDeleteEnabled;
	private final Object[] state;

	private SoftLock lock;

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
			final Object id,
			final Object[] state,
			final Object version,
			final Object instance,
			final EntityPersister persister,
			final boolean isCascadeDeleteEnabled,
			final EventSource session) {
		super( session, id, instance, persister );
		this.version = version;
		this.isCascadeDeleteEnabled = isCascadeDeleteEnabled;
		this.state = state;

		final NaturalIdMapping naturalIdMapping = persister.getNaturalIdMapping();
		if ( naturalIdMapping != null ) {
			naturalIdValues = session.getPersistenceContextInternal().getNaturalIdResolutions()
					.removeLocalResolution(
							getId(),
							naturalIdMapping.extractNaturalIdFromEntityState( state ),
							getPersister()
					);
		}
	}

	/**
	 * Constructs an EntityDeleteAction for an unloaded proxy.
	 *
	 * @param id The entity identifier
	 * @param persister The entity persister
	 * @param session The session
	 */
	public EntityDeleteAction(final Object id, final EntityPersister persister, final EventSource session) {
		super( session, id, null, persister );
		version = null;
		isCascadeDeleteEnabled = false;
		state = null;
	}

	public Object getVersion() {
		return version;
	}

	public boolean isCascadeDeleteEnabled() {
		return isCascadeDeleteEnabled;
	}

	public Object[] getState() {
		return state;
	}

	protected Object getNaturalIdValues() {
		return naturalIdValues;
	}

	protected SoftLock getLock() {
		return lock;
	}

	protected void setLock(SoftLock lock) {
		this.lock = lock;
	}

	private boolean isInstanceLoaded() {
		// A null instance signals that we're deleting an unloaded proxy.
		return getInstance() != null;
	}

	@Override
	public void execute() throws HibernateException {
		final Object id = getId();
		final Object version = getCurrentVersion();
		final EntityPersister persister = getPersister();
		final SharedSessionContractImplementor session = getSession();
		final Object instance = getInstance();

		final boolean veto = isInstanceLoaded() && preDelete();

		final Object ck = lockCacheItem();

		if ( !isCascadeDeleteEnabled && !veto ) {
			final EventMonitor eventMonitor = session.getEventMonitor();
			final DiagnosticEvent event = eventMonitor.beginEntityDeleteEvent();
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
			postDeleteLoaded( id, persister, session, instance, ck );
		}
		else {
			// we're deleting an unloaded proxy
			postDeleteUnloaded( id, persister, session, ck );
		}

		final StatisticsImplementor statistics = getSession().getFactory().getStatistics();
		if ( statistics.isStatisticsEnabled() && !veto ) {
			statistics.deleteEntity( getPersister().getEntityName() );
		}
	}

	protected Object getCurrentVersion() {
		return getPersister().isVersionPropertyGenerated()
						// skip if we're deleting an unloaded proxy, no need for the version
						&& isInstanceLoaded()
				// we need to grab the version value from the entity, otherwise
				// we have issues with generated-version entities that may have
				// multiple actions queued during the same flush
				? getPersister().getVersion( getInstance() )
				: version;
	}

	protected void postDeleteLoaded(
			Object id,
			EntityPersister persister,
			SharedSessionContractImplementor session,
			Object instance,
			Object ck) {
		// After actually deleting a row, record the fact that the instance no longer
		// exists on the database (needed for identity-column key generation), and
		// remove it from the session cache
		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
		final EntityEntry entry = persistenceContext.removeEntry( instance );
		if ( entry == null ) {
			throw new AssertionFailure( "possible non-threadsafe access to session" );
		}
		entry.postDelete();
		final EntityKey key = entry.getEntityKey();
		persistenceContext.removeEntityHolder( key );
		removeCacheItem( ck );
		persistenceContext.getNaturalIdResolutions().removeSharedResolution( id, naturalIdValues, persister, true);
		postDelete();
	}

	protected void postDeleteUnloaded(Object id, EntityPersister persister, SharedSessionContractImplementor session, Object ck) {
		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
		final EntityKey key = session.generateEntityKey( id, persister );
		if ( !persistenceContext.containsDeletedUnloadedEntityKey( key ) ) {
			throw new AssertionFailure( "deleted proxy should be for an unloaded entity: " + key );
		}
		persistenceContext.removeProxy( key );
		removeCacheItem( ck );
	}

	protected boolean preDelete() {
		final EventListenerGroup<PreDeleteEventListener> listenerGroup
				= getEventListenerGroups().eventListenerGroup_PRE_DELETE;
		if ( listenerGroup.isEmpty() ) {
			return false;
		}
		else {
			final PreDeleteEvent event = new PreDeleteEvent( getInstance(), getId(), state, getPersister(), eventSource() );
			boolean veto = false;
			for ( PreDeleteEventListener listener : listenerGroup.listeners() ) {
				veto |= listener.onPreDelete( event );
			}
			return veto;
		}
	}

	protected void postDelete() {
		getEventListenerGroups().eventListenerGroup_POST_DELETE
				.fireLazyEventOnEachListener( this::newPostDeleteEvent, PostDeleteEventListener::onPostDelete );
	}

	PostDeleteEvent newPostDeleteEvent() {
		return new PostDeleteEvent( getInstance(), getId(), state, getPersister(), eventSource() );
	}

	protected void postCommitDelete(boolean success) {
		final EventListenerGroup<PostDeleteEventListener> eventListeners
				= getEventListenerGroups().eventListenerGroup_POST_COMMIT_DELETE;
		if (success) {
			eventListeners.fireLazyEventOnEachListener( this::newPostDeleteEvent, PostDeleteEventListener::onPostDelete );
		}
		else {
			eventListeners.fireLazyEventOnEachListener( this::newPostDeleteEvent, EntityDeleteAction::postCommitDeleteOnUnsuccessful );
		}
	}

	private static void postCommitDeleteOnUnsuccessful(PostDeleteEventListener listener, PostDeleteEvent event) {
		if ( listener instanceof PostCommitDeleteEventListener ) {
			( (PostCommitDeleteEventListener) listener ).onPostDeleteCommitFailed( event );
		}
		else {
			//default to the legacy implementation that always fires the event
			listener.onPostDelete( event );
		}
	}

	@Override
	public void doAfterTransactionCompletion(boolean success, SharedSessionContractImplementor session) throws HibernateException {
		unlockCacheItem();
		postCommitDelete( success );
	}

	@Override
	protected boolean hasPostCommitEventListeners() {
		for ( PostDeleteEventListener listener: getEventListenerGroups().eventListenerGroup_POST_COMMIT_DELETE.listeners() ) {
			if ( listener.requiresPostCommitHandling( getPersister() ) ) {
				return true;
			}
		}
		return false;
	}

	protected Object lockCacheItem() {
		final EntityPersister persister = getPersister();
		if ( persister.canWriteToCache() ) {
			final EntityDataAccess cache = persister.getCacheAccessStrategy();
			final SharedSessionContractImplementor session = getSession();
			final Object ck = cache.generateCacheKey(
					getId(),
					persister,
					session.getFactory(),
					session.getTenantIdentifier()
			);
			lock = cache.lockItem( session, ck, getCurrentVersion() );
			return ck;
		}
		else {
			return null;
		}
	}

	protected void unlockCacheItem() {
		final EntityPersister persister = getPersister();
		if ( persister.canWriteToCache() ) {
			final EntityDataAccess cache = persister.getCacheAccessStrategy();
			final SharedSessionContractImplementor session = getSession();
			final Object ck = cache.generateCacheKey(
					getId(),
					persister,
					session.getFactory(),
					session.getTenantIdentifier()
			);
			cache.unlockItem( session, ck, lock );
		}
	}

	protected void removeCacheItem(Object ck) {
		final EntityPersister persister = getPersister();
		if ( persister.canWriteToCache() ) {
			persister.getCacheAccessStrategy().remove( getSession(), ck );
		}
	}
}
