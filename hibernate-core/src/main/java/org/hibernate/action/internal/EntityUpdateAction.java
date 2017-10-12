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
import org.hibernate.cache.spi.access.EntityDataAccess;
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
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.StateArrayContributor;
import org.hibernate.type.internal.TypeHelper;

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
	 * @param entityDescriptor The entity's entityDescriptor
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
			final EntityDescriptor entityDescriptor,
			final SharedSessionContractImplementor session) {
		super( session, id, instance, entityDescriptor );
		this.state = state;
		this.previousState = previousState;
		this.previousVersion = previousVersion;
		this.nextVersion = nextVersion;
		this.dirtyFields = dirtyProperties;
		this.hasDirtyCollection = hasDirtyCollection;
		this.rowId = rowId;

		this.previousNaturalIdValues = determinePreviousNaturalIdValues( entityDescriptor, previousState, session, id );
		session.getPersistenceContext().getNaturalIdHelper().manageLocalNaturalIdCrossReference(
				entityDescriptor,
				id,
				state,
				previousNaturalIdValues,
				CachedNaturalIdValueSource.UPDATE
		);
	}

	private Object[] determinePreviousNaturalIdValues(
			EntityDescriptor entityDescriptor,
			Object[] previousState,
			SharedSessionContractImplementor session,
			Serializable id) {
		if ( entityDescriptor.getHierarchy().getNaturalIdDescriptor() == null ) {
			return null;
		}

		if ( previousState != null ) {
			return session.getPersistenceContext().getNaturalIdHelper().extractNaturalIdValues( previousState, entityDescriptor );
		}

		return session.getPersistenceContext().getNaturalIdSnapshot( id, entityDescriptor );
	}

	@Override
	public void execute() throws HibernateException {
		final Serializable id = getId();
		final EntityDescriptor entityDescriptor = getEntityDescriptor();
		final SharedSessionContractImplementor session = getSession();
		final Object instance = getInstance();

		final boolean veto = preUpdate();

		final SessionFactoryImplementor factory = session.getFactory();
		Object previousVersion = this.previousVersion;
		if ( entityDescriptor.isVersionPropertyGenerated() ) {
			// we need to grab the version value from the entity, otherwise
			// we have issues with generated-version entities that may have
			// multiple actions queued during the same flush
			previousVersion = entityDescriptor.getVersion( instance );
		}

		final Object ck;
		final EntityDescriptor rootEntityDescriptor = entityDescriptor.getHierarchy().getRootEntityType();
		final EntityDataAccess cacheAccess = factory.getCache().getEntityRegionAccess( rootEntityDescriptor.getHierarchy() );
		if ( cacheAccess != null ) {
			ck = cacheAccess.generateCacheKey(
					id,
					rootEntityDescriptor.getHierarchy(),
					factory,
					session.getTenantIdentifier()
			);
			lock = cacheAccess.lockItem( session, ck, previousVersion );
		}
		else {
			ck = null;
		}

		if ( !veto ) {
			entityDescriptor.update(
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
		
		if ( entry.getStatus()==Status.MANAGED || entityDescriptor.isVersionPropertyGenerated() ) {
			// get the updated snapshot of the entity state by cloning current state;
			// it is safe to copy in place, since by this time no-one else (should have)2
			// has a reference  to the array
			TypeHelper.deepCopy(
					entityDescriptor,
					state,
					previousState,
					StateArrayContributor::isIncludedInDirtyChecking
			);

			if ( entityDescriptor.hasUpdateGeneratedProperties() ) {
				// this entity defines proeprty generation, so process those generated
				// values...
				entityDescriptor.processUpdateGeneratedProperties( id, instance, state, session );
				if ( entityDescriptor.isVersionPropertyGenerated() ) {
					nextVersion = Versioning.getVersion( state, entityDescriptor );
				}
			}

			// have the entity entry doAfterTransactionCompletion post-update processing, passing it the
			// update state and the new version (if one).
			entry.postUpdate( instance, state, nextVersion );
		}

		if ( cacheAccess != null ) {
			if ( entityDescriptor.isCacheInvalidationRequired() || entry.getStatus()!= Status.MANAGED ) {
				cacheAccess.remove( session, ck);
			}
			else if ( session.getCacheMode().isPutEnabled() ) {
				//TODO: inefficient if that cache is just going to ignore the updated state!
				final CacheEntry ce = entityDescriptor.buildCacheEntry( instance, state, nextVersion, getSession() );
				cacheEntry = entityDescriptor.getCacheEntryStructure().structure( ce );

				final boolean put = cacheUpdate( entityDescriptor, previousVersion, ck );
				if ( put && factory.getStatistics().isStatisticsEnabled() ) {
					factory.getStatistics().secondLevelCachePut( cacheAccess.getRegion().getName() );
				}
			}
		}

		session.getPersistenceContext().getNaturalIdHelper().manageSharedNaturalIdCrossReference(
				entityDescriptor,
				id,
				state,
				previousNaturalIdValues,
				CachedNaturalIdValueSource.UPDATE
		);

		postUpdate();

		if ( factory.getStatistics().isStatisticsEnabled() && !veto ) {
			factory.getStatistics().updateEntity( getEntityDescriptor().getEntityName() );
		}
	}

	private boolean cacheUpdate(EntityDescriptor entityDescriptor, Object previousVersion, Object ck) {
		final SharedSessionContractImplementor session = getSession();
		try {
			session.getEventListenerManager().cachePutStart();
			final EntityDescriptor rootDescriptor = entityDescriptor.getHierarchy().getRootEntityType();
			return session.getFactory().getCache().getEntityRegionAccess( rootDescriptor.getHierarchy() ).update(
					session,
					ck,
					cacheEntry,
					nextVersion,
					previousVersion
			);
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
				getEntityDescriptor(),
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
				getEntityDescriptor(),
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
				getEntityDescriptor(),
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
			if ( listener.requiresPostCommitHanding( getEntityDescriptor() ) ) {
				return true;
			}
		}

		return false;
	}

	@Override
	public void doAfterTransactionCompletion(boolean success, SharedSessionContractImplementor session) throws CacheException {
		final SessionFactoryImplementor factory = session.getFactory();
		final EntityDescriptor entityDescriptor = getEntityDescriptor();
		final EntityDescriptor rootEntityDescriptor = entityDescriptor.getHierarchy().getRootEntityType();
		final EntityDataAccess cacheAccess = factory.getCache().getEntityRegionAccess( rootEntityDescriptor.getHierarchy() );
		if ( cacheAccess != null ) {
			final Object ck = cacheAccess.generateCacheKey(
					getId(),
					entityDescriptor.getHierarchy(),
					factory,
					session.getTenantIdentifier()
					
			);

			if ( success
					&& cacheEntry != null
					&& !entityDescriptor.isCacheInvalidationRequired()
					&& session.getCacheMode().isPutEnabled() ) {
				final boolean put = cacheAfterUpdate( cacheAccess, ck );

				if ( put && getSession().getFactory().getStatistics().isStatisticsEnabled() ) {
					factory.getStatistics().secondLevelCachePut( cacheAccess.getRegion().getName() );
				}
			}
			else {
				cacheAccess.unlockItem(session, ck, lock );
			}
		}
		postCommitUpdate( success );
	}

	private boolean cacheAfterUpdate(EntityDataAccess cache, Object ck) {
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
