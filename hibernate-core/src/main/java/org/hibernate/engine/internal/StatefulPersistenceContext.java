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
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;

import org.hibernate.AssertionFailure;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.NonUniqueObjectException;
import org.hibernate.PersistentObjectException;
import org.hibernate.TransientObjectException;
import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributeLoadingInterceptor;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.AssociationKey;
import org.hibernate.engine.spi.BatchFetchQueue;
import org.hibernate.engine.spi.CachedNaturalIdValueSource;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.EntityUniqueKey;
import org.hibernate.engine.spi.ManagedEntity;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.EventSource;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.collections.ConcurrentReferenceHashMap;
import org.hibernate.internal.util.collections.IdentityMap;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.NaturalIdDescriptor;
import org.hibernate.metamodel.model.domain.spi.NaturalIdDescriptor.NaturalIdAttributeInfo;
import org.hibernate.metamodel.model.domain.spi.NonIdPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.PersistentAttributeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.BagPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.SingularPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.StateArrayContributor;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.sql.results.spi.LoadContexts;

import org.jboss.logging.Logger;

/**
 * A <strong>stateful</strong> implementation of the {@link PersistenceContext} contract meaning that we maintain this
 * state throughout the life of the persistence context.
 * <p/>
 * IMPL NOTE: There is meant to be a one-to-one correspondence between a {@link org.hibernate.internal.SessionImpl}
 * and a PersistentContext.  Event listeners and other Session collaborators then use the PersistentContext to drive
 * their processing.
 *
 * @author Steve Ebersole
 */
public class StatefulPersistenceContext implements PersistenceContext {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			StatefulPersistenceContext.class.getName()
	);

	private static final boolean TRACE_ENABLED = LOG.isTraceEnabled();
	private static final int INIT_COLL_SIZE = 8;

	private SharedSessionContractImplementor session;

	// Loaded entity instances, by EntityKey
	private Map<EntityKey, Object> entitiesByKey;

	// Loaded entity instances, by EntityUniqueKey
	private Map<EntityUniqueKey, Object> entitiesByUniqueKey;

	private EntityEntryContext entityEntryContext;
//	private Map<Object,EntityEntry> entityEntries;

	// Entity proxies, by EntityKey
	private ConcurrentMap<EntityKey, Object> proxiesByKey;

	// Snapshots of current database state for entities
	// that have *not* been loaded
	private Map<EntityKey, Object> entitySnapshotsByKey;

	// Identity map of array holder ArrayHolder instances, by the array instance
	private Map<Object, PersistentCollection> arrayHolders;

	// Identity map of CollectionEntry instances, by the collection wrapper
	private IdentityMap<PersistentCollection, CollectionEntry> collectionEntries;

	// Collection wrappers, by the CollectionKey
	private Map<CollectionKey, PersistentCollection> collectionsByKey;

	// Set of EntityKeys of deleted objects
	private HashSet<EntityKey> nullifiableEntityKeys;

	// properties that we have tried to load, and not found in the database
	private HashSet<AssociationKey> nullAssociations;

	// A list of collection wrappers that were instantiating during result set
	// processing, that we will need to initialize at the end of the query
	private List<PersistentCollection> nonlazyCollections;

	// A container for collections we load up when the owning entity is not
	// yet loaded ... for now, this is purely transient!
	private Map<CollectionKey,PersistentCollection> unownedCollections;

	// Parent entities cache by their child for cascading
	// May be empty or not contains all relation
	private Map<Object,Object> parentsByChild;

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

		entitiesByKey = new HashMap<>( INIT_COLL_SIZE );
		entitiesByUniqueKey = new HashMap<>( INIT_COLL_SIZE );
		//noinspection unchecked
		proxiesByKey = new ConcurrentReferenceHashMap<>(
				INIT_COLL_SIZE,
				.75f,
				1,
				ConcurrentReferenceHashMap.ReferenceType.STRONG,
				ConcurrentReferenceHashMap.ReferenceType.WEAK,
				null
		);
		entitySnapshotsByKey = new HashMap<>( INIT_COLL_SIZE );

		entityEntryContext = new EntityEntryContext( this );
//		entityEntries = IdentityMap.instantiateSequenced( INIT_COLL_SIZE );
		collectionEntries = IdentityMap.instantiateSequenced( INIT_COLL_SIZE );
		parentsByChild = new IdentityHashMap<>( INIT_COLL_SIZE );

		collectionsByKey = new HashMap<>( INIT_COLL_SIZE );
		arrayHolders = new IdentityHashMap<>( INIT_COLL_SIZE );

		nullifiableEntityKeys = new HashSet<>();

		initTransientState();
	}

	private void initTransientState() {
		nullAssociations = new HashSet<>( INIT_COLL_SIZE );
		nonlazyCollections = new ArrayList<>( INIT_COLL_SIZE );
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
	public void addUnownedCollection(CollectionKey key, PersistentCollection collection) {
		if (unownedCollections==null) {
			unownedCollections = new HashMap<>( INIT_COLL_SIZE );
		}
		unownedCollections.put( key, collection );
	}

	@Override
	public PersistentCollection useUnownedCollection(CollectionKey key) {
		return ( unownedCollections == null ) ? null : unownedCollections.remove( key );
	}

	@Override
	public BatchFetchQueue getBatchFetchQueue() {
		if (batchFetchQueue==null) {
			batchFetchQueue = new BatchFetchQueue(this);
		}
		return batchFetchQueue;
	}

	@Override
	public void clear() {
		for ( Object o : proxiesByKey.values() ) {
			if ( o == null ) {
				//entry may be GCd
				continue;
			}
			((HibernateProxy) o).getHibernateLazyInitializer().unsetSession();
		}

		for ( Entry<Object, EntityEntry> objectEntityEntryEntry : entityEntryContext.reentrantSafeEntityEntries() ) {
			// todo : I dont think this need be reentrant safe
			if ( objectEntityEntryEntry.getKey() instanceof PersistentAttributeInterceptable ) {
				final PersistentAttributeInterceptor interceptor = ( (PersistentAttributeInterceptable) objectEntityEntryEntry.getKey() ).$$_hibernate_getInterceptor();
				if ( interceptor instanceof LazyAttributeLoadingInterceptor ) {
					( (LazyAttributeLoadingInterceptor) interceptor ).unsetSession();
				}
			}
		}

		for ( Map.Entry<PersistentCollection, CollectionEntry> aCollectionEntryArray : IdentityMap.concurrentEntries( collectionEntries ) ) {
			aCollectionEntryArray.getKey().unsetSession( getSession() );
		}

		arrayHolders.clear();
		entitiesByKey.clear();
		entitiesByUniqueKey.clear();
		entityEntryContext.clear();
//		entityEntries.clear();
		parentsByChild.clear();
		entitySnapshotsByKey.clear();
		collectionsByKey.clear();
		collectionEntries.clear();
		if ( unownedCollections != null ) {
			unownedCollections.clear();
		}
		proxiesByKey.clear();
		nullifiableEntityKeys.clear();
		if ( batchFetchQueue != null ) {
			batchFetchQueue.clear();
		}
		// defaultReadOnly is unaffected by clear()
		hasNonReadOnlyEntities = false;
		if ( loadContexts != null ) {
			loadContexts.cleanup();
		}
		naturalIdXrefDelegate.clear();
	}

	@Override
	public boolean isDefaultReadOnly() {
		return defaultReadOnly;
	}

	@Override
	public void setDefaultReadOnly(boolean defaultReadOnly) {
		this.defaultReadOnly = defaultReadOnly;
	}

	@Override
	public boolean hasNonReadOnlyEntities() {
		return hasNonReadOnlyEntities;
	}

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
		entityEntryContext.downgradeLocks();
//		// Downgrade locks
//		for ( EntityEntry o : entityEntries.values() ) {
//			o.setLockMode( LockMode.NONE );
//		}
	}

	/**
	 * Get the current state of the entity as known to the underlying
	 * database, or null if there is no corresponding row
	 * <p/>
	 * {@inheritDoc}
	 */
	@Override
	public Object[] getDatabaseSnapshot(Object id, EntityTypeDescriptor descriptor) throws HibernateException {
		final EntityKey key = session.generateEntityKey( id, descriptor );
		final Object cached = entitySnapshotsByKey.get( key );
		if ( cached != null ) {
			return cached == NO_ROW ? null : (Object[]) cached;
		}
		else {
			final Object[] snapshot = descriptor.getDatabaseSnapshot( id, session );
			entitySnapshotsByKey.put( key, snapshot == null ? NO_ROW : snapshot );
			return snapshot;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object[] getNaturalIdSnapshot(Object id, EntityTypeDescriptor descriptor) throws HibernateException {
		final NaturalIdDescriptor naturalIdDescriptor = descriptor.getHierarchy().getNaturalIdDescriptor();

		if ( naturalIdDescriptor == null ) {
			return null;
		}

		descriptor = locateProperDescriptor( descriptor );

		// let's first see if it is part of the natural id cache...
		final Object[] cachedValue = naturalIdHelper.findCachedNaturalId( descriptor, id );
		if ( cachedValue != null ) {
			return cachedValue;
		}

		// check to see if the natural id is mutable/immutable
		if ( !naturalIdDescriptor.isMutable() ) {
			// an immutable natural-id is not retrieved during a normal database-snapshot operation...
			final Object[] dbValue = descriptor.getHierarchy().getNaturalIdDescriptor().resolveSnapshot( id, session );
			naturalIdHelper.cacheNaturalIdCrossReferenceFromLoad(
					descriptor,
					id,
					dbValue
			);
			return dbValue;
		}
		else {
			// for a mutable natural there is a likelihood that the the information will already be
			// snapshot-cached.
			final Object[] entitySnapshot = getDatabaseSnapshot( id, descriptor );
			if ( entitySnapshot == NO_ROW || entitySnapshot == null ) {
				return null;
			}

			final Object[] naturalIdSnapshotSubSet = new Object[ naturalIdDescriptor.getAttributeInfos().size() ];

			int i = 0;

			for ( PersistentAttributeDescriptor attribute : ( (EntityTypeDescriptor<?>) descriptor ).getPersistentAttributes() ) {
				if ( !SingularPersistentAttribute.class.isInstance( attribute ) ) {
					continue;
				}

				final SingularPersistentAttribute singularAttribute = (SingularPersistentAttribute) attribute;
				// Must be a better way to indicate this.  Maybe an extended `PersistentAttribute.Nature`
				//		for NATURAL_ID in addition to NORMAL, ID, VERSION.  IN terms of JPA's ENUM
				//		we'd just translate NATURAL_ID as its NORMAL.
				if ( naturalIdDescriptor.getAttributeInfos().contains( singularAttribute ) ) {
					assert StateArrayContributor.class.isInstance( singularAttribute );
					final StateArrayContributor contributor = (StateArrayContributor) singularAttribute;
					naturalIdSnapshotSubSet[i++] = entitySnapshot[contributor.getStateArrayPosition()];
				}

				i++;
			}

			naturalIdHelper.cacheNaturalIdCrossReferenceFromLoad(
					descriptor,
					id,
					naturalIdSnapshotSubSet
			);

			return naturalIdSnapshotSubSet;
		}
	}

	private EntityTypeDescriptor locateProperDescriptor(EntityTypeDescriptor descriptor) {
		// tod0 (6.0) : avoid the lookup if the passed descriptor is the root.
		return session.getFactory().getMetamodel().findEntityDescriptor( descriptor.getHierarchy().getRootEntityType().getEntityName() );
	}

	@Override
	public Object[] getCachedDatabaseSnapshot(EntityKey key) {
		final Object snapshot = entitySnapshotsByKey.get( key );
		if ( snapshot == NO_ROW ) {
			throw new IllegalStateException(
					"persistence context reported no row snapshot for "
							+ MessageHelper.infoString( key.getEntityName(), key.getIdentifier() )
			);
		}
		return (Object[]) snapshot;
	}

	@Override
	public void addEntity(EntityKey key, Object entity) {
		entitiesByKey.put( key, entity );
		if( batchFetchQueue != null ) {
			getBatchFetchQueue().removeBatchLoadableEntityKey(key);
		}
	}

	@Override
	public Object getEntity(EntityKey key) {
		return entitiesByKey.get( key );
	}

	@Override
	public boolean containsEntity(EntityKey key) {
		return entitiesByKey.containsKey( key );
	}

	@Override
	public Object removeEntity(EntityKey key) {
		final Object entity = entitiesByKey.remove( key );
		final Iterator itr = entitiesByUniqueKey.values().iterator();
		while ( itr.hasNext() ) {
			if ( itr.next() == entity ) {
				itr.remove();
			}
		}
		// Clear all parent cache
		parentsByChild.clear();
		entitySnapshotsByKey.remove( key );
		nullifiableEntityKeys.remove( key );
		if( batchFetchQueue != null ) {
			getBatchFetchQueue().removeBatchLoadableEntityKey(key);
			getBatchFetchQueue().removeSubselect(key);
		}
		return entity;
	}

	@Override
	public Object getEntity(EntityUniqueKey euk) {
		return entitiesByUniqueKey.get( euk );
	}

	@Override
	public void addEntity(EntityUniqueKey euk, Object entity) {
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
	public CollectionEntry getCollectionEntry(PersistentCollection coll) {
		return collectionEntries.get( coll );
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
			final EntityTypeDescriptor descriptor,
			final boolean disableVersionIncrement) {
		addEntity( entityKey, entity );
		return addEntry(
				entity,
				status,
				loadedState,
				null,
				entityKey.getIdentifier(),
				version,
				lockMode,
				existsInDatabase,
				descriptor,
				disableVersionIncrement
		);
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
			final EntityTypeDescriptor descriptor,
			final boolean disableVersionIncrement) {
		final EntityEntry e;

		/*
			IMPORTANT!!!

			The following instanceof checks and castings are intentional.

			DO NOT REFACTOR to make calls through the EntityEntryFactory interface, which would result
			in polymorphic call sites which will severely impact performance.

			When a virtual method is called via an interface the JVM needs to resolve which concrete
			implementation to call.  This takes CPU cycles and is a performance penalty.  It also prevents method
			in-ling which further degrades performance.  Casting to an implementation and making a direct method call
			removes the virtual call, and allows the methods to be in-lined.  In this critical code path, it has a very
			large impact on performance to make virtual method calls.
		*/
		if ( descriptor.getEntityEntryFactory() instanceof MutableEntityEntryFactory) {
			//noinspection RedundantCast
			e = ( (MutableEntityEntryFactory) descriptor.getEntityEntryFactory() ).createEntityEntry(
					status,
					loadedState,
					rowId,
					id,
					version,
					lockMode,
					existsInDatabase,
					descriptor,
					disableVersionIncrement,
					this
			);
		}
		else {
			//noinspection RedundantCast
			e = ( (ImmutableEntityEntryFactory) descriptor.getEntityEntryFactory() ).createEntityEntry(
					status,
					loadedState,
					rowId,
					id,
					version,
					lockMode,
					existsInDatabase,
					descriptor,
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

		((ManagedEntity)entity).$$_hibernate_getEntityEntry().setStatus( status );
		entityEntryContext.addEntityEntry( entity, ((ManagedEntity)entity).$$_hibernate_getEntityEntry() );

		setHasNonReadOnlyEnties( status );
		return ((ManagedEntity)entity).$$_hibernate_getEntityEntry();
	}

	@Override
	public boolean containsCollection(PersistentCollection collection) {
		return collectionEntries.containsKey( collection );
	}

	@Override
	public boolean containsProxy(Object entity) {
		return proxiesByKey.containsValue( entity );
	}

	@Override
	public boolean reassociateIfUninitializedProxy(Object value) throws MappingException {
		if ( !Hibernate.isInitialized( value ) ) {
			final HibernateProxy proxy = (HibernateProxy) value;
			final LazyInitializer li = proxy.getHibernateLazyInitializer();
			reassociateProxy( li, proxy );
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public void reassociateProxy(Object value, Object id) throws MappingException {
		if ( value instanceof HibernateProxy ) {
			LOG.debugf( "Setting proxy identifier: %s", id );
			final HibernateProxy proxy = (HibernateProxy) value;
			final LazyInitializer li = proxy.getHibernateLazyInitializer();
			li.setIdentifier( id );
			reassociateProxy( li, proxy );
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
			final EntityTypeDescriptor entityDescriptor = session.getFactory().getMetamodel().findEntityDescriptor( li.getEntityName() );
			final EntityKey key = session.generateEntityKey( li.getIdentifier(), entityDescriptor );
		  	// any earlier proxy takes precedence
			proxiesByKey.putIfAbsent( key, proxy );
			proxy.getHibernateLazyInitializer().setSession( session );
		}
	}

	@Override
	public Object unproxy(Object maybeProxy) throws HibernateException {
		if ( maybeProxy instanceof HibernateProxy ) {
			final HibernateProxy proxy = (HibernateProxy) maybeProxy;
			final LazyInitializer li = proxy.getHibernateLazyInitializer();
			if ( li.isUninitialized() ) {
				throw new PersistentObjectException(
						"object was an uninitialized proxy for " + li.getEntityName()
				);
			}
			//unwrap the object and return
			return li.getImplementation();
		}
		else {
			return maybeProxy;
		}
	}

	@Override
	public Object unproxyAndReassociate(Object maybeProxy) throws HibernateException {
		if ( maybeProxy instanceof HibernateProxy ) {
			final HibernateProxy proxy = (HibernateProxy) maybeProxy;
			final LazyInitializer li = proxy.getHibernateLazyInitializer();
			reassociateProxy( li, proxy );
			//initialize + unwrap the object and return it
			return li.getImplementation();
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
	@SuppressWarnings("unchecked")
	public Object narrowProxy(Object proxy, EntityTypeDescriptor descriptor, EntityKey key, Object object)
			throws HibernateException {

		final Class concreteProxyClass = descriptor.getConcreteProxyClass();
		final boolean alreadyNarrow = concreteProxyClass.isInstance( proxy );

		if ( !alreadyNarrow ) {
			LOG.narrowingProxy( concreteProxyClass );

			// If an impl is passed, there is really no point in creating a proxy.
			// It would just be extra processing.  Just return the impl
			if ( object != null ) {
				proxiesByKey.remove( key );
				return object;
			}

			// Similarly, if the original HibernateProxy is initialized, there
			// is again no point in creating a proxy.  Just return the impl
			final HibernateProxy originalHibernateProxy = (HibernateProxy) proxy;
			if ( !originalHibernateProxy.getHibernateLazyInitializer().isUninitialized() ) {
				final Object impl = originalHibernateProxy.getHibernateLazyInitializer().getImplementation();
				// can we return it?
				if ( concreteProxyClass.isInstance( impl ) ) {
					proxiesByKey.remove( key );
					return impl;
				}
			}


			// Otherwise, create the narrowed proxy
			final HibernateProxy narrowedProxy = (HibernateProxy) descriptor.createProxy( key.getIdentifier(), session );

			// set the read-only/modifiable mode in the new proxy to what it was in the original proxy
			final boolean readOnlyOrig = originalHibernateProxy.getHibernateLazyInitializer().isReadOnly();
			narrowedProxy.getHibernateLazyInitializer().setReadOnly( readOnlyOrig );

			return narrowedProxy;
		}
		else {

			if ( object != null ) {
				final LazyInitializer li = ( (HibernateProxy) proxy ).getHibernateLazyInitializer();
				li.setImplementation( object );
			}
			return proxy;
		}
	}

	@Override
	public Object proxyFor(EntityTypeDescriptor descriptor, EntityKey key, Object impl) throws HibernateException {
		if ( !descriptor.hasProxy() ) {
			return impl;
		}
		final Object proxy = proxiesByKey.get( key );
		return ( proxy != null ) ? narrowProxy( proxy, descriptor, key, impl ) : impl;
	}

	@Override
	public Object proxyFor(Object impl) throws HibernateException {
		final EntityEntry e = getEntry( impl );
		if ( e == null ) {
			return impl;
		}
		return proxyFor( e.getDescriptor(), e.getEntityKey(), impl );
	}

	@Override
	public Object getCollectionOwner(Object key, PersistentCollectionDescriptor descriptor) throws MappingException {
		// todo (6.0) : assumes collection is defined on entity, not composite
		// todo (6.0) : also assumes PK FK mapping
		return getEntity( session.generateEntityKey( key, (EntityTypeDescriptor) descriptor.getContainer() ) );

//		// todo : we really just need to add a split in the notions of:
//		//		1) collection key
//		//		2) collection owner key
//		// these 2 are not always the same.  Same is true in the case of ToOne associations with property-ref...
//		final EntityDescriptor ownerDescriptor = descriptor.findEntityOwnerDescriptor();
//		if ( ownerDescriptor.getIdentifierType().getJavaTypeDescriptor().getJavaType().isInstance( key ) ) {
//			return getEntity( session.generateEntityKey( key, descriptor.findEntityOwnerDescriptor() ) );
//		}
//
//		// we have a property-ref type mapping for the collection key.  But that could show up a few ways here...
//		//
//		//		1) The incoming key could be the entity itself...
//		if ( ownerDescriptor.isInstance( key ) ) {
//			final Serializable owenerId = ownerDescriptor.getIdentifier( key, session );
//			if ( owenerId == null ) {
//				return null;
//			}
//			return getEntity( session.generateEntityKey( owenerId, ownerDescriptor ) );
//		}
//
//		final CollectionType collectionType = descriptor.getOrmType();
//
//		//		2) The incoming key is most likely the collection key which we need to resolve to the owner key
//		//			find the corresponding owner instance
//		//			a) try by EntityUniqueKey
//		if ( collectionType.getLHSPropertyName() != null ) {
//			final Object owner = getEntity(
//					new EntityUniqueKey(
//							ownerDescriptor.getEntityName(),
//							collectionType.getLHSPropertyName(),
//							key,
//							descriptor.getKeyType(),
//							ownerDescriptor.getRepresentationStrategy().getMode(),
//							session.getFactory()
//					)
//			);
//			if ( owner != null ) {
//				return owner;
//			}
//
//			//		b) try by EntityKey, which means we need to resolve owner-key -> collection-key
//			//			IMPL NOTE : yes if we get here this impl is very non-performant, but PersistenceContext
//			//					was never designed to handle this case; adding that capability for real means splitting
//			//					the notions of:
//			//						1) collection key
//			//						2) collection owner key
//			// 					these 2 are not always the same (same is true in the case of ToOne associations with
//			// 					property-ref).  That would require changes to (at least) CollectionEntry and quite
//			//					probably changes to how the sql for collection initializers are generated
//			//
//			//			We could also possibly see if the referenced property is a natural id since we already have caching
//			//			in place of natural id snapshots.  BUt really its better to just do it the right way ^^ if we start
//			// 			going that route
//			final Serializable ownerId = ownerDescriptor.getIdByUniqueKey( key, collectionType.getLHSPropertyName(), session );
//			return getEntity( session.generateEntityKey( ownerId, ownerDescriptor ) );
//		}
//
//		// as a last resort this is what the old code did...
//		return getEntity( session.generateEntityKey( key, descriptor.findEntityOwnerDescriptor() ) );
	}

	@Override
	public Object getLoadedCollectionOwnerOrNull(PersistentCollection collection) {
		final CollectionEntry ce = getCollectionEntry( collection );
		if ( ce.getLoadedCollectionDescriptor() == null ) {
			return null;
		}

		Object loadedOwner = null;
		// TODO: an alternative is to check if the owner has changed; if it hasn't then
		// return collection.getOwner()
		final Object entityId = getLoadedCollectionOwnerIdOrNull( ce );
		if ( entityId != null ) {
			loadedOwner = getCollectionOwner( entityId, ce.getLoadedCollectionDescriptor() );
		}
		return loadedOwner;
	}

	@Override
	public Object getLoadedCollectionOwnerIdOrNull(PersistentCollection collection) {
		return getLoadedCollectionOwnerIdOrNull( getCollectionEntry( collection ) );
	}

	/**
	 * Get the ID for the entity that owned this persistent collection when it was loaded
	 *
	 * @param ce The collection entry
	 * @return the owner ID if available from the collection's loaded key; otherwise, returns null
	 */
	private Object getLoadedCollectionOwnerIdOrNull(CollectionEntry ce) {
		if ( ce == null || ce.getLoadedKey() == null || ce.getLoadedCollectionDescriptor() == null ) {
			return null;
		}

		final Object loadedKey = ce.getLoadedKey();

		// todo (6.0) : need to account for non-PK Fks (aka, "property-ref")
		return loadedKey;
	}

	@Override
	public void addUninitializedCollection(PersistentCollectionDescriptor descriptor, PersistentCollection collection, Object collectionKey) {
		final CollectionEntry ce = new CollectionEntry( collection, descriptor, collectionKey, flushing );
		addCollection( collection, ce, collectionKey );
		if ( descriptor.getBatchSize() > 1 ) {
			getBatchFetchQueue().addBatchLoadableCollection( collection, ce );
		}
	}

	@Override
	public void addUninitializedDetachedCollection(PersistentCollectionDescriptor descriptor, PersistentCollection collection) {
		final CollectionEntry ce = new CollectionEntry( descriptor, collection.getKey() );
		addCollection( collection, ce, collection.getKey() );
		if ( descriptor.getBatchSize() > 1 ) {
			getBatchFetchQueue().addBatchLoadableCollection( collection, ce );
		}
	}

	@Override
	public void addNewCollection(PersistentCollectionDescriptor descriptor, PersistentCollection collection)
			throws HibernateException {
		addCollection( collection, descriptor );
	}

	/**
	 * Add a collection to the cache, with a given collection entry.
	 *  @param coll The collection for which we are adding an entry.
	 * @param entry The entry representing the collection.
	 * @param key The key of the collection's entry.
	 */
	private void addCollection(PersistentCollection coll, CollectionEntry entry, Object key) {
		collectionEntries.put( coll, entry );
		final CollectionKey collectionKey = new CollectionKey( entry.getLoadedCollectionDescriptor(), key );
		final PersistentCollection old = collectionsByKey.put( collectionKey, coll );
		if ( old != null ) {
			if ( old == coll ) {
				throw new AssertionFailure( "bug adding collection twice" );
			}
			// or should it actually throw an exception?
			old.unsetSession( session );
			collectionEntries.remove( old );
			// watch out for a case where old is still referenced
			// somewhere in the object graph! (which is a user error)
		}
	}

	/**
	 * Add a collection to the cache, creating a new collection entry for it
	 *
	 * @param collection The collection for which we are adding an entry.
	 * @param descriptor The collection descriptor
	 */
	private void addCollection(PersistentCollection collection, PersistentCollectionDescriptor descriptor) {
		final CollectionEntry ce = new CollectionEntry( descriptor, collection );
		collectionEntries.put( collection, ce );
	}

	@Override
	public void addInitializedDetachedCollection(PersistentCollectionDescriptor descriptor, PersistentCollection collection)
			throws HibernateException {
		if ( collection.isUnreferenced() ) {
			//treat it just like a new collection
			addCollection( collection, descriptor );
		}
		else {
			final CollectionEntry ce = new CollectionEntry( collection, session.getFactory() );
			addCollection( collection, ce, collection.getKey() );
		}
	}

	@Override
	public CollectionEntry addInitializedCollection(PersistentCollectionDescriptor descriptor, PersistentCollection collection, Object id)
			throws HibernateException {
		final CollectionEntry ce = new CollectionEntry( collection, descriptor, id, flushing );
		ce.postInitialize( collection );
		addCollection( collection, ce, id );
		return ce;
	}

	@Override
	public PersistentCollection getCollection(CollectionKey collectionKey) {
		return collectionsByKey.get( collectionKey );
	}

	@Override
	public void addNonLazyCollection(PersistentCollection collection) {
		nonlazyCollections.add( collection );
	}

	@Override
	public void initializeNonLazyCollections() throws HibernateException {
		if ( loadCounter == 0 ) {
			if ( TRACE_ENABLED ) {
				LOG.trace( "Initializing non-lazy collections" );
			}

			//do this work only at the very highest level of the load
			//don't let this method be called recursively
			loadCounter++;
			try {
				int size;
				while ( ( size = nonlazyCollections.size() ) > 0 ) {
					//note that each iteration of the loop may add new elements
					nonlazyCollections.remove( size - 1 ).forceInitialization();
				}
			}
			finally {
				loadCounter--;
				clearNullProperties();
			}
		}
	}

	@Override
	public PersistentCollection getCollectionHolder(Object array) {
		return arrayHolders.get( array );
	}

	@Override
	public void addCollectionHolder(PersistentCollection holder) {
		//TODO:refactor + make this method private
		arrayHolders.put( holder.getValue(), holder );
	}

	@Override
	public PersistentCollection removeCollectionHolder(Object array) {
		return arrayHolders.remove( array );
	}

	@Override
	public Serializable getSnapshot(PersistentCollection coll) {
		return getCollectionEntry( coll ).getSnapshot();
	}

	@Override
	public CollectionEntry getCollectionEntryOrNull(Object collection) {
		PersistentCollection coll;
		if ( collection instanceof PersistentCollection ) {
			coll = (PersistentCollection) collection;
			//if (collection==null) throw new TransientObjectException("Collection was not yet persistent");
		}
		else {
			coll = getCollectionHolder( collection );
			if ( coll == null ) {
				//it might be an unwrapped collection reference!
				//try to find a wrapper (slowish)
				final Iterator<PersistentCollection> wrappers = collectionEntries.keyIterator();
				while ( wrappers.hasNext() ) {
					final PersistentCollection pc = wrappers.next();
					if ( pc.isWrapper( collection ) ) {
						coll = pc;
						break;
					}
				}
			}
		}

		return (coll == null) ? null : getCollectionEntry( coll );
	}

	@Override
	public Object getProxy(EntityKey key) {
		return proxiesByKey.get( key );
	}

	@Override
	public void addProxy(EntityKey key, Object proxy) {
		proxiesByKey.put( key, proxy );
	}

	@Override
	public Object removeProxy(EntityKey key) {
		if ( batchFetchQueue != null ) {
			batchFetchQueue.removeBatchLoadableEntityKey( key );
			batchFetchQueue.removeSubselect( key );
		}
		return proxiesByKey.remove( key );
	}

	@Override
	public HashSet getNullifiableEntityKeys() {
		return nullifiableEntityKeys;
	}

	@Override
	public Map getEntitiesByKey() {
		return entitiesByKey;
	}

	public Map getProxiesByKey() {
		return proxiesByKey;
	}

	@Override
	public int getNumberOfManagedEntities() {
		return entityEntryContext.getNumberOfManagedEntities();
	}

	@Override
	public Map getEntityEntries() {
		return null;
	}

	@Override
	public Map getCollectionEntries() {
		return collectionEntries;
	}

	@Override
	public Map getCollectionsByKey() {
		return collectionsByKey;
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
			getNaturalIdHelper().cleanupFromSynchronizations();
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
		return "PersistenceContext[entityKeys=" + entitiesByKey.keySet()
				+ ",collectionKeys=" + collectionsByKey.keySet() + "]";
	}

	@Override
	public Entry<Object,EntityEntry>[] reentrantSafeEntityEntries() {
		return entityEntryContext.reentrantSafeEntityEntries();
	}

	@Override
	public Object getOwnerId(String entityName, String propertyName, Object childEntity, Map mergeMap) {
		final String collectionRole = entityName + '.' + propertyName;
		final EntityTypeDescriptor entityDescriptor = session.getFactory().getMetamodel().findEntityDescriptor( entityName );
		final PersistentCollectionDescriptor collectionDescriptor = session.getFactory().getMetamodel().findCollectionDescriptor( collectionRole );

	    // try cache lookup first
		final Object parent = parentsByChild.get( childEntity );
		if ( parent != null ) {
			final EntityEntry entityEntry = entityEntryContext.getEntityEntry( parent );
			//there maybe more than one parent, filter by type
			if ( entityDescriptor.isSubclassTypeName( entityEntry.getEntityName() )
					&& isFoundInParent( propertyName, childEntity, entityDescriptor, collectionDescriptor, parent ) ) {
				return getEntry( parent ).getId();
			}
			else {
				// remove wrong entry
				parentsByChild.remove( childEntity );
			}
		}

		//not found in case, proceed
		// iterate all the entities currently associated with the persistence context.
		for ( Entry<Object,EntityEntry> me : reentrantSafeEntityEntries() ) {
			final EntityEntry entityEntry = me.getValue();
			// does this entity entry pertain to the entity descriptor in which we are interested (owner)?
			if ( entityDescriptor.isSubclassTypeName( entityEntry.getEntityName() ) ) {
				final Object entityEntryInstance = me.getKey();

				//check if the managed object is the parent
				boolean found = isFoundInParent(
						propertyName,
						childEntity,
						entityDescriptor,
						collectionDescriptor,
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
								entityDescriptor,
								collectionDescriptor,
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
				final Entry mergeMapEntry = (Entry) o;
				if ( mergeMapEntry.getKey() instanceof HibernateProxy ) {
					final HibernateProxy proxy = (HibernateProxy) mergeMapEntry.getKey();
					if ( entityDescriptor.isSubclassTypeName( proxy.getHibernateLazyInitializer().getEntityName() ) ) {
						boolean found = isFoundInParent(
								propertyName,
								childEntity,
								entityDescriptor,
								collectionDescriptor,
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
									entityDescriptor,
									collectionDescriptor,
									mergeMap.get( proxy )
							);
							LOG.debugf(
									"Detached proxy being merged has a collection that [%s] the detached child being merged..",
									(found ? "contains" : "does not contain")
							);
						}
						if ( found ) {
							return proxy.getHibernateLazyInitializer().getIdentifier();
						}
					}
				}
			}
		}

		return null;
	}

	private boolean isFoundInParent(
			String property,
			Object childEntity,
			EntityTypeDescriptor entityDescriptor,
			PersistentCollectionDescriptor collectionDescriptor,
			Object potentialParent) {
		final NonIdPersistentAttribute attribute = entityDescriptor.findPersistentAttribute( property );
		assert attribute instanceof BagPersistentAttribute;

		final Object collection = attribute.getPropertyAccess().getGetter().get( potentialParent );
		return collection != null
				&& Hibernate.isInitialized( collection )
				&& collectionDescriptor.contains( collection, childEntity );
	}

	@Override
	public Object getIndexInOwner(String entity, String property, Object childEntity, Map mergeMap) {
		final EntityTypeDescriptor entityDescriptor = session.getFactory().getMetamodel().findEntityDescriptor( entity );
		final PersistentCollectionDescriptor collectionDescriptor = session.getFactory().getMetamodel().findCollectionDescriptor( entity + '.' + property );

	    // try cache lookup first
		final Object parent = parentsByChild.get( childEntity );
		if ( parent != null ) {
			final EntityEntry entityEntry = entityEntryContext.getEntityEntry( parent );
			//there maybe more than one parent, filter by type
			if ( entityDescriptor.isSubclassTypeName( entityEntry.getEntityName() ) ) {
				Object index = getIndexInParent( property, childEntity, entityDescriptor, collectionDescriptor, parent );

				if (index==null && mergeMap!=null) {
					final Object unMergedInstance = mergeMap.get( parent );
					final Object unMergedChild = mergeMap.get( childEntity );
					if ( unMergedInstance != null && unMergedChild != null ) {
						index = getIndexInParent( property, unMergedChild, entityDescriptor, collectionDescriptor, unMergedInstance );
						LOG.debugf(
								"A detached object being merged (corresponding to a parent in parentsByChild) has an indexed collection that [%s] the detached child being merged. ",
								( index != null ? "contains" : "does not contain" )
						);
					}
				}
				if ( index != null ) {
					return index;
				}
			}
			else {
				// remove wrong entry
				parentsByChild.remove( childEntity );
			}
		}

		//Not found in cache, proceed
		for ( Entry<Object, EntityEntry> me : reentrantSafeEntityEntries() ) {
			final EntityEntry ee = me.getValue();
			if ( entityDescriptor.isSubclassTypeName( ee.getEntityName() ) ) {
				final Object instance = me.getKey();

				Object index = getIndexInParent( property, childEntity, entityDescriptor, collectionDescriptor, instance );
				if ( index==null && mergeMap!=null ) {
					final Object unMergedInstance = mergeMap.get( instance );
					final Object unMergedChild = mergeMap.get( childEntity );
					if ( unMergedInstance != null && unMergedChild!=null ) {
						index = getIndexInParent( property, unMergedChild, entityDescriptor, collectionDescriptor, unMergedInstance );
						LOG.debugf(
								"A detached object being merged (corresponding to a managed entity) has an indexed collection that [%s] the detached child being merged. ",
								(index != null ? "contains" : "does not contain" )
						);
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
			EntityTypeDescriptor entityDescriptor,
			PersistentCollectionDescriptor collectionDescriptor,
			Object potentialParent){
		final Object collection = entityDescriptor.findPersistentAttribute( property )
				.getPropertyAccess()
				.getGetter()
				.get( potentialParent );
		if ( collection != null && Hibernate.isInitialized( collection ) ) {
			return collectionDescriptor.indexOf( collection, childEntity );
		}
		else {
			return null;
		}
	}

	@Override
	public void addNullProperty(EntityKey ownerKey, String propertyName) {
		nullAssociations.add( new AssociationKey( ownerKey, propertyName ) );
	}

	@Override
	public boolean isPropertyNull(EntityKey ownerKey, String propertyName) {
		return nullAssociations.contains( new AssociationKey( ownerKey, propertyName ) );
	}

	private void clearNullProperties() {
		nullAssociations.clear();
	}

	@Override
	public boolean isReadOnly(Object entityOrProxy) {
		if ( entityOrProxy == null ) {
			throw new AssertionFailure( "object must be non-null." );
		}
		boolean isReadOnly;
		if ( entityOrProxy instanceof HibernateProxy ) {
			isReadOnly = ( (HibernateProxy) entityOrProxy ).getHibernateLazyInitializer().isReadOnly();
		}
		else {
			final EntityEntry ee =  getEntry( entityOrProxy );
			if ( ee == null ) {
				throw new TransientObjectException("Instance was not associated with this persistence context" );
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
		if ( object instanceof HibernateProxy ) {
			final HibernateProxy proxy = (HibernateProxy) object;
			setProxyReadOnly( proxy, readOnly );
			if ( Hibernate.isInitialized( proxy ) ) {
				setEntityReadOnly(
						proxy.getHibernateLazyInitializer().getImplementation(),
						readOnly
				);
			}
		}
		else {
			setEntityReadOnly( object, readOnly );
			// PersistenceContext.proxyFor( entity ) returns entity if there is no proxy for that entity
			// so need to check the return value to be sure it is really a proxy
			final Object maybeProxy = getSession().getPersistenceContext().proxyFor( object );
			if ( maybeProxy instanceof HibernateProxy ) {
				setProxyReadOnly( (HibernateProxy) maybeProxy, readOnly );
			}
		}
	}

	private void setProxyReadOnly(HibernateProxy proxy, boolean readOnly) {
		if ( proxy.getHibernateLazyInitializer().getSession() != getSession() ) {
			throw new AssertionFailure(
					"Attempt to set a proxy to read-only that is associated with a different session" );
		}
		proxy.getHibernateLazyInitializer().setReadOnly( readOnly );
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
		final Object entity = entitiesByKey.remove( oldKey );
		final EntityEntry oldEntry = entityEntryContext.removeEntityEntry( entity );
		parentsByChild.clear();

		final EntityKey newKey = session.generateEntityKey( generatedId, oldEntry.getDescriptor() );
		addEntity( newKey, entity );
		addEntry(
				entity,
				oldEntry.getStatus(),
				oldEntry.getLoadedState(),
				oldEntry.getRowId(),
				generatedId,
				oldEntry.getVersion(),
				oldEntry.getLockMode(),
				oldEntry.isExistsInDatabase(),
				oldEntry.getDescriptor(),
				oldEntry.isBeingReplicated()
		);
	}

	/**
	 * Used by the owning session to explicitly control serialization of the
	 * persistence context.
	 *
	 * @param oos The stream to which the persistence context should get written
	 * @throws IOException serialization errors.
	 */
	public void serialize(ObjectOutputStream oos) throws IOException {
		final boolean tracing = LOG.isTraceEnabled();
		if ( tracing ) {
			LOG.trace( "Serializing persistence-context" );
		}

		oos.writeBoolean( defaultReadOnly );
		oos.writeBoolean( hasNonReadOnlyEntities );

		oos.writeInt( entitiesByKey.size() );
		if ( tracing ) {
			LOG.trace( "Starting serialization of [" + entitiesByKey.size() + "] entitiesByKey entries" );
		}
		for ( Map.Entry<EntityKey,Object> entry : entitiesByKey.entrySet() ) {
			entry.getKey().serialize( oos );
			oos.writeObject( entry.getValue() );
		}

		oos.writeInt( entitiesByUniqueKey.size() );
		if ( tracing ) {
			LOG.trace( "Starting serialization of [" + entitiesByUniqueKey.size() + "] entitiesByUniqueKey entries" );
		}
		for ( Map.Entry<EntityUniqueKey,Object> entry : entitiesByUniqueKey.entrySet() ) {
			entry.getKey().serialize( oos );
			oos.writeObject( entry.getValue() );
		}

		oos.writeInt( proxiesByKey.size() );
		if ( tracing ) {
			LOG.trace( "Starting serialization of [" + proxiesByKey.size() + "] proxiesByKey entries" );
		}
		for ( Map.Entry<EntityKey,Object> entry : proxiesByKey.entrySet() ) {
			entry.getKey().serialize( oos );
			oos.writeObject( entry.getValue() );
		}

		oos.writeInt( entitySnapshotsByKey.size() );
		if ( tracing ) {
			LOG.trace( "Starting serialization of [" + entitySnapshotsByKey.size() + "] entitySnapshotsByKey entries" );
		}
		for ( Map.Entry<EntityKey,Object> entry : entitySnapshotsByKey.entrySet() ) {
			entry.getKey().serialize( oos );
			oos.writeObject( entry.getValue() );
		}

		entityEntryContext.serialize( oos );

		oos.writeInt( collectionsByKey.size() );
		if ( tracing ) {
			LOG.trace( "Starting serialization of [" + collectionsByKey.size() + "] collectionsByKey entries" );
		}
		for ( Map.Entry<CollectionKey,PersistentCollection> entry : collectionsByKey.entrySet() ) {
			entry.getKey().serialize( oos );
			oos.writeObject( entry.getValue() );
		}

		oos.writeInt( collectionEntries.size() );
		if ( tracing ) {
			LOG.trace( "Starting serialization of [" + collectionEntries.size() + "] collectionEntries entries" );
		}
		for ( Map.Entry<PersistentCollection,CollectionEntry> entry : collectionEntries.entrySet() ) {
			oos.writeObject( entry.getKey() );
			entry.getValue().serialize( oos );
		}

		oos.writeInt( arrayHolders.size() );
		if ( tracing ) {
			LOG.trace( "Starting serialization of [" + arrayHolders.size() + "] arrayHolders entries" );
		}
		for ( Map.Entry<Object,PersistentCollection> entry : arrayHolders.entrySet() ) {
			oos.writeObject( entry.getKey() );
			oos.writeObject( entry.getValue() );
		}

		oos.writeInt( nullifiableEntityKeys.size() );
		if ( tracing ) {
			LOG.trace( "Starting serialization of [" + nullifiableEntityKeys.size() + "] nullifiableEntityKey entries" );
		}
		for ( EntityKey entry : nullifiableEntityKeys ) {
			entry.serialize( oos );
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
		final boolean tracing = LOG.isTraceEnabled();
		if ( tracing ) {
			LOG.trace( "Deserializing persistence-context" );
		}
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
			if ( tracing ) {
				LOG.trace( "Starting deserialization of [" + count + "] entitiesByKey entries" );
			}
			rtn.entitiesByKey = new HashMap<>( count < INIT_COLL_SIZE ? INIT_COLL_SIZE : count );
			for ( int i = 0; i < count; i++ ) {
				rtn.entitiesByKey.put( EntityKey.deserialize( ois, sfi ), ois.readObject() );
			}

			count = ois.readInt();
			if ( tracing ) {
				LOG.trace( "Starting deserialization of [" + count + "] entitiesByUniqueKey entries" );
			}
			rtn.entitiesByUniqueKey = new HashMap<>( count < INIT_COLL_SIZE ? INIT_COLL_SIZE : count );
			for ( int i = 0; i < count; i++ ) {
				rtn.entitiesByUniqueKey.put( EntityUniqueKey.deserialize( ois, session ), ois.readObject() );
			}

			count = ois.readInt();
			if ( tracing ) {
				LOG.trace( "Starting deserialization of [" + count + "] proxiesByKey entries" );
			}
			//noinspection unchecked
			rtn.proxiesByKey = new ConcurrentReferenceHashMap<>(
					count < INIT_COLL_SIZE ? INIT_COLL_SIZE : count,
					.75f,
					1,
					ConcurrentReferenceHashMap.ReferenceType.STRONG,
					ConcurrentReferenceHashMap.ReferenceType.WEAK,
					null
			);
			for ( int i = 0; i < count; i++ ) {
				final EntityKey ek = EntityKey.deserialize( ois, sfi );
				final Object proxy = ois.readObject();
				if ( proxy instanceof HibernateProxy ) {
					( (HibernateProxy) proxy ).getHibernateLazyInitializer().setSession( session );
					rtn.proxiesByKey.put( ek, proxy );
				}
				else {
					// otherwise, the proxy was pruned during the serialization process
					if ( tracing ) {
						LOG.trace( "Encountered pruned proxy" );
					}
				}
			}

			count = ois.readInt();
			if ( tracing ) {
				LOG.trace( "Starting deserialization of [" + count + "] entitySnapshotsByKey entries" );
			}
			rtn.entitySnapshotsByKey = new HashMap<>( count < INIT_COLL_SIZE ? INIT_COLL_SIZE : count );
			for ( int i = 0; i < count; i++ ) {
				rtn.entitySnapshotsByKey.put( EntityKey.deserialize( ois, sfi ), ois.readObject() );
			}

			rtn.entityEntryContext = EntityEntryContext.deserialize( ois, rtn );

			count = ois.readInt();
			if ( tracing ) {
				LOG.trace( "Starting deserialization of [" + count + "] collectionsByKey entries" );
			}
			rtn.collectionsByKey = new HashMap<>( count < INIT_COLL_SIZE ? INIT_COLL_SIZE : count );
			for ( int i = 0; i < count; i++ ) {
				rtn.collectionsByKey.put( CollectionKey.deserialize( ois ), (PersistentCollection) ois.readObject() );
			}

			count = ois.readInt();
			if ( tracing ) {
				LOG.trace( "Starting deserialization of [" + count + "] collectionEntries entries" );
			}
			rtn.collectionEntries = IdentityMap.instantiateSequenced( count < INIT_COLL_SIZE ? INIT_COLL_SIZE : count );
			for ( int i = 0; i < count; i++ ) {
				final PersistentCollection pc = (PersistentCollection) ois.readObject();
				final CollectionEntry ce = CollectionEntry.deserialize( ois, session );
				pc.setCurrentSession( session );
				rtn.collectionEntries.put( pc, ce );
			}

			count = ois.readInt();
			if ( tracing ) {
				LOG.trace( "Starting deserialization of [" + count + "] arrayHolders entries" );
			}
			rtn.arrayHolders = new IdentityHashMap<>( count < INIT_COLL_SIZE ? INIT_COLL_SIZE : count );
			for ( int i = 0; i < count; i++ ) {
				rtn.arrayHolders.put( ois.readObject(), (PersistentCollection) ois.readObject() );
			}

			count = ois.readInt();
			if ( tracing ) {
				LOG.trace( "Starting deserialization of [" + count + "] nullifiableEntityKey entries" );
			}
			rtn.nullifiableEntityKeys = new HashSet<>();
			for ( int i = 0; i < count; i++ ) {
				rtn.nullifiableEntityKeys.add( EntityKey.deserialize( ois, sfi ) );
			}

		}
		catch ( HibernateException he ) {
			throw new InvalidObjectException( he.getMessage() );
		}

		return rtn;
	}

	@Override
	public void addChildParent(Object child, Object parent) {
		parentsByChild.put( child, parent );
	}

	@Override
	public void removeChildParent(Object child) {
		parentsByChild.remove( child );
	}


	// INSERTED KEYS HANDLING ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private HashMap<String,List<Object>> insertedKeysMap;

	@Override
	public void registerInsertedKey(EntityTypeDescriptor entityDescriptor, Object id) {
		// we only are worried about registering these if the entityDescriptor defines caching
		if ( entityDescriptor.canWriteToCache() ) {
			if ( insertedKeysMap == null ) {
				insertedKeysMap = new HashMap<>();
			}
			final String rootEntityName = entityDescriptor.getHierarchy().getRootEntityType().getEntityName();
			List<Object> insertedEntityIds = insertedKeysMap.get( rootEntityName );
			if ( insertedEntityIds == null ) {
				insertedEntityIds = new ArrayList<>();
				insertedKeysMap.put( rootEntityName, insertedEntityIds );
			}
			insertedEntityIds.add( id );
		}
	}

	@Override
	public boolean wasInsertedDuringTransaction(EntityTypeDescriptor descriptor, Object id) {
		// again, we only really care if the entity is cached
		if ( descriptor.canWriteToCache() ) {
			if ( insertedKeysMap != null ) {
				final List<Object> insertedEntityIds = insertedKeysMap.get(
						descriptor.getHierarchy().getRootEntityType().getEntityName()
				);
				if ( insertedEntityIds != null ) {
					return insertedEntityIds.contains( id );
				}
			}
		}
		return false;
	}

	private void cleanUpInsertedKeysAfterTransaction() {
		if ( insertedKeysMap != null ) {
			insertedKeysMap.clear();
		}
	}



	// NATURAL ID RESOLUTION HANDLING ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private final NaturalIdXrefDelegate naturalIdXrefDelegate = new NaturalIdXrefDelegate( this );

	private final NaturalIdHelper naturalIdHelper = new NaturalIdHelper() {
		@Override
		public void cacheNaturalIdCrossReferenceFromLoad(
				EntityTypeDescriptor descriptor,
				Object id,
				Object[] naturalIdValues) {
			if ( descriptor.getHierarchy().getNaturalIdDescriptor() == null ) {
				// nothing to do
				return;
			}

			descriptor = locateProperDescriptor( descriptor );

			// 'justAddedLocally' is meant to handle the case where we would get double stats jounaling
			//	from a single load event.  The first put journal would come from the natural id resolution;
			// the second comes from the entity loading.  In this condition, we want to avoid the multiple
			// 'put' stats incrementing.
			final boolean justAddedLocally = naturalIdXrefDelegate.cacheNaturalIdCrossReference( descriptor, id, naturalIdValues );

			if ( justAddedLocally ) {
				if(descriptor.hasNaturalIdentifier()) {
					final NaturalIdDataAccess cacheAccess = descriptor.getHierarchy().getNaturalIdDescriptor().getCacheAccess();

					if ( cacheAccess != null ) {
						managedSharedCacheEntries(
								descriptor,
								cacheAccess,
								id,
								naturalIdValues,
								null,
								CachedNaturalIdValueSource.LOAD
						);
					}
				}
			}
		}

		@Override
		public void manageLocalNaturalIdCrossReference(
				EntityTypeDescriptor descriptor,
				Object id,
				Object[] state,
				Object[] previousState,
				CachedNaturalIdValueSource source) {
			if ( descriptor.getHierarchy().getNaturalIdDescriptor() == null ) {
				// nothing to do
				return;
			}

			descriptor = locateProperDescriptor( descriptor );
			final Object[] naturalIdValues = extractNaturalIdValues( state, descriptor );

			// cache
			naturalIdXrefDelegate.cacheNaturalIdCrossReference( descriptor, id, naturalIdValues );
		}

		@Override
		public void manageSharedNaturalIdCrossReference(
				EntityTypeDescriptor descriptor,
				final Object id,
				Object[] state,
				Object[] previousState,
				CachedNaturalIdValueSource source) {
			if ( descriptor.getHierarchy().getNaturalIdDescriptor() == null ) {
				// nothing to do
				return;
			}
			if ( descriptor.hasNaturalIdentifier() ) {
				final NaturalIdDataAccess cacheAccess = descriptor.getHierarchy()
						.getNaturalIdDescriptor()
						.getCacheAccess();
				if ( cacheAccess == null ) {
					// nothing to do
					return;
				}
				descriptor = locateProperDescriptor( descriptor );
				final Object[] naturalIdValues = extractNaturalIdValues( state, descriptor );
				final Object[] previousNaturalIdValues = previousState == null ? null : extractNaturalIdValues(
						previousState,
						descriptor
				);

				managedSharedCacheEntries(
						descriptor,
						cacheAccess,
						id,
						naturalIdValues,
						previousNaturalIdValues,
						source
				);
			}
		}

		private void managedSharedCacheEntries(
				EntityTypeDescriptor descriptor,
				NaturalIdDataAccess cacheAccess,
				final Object id,
				Object[] naturalIdValues,
				Object[] previousNaturalIdValues,
				CachedNaturalIdValueSource source) {
			final Object naturalIdCacheKey = cacheAccess.generateCacheKey( naturalIdValues, descriptor.getHierarchy(), session );

			final SessionFactoryImplementor factory = session.getFactory();

			switch ( source ) {
				case LOAD: {
					if ( CacheHelper.fromSharedCache( session, naturalIdCacheKey, cacheAccess ) != null ) {
						// prevent identical re-cachings
						return;
					}
					final boolean put = cacheAccess.putFromLoad(
							session,
							naturalIdCacheKey,
							id,
							null
					);

					if ( put && factory.getStatistics().isStatisticsEnabled() ) {
						factory.getStatistics().naturalIdCachePut(
								descriptor.getNavigableRole(),
								cacheAccess.getRegion().getName()
						);
					}

					break;
				}
				case INSERT: {
					final boolean put = cacheAccess.insert( session, naturalIdCacheKey, id );
					if ( put && factory.getStatistics().isStatisticsEnabled() ) {
						factory.getStatistics().naturalIdCachePut(
								descriptor.getNavigableRole(),
								cacheAccess.getRegion().getName()
						);
					}

					( (EventSource) session ).getActionQueue().registerProcess(
							(success, session) -> {
								if (success) {
									final boolean put1 = cacheAccess.afterInsert( session, naturalIdCacheKey, id );

									if ( put1 && factory.getStatistics().isStatisticsEnabled() ) {
										factory.getStatistics().naturalIdCachePut(
												descriptor.getNavigableRole(),
												cacheAccess.getRegion().getName()
										);
									}
								}
								else {
									cacheAccess.evict( naturalIdCacheKey );
								}
							}
					);

					break;
				}
				case UPDATE: {
					final Object previousCacheKey = cacheAccess.generateCacheKey( previousNaturalIdValues, descriptor.getHierarchy(), session );
					if ( naturalIdCacheKey.equals( previousCacheKey ) ) {
						// prevent identical re-caching, solves HHH-7309
						return;
					}
					final SoftLock removalLock = cacheAccess.lockItem( session, previousCacheKey, null );
					cacheAccess.remove( session, previousCacheKey);

					final SoftLock lock = cacheAccess.lockItem( session, naturalIdCacheKey, null );
					final boolean put = cacheAccess.update( session, naturalIdCacheKey, id );
					if ( put && factory.getStatistics().isStatisticsEnabled() ) {
						factory.getStatistics().naturalIdCachePut(
								descriptor.getNavigableRole(),
								cacheAccess.getRegion().getName()
						);
					}

					( (EventSource) session ).getActionQueue().registerProcess(
							(success, session) -> {
								cacheAccess.unlockItem( session, previousCacheKey, removalLock );
								if ( success ) {
									final boolean afterUpdatePut = cacheAccess.afterUpdate(
											session,
											naturalIdCacheKey,
											id,
											lock
									);

									if ( afterUpdatePut && factory.getStatistics().isStatisticsEnabled() ) {
										factory.getStatistics().naturalIdCachePut(
												descriptor.getNavigableRole(),
												cacheAccess.getRegion().getName()
										);
									}
								}
								else {
									cacheAccess.unlockItem( session, naturalIdCacheKey, lock );
								}
							}
					);

					break;
				}
				default: {
					LOG.debug( "Unexpected CachedNaturalIdValueSource [" + source + "]" );
				}
			}
		}

		@Override
		public Object[] removeLocalNaturalIdCrossReference(EntityTypeDescriptor descriptor, Object id, Object[] state) {
			if ( descriptor.getHierarchy().getNaturalIdDescriptor() == null ) {
				// nothing to do
				return null;
			}

			descriptor = locateProperDescriptor( descriptor );
			final Object[] naturalIdValues = getNaturalIdValues( state, descriptor );

			final Object[] localNaturalIdValues = naturalIdXrefDelegate.removeNaturalIdCrossReference(
					descriptor,
					id,
					naturalIdValues
			);

			return localNaturalIdValues != null ? localNaturalIdValues : naturalIdValues;
		}

		@Override
		public void removeSharedNaturalIdCrossReference(EntityTypeDescriptor descriptor, Object id, Object[] naturalIdValues) {
			if ( descriptor.getHierarchy().getNaturalIdDescriptor() == null ) {
				// nothing to do
				return;
			}

			if ( descriptor.hasNaturalIdentifier() ) {
				final NaturalIdDataAccess cacheAccess = descriptor.getHierarchy()
						.getNaturalIdDescriptor()
						.getCacheAccess();
				if ( cacheAccess == null ) {
					// nothing to do
					return;
				}

				// todo : couple of things wrong here:
				//		1) should be using access strategy, not plain evict..
				//		2) should prefer session-cached values if any (requires interaction from removeLocalNaturalIdCrossReference

				descriptor = locateProperDescriptor( descriptor );
				final Object naturalIdCacheKey = cacheAccess.generateCacheKey( naturalIdValues, descriptor.getHierarchy(), session );
				cacheAccess.evict( naturalIdCacheKey );
			}

//			if ( sessionCachedNaturalIdValues != null
//					&& !Arrays.equals( sessionCachedNaturalIdValues, deletedNaturalIdValues ) ) {
//				final NaturalIdCacheKey sessionNaturalIdCacheKey = new NaturalIdCacheKey( sessionCachedNaturalIdValues, persister, session );
//				naturalIdCacheAccessStrategy.evict( sessionNaturalIdCacheKey );
//			}
		}

		@Override
		public Object[] findCachedNaturalId(EntityTypeDescriptor descriptor, Object pk) {
			return naturalIdXrefDelegate.findCachedNaturalId( locateProperDescriptor( descriptor ), pk );
		}

		@Override
		public Object findCachedNaturalIdResolution(EntityTypeDescriptor descriptor, Object[] naturalIdValues) {
			return naturalIdXrefDelegate.findCachedNaturalIdResolution( locateProperDescriptor( descriptor ), naturalIdValues );
		}

		@Override
		public Object[] extractNaturalIdValues(Object[] state, EntityTypeDescriptor entityDescriptor) {
			final NaturalIdDescriptor<?> naturalIdDescriptor = entityDescriptor.getHierarchy().getNaturalIdDescriptor();
			final int numberOfNaturalIdAttributes = naturalIdDescriptor.getAttributeInfos().size();

			if ( numberOfNaturalIdAttributes == state.length ) {
				// state just so happens to contain just the natural-id values
				return state;
			}

			final Object[] naturalIdValues = new Object[ numberOfNaturalIdAttributes ];
			int naturalIdIndex = 0;
			for ( NaturalIdAttributeInfo attribute : naturalIdDescriptor.getAttributeInfos() ) {
				naturalIdValues[ naturalIdIndex++ ] = state[ attribute.getStateArrayPosition() ];
			}

			return naturalIdValues;
		}

		@Override
		public Object[] extractNaturalIdValues(Object entity, EntityTypeDescriptor entityDescriptor) {
			if ( entity == null ) {
				throw new AssertionFailure( "Entity from which to extract natural id value(s) cannot be null" );
			}

			if ( entityDescriptor == null ) {
				throw new AssertionFailure( "EntityDescriptor to use in extracting natural id value(s) cannot be null" );
			}

			final NaturalIdDescriptor<?> naturalIdDescriptor = entityDescriptor.getHierarchy().getNaturalIdDescriptor();
			final int numberOfNaturalIdAttributes = naturalIdDescriptor.getAttributeInfos().size();

			final Object[] naturalIdValues = new Object[ numberOfNaturalIdAttributes ];
			int naturalIdIndex = 0;
			for ( NaturalIdAttributeInfo attribute : naturalIdDescriptor.getAttributeInfos() ) {
				naturalIdValues[ naturalIdIndex++ ] = attribute.getUnderlyingAttributeDescriptor().getPropertyAccess().getGetter().get( entity );
			}

			return naturalIdValues;
		}

		@Override
		public Collection<Object> getCachedPkResolutions(EntityTypeDescriptor descriptor) {
			return naturalIdXrefDelegate.getCachedPkResolutions( descriptor );
		}

		@Override
		public void handleSynchronization(EntityTypeDescriptor descriptor, Object pk, Object entity) {
			if ( descriptor.getHierarchy().getNaturalIdDescriptor() == null  ) {
				// nothing to do
				return;
			}

			descriptor = locateProperDescriptor( descriptor );

			final Object[] naturalIdValuesFromCurrentObjectState = extractNaturalIdValues( entity, descriptor );
			final boolean changed = ! naturalIdXrefDelegate.sameAsCached(
					descriptor,
					pk,
					naturalIdValuesFromCurrentObjectState
			);

			if ( changed ) {
				final Object[] cachedNaturalIdValues = naturalIdXrefDelegate.findCachedNaturalId( descriptor, pk );
				naturalIdXrefDelegate.cacheNaturalIdCrossReference( descriptor, pk, naturalIdValuesFromCurrentObjectState );
				naturalIdXrefDelegate.stashInvalidNaturalIdReference( descriptor, cachedNaturalIdValues );

				removeSharedNaturalIdCrossReference(
						descriptor,
						pk,
						cachedNaturalIdValues
				);
			}
		}

		@Override
		public void cleanupFromSynchronizations() {
			naturalIdXrefDelegate.unStashInvalidNaturalIdReferences();
		}

		@Override
		public void handleEviction(Object object, EntityTypeDescriptor descriptor, Object identifier) {
			naturalIdXrefDelegate.removeNaturalIdCrossReference(
					descriptor,
					identifier,
					findCachedNaturalId( descriptor, identifier )
			);
		}
	};

	@Override
	public NaturalIdHelper getNaturalIdHelper() {
		return naturalIdHelper;
	}

	private Object[] getNaturalIdValues(Object[] state, EntityTypeDescriptor entityDescriptor) {
		final NaturalIdDescriptor<?> naturalIdDescriptor = entityDescriptor.getHierarchy().getNaturalIdDescriptor();
		final int numberOfNaturalIdAttributes = naturalIdDescriptor.getAttributeInfos().size();

		final Object[] naturalIdValues = new Object[ numberOfNaturalIdAttributes ];
		int naturalIdIndex = 0;
		for ( NaturalIdAttributeInfo attribute : naturalIdDescriptor.getAttributeInfos() ) {
			naturalIdValues[ naturalIdIndex++ ] = state[ attribute.getStateArrayPosition() ];
		}

		return naturalIdValues;
	}
}
