/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.internal;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.AssertionFailure;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.NonUniqueObjectException;
import org.hibernate.PersistentObjectException;
import org.hibernate.TransientObjectException;
import org.hibernate.bytecode.enhance.spi.interceptor.BytecodeLazyAttributeInterceptor;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.AssociationKey;
import org.hibernate.engine.spi.BatchFetchQueue;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityHolder;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.EntityUniqueKey;
import org.hibernate.engine.spi.NaturalIdResolutions;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.spi.PostLoadEvent;
import org.hibernate.event.spi.PostLoadEventListener;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.internal.util.collections.IdentityMap;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.sql.exec.spi.Callback;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingState;
import org.hibernate.sql.results.spi.LoadContexts;
import org.hibernate.type.CollectionType;

import org.jboss.logging.Logger;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.engine.internal.ManagedTypeHelper.asHibernateProxy;
import static org.hibernate.engine.internal.ManagedTypeHelper.asManagedEntity;
import static org.hibernate.engine.internal.ManagedTypeHelper.asPersistentAttributeInterceptable;
import static org.hibernate.engine.internal.ManagedTypeHelper.isPersistentAttributeInterceptable;
import static org.hibernate.proxy.HibernateProxy.extractLazyInitializer;

/**
 * A <em>stateful</em> implementation of the {@link PersistenceContext} contract, meaning that we maintain this
 * state throughout the life of the persistence context.
 *
 * @implNote  There is meant to be a one-to-one correspondence between a {@link org.hibernate.internal.SessionImpl}
 *            and a {@link PersistenceContext}. Event listeners and other session collaborators then use the
 *            {@code PersistenceContext} to drive their processing.
 *
 * @author Steve Ebersole
 * @author Sanne Grinovero
 */
public class StatefulPersistenceContext implements PersistenceContext {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			StatefulPersistenceContext.class.getName()
	);

	private static final int INIT_COLL_SIZE = 8;

	/*
		Eagerly Initialized Fields
		the following fields are used in all circumstances, and are not worth (or not suited) to being converted into lazy
	 */
	private final SharedSessionContractImplementor session;
	private EntityEntryContext entityEntryContext;

	/*
		Everything else below should be carefully initialized only on first need;
		this optimisation is very effective as null checks are free, while allocation costs
		are very often the dominating cost of an application using ORM.
		This is not general advice, but it's worth the added maintenance burden in this case
		as this is a very central component of our library.
	 */

	// Loaded entity instances, by EntityKey
	private HashMap<EntityKey, EntityHolderImpl> entitiesByKey;

	// Loaded entity instances, by EntityUniqueKey
	private HashMap<EntityUniqueKey, Object> entitiesByUniqueKey;


	// Snapshots of current database state for entities
	// that have *not* been loaded
	private HashMap<EntityKey, Object> entitySnapshotsByKey;

	// Identity map of array holder ArrayHolder instances, by the array instance
	private IdentityHashMap<Object, PersistentCollection<?>> arrayHolders;

	// Identity map of CollectionEntry instances, by the collection wrapper
	private IdentityMap<PersistentCollection<?>, CollectionEntry> collectionEntries;

	// Collection wrappers, by the CollectionKey
	private HashMap<CollectionKey, PersistentCollection<?>> collectionsByKey;

	// Set of EntityKeys of deleted objects
	private HashSet<EntityKey> nullifiableEntityKeys;

	// Set of EntityKeys of deleted unloaded proxies
	private HashSet<EntityKey> deletedUnloadedEntityKeys;

	// properties that we have tried to load, and not found in the database
	private HashSet<AssociationKey> nullAssociations;

	// A list of collection wrappers that were instantiating during result set
	// processing, that we will need to initialize at the end of the query
	private ArrayList<PersistentCollection<?>> nonlazyCollections;

	// A container for collections we load up when the owning entity is not
	// yet loaded ... for now, this is purely transient!
	private HashMap<CollectionKey,PersistentCollection<?>> unownedCollections;

	// Parent entities cache by their child for cascading
	// May be empty or not contains all relation
	private IdentityHashMap<Object,Object> parentsByChild;

	private int cascading;
	private int loadCounter;
	private int removeOrphanBeforeUpdatesCounter;
	private boolean flushing;

	private boolean defaultReadOnly;
	private boolean hasNonReadOnlyEntities;

	private LoadContexts loadContexts;
	private BatchFetchQueue batchFetchQueue;

	/**
	 * Constructs a PersistentContext, bound to the given session.
	 *
	 * @param session The session "owning" this context.
	 */
	public StatefulPersistenceContext(SharedSessionContractImplementor session) {
		this.session = session;
		this.entityEntryContext = new EntityEntryContext( this );
	}

	private Map<EntityKey, EntityHolderImpl> getOrInitializeEntitiesByKey() {
		if ( entitiesByKey == null ) {
			entitiesByKey = CollectionHelper.mapOfSize( INIT_COLL_SIZE );
		}
		return entitiesByKey;
	}

	@Override
	public boolean isStateless() {
		return false;
	}

	@Override
	public SharedSessionContractImplementor getSession() {
		return session;
	}

	@Override
	public LoadContexts getLoadContexts() {
		if ( loadContexts == null ) {
			loadContexts = new LoadContexts( this );
		}
		return loadContexts;
	}

	@Override
	public boolean hasLoadContext() {
		return loadContexts != null;
	}

//	@Override
//	public void addUnownedCollection(CollectionKey key, PersistentCollection collection) {
//		if ( unownedCollections == null ) {
//			unownedCollections = CollectionHelper.mapOfSize( INIT_COLL_SIZE );
//		}
//		unownedCollections.put( key, collection );
//	}
//
	@Override
	public PersistentCollection<?> useUnownedCollection(CollectionKey key) {
		return ( unownedCollections == null ) ? null : unownedCollections.remove( key );
	}

	@Override
	public BatchFetchQueue getBatchFetchQueue() {
		if ( batchFetchQueue == null ) {
			batchFetchQueue = new BatchFetchQueue( this );
		}
		return batchFetchQueue;
	}

	@Override
	public void clear() {
		if ( entitiesByKey != null ) {
			//Strictly avoid lambdas in this case
			for ( EntityHolderImpl value : entitiesByKey.values() ) {
				if ( value != null ) {
					value.state = EntityHolderState.DETACHED;
					if ( value.proxy != null ) {
						final LazyInitializer lazyInitializer = extractLazyInitializer( value.proxy );
						if ( lazyInitializer != null ) {
							lazyInitializer.unsetSession();
						}
					}
				}
			}
		}

		final SharedSessionContractImplementor session = getSession();
		if ( collectionEntries != null ) {
			IdentityMap.onEachKey( collectionEntries, k -> k.unsetSession( session ) );
		}

		arrayHolders = null;
		entitiesByKey = null;
		entitiesByUniqueKey = null;
		entityEntryContext.clear();
		parentsByChild = null;
		entitySnapshotsByKey = null;
		collectionsByKey = null;
		nonlazyCollections = null;
		collectionEntries = null;
		unownedCollections = null;
		nullifiableEntityKeys = null;
		deletedUnloadedEntityKeys = null;
		if ( batchFetchQueue != null ) {
			batchFetchQueue.clear();
		}
		// defaultReadOnly is unaffected by clear()
		hasNonReadOnlyEntities = false;
		if ( loadContexts != null ) {
			loadContexts.cleanup();
		}
		naturalIdResolutions = null;
	}

	@Override
	public boolean isDefaultReadOnly() {
		return defaultReadOnly;
	}

	@Override
	public void setDefaultReadOnly(boolean defaultReadOnly) {
		this.defaultReadOnly = defaultReadOnly;
	}

//	@Override
//	public boolean hasNonReadOnlyEntities() {
//		return hasNonReadOnlyEntities;
//	}

	@Override
	public void setEntryStatus(EntityEntry entry, Status status) {
		entry.setStatus( status );
		setHasNonReadOnlyEnties( status );
	}

	private void setHasNonReadOnlyEnties(Status status) {
		if ( status==Status.DELETED || status==Status.MANAGED || status==Status.SAVING ) {
			hasNonReadOnlyEntities = true;
		}
	}

	@Override
	public void afterTransactionCompletion() {
		cleanUpInsertedKeysAfterTransaction();
		// Downgrade locks
		entityEntryContext.downgradeLocks();
	}

	/**
	 * Get the current state of the entity as known to the underlying
	 * database, or null if there is no corresponding row
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	public Object[] getDatabaseSnapshot(Object id, EntityPersister persister) throws HibernateException {
		final EntityKey key = session.generateEntityKey( id, persister );
		final Object cached = entitySnapshotsByKey == null ? null : entitySnapshotsByKey.get( key );
		if ( cached != null ) {
			return cached == NO_ROW ? null : (Object[]) cached;
		}
		else {
			final Object[] snapshot = persister.getDatabaseSnapshot( id, session );
			if ( entitySnapshotsByKey == null ) {
				entitySnapshotsByKey = CollectionHelper.mapOfSize( INIT_COLL_SIZE );
			}
			entitySnapshotsByKey.put( key, snapshot == null ? NO_ROW : snapshot );
			return snapshot;
		}
	}

	@Override
	public Object getNaturalIdSnapshot(Object id, EntityPersister persister) throws HibernateException {
		if ( !persister.hasNaturalIdentifier() ) {
			return null;
		}

		persister = locateProperPersister( persister );

		// let's first see if it is part of the natural id cache...
		final Object cachedValue = getNaturalIdResolutions().findCachedNaturalIdById( id, persister );
		if ( cachedValue != null ) {
			return cachedValue;
		}

		// check to see if the natural id is mutable/immutable
		if ( persister.getEntityMetamodel().hasImmutableNaturalId() ) {
			// an immutable natural-id is not retrieved during a normal database-snapshot operation...
			final Object naturalIdFromDb = persister.getNaturalIdentifierSnapshot( id, session );
			naturalIdResolutions.cacheResolutionFromLoad( id, naturalIdFromDb, persister );
			return naturalIdFromDb;
		}
		else {
			// for a mutable natural id there is a likelihood that the information will already be
			// snapshot-cached.
			final int[] props = persister.getNaturalIdentifierProperties();
			final Object[] entitySnapshot = getDatabaseSnapshot( id, persister );
			if ( entitySnapshot == NO_ROW || entitySnapshot == null ) {
				return null;
			}

			final Object[] naturalIdSnapshotSubSet = new Object[ props.length ];
			for ( int i = 0; i < props.length; i++ ) {
				naturalIdSnapshotSubSet[i] = entitySnapshot[ props[i] ];
			}
			naturalIdResolutions.cacheResolutionFromLoad( id, naturalIdSnapshotSubSet, persister );
			return naturalIdSnapshotSubSet;
		}
	}

	private EntityPersister locateProperPersister(EntityPersister persister) {
		return persister.getRootEntityDescriptor().getEntityPersister();
	}

	@Override
	public Object[] getCachedDatabaseSnapshot(EntityKey key) {
		final Object snapshot = entitySnapshotsByKey == null ? null : entitySnapshotsByKey.get( key );
		if ( snapshot == NO_ROW ) {
			throw new IllegalStateException(
					"persistence context reported no row snapshot for "
							+ MessageHelper.infoString( key.getEntityName(), key.getIdentifier() )
			);
		}
		return (Object[]) snapshot;
	}

	@Override
	public EntityHolder claimEntityHolderIfPossible(
			EntityKey key,
			Object entity,
			JdbcValuesSourceProcessingState processingState,
			EntityInitializer<?> initializer) {
		final Map<EntityKey, EntityHolderImpl> entityHolderMap = getOrInitializeEntitiesByKey();
		final EntityHolderImpl oldHolder = entityHolderMap.get( key );
		final EntityHolderImpl holder;
		if ( oldHolder != null ) {
			if ( entity != null ) {
				assert oldHolder.entity == null || oldHolder.entity == entity;
				oldHolder.entity = entity;
			}
			// Skip setting a new entity initializer if there already is one owner
			// Also skip if an entity exists which is different from the effective optional object.
			// The effective optional object is the current object to be refreshed,
			// which always needs re-initialization, even if already initialized
			if ( oldHolder.entityInitializer != null
					|| oldHolder.entity != null && oldHolder.state != EntityHolderState.ENHANCED_PROXY && (
					processingState.getProcessingOptions().getEffectiveOptionalObject() == null
							|| oldHolder.entity != processingState.getProcessingOptions().getEffectiveOptionalObject() )
			) {
				return oldHolder;
			}
			holder = oldHolder;
		}
		else {
			entityHolderMap.put( key, holder = EntityHolderImpl.forEntity( key, key.getPersister(), entity ) );
		}
		assert holder.entityInitializer == null || holder.entityInitializer == initializer;
		holder.entityInitializer = initializer;
		processingState.registerLoadingEntityHolder( holder );
		return holder;
	}

	@Override
	public @Nullable EntityHolderImpl getEntityHolder(EntityKey key) {
		return entitiesByKey == null ? null : entitiesByKey.get( key );
	}

	@Override
	public boolean containsEntityHolder(EntityKey key) {
		return entitiesByKey != null && entitiesByKey.get( key ) != null;
	}

	@Override
	public void postLoad(JdbcValuesSourceProcessingState processingState, Consumer<EntityHolder> holderConsumer) {
		final Callback callback = processingState.getExecutionContext().getCallback();
		if ( processingState.getLoadingEntityHolders() != null ) {
			final EventListenerGroup<PostLoadEventListener> listenerGroup = getSession().getFactory()
					.getFastSessionServices()
					.eventListenerGroup_POST_LOAD;
			final PostLoadEvent postLoadEvent = processingState.getPostLoadEvent();
			for ( final EntityHolder holder : processingState.getLoadingEntityHolders() ) {
				processLoadedEntityHolder( holder, listenerGroup, postLoadEvent, callback, holderConsumer );
			}
			processingState.getLoadingEntityHolders().clear();
		}
		if ( processingState.getReloadedEntityHolders() != null ) {
			for ( final EntityHolder holder : processingState.getReloadedEntityHolders() ) {
				processLoadedEntityHolder( holder, null, null, callback, holderConsumer );
			}
			processingState.getReloadedEntityHolders().clear();
		}
	}

	private void processLoadedEntityHolder(
			EntityHolder holder,
			EventListenerGroup<PostLoadEventListener> listenerGroup,
			PostLoadEvent postLoadEvent,
			Callback callback,
			Consumer<EntityHolder> holderConsumer) {
		if ( holderConsumer != null ) {
			holderConsumer.accept( holder );
		}
		if ( holder.getEntity() == null ) {
			// It's possible that we tried to load an entity and found out it doesn't exist,
			// in which case we added an entry with a null proxy and entity.
			// Remove that empty entry on post load to avoid unwanted side effects
			entitiesByKey.remove( holder.getEntityKey() );
			return;
		}
		if ( postLoadEvent != null ) {
			postLoadEvent.reset();
			postLoadEvent.setEntity( holder.getEntity() )
					.setId( holder.getEntityKey().getIdentifier() )
					.setPersister( holder.getDescriptor() );
			listenerGroup.fireEventOnEachListener(
					postLoadEvent,
					PostLoadEventListener::onPostLoad
			);
		}
		if ( callback != null ) {
			callback.invokeAfterLoadActions(
					holder.getEntity(),
					holder.getDescriptor(),
					getSession()
			);
		}
		( (EntityHolderImpl) holder ).entityInitializer = null;
	}

	@Override
	public void addEntity(EntityKey key, Object entity) {
		addEntityHolder( key, entity );
	}

	@Override
	public EntityHolder addEntityHolder(EntityKey key, Object entity) {
		final Map<EntityKey, EntityHolderImpl> entityHolderMap = getOrInitializeEntitiesByKey();
		final EntityHolderImpl oldHolder = entityHolderMap.get( key );
		final EntityHolderImpl holder;
		if ( oldHolder != null ) {
//			assert oldHolder.entity == null || oldHolder.entity == entity;
			oldHolder.entity = entity;
			holder = oldHolder;
		}
		else {
			entityHolderMap.put( key, holder = EntityHolderImpl.forEntity( key, key.getPersister(), entity ) );
		}
		holder.state = EntityHolderState.INITIALIZED;
		final BatchFetchQueue fetchQueue = this.batchFetchQueue;
		if ( fetchQueue != null ) {
			fetchQueue.removeBatchLoadableEntityKey( key );
		}
		return holder;
	}

	@Override
	public Object getEntity(EntityKey key) {
		final EntityHolderImpl holder = entitiesByKey == null ? null : entitiesByKey.get( key );
		return holder == null || holder.state == EntityHolderState.UNINITIALIZED ? null : holder.entity;
	}

	@Override
	public boolean containsEntity(EntityKey key) {
		final EntityHolderImpl holder = entitiesByKey == null ? null : entitiesByKey.get( key );
		return holder != null && holder.entity != null && holder.state != EntityHolderState.UNINITIALIZED;
	}

	@Override
	public Object removeEntity(EntityKey key) {
		final EntityHolderImpl holder = removeEntityHolder( key );
		if ( holder != null ) {
			final Object entity = holder.entity;
			if ( holder.proxy != null ) {
				holder.entity = null;
				holder.state = EntityHolderState.UNINITIALIZED;
				entitiesByKey.put( key, holder );
			}
			return entity;
		}
		return null;
	}

	@Override
	public @Nullable EntityHolderImpl removeEntityHolder(EntityKey key) {
		final EntityHolderImpl holder;
		if ( entitiesByKey != null ) {
			holder = entitiesByKey.remove( key );
			if ( entitiesByUniqueKey != null ) {
				final Object entity = holder == null ? null : holder.entity;
				final Iterator<?> itr = entitiesByUniqueKey.values().iterator();
				while ( itr.hasNext() ) {
					if ( itr.next() == entity ) {
						itr.remove();
					}
				}
			}
		}
		else {
			holder = null;
		}

		// Clear all parent cache
		parentsByChild = null;
		if ( entitySnapshotsByKey != null ) {
			entitySnapshotsByKey.remove( key );
		}
		if ( nullifiableEntityKeys != null ) {
			nullifiableEntityKeys.remove( key );
		}
		final BatchFetchQueue fetchQueue = this.batchFetchQueue;
		if ( fetchQueue != null ) {
			fetchQueue.removeBatchLoadableEntityKey( key );
			fetchQueue.removeSubselect( key );
		}
		return holder;
	}

	@Override
	public Object getEntity(EntityUniqueKey euk) {
		return entitiesByUniqueKey == null ? null : entitiesByUniqueKey.get( euk );
	}

	@Override
	public void addEntity(EntityUniqueKey euk, Object entity) {
		if ( entitiesByUniqueKey == null ) {
			entitiesByUniqueKey = CollectionHelper.mapOfSize( INIT_COLL_SIZE );
		}
		entitiesByUniqueKey.put( euk, entity );
	}

	@Override
	public EntityEntry getEntry(Object entity) {
		return entityEntryContext.getEntityEntry( entity );
	}

	@Override
	public EntityEntry removeEntry(Object entity) {
		return entityEntryContext.removeEntityEntry( entity );
	}

	@Override
	public boolean isEntryFor(Object entity) {
		return entityEntryContext.hasEntityEntry( entity );
	}

	@Override
	public CollectionEntry getCollectionEntry(PersistentCollection<?> coll) {
		return collectionEntries == null ? null : collectionEntries.get( coll );
	}

	@Override
	public EntityEntry addEntity(
			final Object entity,
			final Status status,
			final Object[] loadedState,
			final EntityKey entityKey,
			final Object version,
			final LockMode lockMode,
			final boolean existsInDatabase,
			final EntityPersister persister,
			final boolean disableVersionIncrement) {
		EntityHolder entityHolder = addEntityHolder( entityKey, entity );
		EntityEntry entityEntry = addEntry(
				entity,
				status,
				loadedState,
				null,
				entityKey.getIdentifier(),
				version,
				lockMode,
				existsInDatabase,
				persister,
				disableVersionIncrement
		);
		entityHolder.setEntityEntry( entityEntry );
		return entityEntry;
	}

	@Override
	public EntityEntry addEntry(
			final Object entity,
			final Status status,
			final Object[] loadedState,
			final Object rowId,
			final Object id,
			final Object version,
			final LockMode lockMode,
			final boolean existsInDatabase,
			final EntityPersister persister,
			final boolean disableVersionIncrement) {
		assert lockMode != null;

		final EntityEntry e;

		/*
			IMPORTANT!!!

			The following instanceof checks and castings are intentional.

			DO NOT REFACTOR to make calls through the EntityEntryFactory interface, which would result
			in polymorphic call sites which will severely impact performance.

			When a virtual method is called via an interface the JVM needs to resolve which concrete
			implementation to call.  This takes CPU cycles and is a performance penalty.  It also prevents method
			inlining which further degrades performance.  Casting to an implementation and making a direct method call
			removes the virtual call, and allows the methods to be inlined.  In this critical code path, it has a very
			large impact on performance to make virtual method calls.
		*/
		if (persister.getEntityEntryFactory() instanceof MutableEntityEntryFactory) {
			//noinspection RedundantCast
			e = ( (MutableEntityEntryFactory) persister.getEntityEntryFactory() ).createEntityEntry(
					status,
					loadedState,
					rowId,
					id,
					version,
					lockMode,
					existsInDatabase,
					persister,
					disableVersionIncrement,
					this
			);
		}
		else {
			//noinspection RedundantCast
			e = ( (ImmutableEntityEntryFactory) persister.getEntityEntryFactory() ).createEntityEntry(
					status,
					loadedState,
					rowId,
					id,
					version,
					lockMode,
					existsInDatabase,
					persister,
					disableVersionIncrement,
					this
			);
		}

		entityEntryContext.addEntityEntry( entity, e );

		setHasNonReadOnlyEnties( status );
		return e;
	}

	public EntityEntry addReferenceEntry(
			final Object entity,
			final Status status) {
		final EntityEntry entityEntry = asManagedEntity( entity ).$$_hibernate_getEntityEntry();
		entityEntry.setStatus( status );
		entityEntryContext.addEntityEntry( entity, entityEntry );

		setHasNonReadOnlyEnties( status );
		return entityEntry;
	}

	@Override
	public boolean containsCollection(PersistentCollection<?> collection) {
		return collectionEntries != null && collectionEntries.containsKey( collection );
	}

	@Override
	public boolean containsProxy(Object entity) {
		if ( entitiesByKey != null ) {
			for ( EntityHolderImpl holder : entitiesByKey.values() ) {
				if ( holder.proxy == entity ) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean reassociateIfUninitializedProxy(Object value) throws MappingException {
		if ( ! Hibernate.isInitialized( value ) ) {

			// could be a proxy....
			final LazyInitializer lazyInitializer = HibernateProxy.extractLazyInitializer( value );
			if ( lazyInitializer != null ) {
				reassociateProxy( lazyInitializer, asHibernateProxy( value ) );
				return true;
			}

			// or an uninitialized enhanced entity ("bytecode proxy")...
			if ( isPersistentAttributeInterceptable( value ) ) {
				final PersistentAttributeInterceptable bytecodeProxy = asPersistentAttributeInterceptable( value );
				final BytecodeLazyAttributeInterceptor interceptor = (BytecodeLazyAttributeInterceptor) bytecodeProxy.$$_hibernate_getInterceptor();
				if ( interceptor != null ) {
					interceptor.setSession( getSession() );
				}
				return true;
			}

		}

		return false;
	}

	@Override
	public void reassociateProxy(Object value, Object id) throws MappingException {
		final LazyInitializer lazyInitializer = HibernateProxy.extractLazyInitializer( value );
		if ( lazyInitializer != null ) {
			LOG.debugf( "Setting proxy identifier: %s", id );
			lazyInitializer.setIdentifier( id );
			reassociateProxy( lazyInitializer, asHibernateProxy( value ) );
		}
	}

	/**
	 * Associate a proxy that was instantiated by another session with this session
	 *
	 * @param li The proxy initializer.
	 * @param proxy The proxy to reassociate.
	 */
	private void reassociateProxy(LazyInitializer li, HibernateProxy proxy) {
		if ( li.getSession() != this.getSession() ) {
			final EntityPersister persister = session.getFactory()
					.getRuntimeMetamodels()
					.getMappingMetamodel()
					.getEntityDescriptor( li.getEntityName() );
			final EntityKey key = session.generateEntityKey( li.getInternalIdentifier(), persister );
		  	// any earlier proxy takes precedence
			final Map<EntityKey, EntityHolderImpl> entityHolderMap = getOrInitializeEntitiesByKey();
			final EntityHolderImpl oldHolder = entityHolderMap.get( key );
			if ( oldHolder != null ) {
				if ( oldHolder.proxy == null ) {
					oldHolder.proxy = proxy;
				}
			}
			else {
				entityHolderMap.put( key, EntityHolderImpl.forProxy( key, persister, proxy ) );
			}
			proxy.getHibernateLazyInitializer().setSession( session );
		}
	}

	@Override
	public Object unproxy(Object maybeProxy) throws HibernateException {
		final LazyInitializer lazyInitializer = HibernateProxy.extractLazyInitializer( maybeProxy );
		if ( lazyInitializer != null ) {
			if ( lazyInitializer.isUninitialized() ) {
				throw new PersistentObjectException(
						"object was an uninitialized proxy for " + lazyInitializer.getEntityName()
				);
			}
			//unwrap the object and return
			return lazyInitializer.getImplementation();
		}
		else {
			return maybeProxy;
		}
	}

	@Override
	public Object unproxyAndReassociate(final Object maybeProxy) throws HibernateException {
		final LazyInitializer lazyInitializer = HibernateProxy.extractLazyInitializer( maybeProxy );
		if ( lazyInitializer != null ) {
			reassociateProxy( lazyInitializer, asHibernateProxy( maybeProxy ) );
			//initialize + unwrap the object and return it
			return lazyInitializer.getImplementation();
		}
		else if ( isPersistentAttributeInterceptable( maybeProxy ) ) {
			final PersistentAttributeInterceptable interceptable = asPersistentAttributeInterceptable( maybeProxy );
			final PersistentAttributeInterceptor interceptor = interceptable.$$_hibernate_getInterceptor();
			if ( interceptor instanceof EnhancementAsProxyLazinessInterceptor ) {
				( (EnhancementAsProxyLazinessInterceptor) interceptor ).forceInitialize( maybeProxy, null );
			}
			return maybeProxy;
		}
		else {
			return maybeProxy;
		}
	}

	@Override
	public void checkUniqueness(EntityKey key, Object object) throws HibernateException {
		final Object entity = getEntity( key );
		if ( entity == object ) {
			throw new AssertionFailure( "object already associated, but no entry was found" );
		}
		if ( entity != null ) {
			throw new NonUniqueObjectException( key.getIdentifier(), key.getEntityName() );
		}
	}

	@Override
	public Object narrowProxy(Object proxy, EntityPersister persister, EntityKey key, Object object)
			throws HibernateException {

		final Class<?> concreteProxyClass = persister.getConcreteProxyClass();
		final boolean alreadyNarrow = concreteProxyClass.isInstance( proxy );

		if ( !alreadyNarrow ) {
			LOG.narrowingProxy( concreteProxyClass );

			// If an impl is passed, there is really no point in creating a proxy.
			// It would just be extra processing.  Just return the impl
			if ( object != null ) {
				removeProxyByKey( key );
				return object;
			}

			// Similarly, if the original HibernateProxy is initialized, there
			// is again no point in creating a proxy.  Just return the impl
			final LazyInitializer lazyInitializer = HibernateProxy.extractLazyInitializer( proxy );
			if ( !lazyInitializer.isUninitialized() ) {
				final Object impl = lazyInitializer.getImplementation();
				// can we return it?
				if ( concreteProxyClass.isInstance( impl ) ) {
					removeProxyByKey( key );
					return impl;
				}
			}


			// Otherwise, create the narrowed proxy
			final HibernateProxy narrowedProxy = asHibernateProxy( persister.createProxy( key.getIdentifier(), session ) );

			// set the read-only/modifiable mode in the new proxy to what it was in the original proxy
			final boolean readOnlyOrig = lazyInitializer.isReadOnly();
			narrowedProxy.getHibernateLazyInitializer().setReadOnly( readOnlyOrig );

			return narrowedProxy;
		}
		else {
			if ( object != null ) {
				HibernateProxy.extractLazyInitializer( proxy ).setImplementation( object );
			}
			return proxy;
		}
	}

	private Object removeProxyByKey(final EntityKey key) {
		final EntityHolderImpl entityHolder;
		if ( entitiesByKey != null && ( entityHolder = entitiesByKey.get( key ) ) != null ) {
			Object proxy = entityHolder.proxy;
			entityHolder.proxy = null;
			return proxy;
		}
		return null;
	}

	@Override
	public Object proxyFor(EntityPersister persister, EntityKey key, Object impl) throws HibernateException {
		if ( !persister.hasProxy() ) {
			return impl;
		}
		final Object proxy = getProxy( key );
		return ( proxy != null ) ? narrowProxy( proxy, persister, key, impl ) : impl;
	}

	@Override
	public Object proxyFor(Object impl) throws HibernateException {
		final EntityEntry e = getEntry( impl );
		if ( e == null ) {
			return impl;
		}
		return proxyFor( e.getPersister(), e.getEntityKey(), impl );
	}

	@Override
	public Object proxyFor(EntityHolder holder) throws HibernateException {
		return proxyFor( holder, holder.getDescriptor() );
	}

	@Override
	public Object proxyFor(EntityHolder holder, EntityPersister persister) {
		final Object proxy = holder.getProxy();
		return proxy != null && persister.hasProxy()
				? narrowProxy( proxy, persister, holder.getEntityKey(), holder.getEntity() )
				: holder.getEntity();
	}

	@Override
	public void addEnhancedProxy(EntityKey key, PersistentAttributeInterceptable entity) {
		final Map<EntityKey, EntityHolderImpl> entityHolderMap = getOrInitializeEntitiesByKey();
		final EntityHolderImpl oldHolder = entityHolderMap.get( key );
		final EntityHolderImpl holder;
		if ( oldHolder != null ) {
			oldHolder.entity = entity;
			holder = oldHolder;
		}
		else {
			entityHolderMap.put( key, holder = EntityHolderImpl.forEntity( key, key.getPersister(), entity ) );
		}
		holder.state = EntityHolderState.ENHANCED_PROXY;
	}

	@Override
	public Object getCollectionOwner(Object key, CollectionPersister collectionPersister) throws MappingException {
		// todo : we really just need to add a split in the notions of:
		//		1) collection key
		//		2) collection owner key
		// these 2 are not always the same.  Same is true in the case of ToOne associations with property-ref...
		final EntityPersister ownerPersister = collectionPersister.getOwnerEntityPersister();
		if ( ownerPersister.getIdentifierType().getReturnedClass().isInstance( key ) ) {
			return getEntity( session.generateEntityKey( key, collectionPersister.getOwnerEntityPersister() ) );
		}

		// we have a property-ref type mapping for the collection key.  But that could show up a few ways here...
		//
		//		1) The incoming key could be the entity itself...
		if ( ownerPersister.isInstance( key ) ) {
			final Object ownerId = ownerPersister.getIdentifier( key, session );
			if ( ownerId == null ) {
				return null;
			}
			return getEntity( session.generateEntityKey( ownerId, ownerPersister ) );
		}

		final CollectionType collectionType = collectionPersister.getCollectionType();

		//		2) The incoming key is most likely the collection key which we need to resolve to the owner key
		//			find the corresponding owner instance
		//			a) try by EntityUniqueKey
		if ( collectionType.getLHSPropertyName() != null ) {
			final Object owner = getEntity(
					new EntityUniqueKey(
							ownerPersister.getEntityName(),
							collectionType.getLHSPropertyName(),
							key,
							collectionPersister.getKeyType(),
							session.getFactory()
					)
			);
			if ( owner != null ) {
				return owner;
			}

			//		b) try by EntityKey, which means we need to resolve owner-key -> collection-key
			//			IMPL NOTE : yes if we get here this impl is very non-performant, but PersistenceContext
			//					was never designed to handle this case; adding that capability for real means splitting
			//					the notions of:
			//						1) collection key
			//						2) collection owner key
			// 					these 2 are not always the same (same is true in the case of ToOne associations with
			// 					property-ref).  That would require changes to (at least) CollectionEntry and quite
			//					probably changes to how the sql for collection initializers are generated
			//
			//			We could also possibly see if the referenced property is a natural id since we already have caching
			//			in place of natural id snapshots.  BUt really its better to just do it the right way ^^ if we start
			// 			going that route
			final Object ownerId = ownerPersister.getIdByUniqueKey( key, collectionType.getLHSPropertyName(), session );
			return getEntity( session.generateEntityKey( ownerId, ownerPersister ) );
		}

		// as a last resort this is what the old code did...
		return getEntity( session.generateEntityKey( key, collectionPersister.getOwnerEntityPersister() ) );
	}

	@Override
	public Object getLoadedCollectionOwnerOrNull(PersistentCollection<?> collection) {
		final CollectionEntry ce = getCollectionEntry( collection );
		if ( ce == null || ce.getLoadedPersister() == null ) {
			return null;
		}

		Object loadedOwner = null;
		// TODO: an alternative is to check if the owner has changed; if it hasn't then
		// return collection.getOwner()
		final Object entityId = getLoadedCollectionOwnerIdOrNull( ce );
		if ( entityId != null ) {
			loadedOwner = getCollectionOwner( entityId, ce.getLoadedPersister() );
		}
		return loadedOwner;
	}

	@Override
	public Object getLoadedCollectionOwnerIdOrNull(PersistentCollection<?> collection) {
		return getLoadedCollectionOwnerIdOrNull( getCollectionEntry( collection ) );
	}

	/**
	 * Get the ID for the entity that owned this persistent collection when it was loaded
	 *
	 * @param ce The collection entry
	 * @return the owner ID if available from the collection's loaded key; otherwise, returns null
	 */
	private Object getLoadedCollectionOwnerIdOrNull(CollectionEntry ce) {
		if ( ce == null || ce.getLoadedKey() == null || ce.getLoadedPersister() == null ) {
			return null;
		}
		// TODO: an alternative is to check if the owner has changed; if it hasn't then
		// get the ID from collection.getOwner()
		return ce.getLoadedPersister().getCollectionType().getIdOfOwnerOrNull( ce.getLoadedKey(), session );
	}

	@Override
	public void addUninitializedCollection(CollectionPersister persister, PersistentCollection<?> collection, Object id) {
		final CollectionEntry ce = new CollectionEntry( collection, persister, id, flushing );
		addCollection( collection, ce, id );
		if ( session.getLoadQueryInfluencers().effectivelyBatchLoadable( persister ) ) {
			getBatchFetchQueue().addBatchLoadableCollection( collection, ce );
		}
	}

	@Override
	public void addUninitializedDetachedCollection(CollectionPersister persister, PersistentCollection<?> collection) {
		final Object key = collection.getKey();
		assert key != null;
		final CollectionEntry ce = new CollectionEntry( persister, key );
		addCollection( collection, ce, key );
		if ( session.getLoadQueryInfluencers().effectivelyBatchLoadable( persister ) ) {
			getBatchFetchQueue().addBatchLoadableCollection( collection, ce );
		}
	}

	@Override
	public void addNewCollection(CollectionPersister persister, PersistentCollection<?> collection)
			throws HibernateException {
		addCollection( collection, persister );
	}

	/**
	 * Add a collection to the cache, with a given collection entry.
	 *
	 * @param coll The collection for which we are adding an entry.
	 * @param entry The entry representing the collection.
	 * @param key The key of the collection's entry.
	 */
	private void addCollection(PersistentCollection<?> coll, CollectionEntry entry, Object key) {
		getOrInitializeCollectionEntries().put( coll, entry );
		final CollectionKey collectionKey = new CollectionKey( entry.getLoadedPersister(), key );
		final PersistentCollection<?> old = addCollectionByKey( collectionKey, coll );
		if ( old != null ) {
			if ( old == coll ) {
				throw new AssertionFailure( "bug adding collection twice" );
			}
			// or should it actually throw an exception?
			old.unsetSession( session );
			if ( collectionEntries != null ) {
				collectionEntries.remove( old );
			}
			// watch out for a case where old is still referenced
			// somewhere in the object graph! (which is a user error)
		}
	}

	private IdentityMap<PersistentCollection<?>, CollectionEntry> getOrInitializeCollectionEntries() {
		if ( this.collectionEntries == null ) {
			this.collectionEntries = IdentityMap.instantiateSequenced( INIT_COLL_SIZE );
		}
		return this.collectionEntries;
	}

	/**
	 * Add a collection to the cache, creating a new collection entry for it
	 *
	 * @param collection The collection for which we are adding an entry.
	 * @param persister The collection persister
	 */
	private void addCollection(PersistentCollection<?> collection, CollectionPersister persister) {
		final CollectionEntry ce = new CollectionEntry( persister, collection );
		getOrInitializeCollectionEntries().put( collection, ce );
	}

	@Override
	public void addInitializedDetachedCollection(CollectionPersister collectionPersister, PersistentCollection<?> collection)
			throws HibernateException {
		if ( collection.isUnreferenced() || collection.getKey() == null ) {
			//treat it just like a new collection
			addCollection( collection, collectionPersister );
		}
		else {
			final CollectionEntry ce = new CollectionEntry( collection, session.getFactory() );
			addCollection( collection, ce, collection.getKey() );
		}
	}

	@Override
	public CollectionEntry addInitializedCollection(CollectionPersister persister, PersistentCollection<?> collection, Object id)
			throws HibernateException {
		final CollectionEntry ce = new CollectionEntry( collection, persister, id, flushing );
		ce.postInitialize( collection, session );
		addCollection( collection, ce, id );
		return ce;
	}

	@Override
	public PersistentCollection<?> getCollection(CollectionKey collectionKey) {
		return collectionsByKey == null ? null : collectionsByKey.get( collectionKey );
	}

	@Override
	public void addNonLazyCollection(PersistentCollection<?> collection) {
		if ( nonlazyCollections == null ) {
			nonlazyCollections = new ArrayList<>( INIT_COLL_SIZE );
		}
		nonlazyCollections.add( collection );
	}

	@Override
	public void initializeNonLazyCollections() throws HibernateException {
		initializeNonLazyCollections( PersistentCollection::forceInitialization );
	}

	protected void initializeNonLazyCollections(Consumer<PersistentCollection<?>> initializeAction ) {
		if ( loadCounter == 0 ) {
			LOG.trace( "Initializing non-lazy collections" );

			//do this work only at the very highest level of the load
			//don't let this method be called recursively
			loadCounter++;
			try {
				int size;
				while ( nonlazyCollections != null && ( size = nonlazyCollections.size() ) > 0 ) {
					//note that each iteration of the loop may add new elements
					initializeAction.accept( nonlazyCollections.remove( size - 1 ) );
				}
			}
			finally {
				loadCounter--;
				clearNullProperties();
			}
		}
	}

	@Override
	public PersistentCollection<?> getCollectionHolder(Object array) {
		return arrayHolders == null ? null : arrayHolders.get( array );
	}

	@Override
	public void addCollectionHolder(PersistentCollection<?> holder) {
		//TODO:refactor + make this method private
		if ( arrayHolders == null ) {
			arrayHolders = new IdentityHashMap<>( INIT_COLL_SIZE );
		}
		arrayHolders.put( holder.getValue(), holder );
	}

	@Override
	public PersistentCollection<?> removeCollectionHolder(Object array) {
		return arrayHolders != null ? arrayHolders.remove( array ) : null;
	}

	@Override
	public Serializable getSnapshot(PersistentCollection<?> coll) {
		return getCollectionEntry( coll ).getSnapshot();
	}

//	@Override
//	public CollectionEntry getCollectionEntryOrNull(Object collection) {
//		PersistentCollection<?> coll;
//		if ( collection instanceof PersistentCollection ) {
//			coll = (PersistentCollection<?>) collection;
//			//if (collection==null) throw new TransientObjectException("Collection was not yet persistent");
//		}
//		else {
//			coll = getCollectionHolder( collection );
//			if ( coll == null && collectionEntries != null ) {
//				//it might be an unwrapped collection reference!
//				//try to find a wrapper (slowish)
//				final Iterator<PersistentCollection<?>> wrappers = collectionEntries.keyIterator();
//				while ( wrappers.hasNext() ) {
//					final PersistentCollection<?> pc = wrappers.next();
//					if ( pc.isWrapper( collection ) ) {
//						coll = pc;
//						break;
//					}
//				}
//			}
//		}
//
//		return (coll == null) ? null : getCollectionEntry( coll );
//	}

	@Override
	public Object getProxy(EntityKey key) {
		final EntityHolderImpl holder = entitiesByKey == null ? null : entitiesByKey.get( key );
		return holder == null ? null : holder.proxy;
	}

	@Override
	public void addProxy(EntityKey key, Object proxy) {
		final Map<EntityKey, EntityHolderImpl> entityHolderMap = getOrInitializeEntitiesByKey();
		final EntityHolderImpl holder = entityHolderMap.get( key );
		if ( holder != null ) {
			holder.proxy = proxy;
		}
		else {
			entityHolderMap.put( key, EntityHolderImpl.forProxy( key, key.getPersister(), proxy ) );
		}
	}

	@Override
	public Object removeProxy(EntityKey key) {
		final BatchFetchQueue fetchQueue = this.batchFetchQueue;
		if ( fetchQueue != null ) {
			fetchQueue.removeBatchLoadableEntityKey( key );
			fetchQueue.removeSubselect( key );
		}
		return removeProxyByKey( key );
	}

//	@Override
//	public HashSet getNullifiableEntityKeys() {
//		if ( nullifiableEntityKeys == null ) {
//			nullifiableEntityKeys = new HashSet<>();
//		}
//		return nullifiableEntityKeys;
//	}

	/**
	 * @deprecated this will be removed: it provides too wide access, making it hard to optimise the internals
	 * for specific access needs. Consider using #iterateEntities instead.
	 */
	@Deprecated
	@Override
	public Map<EntityKey,Object> getEntitiesByKey() {
		if ( entitiesByKey == null ) {
			return Collections.emptyMap();
		}
		final HashMap<EntityKey, Object> result = CollectionHelper.mapOfSize( entitiesByKey.size() );
		for ( Entry<EntityKey, EntityHolderImpl> entry : entitiesByKey.entrySet() ) {
			if ( entry.getValue().entity != null ) {
				result.put( entry.getKey(), entry.getValue().entity );
			}
		}
		return result;
	}

	@Override
	public Map<EntityKey, EntityHolder> getEntityHoldersByKey() {
		//noinspection unchecked,rawtypes
		return (Map) entitiesByKey;
	}

	@Override
	public Iterator<Object> managedEntitiesIterator() {
		if ( entitiesByKey == null ) {
			return Collections.emptyIterator();
		}
		final Iterator<EntityHolderImpl> iterator = entitiesByKey.values().iterator();
		final var iter = new Iterator<Object>() {
			Object next;
			void prepareNext() {
				next = null;
				while ( iterator.hasNext() ) {
					final EntityHolderImpl next = iterator.next();
					if ( next.entity != null ) {
						this.next = next.entity;
						break;
					}
				}
			}

			@Override
			public boolean hasNext() {
				return next != null;
			}

			@Override
			public Object next() {
				final Object next = this.next;
				if ( next == null ) {
					throw new NoSuchElementException();
				}
				prepareNext();
				return next;
			}
		};
		iter.prepareNext();
		return iter;
	}

	@Override
	public int getNumberOfManagedEntities() {
		return entityEntryContext.getNumberOfManagedEntities();
	}

//	@Override
//	public Map getEntityEntries() {
//		return null;
//	}

	/**
	 * @deprecated We should not expose this directly: the other accessors that have been created as a replacement
	 * have better chances of skipping initializing this map, which is a good performance improvement.
	 * @return the map of managed collection entries.
	 */
	@Override
	@Deprecated
	public @Nullable Map<PersistentCollection<?>,CollectionEntry> getCollectionEntries() {
		return collectionEntries;
	}

	@Override
	public void forEachCollectionEntry(BiConsumer<PersistentCollection<?>, CollectionEntry> action, boolean concurrent) {
		if ( collectionEntries != null ) {
			if ( concurrent ) {
				for ( Entry<PersistentCollection<?>,CollectionEntry> entry : IdentityMap.concurrentEntries( collectionEntries ) ) {
					action.accept( entry.getKey(), entry.getValue() );
				}
			}
			else {
				collectionEntries.forEach( action );
			}
		}
	}

	@Override
	public Map<CollectionKey,PersistentCollection<?>> getCollectionsByKey() {
		return collectionsByKey == null ? Collections.emptyMap() : collectionsByKey;
	}

	@Override
	public int getCascadeLevel() {
		return cascading;
	}

	@Override
	public int incrementCascadeLevel() {
		return ++cascading;
	}

	@Override
	public int decrementCascadeLevel() {
		return --cascading;
	}

	@Override
	public boolean isFlushing() {
		return flushing;
	}

	@Override
	public void setFlushing(boolean flushing) {
		final boolean afterFlush = this.flushing && ! flushing;
		this.flushing = flushing;
		if ( afterFlush ) {
			getNaturalIdResolutions().cleanupFromSynchronizations();
		}
	}

	public boolean isRemovingOrphanBeforeUpates() {
		return removeOrphanBeforeUpdatesCounter > 0;
	}

	public void beginRemoveOrphanBeforeUpdates() {
		if ( getCascadeLevel() < 1 ) {
			throw new IllegalStateException( "Attempt to remove orphan when not cascading." );
		}
		if ( removeOrphanBeforeUpdatesCounter >= getCascadeLevel() ) {
			throw new IllegalStateException(
					String.format(
							"Cascade level [%d] is out of sync with removeOrphanBeforeUpdatesCounter [%d] before incrementing removeOrphanBeforeUpdatesCounter",
							getCascadeLevel(),
							removeOrphanBeforeUpdatesCounter
					)
			);
		}
		removeOrphanBeforeUpdatesCounter++;
	}

	public void endRemoveOrphanBeforeUpdates() {
		if ( getCascadeLevel() < 1 ) {
			throw new IllegalStateException( "Finished removing orphan when not cascading." );
		}
		if ( removeOrphanBeforeUpdatesCounter > getCascadeLevel() ) {
			throw new IllegalStateException(
					String.format(
							"Cascade level [%d] is out of sync with removeOrphanBeforeUpdatesCounter [%d] before decrementing removeOrphanBeforeUpdatesCounter",
							getCascadeLevel(),
							removeOrphanBeforeUpdatesCounter
					)
			);
		}
		removeOrphanBeforeUpdatesCounter--;
	}

	/**
	 * Call this before beginning a two-phase load
	 */
	@Override
	public void beforeLoad() {
		loadCounter++;
	}

	/**
	 * Call this after finishing a two-phase load
	 */
	@Override
	public void afterLoad() {
		loadCounter--;
	}

	@Override
	public boolean isLoadFinished() {
		return loadCounter == 0;
	}

	@Override
	public String toString() {
		final String entityKeySet = entitiesByKey == null ? "[]" :  entitiesByKey.keySet().toString();
		final String collectionsKeySet = collectionsByKey == null ? "[]" : collectionsByKey.keySet().toString();
		return "PersistenceContext[entityKeys=" + entityKeySet + ", collectionKeys=" + collectionsKeySet + "]";
	}

	@Override
	public Entry<Object,EntityEntry>[] reentrantSafeEntityEntries() {
		return entityEntryContext.reentrantSafeEntityEntries();
	}

	@Override
	public Object getOwnerId(String entityName, String propertyName, Object childEntity, Map mergeMap) {
		final String collectionRole = entityName + '.' + propertyName;

		final MappingMetamodelImplementor mappingMetamodel = session.getFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel();
		final EntityPersister persister = mappingMetamodel.getEntityDescriptor( entityName );
		final CollectionPersister collectionPersister = mappingMetamodel.getCollectionDescriptor( collectionRole );

	    // try cache lookup first
		final Object parent = getParentsByChild( childEntity );
		if ( parent != null ) {
			final EntityEntry entityEntry = entityEntryContext.getEntityEntry( parent );
			//there maybe more than one parent, filter by type
			if ( persister.isSubclassEntityName( entityEntry.getEntityName() )
					&& isFoundInParent( propertyName, childEntity, persister, collectionPersister, parent ) ) {
				return getEntry( parent ).getId();
			}
			else {
				// remove wrong entry
				removeChildParent( childEntity );
			}
		}

		//not found in case, proceed
		// iterate all the entities currently associated with the persistence context.
		for ( Entry<Object,EntityEntry> me : reentrantSafeEntityEntries() ) {
			final EntityEntry entityEntry = me.getValue();
			// does this entity entry pertain to the entity persister in which we are interested (owner)?
			if ( persister.isSubclassEntityName( entityEntry.getEntityName() ) ) {
				final Object entityEntryInstance = me.getKey();

				//check if the managed object is the parent
				boolean found = isFoundInParent(
						propertyName,
						childEntity,
						persister,
						collectionPersister,
						entityEntryInstance
				);

				if ( !found && mergeMap != null ) {
					//check if the detached object being merged is the parent
					final Object unmergedInstance = mergeMap.get( entityEntryInstance );
					final Object unmergedChild = mergeMap.get( childEntity );
					if ( unmergedInstance != null && unmergedChild != null ) {
						found = isFoundInParent(
								propertyName,
								unmergedChild,
								persister,
								collectionPersister,
								unmergedInstance
						);
						LOG.debugf(
								"Detached object being merged (corresponding with a managed entity) has a collection that [%s] the detached child.",
								( found ? "contains" : "does not contain" )
						);
					}
				}

				if ( found ) {
					return entityEntry.getId();
				}

			}
		}

		// if we get here, it is possible that we have a proxy 'in the way' of the merge map resolution...
		// 		NOTE: decided to put this here rather than in the above loop as I was nervous about the performance
		//		of the loop-in-loop especially considering this is far more likely the 'edge case'
		if ( mergeMap != null ) {
			for ( Object o : mergeMap.entrySet() ) {
				final Entry<?,?> mergeMapEntry = (Entry<?,?>) o;
				final LazyInitializer lazyInitializer = HibernateProxy.extractLazyInitializer( mergeMapEntry.getKey() );
				if ( lazyInitializer != null ) {
					if ( persister.isSubclassEntityName( lazyInitializer.getEntityName() ) ) {
						HibernateProxy proxy = asHibernateProxy( mergeMapEntry.getKey() );
						boolean found = isFoundInParent(
								propertyName,
								childEntity,
								persister,
								collectionPersister,
								mergeMap.get( proxy )
						);
						LOG.debugf(
								"Detached proxy being merged has a collection that [%s] the managed child.",
								(found ? "contains" : "does not contain")
						);
						if ( !found ) {
							found = isFoundInParent(
									propertyName,
									mergeMap.get( childEntity ),
									persister,
									collectionPersister,
									mergeMap.get( proxy )
							);
							LOG.debugf(
									"Detached proxy being merged has a collection that [%s] the detached child being merged..",
									(found ? "contains" : "does not contain")
							);
						}
						if ( found ) {
							return proxy.getHibernateLazyInitializer().getInternalIdentifier();
						}
					}
				}
			}
		}

		return null;
	}

	private Object getParentsByChild(Object childEntity) {
		if ( parentsByChild != null ) {
			return parentsByChild.get( childEntity );
		}
		return null;
	}

	private boolean isFoundInParent(
			String property,
			Object childEntity,
			EntityPersister persister,
			CollectionPersister collectionPersister,
			Object potentialParent) {
		final Object collection = persister.getPropertyValue( potentialParent, property );
		return collection != null
				&& Hibernate.isInitialized( collection )
				&& collectionPersister.getCollectionType().contains( collection, childEntity, session );
	}

	@Override
	public Object getIndexInOwner(String entity, String property, Object childEntity, Map mergeMap) {
		final MappingMetamodelImplementor metamodel = session.getFactory().getRuntimeMetamodels().getMappingMetamodel();
		final EntityPersister persister = metamodel.getEntityDescriptor( entity );
		final CollectionPersister cp = metamodel.getCollectionDescriptor( entity + '.' + property );

		//Extracted as we're logging within two hot loops
		final boolean debugEnabled = LOG.isDebugEnabled();

		// try cache lookup first
		final Object parent = getParentsByChild( childEntity );
		if ( parent != null ) {
			final EntityEntry entityEntry = entityEntryContext.getEntityEntry( parent );
			//there maybe more than one parent, filter by type
			if ( persister.isSubclassEntityName( entityEntry.getEntityName() ) ) {
				Object index = getIndexInParent( property, childEntity, persister, cp, parent );

				if ( index == null && mergeMap != null ) {
					final Object unMergedInstance = mergeMap.get( parent );
					final Object unMergedChild = mergeMap.get( childEntity );
					if ( unMergedInstance != null && unMergedChild != null ) {
						index = getIndexInParent( property, unMergedChild, persister, cp, unMergedInstance );
						if ( debugEnabled ) {
							LOG.debugf(
									"A detached object being merged (corresponding to a parent in parentsByChild) has an indexed collection that [%s] the detached child being merged. ",
									( index != null ? "contains" : "does not contain" )
							);
						}
					}
				}
				if ( index != null ) {
					return index;
				}
			}
			else {
				// remove wrong entry
				removeChildParent( childEntity );
			}
		}

		//Not found in cache, proceed
		for ( Entry<Object, EntityEntry> me : reentrantSafeEntityEntries() ) {
			final EntityEntry ee = me.getValue();
			if ( persister.isSubclassEntityName( ee.getEntityName() ) ) {
				final Object instance = me.getKey();

				Object index = getIndexInParent( property, childEntity, persister, cp, instance );
				if ( index==null && mergeMap!=null ) {
					final Object unMergedInstance = mergeMap.get( instance );
					final Object unMergedChild = mergeMap.get( childEntity );
					if ( unMergedInstance != null && unMergedChild!=null ) {
						index = getIndexInParent( property, unMergedChild, persister, cp, unMergedInstance );
						if ( debugEnabled ) {
							LOG.debugf(
									"A detached object being merged (corresponding to a managed entity) has an indexed collection that [%s] the detached child being merged. ",
									(index != null ? "contains" : "does not contain" )
							);
						}
					}
				}

				if ( index != null ) {
					return index;
				}
			}
		}
		return null;
	}

	private Object getIndexInParent(
			String property,
			Object childEntity,
			EntityPersister persister,
			CollectionPersister collectionPersister,
			Object potentialParent){
		final Object collection = persister.getPropertyValue( potentialParent, property );
		if ( collection != null && Hibernate.isInitialized( collection ) ) {
			return collectionPersister.getCollectionType().indexOf( collection, childEntity );
		}
		else {
			return null;
		}
	}

	@Override
	public void addNullProperty(EntityKey ownerKey, String propertyName) {
		if ( nullAssociations == null ) {
			nullAssociations = CollectionHelper.setOfSize( INIT_COLL_SIZE );
		}
		nullAssociations.add( new AssociationKey( ownerKey, propertyName ) );
	}

	@Override
	public boolean isPropertyNull(EntityKey ownerKey, String propertyName) {
		return nullAssociations != null && nullAssociations.contains( new AssociationKey( ownerKey, propertyName ) );
	}

	private void clearNullProperties() {
		nullAssociations = null;
	}

	@Override
	public boolean isReadOnly(Object entityOrProxy) {
		if ( entityOrProxy == null ) {
			throw new AssertionFailure( "object must be non-null." );
		}
		boolean isReadOnly;
		final LazyInitializer lazyInitializer = HibernateProxy.extractLazyInitializer( entityOrProxy );
		if ( lazyInitializer != null ) {
			isReadOnly = lazyInitializer.isReadOnly();
		}
		else {
			final EntityEntry ee =  getEntry( entityOrProxy );
			if ( ee == null ) {
				throw new TransientObjectException( "Instance was not associated with this persistence context" );
			}
			isReadOnly = ee.isReadOnly();
		}
		return isReadOnly;
	}

	@Override
	public void setReadOnly(Object object, boolean readOnly) {
		if ( object == null ) {
			throw new AssertionFailure( "object must be non-null." );
		}
		if ( isReadOnly( object ) == readOnly ) {
			return;
		}
		final LazyInitializer lazyInitializer = HibernateProxy.extractLazyInitializer( object );
		if ( lazyInitializer != null ) {
			setProxyReadOnly( lazyInitializer, readOnly );
			if ( ! lazyInitializer.isUninitialized() ) {
				setEntityReadOnly(
						lazyInitializer.getImplementation(),
						readOnly
				);
			}
		}
		else {
			setEntityReadOnly( object, readOnly );
			// PersistenceContext.proxyFor( entity ) returns entity if there is no proxy for that entity
			// so need to check the return value to be sure it is really a proxy
			final Object maybeProxy = getSession().getPersistenceContextInternal().proxyFor( object );
			final LazyInitializer lazyInitializer1 = HibernateProxy.extractLazyInitializer( maybeProxy );
			if ( lazyInitializer1 != null ) {
				setProxyReadOnly( lazyInitializer1, readOnly );
			}
		}
	}

	private void setProxyReadOnly(LazyInitializer hibernateLazyInitializer, boolean readOnly) {
		if ( hibernateLazyInitializer.getSession() != getSession() ) {
			throw new AssertionFailure(
					"Attempt to set a proxy to read-only that is associated with a different session" );
		}
		hibernateLazyInitializer.setReadOnly( readOnly );
	}

	private void setEntityReadOnly(Object entity, boolean readOnly) {
		final EntityEntry entry = getEntry( entity );
		if ( entry == null ) {
			throw new TransientObjectException( "Instance was not associated with this persistence context" );
		}
		entry.setReadOnly( readOnly, entity );
		hasNonReadOnlyEntities = hasNonReadOnlyEntities || ! readOnly;
	}

	@Override
	public void replaceDelayedEntityIdentityInsertKeys(EntityKey oldKey, Object generatedId) {
		final EntityHolderImpl holder = entitiesByKey == null ? null : entitiesByKey.remove( oldKey );
		final Object entity = holder == null ? null : holder.entity;
		final EntityEntry oldEntry = entityEntryContext.removeEntityEntry( entity );
		this.parentsByChild = null;

		final EntityKey newKey = session.generateEntityKey( generatedId, oldEntry.getPersister() );
		EntityHolder entityHolder = addEntityHolder( newKey, entity );
		EntityEntry entityEntry = addEntry(
				entity,
				oldEntry.getStatus(),
				oldEntry.getLoadedState(),
				oldEntry.getRowId(),
				generatedId,
				oldEntry.getVersion(),
				oldEntry.getLockMode(),
				oldEntry.isExistsInDatabase(),
				oldEntry.getPersister(),
				oldEntry.isBeingReplicated()
		);
		entityHolder.setEntityEntry( entityEntry );
	}

	@Override
	public void replaceEntityEntryRowId(Object entity, Object rowId) {
		final EntityEntry oldEntry = entityEntryContext.removeEntityEntry( entity );
		EntityEntry entityEntry = addEntry(
				entity,
				oldEntry.getStatus(),
				oldEntry.getLoadedState(),
				rowId,
				oldEntry.getId(),
				oldEntry.getVersion(),
				oldEntry.getLockMode(),
				oldEntry.isExistsInDatabase(),
				oldEntry.getPersister(),
				oldEntry.isBeingReplicated()
		);
		getEntityHolder( oldEntry.getEntityKey() ).setEntityEntry( entityEntry );
	}

	/**
	 * Used by the owning session to explicitly control serialization of the
	 * persistence context.
	 *
	 * @param oos The stream to which the persistence context should get written
	 * @throws IOException serialization errors.
	 */
	public void serialize(ObjectOutputStream oos) throws IOException {
		LOG.trace( "Serializing persistence-context" );

		oos.writeBoolean( defaultReadOnly );
		oos.writeBoolean( hasNonReadOnlyEntities );
		writeMapToStream( entitiesByUniqueKey, oos, "entitiesByUniqueKey", (entry, stream) -> {
			entry.getKey().serialize( stream );
			stream.writeObject( entry.getValue() );
		} );
		writeMapToStream( entitySnapshotsByKey, oos, "entitySnapshotsByKey", (entry, stream) -> {
			entry.getKey().serialize( stream );
			stream.writeObject( entry.getValue() );
		} );

		entityEntryContext.serialize( oos );

		writeMapToStream( entitiesByKey, oos, "entitiesByKey", (entry, stream) -> {
			entry.getKey().serialize( stream );
			final EntityHolderImpl holder = entry.getValue();
			stream.writeObject( holder.descriptor.getEntityName() );
			stream.writeObject( holder.entity );
			stream.writeObject( holder.proxy );
			stream.writeObject( holder.state );
		} );
		writeMapToStream(
				collectionsByKey,
				oos,
				"collectionsByKey",
				(entry, stream) -> {
					entry.getKey().serialize( stream );
					stream.writeObject( entry.getValue() );
				}
		);
		writeMapToStream(
				collectionEntries,
				oos,
				"collectionEntries",
				(entry, stream) -> {
					stream.writeObject( entry.getKey() );
					entry.getValue().serialize( stream );
				}
		);
		writeMapToStream(
				arrayHolders,
				oos,
				"arrayHolders",
				(entry, stream) -> {
					stream.writeObject( entry.getKey() );
					stream.writeObject( entry.getValue() );
				}
		);
		writeCollectionToStream( nullifiableEntityKeys, oos, "nullifiableEntityKey", EntityKey::serialize );
		writeCollectionToStream( deletedUnloadedEntityKeys, oos, "deletedUnloadedEntityKeys", EntityKey::serialize );
	}

	private interface Serializer<E> {

		void serialize(E element, ObjectOutputStream oos) throws IOException;
	}

	private <K, V> void writeMapToStream(
			Map<K, V> map,
			ObjectOutputStream oos,
			String keysName,
			Serializer<Entry<K, V>> serializer) throws IOException {
		if ( map == null ) {
			oos.writeInt( 0 );
		}
		else {
			writeCollectionToStream( map.entrySet(), oos, keysName, serializer );
		}
	}

	private <E> void writeCollectionToStream(
			Collection<E> collection,
			ObjectOutputStream oos,
			String keysName,
			Serializer<E> serializer) throws IOException {
		if ( collection == null ) {
			oos.writeInt( 0 );
		}
		else {
			oos.writeInt( collection.size() );
			if ( LOG.isTraceEnabled() ) {
				LOG.trace( "Starting serialization of [" + collection.size() + "] " + keysName + " entries" );
			}
			for ( E entry : collection ) {
				serializer.serialize( entry, oos );
			}
		}
	}

	/**
	 * Used by the owning session to explicitly control deserialization of the persistence context.
	 *
	 * @param ois The stream from which the persistence context should be read
	 * @param session The owning session
	 *
	 * @return The deserialized StatefulPersistenceContext
	 *
	 * @throws IOException deserialization errors.
	 * @throws ClassNotFoundException deserialization errors.
	 */
	public static StatefulPersistenceContext deserialize(
			ObjectInputStream ois,
			SessionImplementor session) throws IOException, ClassNotFoundException {
		LOG.trace( "Deserializing persistence-context" );
		final StatefulPersistenceContext rtn = new StatefulPersistenceContext( session );
		SessionFactoryImplementor sfi = session.getFactory();

		// during deserialization, we need to reconnect all proxies and
		// collections to this session, as well as the EntityEntry and
		// CollectionEntry instances; these associations are transient
		// because serialization is used for different things.

		try {
			rtn.defaultReadOnly = ois.readBoolean();
			// todo : we can actually just determine this from the incoming EntityEntry-s
			rtn.hasNonReadOnlyEntities = ois.readBoolean();

			int count = ois.readInt();
			final boolean traceEnabled = LOG.isTraceEnabled();
			if ( traceEnabled ) {
				LOG.trace( "Starting deserialization of [" + count + "] entitiesByUniqueKey entries" );
			}
			if ( count != 0 ) {
				rtn.entitiesByUniqueKey = CollectionHelper.mapOfSize(Math.max(count, INIT_COLL_SIZE));
				for ( int i = 0; i < count; i++ ) {
					rtn.entitiesByUniqueKey.put( EntityUniqueKey.deserialize( ois, session ), ois.readObject() );
				}
			}

			count = ois.readInt();
			if ( traceEnabled ) {
				LOG.trace( "Starting deserialization of [" + count + "] entitySnapshotsByKey entries" );
			}
			rtn.entitySnapshotsByKey = CollectionHelper.mapOfSize(Math.max(count, INIT_COLL_SIZE));
			for ( int i = 0; i < count; i++ ) {
				rtn.entitySnapshotsByKey.put( EntityKey.deserialize( ois, sfi ), ois.readObject() );
			}

			rtn.entityEntryContext = EntityEntryContext.deserialize( ois, rtn );

			count = ois.readInt();
			if ( traceEnabled ) {
				LOG.trace( "Starting deserialization of [" + count + "] entitiesByKey entries" );
			}
			rtn.entitiesByKey = CollectionHelper.mapOfSize(Math.max(count, INIT_COLL_SIZE));
			for ( int i = 0; i < count; i++ ) {
				final EntityKey ek = EntityKey.deserialize( ois, sfi );
				final EntityPersister persister = sfi.getMappingMetamodel().getEntityDescriptor( (String) ois.readObject() );
				final Object entity = ois.readObject();
				final Object proxy = ois.readObject();
				final EntityHolderState state = (EntityHolderState) ois.readObject();
				final EntityHolderImpl holder = EntityHolderImpl.forEntity( ek, persister, entity );
				holder.state = state;
				if ( proxy != null ) {
					final LazyInitializer lazyInitializer = HibernateProxy.extractLazyInitializer( proxy );
					if ( lazyInitializer != null ) {
						lazyInitializer.setSession( session );
						holder.proxy = proxy;
					}
					else {
						// otherwise, the proxy was pruned during the serialization process
						if ( traceEnabled ) {
							LOG.trace( "Encountered pruned proxy" );
						}
					}
				}
				holder.setEntityEntry( rtn.entityEntryContext.getEntityEntry( entity ) );
				rtn.entitiesByKey.put( ek, holder );
			}

			count = ois.readInt();
			if ( traceEnabled ) {
				LOG.trace( "Starting deserialization of [" + count + "] collectionsByKey entries" );
			}
			rtn.collectionsByKey = CollectionHelper.mapOfSize(Math.max(count, INIT_COLL_SIZE));
			for ( int i = 0; i < count; i++ ) {
				rtn.collectionsByKey.put(
						CollectionKey.deserialize( ois, session ),
						(PersistentCollection<?>) ois.readObject()
				);
			}

			count = ois.readInt();
			if ( traceEnabled ) {
				LOG.trace( "Starting deserialization of [" + count + "] collectionEntries entries" );
			}
			for ( int i = 0; i < count; i++ ) {
				final PersistentCollection<?> pc = (PersistentCollection<?>) ois.readObject();
				final CollectionEntry ce = CollectionEntry.deserialize( ois, session );
				pc.setCurrentSession( session );
				rtn.getOrInitializeCollectionEntries().put( pc, ce );
			}

			count = ois.readInt();
			if ( traceEnabled ) {
				LOG.trace( "Starting deserialization of [" + count + "] arrayHolders entries" );
			}
			if ( count != 0 ) {
				rtn.arrayHolders = new IdentityHashMap<>(Math.max(count, INIT_COLL_SIZE));
				for ( int i = 0; i < count; i++ ) {
					rtn.arrayHolders.put( ois.readObject(), (PersistentCollection<?>) ois.readObject() );
				}
			}

			count = ois.readInt();
			if ( traceEnabled ) {
				LOG.trace( "Starting deserialization of [" + count + "] nullifiableEntityKey entries" );
			}
			rtn.nullifiableEntityKeys = new HashSet<>();
			for ( int i = 0; i < count; i++ ) {
				rtn.nullifiableEntityKeys.add( EntityKey.deserialize( ois, sfi ) );
			}
			count = ois.readInt();

			if ( LOG.isTraceEnabled() ) {
				LOG.trace( "Starting deserialization of [" + count + "] deletedUnloadedEntityKeys entries" );
			}
			rtn.deletedUnloadedEntityKeys = new HashSet<>();
			for ( int i = 0; i < count; i++ ) {
				rtn.deletedUnloadedEntityKeys.add( EntityKey.deserialize( ois, sfi ) );
			}

		}
		catch ( HibernateException he ) {
			throw new InvalidObjectException( he.getMessage() );
		}

		return rtn;
	}

	@Override
	public void addChildParent(Object child, Object parent) {
		if ( parentsByChild == null ) {
			parentsByChild = new IdentityHashMap<>( INIT_COLL_SIZE );
		}
		parentsByChild.put( child, parent );
	}

	@Override
	public void removeChildParent(Object child) {
		if ( parentsByChild != null ) {
			parentsByChild.remove( child );
		}
	}

	// INSERTED KEYS HANDLING ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private HashMap<String,HashSet<Object>> insertedKeysMap;

	@Override
	public void registerInsertedKey(EntityPersister persister, Object id) {
		// we only are worried about registering these if the persister defines caching
		if ( persister.canWriteToCache() ) {
			if ( insertedKeysMap == null ) {
				insertedKeysMap = new HashMap<>();
			}
			final String rootEntityName = persister.getRootEntityName();
			HashSet<Object> insertedEntityIds = insertedKeysMap.computeIfAbsent(
					rootEntityName,
					k -> new HashSet<>()
			);
			insertedEntityIds.add( id );
		}
	}

	@Override
	public boolean wasInsertedDuringTransaction(EntityPersister persister, Object id) {
		// again, we only really care if the entity is cached
		if ( persister.canWriteToCache() ) {
			if ( insertedKeysMap != null ) {
				final HashSet<Object> insertedEntityIds = insertedKeysMap.get( persister.getRootEntityName() );
				if ( insertedEntityIds != null ) {
					return insertedEntityIds.contains( id );
				}
			}
		}
		return false;
	}

	@Override
	public boolean containsNullifiableEntityKey(Supplier<EntityKey> sek) {
		return nullifiableEntityKeys != null
			&& nullifiableEntityKeys.size() != 0
			&& nullifiableEntityKeys.contains( sek.get() );
	}

	@Override
	public void registerNullifiableEntityKey(EntityKey key) {
		if ( nullifiableEntityKeys == null ) {
			nullifiableEntityKeys = new HashSet<>();
		}
		nullifiableEntityKeys.add( key );
	}

	@Override
	public boolean isNullifiableEntityKeysEmpty() {
		return nullifiableEntityKeys == null
			|| nullifiableEntityKeys.size() == 0;
	}

	@Override
	public boolean containsDeletedUnloadedEntityKey(EntityKey ek) {
		return deletedUnloadedEntityKeys != null
			&& deletedUnloadedEntityKeys.contains( ek );
	}

	@Override
	public void registerDeletedUnloadedEntityKey(EntityKey key) {
		if ( deletedUnloadedEntityKeys == null ) {
			deletedUnloadedEntityKeys = new HashSet<>();
		}
		deletedUnloadedEntityKeys.add( key );
	}

	@Override
	public void removeDeletedUnloadedEntityKey(EntityKey key) {
		assert deletedUnloadedEntityKeys != null;
		deletedUnloadedEntityKeys.remove( key );
	}

	@Override
	public boolean containsDeletedUnloadedEntityKeys() {
		return deletedUnloadedEntityKeys != null && !deletedUnloadedEntityKeys.isEmpty();
	}

	@Override
	public int getCollectionEntriesSize() {
		return collectionEntries == null ? 0 : collectionEntries.size();
	}

	@Override
	public CollectionEntry removeCollectionEntry(PersistentCollection<?> collection) {
		return collectionEntries == null ? null : collectionEntries.remove(collection);
	}

	@Override
	public void clearCollectionsByKey() {
		if ( collectionsByKey != null ) {
			// A valid alternative would be to set this to null, like we do on close.
			// The difference being that in this case we expect the collection will be
			// used again, so we bet that clear() might allow us to skip having to
			// re-allocate the collection.
			collectionsByKey.clear();
		}
	}

	@Override
	public PersistentCollection<?> addCollectionByKey(CollectionKey collectionKey, PersistentCollection<?> persistentCollection) {
		if ( collectionsByKey == null ) {
			collectionsByKey = CollectionHelper.mapOfSize( INIT_COLL_SIZE );
		}
		return collectionsByKey.put( collectionKey, persistentCollection );
	}

	@Override
	public void removeCollectionByKey(CollectionKey collectionKey) {
		if ( collectionsByKey != null ) {
			collectionsByKey.remove( collectionKey );
		}
	}

	private void cleanUpInsertedKeysAfterTransaction() {
		if ( insertedKeysMap != null ) {
			insertedKeysMap.clear();
		}
	}

	private static class EntityHolderImpl implements EntityHolder, Serializable {
		private final EntityKey entityKey;
		private final EntityPersister descriptor;
		Object entity;
		Object proxy;
		@Nullable EntityEntry entityEntry;
		EntityInitializer<?> entityInitializer;
		EntityHolderState state;

		private EntityHolderImpl(EntityKey entityKey, EntityPersister descriptor, Object entity, Object proxy) {
			assert entityKey != null && descriptor != null;
			this.entityKey = entityKey;
			this.descriptor = descriptor;
			this.entity = entity;
			this.proxy = proxy;
			this.state = EntityHolderState.UNINITIALIZED;
		}

		@Override
		public @Nullable EntityEntry getEntityEntry() {
			return entityEntry;
		}

		@Override
		public void setEntityEntry(@Nullable EntityEntry entityEntry) {
			this.entityEntry = entityEntry;
		}

		@Override
		public EntityKey getEntityKey() {
			return entityKey;
		}

		@Override
		public EntityPersister getDescriptor() {
			return descriptor;
		}

		@Override
		public Object getEntity() {
			return entity;
		}

		@Override
		public Object getProxy() {
			return proxy;
		}

		@Override
		public EntityInitializer<?> getEntityInitializer() {
			return entityInitializer;
		}

		@Override
		public void markAsReloaded(JdbcValuesSourceProcessingState processingState) {
			processingState.registerReloadedEntityHolder( this );
		}

		@Override
		public boolean isInitialized() {
			return state == EntityHolderState.INITIALIZED;
		}

		@Override
		public boolean isEventuallyInitialized() {
			return state == EntityHolderState.INITIALIZED || entityInitializer != null;
		}

		@Override
		public boolean isDetached() {
			return state == EntityHolderState.DETACHED;
		}

		public static EntityHolderImpl forProxy(EntityKey entityKey, EntityPersister descriptor, Object proxy) {
			return new EntityHolderImpl( entityKey, descriptor, null, proxy );
		}

		public static EntityHolderImpl forEntity(EntityKey entityKey, EntityPersister descriptor, Object entity) {
			return new EntityHolderImpl( entityKey, descriptor, entity, null );
		}
	}

	enum EntityHolderState {
		UNINITIALIZED,
		ENHANCED_PROXY,
		INITIALIZED,
		DETACHED
	}

	// NATURAL ID RESOLUTION HANDLING ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private NaturalIdResolutionsImpl naturalIdResolutions;


	@Override
	public NaturalIdResolutions getNaturalIdResolutions() {
		if ( naturalIdResolutions == null ) {
			naturalIdResolutions = new NaturalIdResolutionsImpl( this );
		}
		return naturalIdResolutions;
	}

	@Override
	public EntityHolder detachEntity(EntityKey key) {
		final EntityHolderImpl entityHolder = removeEntityHolder( key );
		if ( entityHolder != null ) {
			entityHolder.state = EntityHolderState.DETACHED;
		}
		return entityHolder;
	}

}
