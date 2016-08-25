/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.action.internal;

import java.io.Serializable;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.engine.internal.Versioning;
import org.hibernate.engine.spi.CachedNaturalIdValueSource;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionEventListenerManager;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostCommitUpdateEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.TypeHelper;

/**
 * The action for performing entity updates.
 */
public final class EntityUpdateAction extends EntityAction {
	private final Object[] state;
	private final Object[] previousState;
	private final Object previousVersion;
	private final int[] dirtyFields;
	private final boolean hasDirtyCollection;
	private final Object rowId;
	private final Object[] previousNaturalIdValues;
	private Object nextVersion;
	private Object cacheEntry;
	private SoftLock lock;

	/**
	 * Constructs an EntityUpdateAction
	 *
	 * @param id The entity identifier
	 * @param state The current (extracted) entity state
	 * @param dirtyProperties The indexes (in reference to state) properties with dirty state
	 * @param hasDirtyCollection Were any collections dirty?
	 * @param previousState The previous (stored) state
	 * @param previousVersion The previous (stored) version
	 * @param nextVersion The incremented version
	 * @param instance The entity instance
	 * @param rowId The entity's rowid
	 * @param persister The entity's persister
	 * @param session The session
	 */
	public EntityUpdateAction(
			final Serializable id,
			final Object[] state,
			final int[] dirtyProperties,
			final boolean hasDirtyCollection,
			final Object[] previousState,
			final Object previousVersion,
			final Object nextVersion,
			final Object instance,
			final Object rowId,
			final EntityPersister persister,
			final SharedSessionContractImplementor session) {
		super( session, id, instance, persister );
		this.state = state;
		this.previousState = previousState;
		this.previousVersion = previousVersion;
		this.nextVersion = nextVersion;
		this.dirtyFields = dirtyProperties;
		this.hasDirtyCollection = hasDirtyCollection;
		this.rowId = rowId;

		this.previousNaturalIdValues = determinePreviousNaturalIdValues( persister, previousState, session, id );
		session.getPersistenceContext().getNaturalIdHelper().manageLocalNaturalIdCrossReference(
				persister,
				id,
				state,
				previousNaturalIdValues,
				CachedNaturalIdValueSource.UPDATE
		);
	}

	private Object[] determinePreviousNaturalIdValues(
			EntityPersister persister,
			Object[] previousState,
			SharedSessionContractImplementor session,
			Serializable id) {
		if ( ! persister.hasNaturalIdentifier() ) {
			return null;
		}

		if ( previousState != null ) {
			return session.getPersistenceContext().getNaturalIdHelper().extractNaturalIdValues( previousState, persister );
		}

		return session.getPersistenceContext().getNaturalIdSnapshot( id, persister );
	}

	@Override
	public void execute() throws HibernateException {
		final Serializable id = getId();
		final EntityPersister persister = getPersister();
		final SharedSessionContractImplementor session = getSession();
		final Object instance = getInstance();

		final boolean veto = preUpdate();

		final SessionFactoryImplementor factory = session.getFactory();
		Object previousVersion = this.previousVersion;
		if ( persister.isVersionPropertyGenerated() ) {
			// we need to grab the version value from the entity, otherwise
			// we have issues with generated-version entities that may have
			// multiple actions queued during the same flush
			previousVersion = persister.getVersion( instance );
		}
		
		final Object ck;
		if ( persister.hasCache() ) {
			final EntityRegionAccessStrategy cache = persister.getCacheAccessStrategy();
			ck = cache.generateCacheKey(
					id, 
					persister,
					factory,
					session.getTenantIdentifier()
			);
			lock = cache.lockItem( session, ck, previousVersion );
		}
		else {
			ck = null;
		}

		if ( !veto ) {
			persister.update( 
					id, 
					state, 
					dirtyFields, 
					hasDirtyCollection, 
					previousState, 
					previousVersion, 
					instance, 
					rowId, 
					session 
			);
		}

		final EntityEntry entry = session.getPersistenceContext().getEntry( instance );
		if ( entry == null ) {
			throw new AssertionFailure( "possible nonthreadsafe access to session" );
		}
		
		if ( entry.getStatus()==Status.MANAGED || persister.isVersionPropertyGenerated() ) {
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
				// this entity defines proeprty generation, so process those generated
				// values...
				persister.processUpdateGeneratedProperties( id, instance, state, session );
				if ( persister.isVersionPropertyGenerated() ) {
					nextVersion = Versioning.getVersion( state, persister );
				}
			}
			// have the entity entry doAfterTransactionCompletion post-update processing, passing it the
			// update state and the new version (if one).
			entry.postUpdate( instance, state, nextVersion );
		}

		if ( persister.hasCache() ) {
			if ( persister.isCacheInvalidationRequired() || entry.getStatus()!= Status.MANAGED ) {
				persister.getCacheAccessStrategy().remove( session, ck);
			}
			else if ( session.getCacheMode().isPutEnabled() ) {
				//TODO: inefficient if that cache is just going to ignore the updated state!
				final CacheEntry ce = persister.buildCacheEntry( instance,state, nextVersion, getSession() );
				cacheEntry = persister.getCacheEntryStructure().structure( ce );

				final boolean put = cacheUpdate( persister, previousVersion, ck );
				if ( put && factory.getStatistics().isStatisticsEnabled() ) {
					factory.getStatistics().secondLevelCachePut( getPersister().getCacheAccessStrategy().getRegion().getName() );
				}
			}
		}

		session.getPersistenceContext().getNaturalIdHelper().manageSharedNaturalIdCrossReference(
				persister,
				id,
				state,
				previousNaturalIdValues,
				CachedNaturalIdValueSource.UPDATE
		);

		postUpdate();

		if ( factory.getStatistics().isStatisticsEnabled() && !veto ) {
			factory.getStatistics().updateEntity( getPersister().getEntityName() );
		}
	}

	private boolean cacheUpdate(EntityPersister persister, Object previousVersion, Object ck) {
		final SharedSessionContractImplementor session = getSession();
		try {
			session.getEventListenerManager().cachePutStart();
			return persister.getCacheAccessStrategy().update( session, ck, cacheEntry, nextVersion, previousVersion );
		}
		finally {
			session.getEventListenerManager().cachePutEnd();
		}
	}

	private boolean preUpdate() {
		boolean veto = false;
		final EventListenerGroup<PreUpdateEventListener> listenerGroup = listenerGroup( EventType.PRE_UPDATE );
		if ( listenerGroup.isEmpty() ) {
			return veto;
		}
		final PreUpdateEvent event = new PreUpdateEvent(
				getInstance(),
				getId(),
				state,
				previousState,
				getPersister(),
				eventSource()
		);
		for ( PreUpdateEventListener listener : listenerGroup.listeners() ) {
			veto |= listener.onPreUpdate( event );
		}
		return veto;
	}

	private void postUpdate() {
		final EventListenerGroup<PostUpdateEventListener> listenerGroup = listenerGroup( EventType.POST_UPDATE );
		if ( listenerGroup.isEmpty() ) {
			return;
		}
		final PostUpdateEvent event = new PostUpdateEvent(
				getInstance(),
				getId(),
				state,
				previousState,
				dirtyFields,
				getPersister(),
				eventSource()
		);
		for ( PostUpdateEventListener listener : listenerGroup.listeners() ) {
			listener.onPostUpdate( event );
		}
	}

	private void postCommitUpdate(boolean success) {
		final EventListenerGroup<PostUpdateEventListener> listenerGroup = listenerGroup( EventType.POST_COMMIT_UPDATE );
		if ( listenerGroup.isEmpty() ) {
			return;
		}
		final PostUpdateEvent event = new PostUpdateEvent(
				getInstance(),
				getId(),
				state,
				previousState,
				dirtyFields,
				getPersister(),
				eventSource()
		);
		for ( PostUpdateEventListener listener : listenerGroup.listeners() ) {
			if ( PostCommitUpdateEventListener.class.isInstance( listener ) ) {
				if ( success ) {
					listener.onPostUpdate( event );
				}
				else {
					((PostCommitUpdateEventListener) listener).onPostUpdateCommitFailed( event );
				}
			}
			else {
				//default to the legacy implementation that always fires the event
				listener.onPostUpdate( event );
			}
		}
	}

	@Override
	protected boolean hasPostCommitEventListeners() {
		final EventListenerGroup<PostUpdateEventListener> group = listenerGroup( EventType.POST_COMMIT_UPDATE );
		for ( PostUpdateEventListener listener : group.listeners() ) {
			if ( listener.requiresPostCommitHanding( getPersister() ) ) {
				return true;
			}
		}

		return false;
	}

	@Override
	public void doAfterTransactionCompletion(boolean success, SharedSessionContractImplementor session) throws CacheException {
		final EntityPersister persister = getPersister();
		if ( persister.hasCache() ) {
			final EntityRegionAccessStrategy cache = persister.getCacheAccessStrategy();
			final Object ck = cache.generateCacheKey(
					getId(),
					persister,
					session.getFactory(),
					session.getTenantIdentifier()
					
			);

			if ( success &&
					cacheEntry != null &&
					!persister.isCacheInvalidationRequired() &&
					session.getCacheMode().isPutEnabled() ) {
				final boolean put = cacheAfterUpdate( cache, ck );

				if ( put && getSession().getFactory().getStatistics().isStatisticsEnabled() ) {
					getSession().getFactory().getStatistics().secondLevelCachePut( cache.getRegion().getName() );
				}
			}
			else {
				cache.unlockItem(session, ck, lock );
			}
		}
		postCommitUpdate( success );
	}

	private boolean cacheAfterUpdate(EntityRegionAccessStrategy cache, Object ck) {
		final SharedSessionContractImplementor session = getSession();
		SessionEventListenerManager eventListenerManager = session.getEventListenerManager();
		try {
			eventListenerManager.cachePutStart();
			return cache.afterUpdate( session, ck, cacheEntry, nextVersion, previousVersion, lock );
		}
		finally {
			eventListenerManager.cachePutEnd();
		}
	}

}
