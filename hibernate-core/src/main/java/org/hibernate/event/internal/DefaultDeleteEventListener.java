/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.internal;

import org.hibernate.CacheMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.StaleObjectStateException;
import org.hibernate.TransientObjectException;
import org.hibernate.action.internal.CollectionRemoveAction;
import org.hibernate.action.internal.EntityDeleteAction;
import org.hibernate.action.internal.OrphanRemovalAction;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.engine.internal.Cascade;
import org.hibernate.engine.internal.CascadePoint;
import org.hibernate.engine.internal.ForeignKeys;
import org.hibernate.engine.internal.Nullability;
import org.hibernate.engine.internal.Nullability.NullabilityCheckType;
import org.hibernate.engine.spi.CascadingActions;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.DeleteContext;
import org.hibernate.event.spi.DeleteEvent;
import org.hibernate.event.spi.DeleteEventListener;
import org.hibernate.event.spi.EventSource;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.EmptyInterceptor;
import org.hibernate.jpa.event.spi.CallbackRegistry;
import org.hibernate.jpa.event.spi.CallbackRegistryConsumer;
import org.hibernate.jpa.event.spi.CallbackType;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.property.access.internal.PropertyAccessStrategyBackRefImpl;
import org.hibernate.type.CollectionType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.Type;
import org.hibernate.type.TypeHelper;

import static java.util.Arrays.fill;
import static org.hibernate.engine.internal.Collections.skipRemoval;
import static org.hibernate.pretty.MessageHelper.infoString;
import static org.hibernate.proxy.HibernateProxy.extractLazyInitializer;

/**
 * Defines the default delete event listener used by hibernate for deleting entities
 * from the datastore in response to generated delete events.
 *
 * @author Steve Ebersole
 */
public class DefaultDeleteEventListener implements DeleteEventListener,	CallbackRegistryConsumer {

	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( DefaultDeleteEventListener.class );

	private CallbackRegistry callbackRegistry;

	@Override
	public void injectCallbackRegistry(CallbackRegistry callbackRegistry) {
		this.callbackRegistry = callbackRegistry;
	}

	/**
	 * Handle the given delete event.
	 *
	 * @param event The delete event to be handled.
	 *
	 */
	@Override
	public void onDelete(DeleteEvent event) throws HibernateException {
		onDelete( event, DeleteContext.create() );
	}

	/**
	 * Handle the given delete event.  This is the cascaded form.
	 *
	 * @param event The delete event.
	 * @param transientEntities The cache of entities already deleted
	 *
	 */
	@Override
	public void onDelete(DeleteEvent event, DeleteContext transientEntities) throws HibernateException {
		if ( !optimizeUnloadedDelete( event ) ) {
			delete( event, transientEntities );
		}
	}

	private boolean optimizeUnloadedDelete(DeleteEvent event) {
		final Object object = event.getObject();
		final var lazyInitializer = extractLazyInitializer( object );
		if ( lazyInitializer != null ) {
			if ( lazyInitializer.isUninitialized() ) {
				final var source = event.getSession();
				final var persister =
						event.getFactory().getMappingMetamodel()
								.findEntityDescriptor( lazyInitializer.getEntityName() );
				final Object id = lazyInitializer.getInternalIdentifier();
				final var key = source.generateEntityKey( id, persister );
				final var persistenceContext = source.getPersistenceContextInternal();
				final var entityHolder = persistenceContext.getEntityHolder( key );
				if ( (entityHolder == null || entityHolder.getEntity() == null || !entityHolder.isInitialized())
						&& canBeDeletedWithoutLoading( source, persister ) ) {
					if ( event.getFactory().getSessionFactoryOptions().isJpaBootstrap() && entityHolder == null ) {
						throw new IllegalArgumentException( "Given entity is not associated with the persistence context" );
					}
					// optimization for deleting certain entities without loading them
					persistenceContext.reassociateProxy( object, id );
					if ( !persistenceContext.containsDeletedUnloadedEntityKey( key ) ) {
						persistenceContext.registerDeletedUnloadedEntityKey( key );

						if ( persister.hasOwnedCollections() ) {
							// we're deleting an unloaded proxy with collections
							for ( Type type : persister.getPropertyTypes() ) { //TODO: when we enable this for subclasses use getSubclassPropertyTypeClosure()
								deleteOwnedCollections( type, id, source );
							}
						}

						source.getActionQueue().addAction( new EntityDeleteAction( id, persister, source ) );
					}
					return true;
				}
			}
		}
		return false;
	}

	private static void deleteOwnedCollections(Type type, Object key, EventSource session) {
		if ( type instanceof CollectionType collectionType ) {
			final var persister =
					session.getFactory().getMappingMetamodel()
							.getCollectionDescriptor( collectionType.getRole() );
			if ( !persister.isInverse() && !skipRemoval( session, persister, key ) ) {
				session.getActionQueue().addAction( new CollectionRemoveAction( persister, key, session ) );
			}
		}
		else if ( type instanceof ComponentType componentType ) {
			for ( Type subtype : componentType.getSubtypes() ) {
				deleteOwnedCollections( subtype, key, session );
			}
		}
	}

	private void delete(DeleteEvent event, DeleteContext transientEntities) {
		final var persistenceContext = event.getSession().getPersistenceContextInternal();
		final Object entity = persistenceContext.unproxyAndReassociate( event.getObject() );
		final var entityEntry = persistenceContext.getEntry( entity );
		if ( entityEntry == null ) {
			deleteUnmanagedInstance( event, transientEntities, entity );
		}
		else {
			deletePersistentInstance( event, transientEntities, entity, entityEntry );
		}
	}

	private void deleteUnmanagedInstance(DeleteEvent event, DeleteContext transientEntities, Object entity) {
		LOG.trace( "Deleted entity was not associated with current session" );
		final var source = event.getSession();
		final var persister = source.getEntityPersister( event.getEntityName(), entity );
		if ( ForeignKeys.isTransient( persister.getEntityName(), entity, null, source ) ) {
			deleteTransientEntity( source, entity, persister, transientEntities );
		}
		else {
			deleteDetachedEntity( event, transientEntities, entity, persister, source );
		}
	}

	private void deleteDetachedEntity(
			DeleteEvent event, DeleteContext transientEntities, Object entity, EntityPersister persister, EventSource source) {
		if ( source.getFactory().getSessionFactoryOptions().isJpaBootstrap() ) {
			throw new IllegalArgumentException( "Given entity is not associated with the persistence context" );
		}
		final Object id = persister.getIdentifier( entity, source );
		if ( id == null ) {
			throw new TransientObjectException( "Cannot delete instance of entity '"
					+ persister.getEntityName() + "' because it has a null identifier" );
		}

		final var key = source.generateEntityKey( id, persister);
		final Object version = persister.getVersion( entity );

//		persistenceContext.checkUniqueness( key, entity );
		if ( !flushAndEvictExistingEntity( key, version, persister, source ) ) {

			new OnUpdateVisitor( source, id, entity ).process( entity, persister );

			final var persistenceContext = source.getPersistenceContextInternal();
			final var entityEntry = persistenceContext.addEntity(
					entity,
					persister.isMutable() ? Status.MANAGED : Status.READ_ONLY,
					persister.getValues( entity ),
					key,
					version,
					LockMode.NONE,
					true,
					persister,
					false
			);
			persister.afterReassociate( entity, source );

			delete( event, transientEntities, source, entity, persister, id, version, entityEntry );
		}
	}

	/**
	 * Since Hibernate 7, if a detached instance is passed to remove(),
	 * and if there is already an existing managed entity with the same
	 * id, flush and evict it, after checking that the versions match.
	 *
	 * @return true if the managed entity was already deleted
	 */
	private static boolean flushAndEvictExistingEntity(
			EntityKey key, Object version, EntityPersister persister, EventSource source) {
		final var persistenceContext = source.getPersistenceContextInternal();
		final Object existingEntity = persistenceContext.getEntity( key );
		if ( existingEntity != null ) {
			if ( persistenceContext.getEntry( existingEntity ).getStatus().isDeletedOrGone() ) {
				// already deleted, no work to do
				return true;
			}
			else {
				LOG.flushAndEvictOnRemove( key.getEntityName() );
				source.flush();
				if ( !persister.isVersioned()
						|| persister.getVersionType()
								.isEqual( version, persister.getVersion( existingEntity ) ) ) {
					source.evict( existingEntity );
					return false;
				}
				else {
					throw new StaleObjectStateException( key.getEntityName(), key.getIdentifier(),
							"Persistence context contains a more recent version of the given entity" );
				}
			}
		}
		else {
			return false;
		}
	}

	private void deletePersistentInstance(
			DeleteEvent event,
			DeleteContext transientEntities,
			Object entity,
			EntityEntry entityEntry) {
		LOG.trace( "Deleting a persistent instance" );
		final var source = event.getSession();
		if ( entityEntry.getStatus().isDeletedOrGone()
				|| source.getPersistenceContextInternal()
						.containsDeletedUnloadedEntityKey( entityEntry.getEntityKey() ) ) {
			LOG.trace( "Object was already deleted" );
		}
		else {
			delete(
					event,
					transientEntities,
					source,
					entity,
					entityEntry.getPersister(),
					entityEntry.getId(),
					entityEntry.getVersion(),
					entityEntry
			);
		}
	}

	private void delete(
			DeleteEvent event,
			DeleteContext transientEntities,
			EventSource source,
			Object entity,
			EntityPersister persister,
			Object id,
			Object version,
			EntityEntry entityEntry) {
		callbackRegistry.preRemove( entity );
		deleteEntity(
				source,
				entity,
				entityEntry,
				event.isCascadeDeleteEnabled(),
				event.isOrphanRemovalBeforeUpdates(),
				persister,
				transientEntities
		);
		if ( source.getFactory().getSessionFactoryOptions().isIdentifierRollbackEnabled() ) {
			persister.resetIdentifier( entity, id, version, source );
		}
	}

	/**
	 * Can we delete the row represented by the proxy without loading the entity?
	 */
	private boolean canBeDeletedWithoutLoading(EventSource source, EntityPersister persister) {
		return source.getInterceptor() == EmptyInterceptor.INSTANCE
			&& !persister.hasSubclasses() //TODO: should be unnecessary, using EntityPersister.getSubclassPropertyTypeClosure(), etc
			&& !persister.hasCascadeDelete()
			&& !persister.hasNaturalIdentifier()
			&& !persister.hasCollectionNotReferencingPK()
			&& !hasRegisteredRemoveCallbacks( persister )
			&& !hasCustomEventListeners( source );
	}

	private static boolean hasCustomEventListeners(EventSource source) {
		final var eventListenerGroups = source.getFactory().getEventListenerGroups();
		// Bean Validation adds a PRE_DELETE listener
		// and Envers adds a POST_DELETE listener
		return eventListenerGroups.eventListenerGroup_PRE_DELETE.count() > 0
			|| eventListenerGroups.eventListenerGroup_POST_COMMIT_DELETE.count() > 0
			|| eventListenerGroups.eventListenerGroup_POST_DELETE.count() > 1
			|| eventListenerGroups.eventListenerGroup_POST_DELETE.count() == 1
				&& !(eventListenerGroups.eventListenerGroup_POST_DELETE.listeners().iterator().next()
						instanceof PostDeleteEventListenerStandardImpl);
	}

	private boolean hasRegisteredRemoveCallbacks(EntityPersister persister) {
		final Class<?> mappedClass = persister.getMappedClass();
		return callbackRegistry.hasRegisteredCallbacks( mappedClass, CallbackType.PRE_REMOVE )
			|| callbackRegistry.hasRegisteredCallbacks( mappedClass, CallbackType.POST_REMOVE );
	}

	/**
	 * We encountered a delete request on a transient instance.
	 * <p>
	 * This is a deviation from historical Hibernate (pre-3.2) behavior to
	 * align with the JPA spec, which states that transient entities can be
	 * passed to remove operation in which case cascades still need to be
	 * performed.
	 *
	 * @param session The session which is the source of the event
	 * @param entity The entity being delete processed
	 * @param persister The entity persister
	 * @param transientEntities A cache of already visited transient entities
	 * (to avoid infinite recursion).
	 */
	protected void deleteTransientEntity(
			EventSource session,
			Object entity,
			EntityPersister persister,
			DeleteContext transientEntities) {
		LOG.handlingTransientEntity();
		if ( transientEntities.add( entity ) ) {
			cascadeBeforeDelete( session, persister, entity, transientEntities );
			cascadeAfterDelete( session, persister, entity, transientEntities );
		}
		else {
			LOG.trace( "Already handled transient entity; skipping" );
		}
	}

	/**
	 * Perform the entity deletion.  Well, as with most operations, does not
	 * really perform it; just schedules an action/execution with the
	 * {@link org.hibernate.engine.spi.ActionQueue} for execution during flush.
	 *
	 * @param session The originating session
	 * @param entity The entity to delete
	 * @param entityEntry The entity's entry in the {@link PersistenceContext}
	 * @param isCascadeDeleteEnabled Is delete cascading enabled?
	 * @param persister The entity persister.
	 * @param transientEntities A cache of already deleted entities.
	 */
	protected final void deleteEntity(
			final EventSource session,
			final Object entity,
			final EntityEntry entityEntry,
			final boolean isCascadeDeleteEnabled,
			final boolean isOrphanRemovalBeforeUpdates,
			final EntityPersister persister,
			final DeleteContext transientEntities) {

		if ( LOG.isTraceEnabled() ) {
			LOG.trace( "Deleting " + infoString( persister, entityEntry.getId(), session.getFactory() ) );
		}

		final Object version = entityEntry.getVersion();

		final Object[] currentState =
				entityEntry.getLoadedState() == null
						? persister.getValues( entity ) //i.e. the entity came in from update()
						: entityEntry.getLoadedState();

		final Object[] deletedState = createDeletedState( persister, entity, currentState, session );
		entityEntry.setDeletedState( deletedState );

		session.getInterceptor().onRemove(
				entity,
				entityEntry.getId(),
				deletedState,
				persister.getPropertyNames(),
				persister.getPropertyTypes()
		);

		final var persistenceContext = session.getPersistenceContextInternal();

		// before any callbacks, etc., so subdeletions see that this deletion happened first
		persistenceContext.setEntryStatus( entityEntry, Status.DELETED );
		final var key = session.generateEntityKey( entityEntry.getId(), persister );

		cascadeBeforeDelete( session, persister, entity, transientEntities );

		new ForeignKeys.Nullifier(  entity, true, false, session, persister )
				.nullifyTransientReferences( entityEntry.getDeletedState() );
		new Nullability( session, NullabilityCheckType.DELETE )
				.checkNullability( entityEntry.getDeletedState(), persister );
		persistenceContext.registerNullifiableEntityKey( key );

		final var actionQueue = session.getActionQueue();
		if ( isOrphanRemovalBeforeUpdates ) {
			// TODO: The removeOrphan concept is a temporary "hack" for HHH-6484.
			//  This should be removed once action/task
			// ordering is improved.
			actionQueue.addAction(
					new OrphanRemovalAction(
							entityEntry.getId(),
							deletedState,
							version,
							entity,
							persister,
							isCascadeDeleteEnabled,
							session
					)
			);
		}
		else {
			// Ensures that containing deletions happen before sub-deletions
			actionQueue.addAction(
					new EntityDeleteAction(
							entityEntry.getId(),
							deletedState,
							version,
							entity,
							persister,
							isCascadeDeleteEnabled,
							session
					)
			);
		}

		cascadeAfterDelete( session, persister, entity, transientEntities );

		// the entry will be removed after the flush, and will no longer
		// override the stale snapshot
		// This is now handled by removeEntity() in EntityDeleteAction
		//persistenceContext.removeDatabaseSnapshot(key);
	}

	private Object[] createDeletedState(
			EntityPersister persister,
			Object parent,
			Object[] currentState,
			EventSource eventSource) {
		final Type[] types = persister.getPropertyTypes();
		final Object[] deletedState = new Object[types.length];
		if ( !persister.hasCollections() || !persister.hasUninitializedLazyProperties( parent ) ) {
			final boolean[] copyability = new boolean[types.length];
			fill( copyability, true );
			TypeHelper.deepCopy( currentState, types, copyability, deletedState, eventSource );
			return deletedState;
		}

		final String[] propertyNames = persister.getPropertyNames();
		final var enhancementMetadata = persister.getBytecodeEnhancementMetadata();
		final var metamodel = persister.getFactory().getMappingMetamodel();
		for ( int i = 0; i < types.length; i++) {
			if ( types[i] instanceof CollectionType collectionType
					&& !enhancementMetadata.isAttributeLoaded( parent, propertyNames[i] ) ) {
				final var collectionDescriptor =
						metamodel.getCollectionDescriptor( collectionType.getRole() );
				if ( collectionDescriptor.needsRemove() || collectionDescriptor.hasCache() ) {
					final Object keyOfOwner = collectionType.getKeyOfOwner( parent, eventSource.getSession() );
					// This will make sure that a CollectionEntry exists
					deletedState[i] = collectionType.getCollection( keyOfOwner, eventSource.getSession(), parent, false );
				}
				else {
					deletedState[i] = currentState[i];
				}
			}
			else if ( currentState[i] == LazyPropertyInitializer.UNFETCHED_PROPERTY
					|| currentState[i] == PropertyAccessStrategyBackRefImpl.UNKNOWN ) {
				deletedState[i] = currentState[i];
			}
			else {
				deletedState[i] = types[i].deepCopy( currentState[i], eventSource.getFactory() );
			}
		}
		return deletedState;
	}

	protected void cascadeBeforeDelete(
			EventSource session,
			EntityPersister persister,
			Object entity,
			DeleteContext transientEntities) throws HibernateException {

		final var cacheMode = session.getCacheMode();
		session.setCacheMode( CacheMode.GET );
		final var persistenceContext = session.getPersistenceContextInternal();
		persistenceContext.incrementCascadeLevel();
		try {
			// cascade-delete to collections BEFORE the collection owner is deleted
			Cascade.cascade(
					CascadingActions.REMOVE,
					CascadePoint.AFTER_INSERT_BEFORE_DELETE,
					session,
					persister,
					entity,
					transientEntities
			);
		}
		finally {
			persistenceContext.decrementCascadeLevel();
			session.setCacheMode( cacheMode );
		}
	}

	protected void cascadeAfterDelete(
			EventSource session,
			EntityPersister persister,
			Object entity,
			DeleteContext transientEntities) throws HibernateException {

		final var cacheMode = session.getCacheMode();
		session.setCacheMode( CacheMode.GET );
		final var persistenceContext = session.getPersistenceContextInternal();
		persistenceContext.incrementCascadeLevel();
		try {
			// cascade-delete to many-to-one AFTER the parent was deleted
			Cascade.cascade(
					CascadingActions.REMOVE,
					CascadePoint.BEFORE_INSERT_AFTER_DELETE,
					session,
					persister,
					entity,
					transientEntities
			);
		}
		finally {
			persistenceContext.decrementCascadeLevel();
			session.setCacheMode( cacheMode );
		}
	}
}
