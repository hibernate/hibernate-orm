/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.FindOption;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.SystemException;
import org.hibernate.AssertionFailure;
import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.SessionException;
import org.hibernate.StatelessSession;
import org.hibernate.TransientObjectException;
import org.hibernate.UnresolvableObjectException;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.creation.internal.SessionCreationOptions;
import org.hibernate.engine.creation.internal.SharedSessionCreationOptions;
import org.hibernate.engine.internal.TransactionCompletionCallbacksImpl;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.StatelessSessionImplementor;
import org.hibernate.engine.spi.TransactionCompletionCallbacks;
import org.hibernate.engine.spi.TransactionCompletionCallbacksImplementor;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.event.service.spi.EventListenerGroups;
import org.hibernate.event.spi.PostCollectionRecreateEvent;
import org.hibernate.event.spi.PostCollectionRecreateEventListener;
import org.hibernate.event.spi.PostCollectionRemoveEvent;
import org.hibernate.event.spi.PostCollectionRemoveEventListener;
import org.hibernate.event.spi.PostCollectionUpdateEvent;
import org.hibernate.event.spi.PostCollectionUpdateEventListener;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.event.spi.PostUpsertEvent;
import org.hibernate.event.spi.PostUpsertEventListener;
import org.hibernate.event.spi.PreCollectionRecreateEvent;
import org.hibernate.event.spi.PreCollectionRecreateEventListener;
import org.hibernate.event.spi.PreCollectionRemoveEvent;
import org.hibernate.event.spi.PreCollectionRemoveEventListener;
import org.hibernate.event.spi.PreCollectionUpdateEvent;
import org.hibernate.event.spi.PreCollectionUpdateEventListener;
import org.hibernate.event.spi.PreDeleteEvent;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpsertEvent;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.id.IdentifierGenerationException;
import org.hibernate.internal.find.FindByKeyOperation;
import org.hibernate.internal.find.StatelessFindByKeyOperation;
import org.hibernate.internal.find.StatelessFindMultipleByKeyOperation;
import org.hibernate.internal.find.StatelessLoadAccessContext;
import org.hibernate.loader.ast.internal.LoaderHelper;
import org.hibernate.loader.ast.spi.CascadingFetchProfile;
import org.hibernate.loader.internal.CacheLoadHelper;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.stat.spi.StatisticsImplementor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;
import static org.hibernate.cfg.BatchSettings.STATEMENT_BATCH_SIZE;
import static org.hibernate.cfg.CacheSettings.JAKARTA_SHARED_CACHE_RETRIEVE_MODE;
import static org.hibernate.cfg.CacheSettings.JAKARTA_SHARED_CACHE_STORE_MODE;
import static org.hibernate.cfg.CacheSettings.JPA_SHARED_CACHE_RETRIEVE_MODE;
import static org.hibernate.cfg.CacheSettings.JPA_SHARED_CACHE_STORE_MODE;
import static org.hibernate.cfg.QuerySettings.CRITERIA_COPY_TREE;
import static org.hibernate.engine.internal.ManagedTypeHelper.asPersistentAttributeInterceptable;
import static org.hibernate.engine.internal.ManagedTypeHelper.isPersistentAttributeInterceptable;
import static org.hibernate.engine.internal.PersistenceContexts.createPersistenceContext;
import static org.hibernate.engine.internal.Versioning.incrementVersion;
import static org.hibernate.engine.internal.Versioning.seedVersion;
import static org.hibernate.engine.internal.Versioning.setVersion;
import static org.hibernate.engine.transaction.internal.jta.JtaStatusHelper.isRollback;
import static org.hibernate.event.internal.DefaultInitializeCollectionEventListener.handlePotentiallyEmptyCollection;
import static org.hibernate.generator.EventType.INSERT;
import static org.hibernate.internal.SessionLogging.SESSION_LOGGER;
import static org.hibernate.internal.util.NullnessUtil.castNonNull;
import static org.hibernate.jpa.HibernateHints.HINT_JDBC_BATCH_SIZE;
import static org.hibernate.jpa.internal.util.CacheModeHelper.interpretCacheMode;
import static org.hibernate.loader.internal.CacheLoadHelper.initializeCollectionFromCache;
import static org.hibernate.pretty.MessageHelper.collectionInfoString;
import static org.hibernate.pretty.MessageHelper.infoString;
import static org.hibernate.proxy.HibernateProxy.extractLazyInitializer;

/**
 * Concrete implementation of the {@link StatelessSession} API.
 * <p>
 * Exposes two interfaces:
 * <ul>
 * <li>{@link StatelessSession} to the application, and
 * <li>{@link org.hibernate.engine.spi.SharedSessionContractImplementor} (an SPI interface) to other subsystems.
 * </ul>
 * <p>
 * This class is not thread-safe.
 *
 * @implNote The {@code StatelessSessionImpl} is not an {@link org.hibernate.event.spi.EventSource} and does not
 * make use of the usual {@linkplain org.hibernate.event.spi eventing infrastructure} to implement persistence
 * operations. It does raise pre- and post- events for the benefit of integration, however. Since it performs all
 * operations synchronously, it does not maintain an {@link org.hibernate.engine.spi.ActionQueue}. Therefore, it
 * cannot, unfortunately, reuse the various {@link org.hibernate.action.internal.EntityAction} subtypes. This is
 * a pity, since it results in some code duplication. On the other hand, a {@code StatelessSession} is easier to
 * debug and understand. A {@code StatelessSession} does hold state in a long-lived {@link PersistenceContext},
 * but it does temporarily keep state within an instance of {@code StatefulPersistenceContext} while processing
 * the results of a given query.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
// Extended by Hibernate Reactive
public class StatelessSessionImpl
		extends AbstractSharedSessionContract
		implements StatelessSessionImplementor, StatelessLoadAccessContext {

	private final LoadQueryInfluencers influencers;
	private final PersistenceContext temporaryPersistenceContext;
	private final boolean connectionProvided;
	private final TransactionCompletionCallbacksImplementor transactionCompletionCallbacks;

	private final FlushMode flushMode;
	private final EventListenerGroups eventListenerGroups;

	// Defaults to null, meaning the properties
	// are the default properties of the factory.
	private Map<String, Object> properties;

	public StatelessSessionImpl(SessionFactoryImpl factory, SessionCreationOptions options) {
		super( factory, options );
		connectionProvided = options.getConnection() != null;
		if ( options instanceof SharedSessionCreationOptions sharedOptions
				&& sharedOptions.isTransactionCoordinatorShared() ) {
			transactionCompletionCallbacks = sharedOptions.getTransactionCompletionCallbacks();
//			// register a callback with the child session to propagate auto flushing
//			transactionCompletionCallbacks.registerCallback( session -> {
//				// NOTE: `session` here is the parent
//				if ( !isClosed() ) {
//					triggerChildAutoFlush();
//				}
//			} );
			flushMode = FlushMode.AUTO;
		}
		else {
			transactionCompletionCallbacks = new TransactionCompletionCallbacksImpl( this );
			flushMode = FlushMode.MANUAL;
		}
		temporaryPersistenceContext = createPersistenceContext( this );
		influencers = new LoadQueryInfluencers( getFactory() );
		eventListenerGroups = factory.getEventListenerGroups();
		setUpMultitenancy( factory, influencers );
		// A nonzero batch size forces the use of write-behind
		// Therefore, ignore the value of hibernate.jdbc.batch_size
		setJdbcBatchSize( 0 );
	}

	@Override
	public StatelessSessionImplementor getStatelessSession() {
		return this;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// After the "un seal" PR is applied, this should all be consolidated
	// with the Session forms on SharedSessionContract

	@Override
	protected Map<String, Object> getInitialProperties() {
		return new HashMap<>();
	}

	/**
	 * NOTE : this one should be abstract on AbstractSharedSessionContract
	 * and implemented here and SessionImpl...
	 */
	@SuppressWarnings("deprecation")
	protected void interpretProperty(String propertyName, Object value) {
		switch ( propertyName ) {
			case JPA_SHARED_CACHE_RETRIEVE_MODE:
			case JAKARTA_SHARED_CACHE_RETRIEVE_MODE:
				properties.put( JPA_SHARED_CACHE_RETRIEVE_MODE, value );
				properties.put( JAKARTA_SHARED_CACHE_RETRIEVE_MODE, value );
				setCacheMode(
						interpretCacheMode(
								determineCacheStoreMode( properties ),
								(CacheRetrieveMode) value
						)
				);
				break;
			case JPA_SHARED_CACHE_STORE_MODE:
			case JAKARTA_SHARED_CACHE_STORE_MODE:
				properties.put( JPA_SHARED_CACHE_STORE_MODE, value );
				properties.put( JAKARTA_SHARED_CACHE_STORE_MODE, value );
				setCacheMode(
						interpretCacheMode(
								(CacheStoreMode) value,
								determineCacheRetrieveMode( properties )
						)
				);
				break;
			case CRITERIA_COPY_TREE:
				setCriteriaCopyTreeEnabled( parseBoolean( value.toString() ) );
				break;
			case STATEMENT_BATCH_SIZE:
			case HINT_JDBC_BATCH_SIZE:
				setJdbcBatchSize( parseInt( value.toString() ) );
				break;
			default:
				SESSION_LOGGER.tracef( "Unsupported property : %s", propertyName  );
				break;
		}
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean shouldAutoJoinTransaction() {
		return true;
	}

	@Override
	public FlushMode getHibernateFlushMode() {
		// NOTE: only ever *not* MANUAL when this is a "child session"
		return flushMode;
	}

	private StatisticsImplementor getStatistics() {
		return getFactory().getStatistics();
	}

	private CacheMode getCacheModeSafely() {
		var mode = getCacheMode();
		return mode == null ? CacheMode.NORMAL : mode;
	}

	@Override
	public void setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode) {
		var currentMode = getCacheModeSafely();
		setCacheMode( CacheMode.fromJpaModes( cacheRetrieveMode, currentMode.getJpaStoreMode() ) );
	}

	@Override
	public void setCacheStoreMode(CacheStoreMode cacheStoreMode) {
		var currentMode = getCacheModeSafely();
		setCacheMode( CacheMode.fromJpaModes( currentMode.getJpaRetrieveMode(), cacheStoreMode ) );
	}

	@Override
	public CacheRetrieveMode getCacheRetrieveMode() {
		return getCacheModeSafely().getJpaRetrieveMode();
	}

	@Override
	public CacheStoreMode getCacheStoreMode() {
		return getCacheModeSafely().getJpaStoreMode();
	}

	// inserts ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public void insert(Object entity) {
		insert( null, entity );
	}

	@Override
	public void insertMultiple(List<?> entities) {
		final Integer batchSize = getJdbcBatchSize();
		setJdbcBatchSize( entities.size() );
		try {
			for ( Object entity : entities ) {
				insert( null, entity );
			}
		}
		finally {
			setJdbcBatchSize( batchSize );
		}
	}

	@Override
	public Object insert(String entityName, Object entity) {
		checkOpen();
		checkNotReadOnly();
		final var persister = getEntityPersister( entityName, entity );
		final Object id;
		final Object[] state = persister.getValues( entity );
		if ( persister.isVersioned() ) {
			if ( seedVersion( entity, state, persister, this ) ) {
				persister.setValues( entity, state );
			}
		}
		final var generator = persister.getGenerator();
		if ( generator.generatedBeforeExecution( entity, this ) ) {
			if ( !generator.generatesOnInsert() ) {
				throw new IdentifierGenerationException( "Identifier generator must generate on insert" );
			}
			final Object currentValue = generator.allowAssignedIdentifiers() ? persister.getIdentifier( entity ) : null;
			id = ( (BeforeExecutionGenerator) generator ).generate( this, entity, currentValue, INSERT );
			persister.setIdentifier( entity, id, this );
			if ( firePreInsert(entity, id, state, persister) ) {
				return id;
			}
			else {
				getInterceptor().onInsert( entity, id, state, persister.getPropertyNames(), persister.getPropertyTypes() );
				final var eventMonitor = getEventMonitor();
				final var event = eventMonitor.beginEntityInsertEvent();
				boolean success = false;
				try {
					persister.getInsertCoordinator().insert( entity, id, state, this );
					success = true;
				}
				finally {
					eventMonitor.completeEntityInsertEvent( event, id, persister.getEntityName(), success, this );
				}
			}
		}
		else if ( generator.generatedOnExecution( entity, this ) ) {
			if ( !generator.generatesOnInsert() ) {
				throw new IdentifierGenerationException( "Identifier generator must generate on insert" );
			}
			if ( firePreInsert(entity, null, state, persister) ) {
				return null;
			}
			else {
				getInterceptor().onInsert( entity, null, state, persister.getPropertyNames(), persister.getPropertyTypes() );
				final var eventMonitor = getEventMonitor();
				final var event = eventMonitor.beginEntityInsertEvent();
				boolean success = false;
				Object generatedId = null;
				try {
					final var generatedValues = persister.getInsertCoordinator().insert( entity, state, this );
					generatedId = castNonNull( generatedValues ).getGeneratedValue( persister.getIdentifierMapping() );
					id = generatedId;
					success = true;
				}
				finally {
					eventMonitor.completeEntityInsertEvent( event, generatedId, persister.getEntityName(), success, this );
				}
				persister.setIdentifier( entity, id, this );
			}
		}
		else { // assigned identifier
			id = persister.getIdentifier( entity, this );
			if ( id == null ) {
				throw new IdentifierGenerationException( "Identifier of entity '" + persister.getEntityName() + "' must be manually assigned before calling 'insert()'" );
			}
			if ( firePreInsert(entity, id, state, persister) ) {
				return id;
			}
			else {
				getInterceptor().onInsert( entity, id, state, persister.getPropertyNames(), persister.getPropertyTypes() );
				final var eventMonitor = getEventMonitor();
				final var event = eventMonitor.beginEntityInsertEvent();
				boolean success = false;
				try {
					persister.getInsertCoordinator().insert( entity, id, state, this );
					success = true;
				}
				finally {
					eventMonitor.completeEntityInsertEvent( event, id, persister.getEntityName(), success, this );
				}
			}
		}
		recreateCollections( entity, id, persister );
		firePostInsert( entity, id, state, persister );
		final var statistics = getStatistics();
		if ( statistics.isStatisticsEnabled() ) {
			statistics.insertEntity( persister.getEntityName() );
		}
		return id;
	}

	private void recreateCollections(Object entity, Object id, EntityPersister persister) {
		if ( persister.hasOwnedCollections() ) {
			final String entityName = persister.getEntityName();
			final var eventMonitor = getEventMonitor();
			final var statistics = getStatistics();
			forEachOwnedCollection( entity, id, persister,
					(descriptor, collection) -> {
						final String role = descriptor.getRole();
						firePreRecreate( descriptor, collection, id, entityName, entity );
						final var event = eventMonitor.beginCollectionRecreateEvent();
						boolean success = false;
						try {
							descriptor.recreate( collection, id, this );
							success = true;
						}
						finally {
							eventMonitor.completeCollectionRecreateEvent( event, id, role, success, this );
						}
						if ( statistics.isStatisticsEnabled() ) {
							statistics.recreateCollection( role );
						}
						firePostRecreate( descriptor, collection, id, entityName, entity );
					} );
		}
	}

	// deletes ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public void delete(Object entity) {
		delete( null, entity );
	}

	@Override
	public void deleteMultiple(List<?> entities) {
		final Integer batchSize = getJdbcBatchSize();
		setJdbcBatchSize( entities.size() );
		try {
			for ( Object entity : entities ) {
				delete( null, entity );
			}
		}
		finally {
			setJdbcBatchSize( batchSize );
		}
	}

	@Override
	public void delete(String entityName, Object entity) {
		checkOpen();
		checkNotReadOnly();
		final var persister = getEntityPersister( entityName, entity );
		final Object id = persister.getIdentifier( entity, this );
		final Object version = persister.getVersion( entity );
		if ( !firePreDelete(entity, id, persister) ) {
			getInterceptor().onDelete( entity, id, persister.getPropertyNames(), persister.getPropertyTypes() );
			removeCollections( entity, id, persister );
			final Object cacheKey = lockCacheItem( id, version, persister );
			final var eventMonitor = getEventMonitor();
			final var event = eventMonitor.beginEntityDeleteEvent();
			boolean success = false;
			try {
				persister.getDeleteCoordinator().delete( entity, id, version, this );
				success = true;
			}
			finally {
				eventMonitor.completeEntityDeleteEvent( event, id, persister.getEntityName(), success, this );
			}
			removeCacheItem( cacheKey, persister );
			firePostDelete( entity, id, persister );
			final var statistics = getStatistics();
			if ( statistics.isStatisticsEnabled() ) {
				statistics.deleteEntity( persister.getEntityName() );
			}
		}
	}

	private void removeCollections(Object entity, Object id, EntityPersister persister) {
		if ( persister.hasOwnedCollections() ) {
			final String entityName = persister.getEntityName();
			final var eventMonitor = getEventMonitor();
			final var statistics = getStatistics();
			forEachOwnedCollection( entity, id, persister,
					(descriptor, collection) -> {
						final String role = descriptor.getRole();
						firePreRemove( descriptor, collection, id, entityName, entity );
						final var event = eventMonitor.beginCollectionRemoveEvent();
						boolean success = false;
						try {
							descriptor.remove( id, this );
							success = true;
						}
						finally {
							eventMonitor.completeCollectionRemoveEvent( event, id, role, success, this );
						}
						firePostRemove( descriptor, collection, id, entityName, entity );
						if ( statistics.isStatisticsEnabled() ) {
							statistics.removeCollection( role );
						}
					} );
		}
	}


	// updates ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public void update(Object entity) {
		update( null, entity );
	}

	@Override
	public void updateMultiple(List<?> entities) {
		final Integer batchSize = getJdbcBatchSize();
		setJdbcBatchSize( entities.size() );
		try {
			for ( Object entity : entities ) {
				update( null, entity );
			}
		}
		finally {
			setJdbcBatchSize( batchSize );
		}
	}

	@Override
	public void update(String entityName, Object entity) {
		checkOpen();
		checkNotReadOnly();
		final var persister = getEntityPersister( entityName, entity );
		final Object id = persister.getIdentifier( entity, this );
		final Object[] state = persister.getValues( entity );
		final Object oldVersion;
		if ( persister.isVersioned() ) {
			oldVersion = persister.getVersion( entity );
			final Object newVersion = incrementVersion( entity, oldVersion, persister, this );
			setVersion( state, newVersion, persister );
			persister.setValues( entity, state );
		}
		else {
			oldVersion = null;
		}
		if ( !firePreUpdate(entity, id, state, persister) ) {
			getInterceptor().onUpdate( entity, id, state, persister.getPropertyNames(), persister.getPropertyTypes() );
			final Object cacheKey = lockCacheItem( id, oldVersion, persister );
			final var eventMonitor = getEventMonitor();
			final var event = eventMonitor.beginEntityUpdateEvent();
			boolean success = false;
			try {
				persister.getUpdateCoordinator().update( entity, id, null, state, oldVersion, null, null, false, this );
				success = true;
			}
			finally {
				eventMonitor.completeEntityUpdateEvent( event, id, persister.getEntityName(), success, this );
			}
			removeCacheItem( cacheKey, persister );
			removeAndRecreateCollections( entity, id, persister );
			firePostUpdate( entity, id, state, persister );
			final var statistics = getStatistics();
			if ( statistics.isStatisticsEnabled() ) {
				statistics.updateEntity( persister.getEntityName() );
			}
		}
	}

	private void removeAndRecreateCollections(Object entity, Object id, EntityPersister persister) {
		if ( persister.hasOwnedCollections() ) {
			final String entityName = persister.getEntityName();
			final var eventMonitor = getEventMonitor();
			final var statistics = getStatistics();
			forEachOwnedCollection( entity, id, persister,
					(descriptor, collection) -> {
						final String role = descriptor.getRole();
						firePreUpdate( descriptor, collection, id, entityName, entity );
						final var event = eventMonitor.beginCollectionRemoveEvent();
						boolean success = false;
						try {
							// TODO: can we do better here?
							descriptor.remove( id, this );
							descriptor.recreate( collection, id, this );
							success = true;
						}
						finally {
							eventMonitor.completeCollectionRemoveEvent( event, id, role, success, this );
						}
						firePostUpdate( descriptor, collection, id, entityName, entity );
						if ( statistics.isStatisticsEnabled() ) {
							statistics.updateCollection( role );
						}
					} );
		}
	}

	@Override
	public void upsert(Object entity) {
		upsert( null, entity );
	}

	@Override
	public void upsertMultiple(List<?> entities) {
		final Integer batchSize = getJdbcBatchSize();
		setJdbcBatchSize( entities.size() );
		try {
			for ( Object entity : entities ) {
				upsert( null, entity );
			}
		}
		finally {
			setJdbcBatchSize( batchSize );
		}
	}

	@Override
	public void upsert(String entityName, Object entity) {
		checkOpen();
		checkNotReadOnly();
		final var persister = getEntityPersister( entityName, entity );
		final Object id = idToUpsert( entity, persister );
		final Object[] state = persister.getValues( entity );
		if ( !firePreUpsert(entity, id, state, persister) ) {
			getInterceptor().onUpsert( entity, id, state, persister.getPropertyNames(), persister.getPropertyTypes() );
			final Object oldVersion = versionToUpsert( entity, persister, state );
			final Object cacheKey = lockCacheItem( id, oldVersion, persister );
			final var eventMonitor = getEventMonitor();
			final var event = eventMonitor.beginEntityUpsertEvent();
			boolean success = false;
			try {
				persister.getMergeCoordinator().update( entity, id, null, state, oldVersion, null, null, false, this );
				success = true;
			}
			finally {
				eventMonitor.completeEntityUpsertEvent( event, id, persister.getEntityName(), success, this );
			}
			removeCacheItem( cacheKey, persister );
			final var statistics = getStatistics();
			if ( statistics.isStatisticsEnabled() ) {
				statistics.upsertEntity( persister.getEntityName() );
			}
			removeAndRecreateCollections( entity, id, persister );
			firePostUpsert(entity, id, state, persister);
		}
	}

	// Hibernate Reactive calls this
	protected Object versionToUpsert(Object entity, EntityPersister persister, Object[] state) {
		if ( persister.isVersioned() ) {
			final Object oldVersion = persister.getVersion( entity );
			final Boolean knownTransient =
					persister.getVersionMapping()
							.getUnsavedStrategy()
							.isUnsaved( oldVersion );
			if ( knownTransient != null && knownTransient ) {
				if ( seedVersion( entity, state, persister, this ) ) {
					persister.setValues( entity, state );
				}
				// this is a nonsense but avoids setting version restriction
				// parameter to null later on deep in the guts
				return state[persister.getVersionPropertyIndex()];
			}
			else {
				final Object newVersion = incrementVersion( entity, oldVersion, persister, this );
				setVersion( state, newVersion, persister );
				persister.setValues( entity, state );
				return oldVersion;
			}
		}
		else {
			return null;
		}
	}

	// Hibernate Reactive calls this
	protected Object idToUpsert(Object entity, EntityPersister persister) {
		final Object id = persister.getIdentifier( entity, this );
		final Boolean unsaved =
				persister.getIdentifierMapping()
						.getUnsavedStrategy()
						.isUnsaved( id );
		if ( unsaved != null && unsaved ) {
			throw new TransientObjectException( "Object passed to upsert() has an unsaved identifier value: "
					+ persister.getEntityName() );
		}
		return id;
	}

	// event processing ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	// Hibernate Reactive may need to call this
	protected boolean firePreInsert(Object entity, Object id, Object[] state, EntityPersister persister) {
		persister.getEntityCallbacks().preCreate( entity );

		if ( eventListenerGroups.eventListenerGroup_PRE_INSERT.isEmpty() ) {
			return false;
		}
		else {
			boolean veto = false;
			final var event = new PreInsertEvent( entity, id, state, persister, this );
			for ( var listener : eventListenerGroups.eventListenerGroup_PRE_INSERT.listeners() ) {
				veto |= listener.onPreInsert( event );
			}
			return veto;
		}
	}

	// Hibernate Reactive may need to call this
	protected boolean firePreUpdate(Object entity, Object id, Object[] state, EntityPersister persister) {
		persister.getEntityCallbacks().preUpdate( entity );

		if ( eventListenerGroups.eventListenerGroup_PRE_UPDATE.isEmpty() ) {
			return false;
		}
		else {
			boolean veto = false;
			final var event = new PreUpdateEvent( entity, id, state, null, persister, this );
			for ( var listener : eventListenerGroups.eventListenerGroup_PRE_UPDATE.listeners() ) {
				veto |= listener.onPreUpdate( event );
			}
			return veto;
		}
	}

	// Hibernate Reactive may need to call this
	protected boolean firePreUpsert(Object entity, Object id, Object[] state, EntityPersister persister) {
		if ( eventListenerGroups.eventListenerGroup_PRE_UPSERT.isEmpty() ) {
			return false;
		}
		else {
			boolean veto = false;
			final var event = new PreUpsertEvent( entity, id, state, persister, this );
			for ( var listener : eventListenerGroups.eventListenerGroup_PRE_UPSERT.listeners() ) {
				veto |= listener.onPreUpsert( event );
			}
			return veto;
		}
	}

	// Hibernate Reactive may need to call this
	protected boolean firePreDelete(Object entity, Object id, EntityPersister persister) {
		persister.getEntityCallbacks().preRemove( entity );

		if ( eventListenerGroups.eventListenerGroup_PRE_DELETE.isEmpty() ) {
			return false;
		}
		else {
			boolean veto = false;
			final var event = new PreDeleteEvent( entity, id, null, persister, this );
			for ( var listener : eventListenerGroups.eventListenerGroup_PRE_DELETE.listeners() ) {
				veto |= listener.onPreDelete( event );
			}
			return veto;
		}
	}

	// Hibernate Reactive may need to call this
	protected void firePostInsert(Object entity, Object id, Object[] state, EntityPersister persister) {
		eventListenerGroups.eventListenerGroup_POST_INSERT.fireLazyEventOnEachListener(
				() -> new PostInsertEvent( entity, id, state, persister, this ),
				PostInsertEventListener::onPostInsert );
	}

	// Hibernate Reactive may need to call this
	protected void firePostUpdate(Object entity, Object id, Object[] state, EntityPersister persister) {
		eventListenerGroups.eventListenerGroup_POST_UPDATE.fireLazyEventOnEachListener(
				() -> new PostUpdateEvent( entity, id, state, null, null, persister, this ),
				PostUpdateEventListener::onPostUpdate );
	}

	// Hibernate Reactive may need to call this
	protected void firePostUpsert(Object entity, Object id, Object[] state, EntityPersister persister) {
		eventListenerGroups.eventListenerGroup_POST_UPSERT.fireLazyEventOnEachListener(
				() -> new PostUpsertEvent( entity, id, state, null, persister, this ),
				PostUpsertEventListener::onPostUpsert );
	}

	// Hibernate Reactive may need to call this
	protected void firePostDelete(Object entity, Object id, EntityPersister persister) {
		eventListenerGroups.eventListenerGroup_POST_DELETE.fireLazyEventOnEachListener(
				() -> new PostDeleteEvent( entity, id, null, persister, this ),
				PostDeleteEventListener::onPostDelete );
	}

	// Hibernate Reactive may need to call this
	protected void firePreRecreate(
			CollectionPersister collectionPersister,
			PersistentCollection<?> collection,
			Object id,
			String entityName,
			Object owner) {
		eventListenerGroups.eventListenerGroup_PRE_COLLECTION_RECREATE.fireLazyEventOnEachListener(
				() -> new PreCollectionRecreateEvent( collectionPersister, collection, id, entityName, owner ),
				PreCollectionRecreateEventListener::onPreRecreateCollection );
	}

	// Hibernate Reactive may need to call this
	protected void firePreUpdate(
			CollectionPersister collectionPersister,
			PersistentCollection<?> collection,
			Object id,
			String entityName,
			Object owner) {
		eventListenerGroups.eventListenerGroup_PRE_COLLECTION_UPDATE.fireLazyEventOnEachListener(
				() -> new PreCollectionUpdateEvent( collectionPersister, collection, id, entityName, owner ),
				PreCollectionUpdateEventListener::onPreUpdateCollection );
	}

	// Hibernate Reactive may need to call this
	protected void firePreRemove(
			CollectionPersister collectionPersister,
			PersistentCollection<?> collection,
			Object id,
			String entityName,
			Object owner) {
		eventListenerGroups.eventListenerGroup_PRE_COLLECTION_REMOVE.fireLazyEventOnEachListener(
				() -> new PreCollectionRemoveEvent( collectionPersister, collection, id, entityName, owner ),
				PreCollectionRemoveEventListener::onPreRemoveCollection );
	}

	// Hibernate Reactive may need to call this
	protected void firePostRecreate(
			CollectionPersister collectionPersister,
			PersistentCollection<?> collection,
			Object id,
			String entityName,
			Object owner) {
		eventListenerGroups.eventListenerGroup_POST_COLLECTION_RECREATE.fireLazyEventOnEachListener(
				() -> new PostCollectionRecreateEvent( collectionPersister, collection, id, entityName, owner ),
				PostCollectionRecreateEventListener::onPostRecreateCollection );
	}

	// Hibernate Reactive may need to call this
	protected void firePostUpdate(
			CollectionPersister collectionPersister,
			PersistentCollection<?> collection,
			Object id,
			String entityName,
			Object owner) {
		eventListenerGroups.eventListenerGroup_POST_COLLECTION_UPDATE.fireLazyEventOnEachListener(
				() -> new PostCollectionUpdateEvent( collectionPersister, collection, id, entityName, owner ),
				PostCollectionUpdateEventListener::onPostUpdateCollection );
	}

	// Hibernate Reactive may need to call this
	protected void firePostRemove(
			CollectionPersister collectionPersister,
			PersistentCollection<?> collection,
			Object id,
			String entityName,
			Object owner) {
		eventListenerGroups.eventListenerGroup_POST_COLLECTION_REMOVE.fireLazyEventOnEachListener(
				() -> new PostCollectionRemoveEvent( collectionPersister, collection, id, entityName, owner ),
				PostCollectionRemoveEventListener::onPostRemoveCollection );
	}

	// collections ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	// Hibernate Reactive calls this
	protected void forEachOwnedCollection(
			Object entity, Object key,
			EntityPersister persister, BiConsumer<CollectionPersister, PersistentCollection<?>> action) {
		persister.visitAttributeMappings( attribute -> {
			if ( attribute.isPluralAttributeMapping() ) {
				final var descriptor = attribute.asPluralAttributeMapping().getCollectionDescriptor();
				final Object cacheKey = lockCacheItem( key, descriptor );
				if ( !descriptor.isInverse() ) {
					final Object value = attribute.getPropertyAccess().getGetter().get(entity);
					final PersistentCollection<?> collection;
					if ( value instanceof PersistentCollection<?> persistentCollection ) {
						if ( !persistentCollection.wasInitialized() ) {
							return;
						}
						collection = persistentCollection;
					}
					else {
						collection = wrapOrInstantiateCollection( entity, key, value, descriptor );
					}
					action.accept( descriptor, collection );
				}
				removeCacheItem( cacheKey, descriptor );
			}
		} );
	}

	private PersistentCollection<?> wrapOrInstantiateCollection(
			Object entity, Object key, Object value, CollectionPersister descriptor) {
		return value == null
				? instantiateEmpty( key, descriptor )
				: wrap( descriptor, value, entity );
	}

	// Hibernate Reactive calls this
	protected PersistentCollection<?> instantiateEmpty(Object key, CollectionPersister descriptor) {
		return descriptor.getCollectionSemantics().instantiateWrapper(key, descriptor, this);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	protected PersistentCollection<?> wrap(CollectionPersister descriptor, Object collection, Object owner) {
		final CollectionSemantics collectionSemantics = descriptor.getCollectionSemantics();
		var wrapped = collectionSemantics.wrap( collection, descriptor, this );
		wrapped.setOwner( owner );
		return wrapped;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// loading

	@Override
	protected <T> FindByKeyOperation<T> byKey(EntityPersister entityDescriptor, FindOption... options) {
		return byKey( entityDescriptor, null, null, options );
	}

	@Override
	protected <T> FindByKeyOperation<T> byKey(
			EntityPersister entityDescriptor,
			GraphSemantic graphSemantic,
			RootGraphImplementor<?> rootGraph,
			FindOption... options) {
		return new StatelessFindByKeyOperation<>(
				entityDescriptor,
				this,
				graphSemantic,
				rootGraph,
				null,
				getCacheMode(),
				isDefaultReadOnly(),
				getFactory(),
				options
		);
	}

	@Override
	public <T> T get(Class<T> entityClass, Object id, LockMode lockMode) {
		return get( entityClass, id, (FindOption) lockMode );
	}

	@Override
	public Object get(String entityName, Object key, FindOption... findOptions) {
		var result = find( entityName, key, findOptions );
		if ( result == null ) {
			throw notFound( entityName, key );
		}
		return result;
	}

	@Override
	public Object get(String entityName, Object id, LockMode lockMode) {
		return get( entityName, id, (FindOption) lockMode );
	}

	@Override
	public <T> T get(EntityGraph<T> entityGraph, Object id) {
		var result = find( entityGraph, id );
		if ( result == null ) {
			final var graph = (RootGraphImplementor<T>) entityGraph;
			throw notFound( graph.getGraphedType().getTypeName(), id );
		}
		return result;
	}

	@Override
	public <T> T get(EntityGraph<T> entityGraph, Object id, LockMode lockMode) {
		var result = find( entityGraph, id, lockMode );
		if ( result == null ) {
			final var graph = (RootGraphImplementor<T>) entityGraph;
			throw notFound( graph.getGraphedType().getTypeName(), id );
		}
		return result;
	}

	@Override
	public <T> T get(EntityGraph<T> graph, GraphSemantic graphSemantic, Object id) {
		return get( graph, graphSemantic, id, LockMode.NONE );
	}

	@Override @SuppressWarnings("unchecked")
	public  <T> T get(
			EntityGraph<T> graph, GraphSemantic graphSemantic,
			Object id, LockMode lockMode) {
		final var rootGraph = (RootGraphImplementor<T>) graph;
		checkOpen();

		final var effectiveEntityGraph = getLoadQueryInfluencers().getEffectiveEntityGraph();
		effectiveEntityGraph.applyGraph( rootGraph, graphSemantic );

		try {
			return (T) get( rootGraph.getGraphedType().getTypeName(), id, lockMode );
		}
		finally {
			effectiveEntityGraph.clear();
		}
	}

	@Override
	public <T> List<T> findMultiple(Class<T> entityClass, List<?> keys, FindOption... findOptions) {
		checkOpen();

		final var entityDescriptor = requireEntityPersister( entityClass.getName() );
		var operation = new StatelessFindMultipleByKeyOperation<T>(
				entityDescriptor,
				this,
				null,
				getCacheMode(),
				isDefaultReadOnly(),
				getFactory(),
				findOptions
		);
		return operation.performFind( keys, null, null );
	}

	@Override
	public <T> List<T> findMultiple(EntityGraph<T> entityGraph, List<?> keys, FindOption... findOptions) {
		checkOpen();

		final var graph = (RootGraphImplementor<T>) entityGraph;
		final var type = graph.getGraphedType();

		final EntityPersister entityDescriptor = switch ( type.getRepresentationMode() ) {
			case POJO -> requireEntityPersister( type.getJavaType() );
			case MAP -> requireEntityPersister( type.getTypeName() );
		};
		var operation = new StatelessFindMultipleByKeyOperation<T>(
				entityDescriptor,
				this,
				null,
				getCacheMode(),
				isDefaultReadOnly(),
				getFactory(),
				findOptions
		);
		return operation.performFind( keys, GraphSemantic.LOAD, graph );
	}

	@Override
	public <T> List<T> getMultiple(Class<T> entityClass, List<?> ids, LockMode lockMode) {
		checkOpen();

		final var persister = requireEntityPersister( entityClass.getName() );
		var operation = new StatelessFindMultipleByKeyOperation<T>(
				persister,
				this,
				null,
				getCacheMode(),
				isDefaultReadOnly(),
				getFactory(),
				lockMode
		);
		return operation.performFind( ids, null, null );
	}

	@Override
	public <T> List<T> getMultiple(EntityGraph<T> entityGraph, List<?> ids) {
		return getMultiple( entityGraph, GraphSemantic.LOAD, ids );
	}

	@Override
	public <T> List<T> getMultiple(EntityGraph<T> entityGraph, GraphSemantic graphSemantic, List<?> ids) {
		final var rootGraph = (RootGraphImplementor<T>) entityGraph;
		final var entityDescriptor = requireEntityPersister( rootGraph.getGraphedType().getTypeName() );
		var operation = new StatelessFindMultipleByKeyOperation<T>(
				entityDescriptor,
				this,
				null,
				getCacheMode(),
				isDefaultReadOnly(),
				getFactory()
		);
		return operation.performFind( ids, GraphSemantic.LOAD, rootGraph );
	}

	@Override
	public <T> List<T> getMultiple(Class<T> entityClass, List<?> ids) {
		final var entityDescriptor = requireEntityPersister( entityClass.getName() );
		var operation = new StatelessFindMultipleByKeyOperation<T>(
				entityDescriptor,
				this,
				null,
				getCacheMode(),
				isDefaultReadOnly(),
				getFactory()
		);
		return operation.performFind( ids, null, null );
	}

	@Override
	public void refresh(Object entity) {
		refresh( bestGuessEntityName( entity ), entity, LockMode.NONE );
	}

	@Override
	public void refresh(String entityName, Object entity) {
		refresh( entityName, entity, LockMode.NONE );
	}

	@Override
	public void refresh(Object entity, LockMode lockMode) {
		refresh( bestGuessEntityName( entity ), entity, lockMode );
	}

	@Override
	public void refresh(String entityName, Object entity, LockMode lockMode) {
		checkOpen();
		final var persister = getEntityPersister( entityName, entity );
		final Object id = persister.getIdentifier( entity, this );
		if ( SESSION_LOGGER.isTraceEnabled() ) {
			SESSION_LOGGER.refreshingTransient( infoString( persister, id, getFactory() ) );
		}

		if ( persister.canWriteToCache() ) {
			final var cacheAccess = persister.getCacheAccessStrategy();
			if ( cacheAccess != null ) {
				final Object cacheKey = cacheAccess.generateCacheKey(
						id,
						persister,
						getFactory(),
						getTenantIdentifier()
				);
				cacheAccess.evict( cacheKey );
			}
		}

		final Object result =
				getLoadQueryInfluencers()
						// TODO: This is wrong. StatelessSession is not supposed
						//       to be affected by cascade = REFRESH. Fix in H8.
						.fromInternalFetchProfile( CascadingFetchProfile.REFRESH,
								() -> persister.load( id, entity, getNullSafeLockMode( lockMode ), this ) );
		UnresolvableObjectException.throwIfNull( result, id, persister.getEntityName() );
		if ( temporaryPersistenceContext.isLoadFinished() ) {
			temporaryPersistenceContext.clear();
		}
	}

	@Override
	public void refresh(Object entity, LockModeType lockModeType) {
		refresh( bestGuessEntityName( entity ), entity, LockMode.fromJpaLockMode( lockModeType ) );
	}

	@Override
	public void refreshMultiple(List<?> entities) {
		checkOpen();

		// NOTE: the incoming list of entities can be heterogeneous - the elements
		// need not be all the same type.

		// for now, a simple implementation -
		// todo (jpa4) : we will definitely want to improve this.

		entities.forEach( this::refresh );
	}

	@Override
	public Object immediateLoad(String entityName, Object id) {
		if ( getPersistenceContextInternal().isLoadFinished() ) {
			throw new SessionException( "proxies cannot be fetched by a stateless session" );
		}
		// unless we are still in the process of handling a top-level load
		return get( entityName, id );
	}

	@Override
	public void initializeCollection(PersistentCollection<?> collection, boolean writing) {
		checkOpen();
		final var persistenceContext = getPersistenceContextInternal();
		final var ce = persistenceContext.getCollectionEntry( collection );
		if ( ce == null ) {
			throw new HibernateException( "no entry for collection" );
		}
		if ( !collection.wasInitialized() ) {
			final var loadedPersister = ce.getLoadedPersister();
			final Object loadedKey = ce.getLoadedKey();
			if ( SESSION_LOGGER.isTraceEnabled() ) {
				SESSION_LOGGER.initializingCollection(
						collectionInfoString( loadedPersister, collection, loadedKey, this ) );
			}
			final boolean foundInCache =
					initializeCollectionFromCache( loadedKey, loadedPersister, collection, this );
			if ( foundInCache ) {
				SESSION_LOGGER.collectionInitializedFromCache();
			}
			else {
				loadedPersister.initialize( loadedKey, this );
				handlePotentiallyEmptyCollection( collection, persistenceContext, loadedKey, loadedPersister );
				SESSION_LOGGER.collectionInitialized();
				final var statistics = getStatistics();
				if ( statistics.isStatisticsEnabled() ) {
					statistics.fetchCollection( loadedPersister.getRole() );
				}
			}
		}
	}

	@Override @Deprecated
	public Object instantiate(String entityName, Object id) {
		return instantiate( requireEntityPersister( entityName ), id );
	}

	@Override
	public Object instantiate(EntityPersister persister, Object id) {
		checkOpen();
		return persister.instantiate( id, this );
	}

	@Override
	public Object internalLoad(
			String entityName,
			Object id,
			boolean eager,
			boolean nullable) {
		checkOpen();

		final var persister = requireEntityPersister( entityName );
		final var entityKey = generateEntityKey( id, persister );

		// First, try to load it from the temporary PersistenceContext
		final var persistenceContext = getPersistenceContext();
		final var holder = persistenceContext.getEntityHolder( entityKey );
		if ( holder != null && holder.getEntity() != null ) {
			// We found it in the temporary persistence context.
			// Should indicate we are in the midst of processing a
			// result set containing eager fetches via join fetch.
			return holder.getEntity();
		}

		if ( !eager ) {
			// The caller did not request forceful eager loading;
			// see if we can create some form of proxy.

			// first, check to see if we can use "bytecode proxies"
			final var enhancementMetadata = persister.getBytecodeEnhancementMetadata();
			if ( enhancementMetadata.isEnhancedForLazyLoading() ) {
				// If the entity defines a HibernateProxy factory,
				// see if there is an existing proxy associated with
				// the persistence context - and if so, use it
				if ( persister.getRepresentationStrategy().getProxyFactory() != null ) {
					final Object proxy = holder == null ? null : holder.getProxy();

					if ( proxy != null ) {
						SESSION_LOGGER.entityProxyFoundInSessionCache();
						if ( SESSION_LOGGER.isDebugEnabled() && extractLazyInitializer( proxy ).isUnwrap() ) {
							SESSION_LOGGER.ignoringNoProxyToHonorLaziness();
						}

						return persistenceContext.narrowProxy( proxy, persister, entityKey, null );
					}

					// Specialized handling for entities with subclasses with
					// a HibernateProxy factory.
					if ( persister.hasSubclasses() ) {
						// Entities with subclasses that define a ProxyFactory
						// can create a HibernateProxy.
						SESSION_LOGGER.creatingHibernateProxyToHonorLaziness();
						return createProxy( entityKey );
					}
					return enhancementMetadata.createEnhancedProxy( entityKey, false, this );
				}
				else if ( !persister.hasSubclasses() ) {
					return enhancementMetadata.createEnhancedProxy( entityKey, false, this );
				}
				// If we get here, then the entity class has subclasses and there
				// is no HibernateProxy factory. The entity will be loaded below.
			}
			else {
				if ( persister.hasProxy() ) {
					final Object existingProxy = holder == null ? null : holder.getProxy();
					return existingProxy != null
							? persistenceContext.narrowProxy( existingProxy, persister, entityKey, null )
							: createProxy( entityKey );
				}
			}
		}

		// Otherwise, immediately materialize it.
		return internalLoadGet( entityName, id, persistenceContext );
	}

	// For Hibernate Reactive
	protected Object internalLoadGet(String entityName, Object id, PersistenceContext persistenceContext) {
		// Increment/decrement the load count before/after getting the value
		// to ensure that #get does not clear the PersistenceContext.
		persistenceContext.beforeLoad();
		try {
			return get( entityName, id );
		}
		finally {
			persistenceContext.afterLoad();
		}
	}

	private Object createProxy(EntityKey entityKey) {
		final Object proxy = entityKey.getPersister().createProxy( entityKey.getIdentifier(), this );
		getPersistenceContext().addProxy( entityKey, proxy );
		return proxy;
	}

	@Override
	public <T> T fetch(T association) {
		checkOpen();
		final var persistenceContext = getPersistenceContext();
		final var initializer = extractLazyInitializer( association );
		if ( initializer != null ) {
			if ( initializer.isUninitialized() ) {
				final String entityName = initializer.getEntityName();
				final Object id = initializer.getInternalIdentifier();
				initializer.setSession( this );
				persistenceContext.beforeLoad();
				try {
					final Object entity = initializer.getImplementation(); //forces the load to occur
					if ( entity==null ) {
						getFactory().getEntityNotFoundDelegate().handleEntityNotFound( entityName, id );
					}
					initializer.setImplementation( entity );
				}
				finally {
					initializer.unsetSession();
					persistenceContext.afterLoad();
					if ( persistenceContext.isLoadFinished() ) {
						persistenceContext.clear();
					}
				}
			}
		}
		else if ( isPersistentAttributeInterceptable( association ) ) {
			if ( asPersistentAttributeInterceptable( association ).$$_hibernate_getInterceptor()
					instanceof EnhancementAsProxyLazinessInterceptor proxyInterceptor ) {
				proxyInterceptor.setSession( this );
				try {
					proxyInterceptor.forceInitialize( association, null );
					// TODO: statistics?? call statistics.fetchEntity()
				}
				finally {
					proxyInterceptor.unsetSession();
					if ( persistenceContext.isLoadFinished() ) {
						persistenceContext.clear();
					}
				}
			}
		}
		else if ( association instanceof PersistentCollection<?> collection ) {
			if ( !collection.wasInitialized() ) {
				final var collectionDescriptor = requireCollectionPersister( collection.getRole() );
				final Object key = collection.getKey();
				persistenceContext.addUninitializedCollection( collectionDescriptor, collection, key );
				collection.setCurrentSession( this );
				try {
					final boolean foundInCache =
							initializeCollectionFromCache( key, collectionDescriptor, collection, this );
					if ( foundInCache ) {
						SESSION_LOGGER.collectionFetchedFromCache();
					}
					else {
						collectionDescriptor.initialize( key, this );
						handlePotentiallyEmptyCollection( collection, getPersistenceContextInternal(), key,
								collectionDescriptor );
						SESSION_LOGGER.collectionFetched();
						final var statistics = getStatistics();
						if ( statistics.isStatisticsEnabled() ) {
							statistics.fetchCollection( collectionDescriptor.getRole() );
						}
					}
				}
				finally {
					collection.$$_hibernate_setInstanceId( 0 );
					collection.unsetSession( this );
					if ( persistenceContext.isLoadFinished() ) {
						persistenceContext.clear();
					}
				}
			}
		}

		// per JPA, always just returns the argument
		return association;
	}

	@Override
	public Object getIdentifier(Object entity) {
		checkOpen();
		return getFactory().getPersistenceUnitUtil().getIdentifier(entity);
	}

	public boolean isAutoCloseSessionEnabled() {
		return getSessionFactoryOptions().isAutoCloseSessionEnabled();
	}

	public boolean shouldAutoClose() {
		return isAutoCloseSessionEnabled() && !isClosed();
	}

	private boolean isFlushModeNever() {
		return false;
	}

	private void managedClose() {
		if ( isClosed() ) {
			throw new SessionException( "Session was already closed" );
		}
		close();
	}

	private void managedFlush() {
		checkOpen();
		getJdbcCoordinator().executeBatch();
	}

	@Override
	public void propagateFlush() {
		if ( isClosed() ) {
			return;
		}
		SESSION_LOGGER.automaticallyFlushingChildSession();
		getJdbcCoordinator().executeBatch();
	}

	@Override
	protected void propagateClose() {
		if ( isClosed() ) {
			return;
		}
		SESSION_LOGGER.automaticallyClosingChildSession();
		close();
	}

	@Override
	public String bestGuessEntityName(Object object) {
		final var lazyInitializer = extractLazyInitializer( object );
		if ( lazyInitializer != null ) {
			object = lazyInitializer.getImplementation();
		}
		return guessEntityName( object );
	}

	@Override
	public Object getContextEntityIdentifier(Object object) {
		checkOpen();
		return null;
	}

	@Override
	public String guessEntityName(Object entity) {
		checkOpen();
		if ( entity == null ) {
			throw new IllegalArgumentException( "Entity may not be null" );
		}
		return entity.getClass().getName();
	}

	@Override
	public EntityPersister getEntityPersister(String entityName, Object entity) {
		checkOpen();
		if ( entity == null ) {
			throw new IllegalArgumentException( "Entity may not be null" );
		}
		return entityName == null
				? requireEntityPersister( guessEntityName( entity ) )
				: requireEntityPersister( entityName ).getSubclassEntityPersister( entity, getFactory() );
	}

	@Override
	public Object getEntityUsingInterceptor(EntityKey key) {
		checkOpen();

		final var persistenceContext = getPersistenceContext();
		final Object result = persistenceContext.getEntity( key );
		if ( result != null ) {
			return result;
		}

		final Object newObject = getInterceptor().getEntity( key.getEntityName(), key.getIdentifier() );
		if ( newObject != null ) {
			persistenceContext.addEntity( key, newObject );
			return newObject;
		}

		return null;
	}

	@Override
	public PersistenceContext getPersistenceContext() {
		return temporaryPersistenceContext;
	}

	public boolean isDefaultReadOnly() {
		return false;
	}

	@Override
	public boolean isIdentifierRollbackEnabled() {
		// not yet implemented
		return false;
	}

	@Override
	public boolean isManaged(Object entity) {
		return false;
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////

	//TODO: COPY/PASTE FROM SessionImpl, pull up!


	public void afterOperation(boolean success) {
		temporaryPersistenceContext.clear();
		if ( !isTransactionInProgress() ) {
			getJdbcCoordinator().afterTransaction();
		}
	}

	@Override
	public void afterScrollOperation() {
		temporaryPersistenceContext.clear();
	}

	@Override
	public boolean autoPreFlushIfRequired(QueryParameterBindings parameterBindings) {
		return false;
	}

	@Override
	public void flush() {
	}

	@Override
	public LoadQueryInfluencers getLoadQueryInfluencers() {
		return influencers;
	}

	@Override
	public PersistenceContext getPersistenceContextInternal() {
		//In this case implemented the same as #getPersistenceContext
		return temporaryPersistenceContext;
	}

	@Override
	public boolean autoFlushIfRequired(Set<String> querySpaces, boolean skipPreFlush) {
		return false;
	}

	@Override
	public void afterTransactionBegin() {
		afterTransactionBeginEvents();
	}

	@Override
	public void beforeTransactionCompletion() {
		transactionCompletionCallbacks.beforeTransactionCompletion();
		flushBeforeTransactionCompletion();
		beforeTransactionCompletionEvents();
	}

	@Override
	public void afterTransactionCompletion(boolean successful, boolean delayed) {
		transactionCompletionCallbacks.afterTransactionCompletion( successful );
		afterTransactionCompletionEvents( successful );
		if ( shouldAutoClose() && !isClosed() ) {
			managedClose();
		}
	}

	@Override
	public boolean isTransactionInProgress() {
		return connectionProvided || super.isTransactionInProgress();
	}

	@Override
	public TransactionCompletionCallbacks getTransactionCompletionCallbacks() {
		return transactionCompletionCallbacks;
	}

	@Override
	public void flushBeforeTransactionCompletion() {
		if ( shouldFlushBeforeCompletion() ) {
			managedFlush();
		}
	}

	private boolean shouldFlushBeforeCompletion() {
		try {
			return !isClosed()
				&& !isFlushModeNever()
				&& !isRollback( getJtaPlatform().getCurrentStatus() );
		}
		catch ( SystemException se ) {
			throw new HibernateException( "could not determine transaction status in beforeCompletion()", se );
		}
	}

	private JtaPlatform getJtaPlatform() {
		return getFactory().getServiceRegistry().requireService( JtaPlatform.class );
	}

	private LockMode getNullSafeLockMode(LockMode lockMode) {
		return lockMode == null ? LockMode.NONE : lockMode;
	}

	protected Object lockCacheItem(Object id, Object previousVersion, EntityPersister persister) {
		if ( persister.canWriteToCache() ) {
			final var cache = persister.getCacheAccessStrategy();
			final Object cacheKey = cache.generateCacheKey(
					id,
					persister,
					getFactory(),
					getTenantIdentifier()
			);
			final var lock = cache.lockItem( this, cacheKey, previousVersion );
			transactionCompletionCallbacks.registerCallback( (success, session) -> {
				cache.unlockItem( session, cacheKey, lock );
			} );
			return cacheKey;
		}
		else {
			return null;
		}
	}

	protected void removeCacheItem(Object cacheKey, EntityPersister persister) {
		if ( persister.canWriteToCache() ) {
			persister.getCacheAccessStrategy().remove( this, cacheKey );
		}
	}

	protected Object lockCacheItem(Object key, CollectionPersister persister) {
		if ( persister.hasCache() ) {
			final var cache = persister.getCacheAccessStrategy();
			final Object cacheKey = cache.generateCacheKey(
					key,
					persister,
					getFactory(),
					getTenantIdentifier()
			);
			final var lock = cache.lockItem( this, cacheKey, null );
			transactionCompletionCallbacks.registerCallback( (success, session) -> {
				cache.unlockItem( this, cacheKey, lock );
			} );
			return cacheKey;
		}
		else {
			return null;
		}
	}

	protected void removeCacheItem(Object cacheKey, CollectionPersister persister) {
		if ( persister.hasCache() ) {
			persister.getCacheAccessStrategy().remove( this, cacheKey );
		}
	}

	@Override
	public void lock(String entityName, Object child, LockOptions lockOptions) {
		final var persister = getEntityPersister( entityName, child );
		persister.lock( persister.getIdentifier( child ), persister.getVersion( child ), child, lockOptions, this );
		final var entry = getPersistenceContextInternal().getEntry( child );
		if ( entry == null ) {
			throw new AssertionFailure( "no entry in temporary persistence context" );
		}
		LoaderHelper.upgradeLock( child, entry, lockOptions, this );
	}

	@Override
	public Object loadFromSecondLevelCache(EntityPersister persister, EntityKey entityKey, Object instanceToLoad, LockMode lockMode) {
		return CacheLoadHelper.loadFromSecondLevelCache( this, instanceToLoad, lockMode, persister, entityKey );
	}

	@Override
	public TransactionCompletionCallbacksImplementor getTransactionCompletionCallbacksImplementor() {
		return transactionCompletionCallbacks.forSharing();
	}

	@Override
	public <T> T unwrap(Class<T> type) {
		checkOpen();

		if ( type.isInstance( this ) ) {
			return type.cast( this );
		}

		throw new PersistenceException( "Hibernate cannot unwrap '" + getClass().getName()
										+ "' as '" + type.getName() + "'" );
	}

}
