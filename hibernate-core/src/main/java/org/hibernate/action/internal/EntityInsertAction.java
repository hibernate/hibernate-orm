/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.internal;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.engine.internal.Versioning;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.monitor.spi.EventMonitor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.PostCommitInsertEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.stat.internal.StatsHelper;

/**
 * The action for performing an entity insertion, for entities not defined to use {@code IDENTITY} generation.
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
	protected Object getRowId() {
		return null ;
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

		final var session = getSession();
		final Object id = getId();
		final boolean veto = preInsert();
		if ( !veto ) {
			final var persister = getPersister();
			final Object instance = getInstance();
			final var eventMonitor = session.getEventMonitor();
			final var event = eventMonitor.beginEntityInsertEvent();
			boolean success = false;
			final GeneratedValues generatedValues;
			try {
				generatedValues = persister.getInsertCoordinator().insert( instance, id, getState(), session );
				success = true;
			}
			finally {
				eventMonitor.completeEntityInsertEvent( event, id, persister.getEntityName(), success, session );
			}
			final var persistenceContext = session.getPersistenceContextInternal();
			final var entry = persistenceContext.getEntry( instance );
			if ( entry == null ) {
				throw new AssertionFailure( "possible non-threadsafe access to session" );
			}
			entry.postInsert( getState() );
			handleGeneratedProperties( entry, generatedValues, persistenceContext );
			persistenceContext.registerInsertedKey( persister, id );
			addCollectionsByKeyToPersistenceContext( persistenceContext, getState() );
		}
		putCacheIfNecessary();
		handleNaturalIdPostSaveNotifications( id );
		postInsert();

		final var statistics = session.getFactory().getStatistics();
		if ( statistics.isStatisticsEnabled() && !veto ) {
			statistics.insertEntity( getPersister().getEntityName() );
		}

		markExecuted();
	}

	private void handleGeneratedProperties(
			EntityEntry entry,
			GeneratedValues generatedValues,
			PersistenceContext persistenceContext) {
		final var persister = getPersister();
		final Object[] state = getState();
		if ( persister.hasInsertGeneratedProperties() ) {
			final Object instance = getInstance();
			persister.processInsertGeneratedProperties( getId(), instance, state, generatedValues, getSession() );
			if ( persister.isVersionPropertyGenerated() ) {
				version = Versioning.getVersion( state, persister );
			}
			entry.postUpdate( instance, state, version );
		}
		else if ( persister.isVersionPropertyGenerated() ) {
			version = Versioning.getVersion( state, persister );
			entry.postInsert( version );
		}
		// Process row-id values when available early by replacing the entity entry
		if ( generatedValues != null && persister.getRowIdMapping() != null ) {
			final Object rowId = generatedValues.getGeneratedValue( persister.getRowIdMapping() );
			if ( rowId != null ) {
				persistenceContext.replaceEntityEntryRowId( getInstance(), rowId );
			}
		}
	}

	protected void putCacheIfNecessary() {
		final var persister = getPersister();
		final var session = getSession();
		if ( isCachePutEnabled( persister, session ) ) {
			final var factory = session.getFactory();
			final var cacheEntry = persister.buildCacheEntry( getInstance(), getState(), version, session );
			this.cacheEntry = persister.getCacheEntryStructure().structure( cacheEntry );
			final var cache = persister.getCacheAccessStrategy();
			final Object ck = cache.generateCacheKey( getId(), persister, factory, session.getTenantIdentifier() );
			final boolean put = cacheInsert( persister, ck );

			final var statistics = factory.getStatistics();
			if ( put && statistics.isStatisticsEnabled() ) {
				statistics.entityCachePut(
						StatsHelper.getRootEntityRole( persister ),
						cache.getRegion().getName()
				);
			}
		}
	}

	protected boolean cacheInsert(EntityPersister persister, Object ck) {
		final var session = getSession();
		final var eventMonitor = session.getEventMonitor();
		final var cachePutEvent = eventMonitor.beginCachePutEvent();
		final var cacheAccessStrategy = persister.getCacheAccessStrategy();
		final var eventListenerManager = session.getEventListenerManager();
		boolean insert = false;
		try {
			eventListenerManager.cachePutStart();
			insert = cacheAccessStrategy.insert( session, ck, cacheEntry, version );
			return insert;
		}
		finally {
			eventMonitor.completeCachePutEvent(
					cachePutEvent,
					session,
					cacheAccessStrategy,
					getPersister(),
					insert,
					EventMonitor.CacheActionDescription.ENTITY_INSERT
			);
			eventListenerManager.cachePutEnd();
		}
	}

	protected void postInsert() {
		getEventListenerGroups()
				.eventListenerGroup_POST_INSERT
				.fireLazyEventOnEachListener( this::newPostInsertEvent, PostInsertEventListener::onPostInsert );
	}

	private PostInsertEvent newPostInsertEvent() {
		return new PostInsertEvent( getInstance(), getId(), getState(), getPersister(), eventSource() );
	}

	protected void postCommitInsert(boolean success) {
		getEventListenerGroups().eventListenerGroup_POST_COMMIT_INSERT
				.fireLazyEventOnEachListener( this::newPostInsertEvent,
						success ? PostInsertEventListener::onPostInsert : this::postCommitOnFailure );
	}

	private void postCommitOnFailure(PostInsertEventListener listener, PostInsertEvent event) {
		if ( listener instanceof PostCommitInsertEventListener postCommitInsertEventListener ) {
			postCommitInsertEventListener.onPostInsertCommitFailed( event );
		}
		else {
			//default to the legacy implementation that always fires the event
			listener.onPostInsert( event );
		}
	}

	protected boolean preInsert() {
		final var listenerGroup = getEventListenerGroups().eventListenerGroup_PRE_INSERT;
		if ( listenerGroup.isEmpty() ) {
			return false;
		}
		else {
			boolean veto = false;
			final PreInsertEvent event =
					new PreInsertEvent( getInstance(), getId(), getState(), getPersister(), eventSource() );
			for ( var listener : listenerGroup.listeners() ) {
				veto |= listener.onPreInsert( event );
			}
			return veto;
		}
	}

	@Override
	public void doAfterTransactionCompletion(boolean success, SharedSessionContractImplementor session) throws HibernateException {
		final var persister = getPersister();
		if ( success && isCachePutEnabled( persister, getSession() ) ) {
			final var cache = persister.getCacheAccessStrategy();
			final var factory = session.getFactory();
			final Object ck = cache.generateCacheKey( getId(), persister, factory, session.getTenantIdentifier() );
			final boolean put = cacheAfterInsert( cache, ck );

			final var statistics = factory.getStatistics();
			if ( put && statistics.isStatisticsEnabled() ) {
				statistics.entityCachePut(
						StatsHelper.getRootEntityRole( persister ),
						cache.getRegion().getName()
				);
			}
		}
		postCommitInsert( success );
	}

	protected boolean cacheAfterInsert(EntityDataAccess cache, Object ck) {
		final var session = getSession();
		final var eventListenerManager = session.getEventListenerManager();
		final var eventMonitor = session.getEventMonitor();
		final var cachePutEvent = eventMonitor.beginCachePutEvent();
		boolean afterInsert = false;
		try {
			eventListenerManager.cachePutStart();
			afterInsert = cache.afterInsert( session, ck, cacheEntry, version );
			return afterInsert;
		}
		finally {
			eventMonitor.completeCachePutEvent(
					cachePutEvent,
					session,
					cache,
					getPersister(),
					afterInsert,
					EventMonitor.CacheActionDescription.ENTITY_AFTER_INSERT
			);
			eventListenerManager.cachePutEnd();
		}
	}

	@Override
	protected boolean hasPostCommitEventListeners() {
		final var group = getEventListenerGroups().eventListenerGroup_POST_COMMIT_INSERT;
		for ( var listener : group.listeners() ) {
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
