/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.PersistenceException;
import org.hibernate.AssertionFailure;
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
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.stat.internal.StatsHelper;

import static org.hibernate.cache.spi.entry.CacheEntryHelper.buildStructuredCacheEntry;

/**
 * The action for performing an entity insertion, for entities not defined to use {@code IDENTITY} generation.
 *
 * @see EntityIdentityInsertAction
 */
public class EntityInsertAction extends AbstractEntityInsertAction {

	private @Nullable Object version;
	private @Nullable Object cacheEntry;

	/**
	 * Constructs an EntityInsertAction.
	 *  @param id The entity identifier
	 * @param state The current (extracted) entity state
	 * @param instance The entity instance
	 * @param version The current entity version value
	 * @param persister The entity's persister
	 * @param session The session
	 */
	public EntityInsertAction(
			final @Nonnull Object id,
			final @Nonnull Object[] state,
			final @Nonnull Object instance,
			final @Nullable Object version,
			final @Nonnull EntityPersister persister,
			final @Nonnull EventSource session) {
		super( id, state, instance, persister, session );
		this.version = version;
	}

	@Nullable
	public Object getVersion() {
		return version;
	}

	public void setVersion(@Nullable Object version) {
		this.version = version;
	}

	@Nullable
	protected Object getCacheEntry() {
		return cacheEntry;
	}

	protected void setCacheEntry(@Nullable Object cacheEntry) {
		this.cacheEntry = cacheEntry;
	}

	@Override
	public boolean isEarlyInsert() {
		return false;
	}

	@Override
	@Nullable
	protected Object getRowId() {
		return null ;
	}

	@Override
	@Nonnull
	protected EntityKey getEntityKey() {
		final Object id = getId();
		assert id != null;
		return getSession().generateEntityKey( id, getPersister() );
	}

	@Override
	public void execute() {
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
			catch (ConstraintViolationException cve) {
				throw convertException( cve, session );
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

	private static @Nonnull PersistenceException convertException(
			@Nonnull ConstraintViolationException cve,
			@Nonnull EventSource session) {
		return session.getFactory().getSessionFactoryOptions().isJpaBootstrap()
			&& cve.getKind() == ConstraintKind.UNIQUE
				? new EntityExistsException( cve )
				: cve;
	}

	private void handleGeneratedProperties(
			@Nonnull EntityEntry entry,
			@Nullable GeneratedValues generatedValues,
			@Nonnull PersistenceContext persistenceContext) {
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

	public void putCacheIfNecessary() {
		final var persister = getPersister();
		final var session = getSession();
		if ( isCachePutEnabled( persister, session ) ) {
			final var factory = session.getFactory();
			final Object instance = getInstance();
			final Object entry = buildStructuredCacheEntry( instance, version, getState(), persister, session );
			cacheEntry = entry;
			final var cache = persister.getCacheAccessStrategy();
			assert cache != null;
			final Object id = getId();
			assert id != null;
			final Object cacheKey = cache.generateCacheKey( id, persister, factory, session.getTenantIdentifier() );
			final boolean put = cacheInsert( persister, cacheKey, entry );

			final var statistics = factory.getStatistics();
			if ( put && statistics.isStatisticsEnabled() ) {
				statistics.entityCachePut(
						StatsHelper.getRootEntityRole( persister ),
						cache.getRegion().getName()
				);
			}
		}
	}

	protected boolean cacheInsert(@Nonnull EntityPersister persister, @Nonnull Object cacheKey, Object entry) {
		final var session = getSession();
		final var eventMonitor = session.getEventMonitor();
		final var cachePutEvent = eventMonitor.beginCachePutEvent();
		final var cache = persister.getCacheAccessStrategy();
		assert cache != null;
		final var eventListenerManager = session.getEventListenerManager();
		boolean insert = false;
		try {
			eventListenerManager.cachePutStart();
			insert = cache.insert( session, cacheKey, entry, version );
			return insert;
		}
		finally {
			eventMonitor.completeCachePutEvent(
					cachePutEvent,
					session,
					cache,
					getPersister(),
					insert,
					EventMonitor.CacheActionDescription.ENTITY_INSERT
			);
			eventListenerManager.cachePutEnd();
		}
	}

	public void postInsert() {
		getEventListenerGroups()
				.eventListenerGroup_POST_INSERT
				.fireLazyEventOnEachListener( this::newPostInsertEvent, PostInsertEventListener::onPostInsert );
	}

	@Nonnull
	private PostInsertEvent newPostInsertEvent() {
		final Object id = getId();
		assert id != null;
		return new PostInsertEvent( getInstance(), id, getState(), getPersister(), eventSource() );
	}

	protected void postCommitInsert(boolean success) {
		getEventListenerGroups().eventListenerGroup_POST_COMMIT_INSERT
				.fireLazyEventOnEachListener( this::newPostInsertEvent,
						success ? PostInsertEventListener::onPostInsert : this::postCommitOnFailure );
	}

	private void postCommitOnFailure(@Nonnull PostInsertEventListener listener, @Nonnull PostInsertEvent event) {
		if ( listener instanceof PostCommitInsertEventListener postCommitInsertEventListener ) {
			postCommitInsertEventListener.onPostInsertCommitFailed( event );
		}
		else {
			//default to the legacy implementation that always fires the event
			listener.onPostInsert( event );
		}
	}

	protected boolean preInsert() {
		executePreInsertCallbacks( eventSource() );

		final var listenerGroup = getEventListenerGroups().eventListenerGroup_PRE_INSERT;
		if ( listenerGroup.isEmpty() ) {
			return false;
		}
		else {
			boolean veto = false;
			final var event = new PreInsertEvent( getInstance(), getId(), getState(), getPersister(), eventSource() );
			for ( var listener : listenerGroup.listeners() ) {
				veto |= listener.onPreInsert( event );
			}
			return veto;
		}
	}

	@Override
	public void doAfterTransactionCompletion(boolean success, @Nonnull SharedSessionContractImplementor session) {
		final var persister = getPersister();
		if ( success && isCachePutEnabled( persister, getSession() ) ) {
			final var cache = persister.getCacheAccessStrategy();
			assert cache != null;
			final var factory = session.getFactory();
			final Object id = getId();
			assert id != null;
			final Object cacheKey = cache.generateCacheKey( id, persister, factory, session.getTenantIdentifier() );
			final boolean put = cacheAfterInsert( cache, cacheKey );

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

	protected boolean cacheAfterInsert(@Nonnull EntityDataAccess cache, @Nonnull Object cacheKey) {
		final var session = getSession();
		final var eventListenerManager = session.getEventListenerManager();
		final var eventMonitor = session.getEventMonitor();
		final var cachePutEvent = eventMonitor.beginCachePutEvent();
		boolean afterInsert = false;
		try {
			eventListenerManager.cachePutStart();
			assert cacheEntry != null;
			afterInsert = cache.afterInsert( session, cacheKey, cacheEntry, version );
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

	protected boolean isCachePutEnabled(
			@Nonnull EntityPersister persister,
			@Nonnull SharedSessionContractImplementor session) {
		return persister.canWriteToCache()
			&& !persister.isCacheInvalidationRequired()
			&& session.getCacheMode().isPutEnabled();
	}

}
