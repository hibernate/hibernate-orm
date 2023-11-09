/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.action.internal;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.engine.internal.Versioning;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionEventListenerManager;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.EventManager;
import org.hibernate.event.spi.HibernateEvent;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.PostCommitInsertEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreInsertEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.stat.internal.StatsHelper;
import org.hibernate.stat.spi.StatisticsImplementor;

/**
 * The action for performing an entity insertion, for entities not defined to use IDENTITY generation.
 *
 * @see EntityIdentityInsertAction
 */
public class EntityInsertAction extends AbstractEntityInsertAction {

	private Object version;
	private Object cacheEntry;

	/**
	 * Constructs an EntityInsertAction.
	 *  @param id The entity identifier
	 * @param state The current (extracted) entity state
	 * @param instance The entity instance
	 * @param version The current entity version value
	 * @param persister The entity's persister
	 * @param isVersionIncrementDisabled Whether version incrementing is disabled.
	 * @param session The session
	 */
	public EntityInsertAction(
			final Object id,
			final Object[] state,
			final Object instance,
			final Object version,
			final EntityPersister persister,
			final boolean isVersionIncrementDisabled,
			final EventSource session) {
		super( id, state, instance, isVersionIncrementDisabled, persister, session );
		this.version = version;
	}

	public Object getVersion() {
		return version;
	}

	public void setVersion(Object version) {
		this.version = version;
	}

	protected Object getCacheEntry() {
		return cacheEntry;
	}

	protected void setCacheEntry(Object cacheEntry) {
		this.cacheEntry = cacheEntry;
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

		// Don't need to lock the cache here, since if someone
		// else inserted the same pk first, the insert would fail

		final SharedSessionContractImplementor session = getSession();
		final Object id = getId();
		final boolean veto = preInsert();
		if ( !veto ) {
			final EntityPersister persister = getPersister();
			final Object instance = getInstance();
			persister.insert( id, getState(), instance, session );
			final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
			final EntityEntry entry = persistenceContext.getEntry( instance );
			if ( entry == null ) {
				throw new AssertionFailure( "possible non-threadsafe access to session" );
			}
			entry.postInsert( getState() );
			handleGeneratedProperties( entry );
			persistenceContext.registerInsertedKey( persister, getId() );
			addCollectionsByKeyToPersistenceContext( persistenceContext, getState() );
		}
		putCacheIfNecessary();
		handleNaturalIdPostSaveNotifications( id );
		postInsert();

		final StatisticsImplementor statistics = session.getFactory().getStatistics();
		if ( statistics.isStatisticsEnabled() && !veto ) {
			statistics.insertEntity( getPersister().getEntityName() );
		}

		markExecuted();
	}

	private void handleGeneratedProperties(EntityEntry entry) {
		final EntityPersister persister = getPersister();
		if ( persister.hasInsertGeneratedProperties() ) {
			final Object instance = getInstance();
			persister.processInsertGeneratedProperties( getId(), instance, getState(), getSession() );
			if ( persister.isVersionPropertyGenerated() ) {
				version = Versioning.getVersion( getState(), persister );
			}
			entry.postUpdate( instance, getState(), version );
		}
		else if ( persister.isVersionPropertyGenerated() ) {
			version = Versioning.getVersion( getState(), persister );
			entry.postInsert( version );
		}
	}

	protected void putCacheIfNecessary() {
		final EntityPersister persister = getPersister();
		final SharedSessionContractImplementor session = getSession();
		if ( isCachePutEnabled( persister, session ) ) {
			final SessionFactoryImplementor factory = session.getFactory();
			final CacheEntry ce = persister.buildCacheEntry( getInstance(), getState(), version, session );
			cacheEntry = persister.getCacheEntryStructure().structure( ce );
			final EntityDataAccess cache = persister.getCacheAccessStrategy();
			final Object ck = cache.generateCacheKey( getId(), persister, factory, session.getTenantIdentifier() );
			final boolean put = cacheInsert( persister, ck );

			final StatisticsImplementor statistics = factory.getStatistics();
			if ( put && statistics.isStatisticsEnabled() ) {
				statistics.entityCachePut(
						StatsHelper.INSTANCE.getRootEntityRole( persister ),
						cache.getRegion().getName()
				);
			}
		}
	}

	protected boolean cacheInsert(EntityPersister persister, Object ck) {
		SharedSessionContractImplementor session = getSession();
		final EventManager eventManager = session.getEventManager();
		final HibernateEvent cachePutEvent = eventManager.beginCachePutEvent();
		final EntityDataAccess cacheAccessStrategy = persister.getCacheAccessStrategy();
		boolean insert = false;
		try {
			session.getEventListenerManager().cachePutStart();
			insert = cacheAccessStrategy.insert( session, ck, cacheEntry, version );
			return insert;
		}
		finally {
			eventManager.completeCachePutEvent(
					cachePutEvent,
					session,
					cacheAccessStrategy,
					getPersister(),
					insert,
					EventManager.CacheActionDescription.ENTITY_INSERT
			);
			session.getEventListenerManager().cachePutEnd();
		}
	}

	protected void postInsert() {
		getFastSessionServices()
				.eventListenerGroup_POST_INSERT
				.fireLazyEventOnEachListener( this::newPostInsertEvent, PostInsertEventListener::onPostInsert );
	}

	private PostInsertEvent newPostInsertEvent() {
		return new PostInsertEvent(
				getInstance(),
				getId(),
				getState(),
				getPersister(),
				eventSource()
		);
	}

	protected void postCommitInsert(boolean success) {
		getFastSessionServices().eventListenerGroup_POST_COMMIT_INSERT
				.fireLazyEventOnEachListener( this::newPostInsertEvent,
						success ? PostInsertEventListener::onPostInsert : this::postCommitOnFailure );
	}

	private void postCommitOnFailure(PostInsertEventListener listener, PostInsertEvent event) {
		if ( listener instanceof PostCommitInsertEventListener ) {
			((PostCommitInsertEventListener) listener).onPostInsertCommitFailed( event );
		}
		else {
			//default to the legacy implementation that always fires the event
			listener.onPostInsert( event );
		}
	}

	protected boolean preInsert() {
		boolean veto = false;

		final EventListenerGroup<PreInsertEventListener> listenerGroup
				= getFastSessionServices().eventListenerGroup_PRE_INSERT;
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
			final EntityDataAccess cache = persister.getCacheAccessStrategy();
			SessionFactoryImplementor factory = session.getFactory();
			final Object ck = cache.generateCacheKey( getId(), persister, factory, session.getTenantIdentifier() );
			final boolean put = cacheAfterInsert( cache, ck );

			final StatisticsImplementor statistics = factory.getStatistics();
			if ( put && statistics.isStatisticsEnabled() ) {
				statistics.entityCachePut(
						StatsHelper.INSTANCE.getRootEntityRole( persister ),
						cache.getRegion().getName()
				);
			}
		}
		postCommitInsert( success );
	}

	protected boolean cacheAfterInsert(EntityDataAccess cache, Object ck) {
		SharedSessionContractImplementor session = getSession();
		final SessionEventListenerManager eventListenerManager = session.getEventListenerManager();
		final EventManager eventManager = session.getEventManager();
		final HibernateEvent cachePutEvent = eventManager.beginCachePutEvent();
		boolean afterInsert = false;
		try {
			eventListenerManager.cachePutStart();
			afterInsert = cache.afterInsert( session, ck, cacheEntry, version );
			return afterInsert;
		}
		finally {
			eventManager.completeCachePutEvent(
					cachePutEvent,
					session,
					cache,
					getPersister(),
					afterInsert,
					EventManager.CacheActionDescription.ENTITY_AFTER_INSERT
			);
			eventListenerManager.cachePutEnd();
		}
	}

	@Override
	protected boolean hasPostCommitEventListeners() {
		final EventListenerGroup<PostInsertEventListener> group
				= getFastSessionServices().eventListenerGroup_POST_COMMIT_INSERT;
		for ( PostInsertEventListener listener : group.listeners() ) {
			if ( listener.requiresPostCommitHandling( getPersister() ) ) {
				return true;
			}
		}
		return false;
	}

	protected boolean isCachePutEnabled(EntityPersister persister, SharedSessionContractImplementor session) {
		return persister.canWriteToCache()
			&& !persister.isCacheInvalidationRequired()
			&& session.getCacheMode().isPutEnabled();
	}

}
