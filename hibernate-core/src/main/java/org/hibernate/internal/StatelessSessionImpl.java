/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

import org.hibernate.AssertionFailure;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.SessionException;
import org.hibernate.StatelessSession;
import org.hibernate.TransientObjectException;
import org.hibernate.UnresolvableObjectException;
import org.hibernate.action.spi.AfterTransactionCompletionProcess;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.bytecode.spi.BytecodeEnhancementMetadata;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.EffectiveEntityGraph;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityHolder;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.transaction.internal.jta.JtaStatusHelper;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.event.monitor.spi.EventMonitor;
import org.hibernate.event.monitor.spi.DiagnosticEvent;
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
import org.hibernate.event.spi.PreDeleteEventListener;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreInsertEventListener;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.hibernate.event.spi.PreUpsertEvent;
import org.hibernate.event.spi.PreUpsertEventListener;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.Generator;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.id.IdentifierGenerationException;
import org.hibernate.loader.ast.internal.LoaderHelper;
import org.hibernate.loader.ast.spi.CascadingFetchProfile;
import org.hibernate.loader.ast.spi.MultiIdLoadOptions;
import org.hibernate.loader.internal.CacheLoadHelper;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.tuple.entity.EntityMetamodel;

import jakarta.persistence.EntityGraph;
import jakarta.transaction.SystemException;

import static org.hibernate.engine.internal.ManagedTypeHelper.asPersistentAttributeInterceptable;
import static org.hibernate.engine.internal.ManagedTypeHelper.isPersistentAttributeInterceptable;
import static org.hibernate.engine.internal.PersistenceContexts.createPersistenceContext;
import static org.hibernate.engine.internal.Versioning.incrementVersion;
import static org.hibernate.engine.internal.Versioning.seedVersion;
import static org.hibernate.engine.internal.Versioning.setVersion;
import static org.hibernate.event.internal.DefaultInitializeCollectionEventListener.handlePotentiallyEmptyCollection;
import static org.hibernate.generator.EventType.INSERT;
import static org.hibernate.internal.util.NullnessUtil.castNonNull;
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
public class StatelessSessionImpl extends AbstractSharedSessionContract implements StatelessSession {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( StatelessSessionImpl.class );

	public static final MultiIdLoadOptions MULTI_ID_LOAD_OPTIONS = new MultiLoadOptions();

	private final LoadQueryInfluencers influencers;
	private final PersistenceContext temporaryPersistenceContext;
	private final boolean connectionProvided;
	private final List<AfterTransactionCompletionProcess> afterCompletions = new ArrayList<>();

	private final EventListenerGroups eventListenerGroups;

	public StatelessSessionImpl(SessionFactoryImpl factory, SessionCreationOptions options) {
		super( factory, options );
		connectionProvided = options.getConnection() != null;
		temporaryPersistenceContext = createPersistenceContext( this );
		influencers = new LoadQueryInfluencers( getFactory() );
		eventListenerGroups = factory.getEventListenerGroups();
		setUpMultitenancy( factory, influencers );
		// a nonzero batch size forces use of write-behind
		// therefore ignore the value of hibernate.jdbc.batch_size
		setJdbcBatchSize( 0 );
	}

	@Override
	public boolean shouldAutoJoinTransaction() {
		return true;
	}

	@Override
	public FlushMode getHibernateFlushMode() {
		return FlushMode.MANUAL;
	}

	// inserts ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public Object insert(Object entity) {
		return insert( null, entity );
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
		final EntityPersister persister = getEntityPersister( entityName, entity );
		final Object id;
		final Object[] state = persister.getValues( entity );
		if ( persister.isVersioned() ) {
			if ( seedVersion( entity, state, persister, this ) ) {
				persister.setValues( entity, state );
			}
		}
		final Generator generator = persister.getGenerator();
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
				final EventMonitor eventMonitor = getEventMonitor();
				final DiagnosticEvent event = eventMonitor.beginEntityInsertEvent();
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
				final GeneratedValues generatedValues;
				final EventMonitor eventMonitor = getEventMonitor();
				final DiagnosticEvent event = eventMonitor.beginEntityInsertEvent();
				boolean success = false;
				Object generatedId = null;
				try {
					generatedValues = persister.getInsertCoordinator().insert( entity, state, this );
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
				final EventMonitor eventMonitor = getEventMonitor();
				final DiagnosticEvent event = eventMonitor.beginEntityInsertEvent();
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
		final StatisticsImplementor statistics = getFactory().getStatistics();
		if ( statistics.isStatisticsEnabled() ) {
			statistics.insertEntity( persister.getEntityName() );
		}
		return id;
	}

	private void recreateCollections(Object entity, Object id, EntityPersister persister) {
		forEachOwnedCollection( entity, id, persister,
				(descriptor, collection) -> {
					firePreRecreate( collection, descriptor );
					final EventMonitor eventMonitor = getEventMonitor();
					final DiagnosticEvent event = eventMonitor.beginCollectionRecreateEvent();
					boolean success = false;
					try {
						descriptor.recreate( collection, id, this );
						success = true;
					}
					finally {
						eventMonitor.completeCollectionRecreateEvent( event, id, descriptor.getRole(), success, this );
					}
					final StatisticsImplementor statistics = getFactory().getStatistics();
					if ( statistics.isStatisticsEnabled() ) {
						statistics.recreateCollection( descriptor.getRole() );
					}
					firePostRecreate( collection, descriptor );
				} );
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
		final EntityPersister persister = getEntityPersister( entityName, entity );
		final Object id = persister.getIdentifier( entity, this );
		final Object version = persister.getVersion( entity );
		if ( !firePreDelete(entity, id, persister) ) {
			getInterceptor().onDelete( entity, id, persister.getPropertyNames(), persister.getPropertyTypes() );
			removeCollections( entity, id, persister );
			final Object ck = lockCacheItem( id, version, persister );
			final EventMonitor eventMonitor = getEventMonitor();
			final DiagnosticEvent event = eventMonitor.beginEntityDeleteEvent();
			boolean success = false;
			try {
				persister.getDeleteCoordinator().delete( entity, id, version, this );
				success = true;
			}
			finally {
				eventMonitor.completeEntityDeleteEvent( event, id, persister.getEntityName(), success, this );
			}
			removeCacheItem( ck, persister );
			firePostDelete( entity, id, persister );
			final StatisticsImplementor statistics = getFactory().getStatistics();
			if ( statistics.isStatisticsEnabled() ) {
				statistics.deleteEntity( persister.getEntityName() );
			}
		}
	}

	private void removeCollections(Object entity, Object id, EntityPersister persister) {
		forEachOwnedCollection( entity, id, persister,
				(descriptor, collection) -> {
					firePreRemove( collection, entity, descriptor );
					final EventMonitor eventMonitor = getEventMonitor();
					final DiagnosticEvent event = eventMonitor.beginCollectionRemoveEvent();
					boolean success = false;
					try {
						descriptor.remove( id, this );
						success = true;
					}
					finally {
						eventMonitor.completeCollectionRemoveEvent( event, id, descriptor.getRole(), success, this );
					}
					firePostRemove( collection, entity, descriptor );
					final StatisticsImplementor statistics = getFactory().getStatistics();
					if ( statistics.isStatisticsEnabled() ) {
						statistics.removeCollection( descriptor.getRole() );
					}
				} );
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
		final EntityPersister persister = getEntityPersister( entityName, entity );
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
			final Object ck = lockCacheItem( id, oldVersion, persister );
			final EventMonitor eventMonitor = getEventMonitor();
			final DiagnosticEvent event = eventMonitor.beginEntityUpdateEvent();
			boolean success = false;
			try {
				persister.getUpdateCoordinator().update( entity, id, null, state, oldVersion, null, null, false, this );
				success = true;
			}
			finally {
				eventMonitor.completeEntityUpdateEvent( event, id, persister.getEntityName(), success, this );
			}
			removeCacheItem( ck, persister );
			removeAndRecreateCollections( entity, id, persister );
			firePostUpdate( entity, id, state, persister );
			final StatisticsImplementor statistics = getFactory().getStatistics();
			if ( statistics.isStatisticsEnabled() ) {
				statistics.updateEntity( persister.getEntityName() );
			}
		}
	}

	private void removeAndRecreateCollections(Object entity, Object id, EntityPersister persister) {
		forEachOwnedCollection( entity, id, persister,
				(descriptor, collection) -> {
					firePreUpdate( collection, descriptor );
					final EventMonitor eventMonitor = getEventMonitor();
					final DiagnosticEvent event = eventMonitor.beginCollectionRemoveEvent();
					boolean success = false;
					try {
						// TODO: can we do better here?
						descriptor.remove( id, this );
						descriptor.recreate( collection, id, this );
						success = true;
					}
					finally {
						eventMonitor.completeCollectionRemoveEvent( event, id, descriptor.getRole(), success, this );
					}
					firePostUpdate( collection, descriptor );
					final StatisticsImplementor statistics = getFactory().getStatistics();
					if ( statistics.isStatisticsEnabled() ) {
						statistics.updateCollection( descriptor.getRole() );
					}
				} );
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
		final EntityPersister persister = getEntityPersister( entityName, entity );
		final Object id = idToUpsert( entity, persister );
		final Object[] state = persister.getValues( entity );
		if ( !firePreUpsert(entity, id, state, persister) ) {
			getInterceptor().onUpsert( entity, id, state, persister.getPropertyNames(), persister.getPropertyTypes() );
			final Object oldVersion = versionToUpsert( entity, persister, state );
			final Object ck = lockCacheItem( id, oldVersion, persister );
			final EventMonitor eventMonitor = getEventMonitor();
			final DiagnosticEvent event = eventMonitor.beginEntityUpsertEvent();
			boolean success = false;
			try {
				persister.getMergeCoordinator().update( entity, id, null, state, oldVersion, null, null, false, this );
				success = true;
			}
			finally {
				eventMonitor.completeEntityUpsertEvent( event, id, persister.getEntityName(), success, this );
			}
			removeCacheItem( ck, persister );
			final StatisticsImplementor statistics = getFactory().getStatistics();
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
				return state[persister.getVersionProperty()];
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
		if ( eventListenerGroups.eventListenerGroup_PRE_INSERT.isEmpty() ) {
			return false;
		}
		else {
			boolean veto = false;
			final PreInsertEvent event = new PreInsertEvent( entity, id, state, persister, null );
			for ( PreInsertEventListener listener : eventListenerGroups.eventListenerGroup_PRE_INSERT.listeners() ) {
				veto |= listener.onPreInsert( event );
			}
			return veto;
		}
	}

	// Hibernate Reactive may need to call this
	protected boolean firePreUpdate(Object entity, Object id, Object[] state, EntityPersister persister) {
		if ( eventListenerGroups.eventListenerGroup_PRE_UPDATE.isEmpty() ) {
			return false;
		}
		else {
			boolean veto = false;
			final PreUpdateEvent event = new PreUpdateEvent( entity, id, state, null, persister, null );
			for ( PreUpdateEventListener listener : eventListenerGroups.eventListenerGroup_PRE_UPDATE.listeners() ) {
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
			final PreUpsertEvent event = new PreUpsertEvent( entity, id, state, persister, null );
			for ( PreUpsertEventListener listener : eventListenerGroups.eventListenerGroup_PRE_UPSERT.listeners() ) {
				veto |= listener.onPreUpsert( event );
			}
			return veto;
		}
	}

	// Hibernate Reactive may need to call this
	protected boolean firePreDelete(Object entity, Object id, EntityPersister persister) {
		if ( eventListenerGroups.eventListenerGroup_PRE_DELETE.isEmpty() ) {
			return false;
		}
		else {
			boolean veto = false;
			final PreDeleteEvent event = new PreDeleteEvent( entity, id, null, persister, null );
			for ( PreDeleteEventListener listener : eventListenerGroups.eventListenerGroup_PRE_DELETE.listeners() ) {
				veto |= listener.onPreDelete( event );
			}
			return veto;
		}
	}

	// Hibernate Reactive may need to call this
	protected void firePostInsert(Object entity, Object id, Object[] state, EntityPersister persister) {
		eventListenerGroups.eventListenerGroup_POST_INSERT.fireLazyEventOnEachListener(
				() -> new PostInsertEvent( entity, id, state, persister, null ),
				PostInsertEventListener::onPostInsert );
	}

	// Hibernate Reactive may need to call this
	protected void firePostUpdate(Object entity, Object id, Object[] state, EntityPersister persister) {
		eventListenerGroups.eventListenerGroup_POST_UPDATE.fireLazyEventOnEachListener(
				() -> new PostUpdateEvent( entity, id, state, null, null, persister, null ),
				PostUpdateEventListener::onPostUpdate );
	}

	// Hibernate Reactive may need to call this
	protected void firePostUpsert(Object entity, Object id, Object[] state, EntityPersister persister) {
		eventListenerGroups.eventListenerGroup_POST_UPSERT.fireLazyEventOnEachListener(
				() -> new PostUpsertEvent( entity, id, state, null, persister, null ),
				PostUpsertEventListener::onPostUpsert );
	}

	// Hibernate Reactive may need to call this
	protected void firePostDelete(Object entity, Object id, EntityPersister persister) {
		eventListenerGroups.eventListenerGroup_POST_DELETE.fireLazyEventOnEachListener(
				() -> new PostDeleteEvent( entity, id, null, persister, null ),
				PostDeleteEventListener::onPostDelete );
	}

	// Hibernate Reactive may need to call this
	protected void firePreRecreate(PersistentCollection<?> collection, CollectionPersister persister) {
		eventListenerGroups.eventListenerGroup_PRE_COLLECTION_RECREATE.fireLazyEventOnEachListener(
				() -> new PreCollectionRecreateEvent(  persister, collection, null ),
				PreCollectionRecreateEventListener::onPreRecreateCollection );
	}

	// Hibernate Reactive may need to call this
	protected void firePreUpdate(PersistentCollection<?> collection, CollectionPersister persister) {
		eventListenerGroups.eventListenerGroup_PRE_COLLECTION_UPDATE.fireLazyEventOnEachListener(
				() -> new PreCollectionUpdateEvent(  persister, collection, null ),
				PreCollectionUpdateEventListener::onPreUpdateCollection );
	}

	// Hibernate Reactive may need to call this
	protected void firePreRemove(PersistentCollection<?> collection, Object owner, CollectionPersister persister) {
		eventListenerGroups.eventListenerGroup_PRE_COLLECTION_REMOVE.fireLazyEventOnEachListener(
				() -> new PreCollectionRemoveEvent(  persister, collection, null, owner ),
				PreCollectionRemoveEventListener::onPreRemoveCollection );
	}

	// Hibernate Reactive may need to call this
	protected void firePostRecreate(PersistentCollection<?> collection, CollectionPersister persister) {
		eventListenerGroups.eventListenerGroup_POST_COLLECTION_RECREATE.fireLazyEventOnEachListener(
				() -> new PostCollectionRecreateEvent(  persister, collection, null ),
				PostCollectionRecreateEventListener::onPostRecreateCollection );
	}

	// Hibernate Reactive may need to call this
	protected void firePostUpdate(PersistentCollection<?> collection, CollectionPersister persister) {
		eventListenerGroups.eventListenerGroup_POST_COLLECTION_UPDATE.fireLazyEventOnEachListener(
				() -> new PostCollectionUpdateEvent(  persister, collection, null ),
				PostCollectionUpdateEventListener::onPostUpdateCollection );
	}

	// Hibernate Reactive may need to call this
	protected void firePostRemove(PersistentCollection<?> collection, Object owner, CollectionPersister persister) {
		eventListenerGroups.eventListenerGroup_POST_COLLECTION_REMOVE.fireLazyEventOnEachListener(
				() -> new PostCollectionRemoveEvent(  persister, collection, null, owner ),
				PostCollectionRemoveEventListener::onPostRemoveCollection );
	}

	// collections ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	// Hibernate Reactive overrides this
	protected void forEachOwnedCollection(
			Object entity, Object key,
			EntityPersister persister, BiConsumer<CollectionPersister, PersistentCollection<?>> action) {
		persister.visitAttributeMappings( attribute -> {
			if ( attribute.isPluralAttributeMapping() ) {
				final CollectionPersister descriptor =
						attribute.asPluralAttributeMapping().getCollectionDescriptor();
				final Object ck = lockCacheItem( key, descriptor );
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
						collection =
								value == null
										? instantiateEmpty( key, descriptor )
										: wrap( descriptor, value );
					}
					action.accept( descriptor, collection );
				}
				removeCacheItem( ck, descriptor );
			}
		} );
	}

	// Hibernate Reactive calls this
	protected PersistentCollection<?> instantiateEmpty(Object key, CollectionPersister descriptor) {
		return descriptor.getCollectionSemantics().instantiateWrapper(key, descriptor, this);
	}

	//TODO: is this the right way to do this?
	// Hibernate Reactive calls this
	@SuppressWarnings({"rawtypes", "unchecked"})
	protected PersistentCollection<?> wrap(CollectionPersister descriptor, Object collection) {
		final CollectionSemantics collectionSemantics = descriptor.getCollectionSemantics();
		return collectionSemantics.wrap(collection, descriptor, this);
	}

	// loading ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override @SuppressWarnings("unchecked")
	public <T> T get(Class<T> entityClass, Object id) {
		return (T) get( entityClass.getName(), id );
	}

	@Override @SuppressWarnings("unchecked")
	public <T> T get(Class<T> entityClass, Object id, LockMode lockMode) {
		return (T) get( entityClass.getName(), id, lockMode );
	}

	@Override
	public Object get(String entityName, Object id) {
		return get( entityName, id, LockMode.NONE );
	}

	@Override
	public Object get(String entityName, Object id, LockMode lockMode) {
		checkOpen();

		final EntityPersister persister = requireEntityPersister( entityName );
		if ( persister.canReadFromCache() ) {
			final Object cachedEntity =
					loadFromSecondLevelCache( persister, generateEntityKey( id, persister ), null, lockMode );
			if ( cachedEntity != null ) {
				temporaryPersistenceContext.clear();
				return cachedEntity;
			}
		}
		final Object result = persister.load( id, null, getNullSafeLockMode( lockMode ), this );
		if ( temporaryPersistenceContext.isLoadFinished() ) {
			temporaryPersistenceContext.clear();
		}
		return result;
	}

	@Override
	public <T> T get(EntityGraph<T> graph, Object id) {
		return get( graph, GraphSemantic.LOAD , id);
	}

	@Override
	public <T> T get(EntityGraph<T> graph, Object id, LockMode lockMode) {
		return get( graph, GraphSemantic.LOAD, id, lockMode);
	}

	@Override
	public <T> T get(EntityGraph<T> graph, GraphSemantic graphSemantic, Object id) {
		return get( graph, graphSemantic, id, LockMode.NONE );
	}

	@Override @SuppressWarnings("unchecked")
	public  <T> T get(
			EntityGraph<T> graph, GraphSemantic graphSemantic,
			Object id, LockMode lockMode) {
		final RootGraphImplementor<T> rootGraph = (RootGraphImplementor<T>) graph;
		checkOpen();

		final EffectiveEntityGraph effectiveEntityGraph =
				getLoadQueryInfluencers().getEffectiveEntityGraph();
		effectiveEntityGraph.applyGraph( rootGraph, graphSemantic );

		try {
			return (T) get( rootGraph.getGraphedType().getTypeName(), id, lockMode );
		}
		finally {
			effectiveEntityGraph.clear();
		}
	}

	@Override
	public <T> List<T> getMultiple(Class<T> entityClass, List<?> ids, LockMode lockMode) {
		for ( Object id : ids ) {
			if ( id == null ) {
				throw new IllegalArgumentException( "Null id" );
			}
		}

		final EntityPersister persister = requireEntityPersister( entityClass.getName() );

		final List<?> results = persister.multiLoad( ids.toArray(), this, new MultiLoadOptions(lockMode) );
		//noinspection unchecked
		return (List<T>) results;
	}

	@Override
	public <T> List<T> getMultiple(Class<T> entityClass, List<?> ids) {
		for ( Object id : ids ) {
			if ( id == null ) {
				throw new IllegalArgumentException("Null id");
			}
		}

		final EntityPersister persister = requireEntityPersister( entityClass.getName() );

		final List<?> results = persister.multiLoad( ids.toArray(), this, MULTI_ID_LOAD_OPTIONS );
		//noinspection unchecked
		return (List<T>) results;

//		final List<Object> uncachedIds;
//		final List<T> list = new ArrayList<>( ids.size() );
//		if ( persister.canReadFromCache() ) {
//			uncachedIds = new ArrayList<>( ids.size() );
//			for (Object id : ids) {
//				final Object cachedEntity =
//						loadFromSecondLevelCache( persister, generateEntityKey( id, persister ), null, LockMode.NONE );
//				if ( cachedEntity == null ) {
//					uncachedIds.add( id );
//					list.add( null );
//				}
//				else {
//					//noinspection unchecked
//					list.add( (T) cachedEntity );
//				}
//			}
//		}
//		else {
//			uncachedIds = unmodifiableList(ids);
//			for (int i = 0; i < ids.size(); i++) {
//				list.add( null );
//			}
//		}
//
//		final JpaCriteriaQuery<T> query = getCriteriaBuilder().createQuery(entityClass);
//		final JpaRoot<T> from = query.from(entityClass);
//		query.where( from.get( persister.getIdentifierPropertyName() ).in(uncachedIds) );
//		final List<T> resultList = createSelectionQuery(query).getResultList();
//		for (int i = 0; i < ids.size(); i++) {
//			if ( list.get(i) == null ) {
//				final Object id = ids.get(i);
//				list.set( i, resultList.stream()
//						.filter( entity -> entity != null && persister.getIdentifier( entity, this ).equals(id) )
//						.findFirst().orElse( null ) );
//			}
//		}
//		return list;
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
		final EntityPersister persister = getEntityPersister( entityName, entity );
		final Object id = persister.getIdentifier( entity, this );
		if ( LOG.isTraceEnabled() ) {
			LOG.tracev( "Refreshing transient {0}", infoString( persister, id, getFactory() ) );
		}

		if ( persister.canWriteToCache() ) {
			final EntityDataAccess cacheAccess = persister.getCacheAccessStrategy();
			if ( cacheAccess != null ) {
				final Object ck = cacheAccess.generateCacheKey(
						id,
						persister,
						getFactory(),
						getTenantIdentifier()
				);
				cacheAccess.evict( ck );
			}
		}

		final Object result = getLoadQueryInfluencers().fromInternalFetchProfile(
				CascadingFetchProfile.REFRESH,
				() -> persister.load( id, entity, getNullSafeLockMode( lockMode ), this )
		);
		UnresolvableObjectException.throwIfNull( result, id, persister.getEntityName() );
		if ( temporaryPersistenceContext.isLoadFinished() ) {
			temporaryPersistenceContext.clear();
		}
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
		final PersistenceContext persistenceContext = getPersistenceContextInternal();
		final CollectionEntry ce = persistenceContext.getCollectionEntry( collection );
		if ( ce == null ) {
			throw new HibernateException( "no entry for collection" );
		}
		if ( !collection.wasInitialized() ) {
			final CollectionPersister loadedPersister = ce.getLoadedPersister();
			final Object loadedKey = ce.getLoadedKey();
			if ( LOG.isTraceEnabled() ) {
				LOG.trace( "Initializing collection "
							+ collectionInfoString( loadedPersister, collection, loadedKey, this ) );
			}
			final boolean foundInCache =
					initializeCollectionFromCache( loadedKey, loadedPersister, collection, this );
			if ( foundInCache ) {
				LOG.trace( "Collection initialized from cache" );
			}
			else {
				loadedPersister.initialize( loadedKey, this );
				handlePotentiallyEmptyCollection( collection, persistenceContext, loadedKey, loadedPersister );
				LOG.trace( "Collection initialized" );
				final StatisticsImplementor statistics = getFactory().getStatistics();
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

		final EntityPersister persister = requireEntityPersister( entityName );
		final EntityKey entityKey = generateEntityKey( id, persister );

		// first, try to load it from the temp PC associated to this SS
		final PersistenceContext persistenceContext = getPersistenceContext();
		final EntityHolder holder = persistenceContext.getEntityHolder( entityKey );
		if ( holder != null && holder.getEntity() != null ) {
			// we found it in the temp PC.  Should indicate we are in the midst of processing a result set
			// containing eager fetches via join fetch
			return holder.getEntity();
		}

		if ( !eager ) {
			// caller did not request forceful eager loading, see if we can create
			// some form of proxy

			// first, check to see if we can use "bytecode proxies"

			final EntityMetamodel entityMetamodel = persister.getEntityMetamodel();
			final BytecodeEnhancementMetadata enhancementMetadata = entityMetamodel.getBytecodeEnhancementMetadata();
			if ( enhancementMetadata.isEnhancedForLazyLoading() ) {
				// if the entity defines a HibernateProxy factory, see if there is an
				// existing proxy associated with the PC - and if so, use it
				if ( persister.getRepresentationStrategy().getProxyFactory() != null ) {
					final Object proxy = holder == null ? null : holder.getProxy();

					if ( proxy != null ) {
						if ( LOG.isTraceEnabled() ) {
							LOG.trace( "Entity proxy found in session cache" );
						}
						if ( LOG.isDebugEnabled() && extractLazyInitializer( proxy ).isUnwrap() ) {
							LOG.debug( "Ignoring NO_PROXY to honor laziness" );
						}

						return persistenceContext.narrowProxy( proxy, persister, entityKey, null );
					}

					// specialized handling for entities with subclasses with a HibernateProxy factory
					if ( entityMetamodel.hasSubclasses() ) {
						// entities with subclasses that define a ProxyFactory can create
						// a HibernateProxy.
						LOG.debug( "Creating a HibernateProxy for to-one association with subclasses to honor laziness" );
						return createProxy( entityKey );
					}
					return enhancementMetadata.createEnhancedProxy( entityKey, false, this );
				}
				else if ( !entityMetamodel.hasSubclasses() ) {
					return enhancementMetadata.createEnhancedProxy( entityKey, false, this );
				}
				// If we get here, then the entity class has subclasses and there is no HibernateProxy factory.
				// The entity will get loaded below.
			}
			else {
				if ( persister.hasProxy() ) {
					final Object existingProxy = holder == null ? null : holder.getProxy();
					if ( existingProxy != null ) {
						return persistenceContext.narrowProxy( existingProxy, persister, entityKey, null );
					}
					else {
						return createProxy( entityKey );
					}
				}
			}
		}

		// otherwise immediately materialize it
		return internalLoadGet( entityName, id, persistenceContext );
	}

	// For Hibernate Reactive
	protected Object internalLoadGet(String entityName, Object id, PersistenceContext persistenceContext) {
		// IMPLEMENTATION NOTE: increment/decrement the load count before/after getting the value
		//                      to ensure that #get does not clear the PersistenceContext.
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
	public void fetch(Object association) {
		checkOpen();
		final PersistenceContext persistenceContext = getPersistenceContext();
		final LazyInitializer initializer = extractLazyInitializer( association );
		if ( initializer != null ) {
			if ( initializer.isUninitialized() ) {
				final String entityName = initializer.getEntityName();
				final Object id = initializer.getIdentifier();
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
				final CollectionPersister collectionDescriptor = requireCollectionPersister( collection.getRole() );
				final Object key = collection.getKey();
				persistenceContext.addUninitializedCollection( collectionDescriptor, collection, key );
				collection.setCurrentSession( this );
				try {
					final boolean foundInCache =
							initializeCollectionFromCache( key, collectionDescriptor, collection, this );
					if ( foundInCache ) {
						LOG.trace( "Collection fetched from cache" );
					}
					else {
						collectionDescriptor.initialize( key, this );
						handlePotentiallyEmptyCollection( collection, getPersistenceContextInternal(), key,
								collectionDescriptor );
						LOG.trace( "Collection fetched" );
						final StatisticsImplementor statistics = getFactory().getStatistics();
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
	public String bestGuessEntityName(Object object) {
		final LazyInitializer lazyInitializer = extractLazyInitializer( object );
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
		return entity.getClass().getName();
	}

	@Override
	public EntityPersister getEntityPersister(String entityName, Object object) {
		checkOpen();
		return entityName == null
				? requireEntityPersister( guessEntityName( object ) )
				: requireEntityPersister( entityName ).getSubclassEntityPersister( object, getFactory() );
	}

	@Override
	public Object getEntityUsingInterceptor(EntityKey key) {
		checkOpen();

		final PersistenceContext persistenceContext = getPersistenceContext();
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
	public void autoPreFlush() {
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
		flushBeforeTransactionCompletion();
		beforeTransactionCompletionEvents();
	}

	@Override
	public void afterTransactionCompletion(boolean successful, boolean delayed) {
		processAfterCompletions( successful );
		afterTransactionCompletionEvents( successful );
		if ( shouldAutoClose() && !isClosed() ) {
			managedClose();
		}
	}

	private void processAfterCompletions(boolean successful) {
		for ( AfterTransactionCompletionProcess completion: afterCompletions ) {
			try {
				completion.doAfterTransactionCompletion( successful, this );
			}
			catch (CacheException ce) {
				LOG.unableToReleaseCacheLock( ce );
				// continue loop
			}
			catch (Exception e) {
				throw new HibernateException( "Unable to perform afterTransactionCompletion callback: " + e.getMessage(), e );
			}
		}
		afterCompletions.clear();
	}

	@Override
	public boolean isTransactionInProgress() {
		return connectionProvided || super.isTransactionInProgress();
	}

	@Override
	public void flushBeforeTransactionCompletion() {
		final boolean flush;
		try {
			flush = !isClosed()
					&& !isFlushModeNever()
					&& !JtaStatusHelper.isRollback( getJtaPlatform().getCurrentStatus() );
		}
		catch ( SystemException se ) {
			throw new HibernateException( "could not determine transaction status in beforeCompletion()", se );
		}
		if ( flush ) {
			managedFlush();
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
			final EntityDataAccess cache = persister.getCacheAccessStrategy();
			final Object ck = cache.generateCacheKey(
					id,
					persister,
					getFactory(),
					getTenantIdentifier()
			);
			final SoftLock lock = cache.lockItem( this, ck, previousVersion );
			afterCompletions.add( (success, session) -> cache.unlockItem( session, ck, lock ) );
			return ck;
		}
		else {
			return null;
		}
	}

	protected void removeCacheItem(Object ck, EntityPersister persister) {
		if ( persister.canWriteToCache() ) {
			persister.getCacheAccessStrategy().remove( this, ck );
		}
	}

	protected Object lockCacheItem(Object key, CollectionPersister persister) {
		if ( persister.hasCache() ) {
			final CollectionDataAccess cache = persister.getCacheAccessStrategy();
			final Object ck = cache.generateCacheKey(
					key,
					persister,
					getFactory(),
					getTenantIdentifier()
			);
			final SoftLock lock = cache.lockItem( this, ck, null );
			afterCompletions.add( (success, session) -> cache.unlockItem( this, ck, lock ) );
			return ck;
		}
		else {
			return null;
		}
	}

	protected void removeCacheItem(Object ck, CollectionPersister persister) {
		if ( persister.hasCache() ) {
			persister.getCacheAccessStrategy().remove( this, ck );
		}
	}

	@Override
	public void registerProcess(AfterTransactionCompletionProcess process) {
		afterCompletions.add( process );
	}

	@Override
	public void lock(String entityName, Object child, LockOptions lockOptions) {
		final EntityPersister persister = getEntityPersister( entityName, child );
		persister.lock( persister.getIdentifier( child ), persister.getVersion( child ), child, lockOptions, this );
		final EntityEntry entry = getPersistenceContextInternal().getEntry( child );
		if ( entry == null ) {
			throw new AssertionFailure( "no entry in temporary persistence context" );
		}
		LoaderHelper.upgradeLock( child, entry, lockOptions, this );
	}

	@Override
	public Object loadFromSecondLevelCache(EntityPersister persister, EntityKey entityKey, Object instanceToLoad, LockMode lockMode) {
		return CacheLoadHelper.loadFromSecondLevelCache( this, instanceToLoad, lockMode, persister, entityKey );
	}

	private static final class MultiLoadOptions implements MultiIdLoadOptions {
		private final  LockOptions lockOptions;

		private MultiLoadOptions() {
			this.lockOptions = null;
		}

		private MultiLoadOptions(LockMode lockOptions) {
			this.lockOptions = new LockOptions( lockOptions );
		}

		@Override
		public boolean isSessionCheckingEnabled() {
			return false;
		}

		@Override
		public boolean isSecondLevelCacheCheckingEnabled() {
			return true;
		}

		@Override
		public Boolean getReadOnly(SessionImplementor session) {
			return null;
		}

		@Override
		public boolean isReturnOfDeletedEntitiesEnabled() {
			return false;
		}

		@Override
		public boolean isOrderReturnEnabled() {
			return true;
		}

		@Override
		public LockOptions getLockOptions() {
			return lockOptions;
		}

		@Override
		public Integer getBatchSize() {
			return null;
		}
	}
}
