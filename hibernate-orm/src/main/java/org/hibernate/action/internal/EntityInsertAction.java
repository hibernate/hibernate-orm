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
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.engine.internal.Versioning;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionEventListenerManager;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostCommitInsertEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreInsertEventListener;
import org.hibernate.persister.entity.EntityPersister;

/**
 * The action for performing an entity insertion, for entities not defined to use IDENTITY generation.
 *
 * @see EntityIdentityInsertAction
 */
public final class EntityInsertAction extends AbstractEntityInsertAction {
	private Object version;
	private Object cacheEntry;

	/**
	 * Constructs an EntityInsertAction.
	 *
	 * @param id The entity identifier
	 * @param state The current (extracted) entity state
	 * @param instance The entity instance
	 * @param version The current entity version value
	 * @param persister The entity's persister
	 * @param isVersionIncrementDisabled Whether version incrementing is disabled.
	 * @param session The session
	 */
	public EntityInsertAction(
			Serializable id,
			Object[] state,
			Object instance,
			Object version,
			EntityPersister persister,
			boolean isVersionIncrementDisabled,
			SharedSessionContractImplementor session) {
		super( id, state, instance, isVersionIncrementDisabled, persister, session );
		this.version = version;
	}

	@Override
	public boolean isEarlyInsert() {
		return false;
	}

	@Override
	protected EntityKey getEntityKey() {
		return getSession().generateEntityKey( getId(), getPersister() );
	}

	@Override
	public void execute() throws HibernateException {
		nullifyTransientReferencesIfNotAlready();

		final EntityPersister persister = getPersister();
		final SharedSessionContractImplementor session = getSession();
		final Object instance = getInstance();
		final Serializable id = getId();

		final boolean veto = preInsert();

		// Don't need to lock the cache here, since if someone
		// else inserted the same pk first, the insert would fail

		if ( !veto ) {
			
			persister.insert( id, getState(), instance, session );
			PersistenceContext persistenceContext = session.getPersistenceContext();
			final EntityEntry entry = persistenceContext.getEntry( instance );
			if ( entry == null ) {
				throw new AssertionFailure( "possible non-threadsafe access to session" );
			}
			
			entry.postInsert( getState() );
	
			if ( persister.hasInsertGeneratedProperties() ) {
				persister.processInsertGeneratedProperties( id, instance, getState(), session );
				if ( persister.isVersionPropertyGenerated() ) {
					version = Versioning.getVersion( getState(), persister );
				}
				entry.postUpdate( instance, getState(), version );
			}

			persistenceContext.registerInsertedKey( persister, getId() );
		}

		final SessionFactoryImplementor factory = session.getFactory();

		if ( isCachePutEnabled( persister, session ) ) {
			final CacheEntry ce = persister.buildCacheEntry(
					instance,
					getState(),
					version,
					session
			);
			cacheEntry = persister.getCacheEntryStructure().structure( ce );
			final EntityRegionAccessStrategy cache = persister.getCacheAccessStrategy();
			final Object ck = cache.generateCacheKey( id, persister, factory, session.getTenantIdentifier() );

			final boolean put = cacheInsert( persister, ck );

			if ( put && factory.getStatistics().isStatisticsEnabled() ) {
				factory.getStatistics().secondLevelCachePut( cache.getRegion().getName() );
			}
		}

		handleNaturalIdPostSaveNotifications( id );

		postInsert();

		if ( factory.getStatistics().isStatisticsEnabled() && !veto ) {
			factory.getStatistics().insertEntity( getPersister().getEntityName() );
		}

		markExecuted();
	}

	private boolean cacheInsert(EntityPersister persister, Object ck) {
		SharedSessionContractImplementor session = getSession();
		try {
			session.getEventListenerManager().cachePutStart();
			return persister.getCacheAccessStrategy().insert( session, ck, cacheEntry, version);
		}
		finally {
			session.getEventListenerManager().cachePutEnd();
		}
	}

	private void postInsert() {
		final EventListenerGroup<PostInsertEventListener> listenerGroup = listenerGroup( EventType.POST_INSERT );
		if ( listenerGroup.isEmpty() ) {
			return;
		}
		final PostInsertEvent event = new PostInsertEvent(
				getInstance(),
				getId(),
				getState(),
				getPersister(),
				eventSource()
		);
		for ( PostInsertEventListener listener : listenerGroup.listeners() ) {
			listener.onPostInsert( event );
		}
	}

	private void postCommitInsert(boolean success) {
		final EventListenerGroup<PostInsertEventListener> listenerGroup = listenerGroup( EventType.POST_COMMIT_INSERT );
		if ( listenerGroup.isEmpty() ) {
			return;
		}
		final PostInsertEvent event = new PostInsertEvent(
				getInstance(),
				getId(),
				getState(),
				getPersister(),
				eventSource()
		);
		for ( PostInsertEventListener listener : listenerGroup.listeners() ) {
			if ( PostCommitInsertEventListener.class.isInstance( listener ) ) {
				if ( success ) {
					listener.onPostInsert( event );
				}
				else {
					((PostCommitInsertEventListener) listener).onPostInsertCommitFailed( event );
				}
			}
			else {
				//default to the legacy implementation that always fires the event
				listener.onPostInsert( event );
			}
		}
	}

	private boolean preInsert() {
		boolean veto = false;

		final EventListenerGroup<PreInsertEventListener> listenerGroup = listenerGroup( EventType.PRE_INSERT );
		if ( listenerGroup.isEmpty() ) {
			return veto;
		}
		final PreInsertEvent event = new PreInsertEvent( getInstance(), getId(), getState(), getPersister(), eventSource() );
		for ( PreInsertEventListener listener : listenerGroup.listeners() ) {
			veto |= listener.onPreInsert( event );
		}
		return veto;
	}

	@Override
	public void doAfterTransactionCompletion(boolean success, SharedSessionContractImplementor session) throws HibernateException {
		final EntityPersister persister = getPersister();
		if ( success && isCachePutEnabled( persister, getSession() ) ) {
			final EntityRegionAccessStrategy cache = persister.getCacheAccessStrategy();
			SessionFactoryImplementor sessionFactoryImplementor = session.getFactory();
			final Object ck = cache.generateCacheKey( getId(), persister, sessionFactoryImplementor, session.getTenantIdentifier() );
			final boolean put = cacheAfterInsert( cache, ck );

			if ( put && sessionFactoryImplementor.getStatistics().isStatisticsEnabled() ) {
				sessionFactoryImplementor.getStatisticsImplementor()
						.secondLevelCachePut( cache.getRegion().getName() );
			}
		}
		postCommitInsert( success );
	}

	private boolean cacheAfterInsert(EntityRegionAccessStrategy cache, Object ck) {
		SharedSessionContractImplementor session = getSession();
		final SessionEventListenerManager eventListenerManager = session.getEventListenerManager();
		try {
			eventListenerManager.cachePutStart();
			return cache.afterInsert( session, ck, cacheEntry, version );
		}
		finally {
			eventListenerManager.cachePutEnd();
		}
	}

	@Override
	protected boolean hasPostCommitEventListeners() {
		final EventListenerGroup<PostInsertEventListener> group = listenerGroup( EventType.POST_COMMIT_INSERT );
		for ( PostInsertEventListener listener : group.listeners() ) {
			if ( listener.requiresPostCommitHanding( getPersister() ) ) {
				return true;
			}
		}

		return false;
	}
	
	private boolean isCachePutEnabled(EntityPersister persister, SharedSessionContractImplementor session) {
		return persister.hasCache()
				&& !persister.isCacheInvalidationRequired()
				&& session.getCacheMode().isPutEnabled();
	}

}
