/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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

import org.jboss.logging.Logger;

import org.hibernate.AssertionFailure;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.NonUniqueObjectException;
import org.hibernate.PersistentObjectException;
import org.hibernate.TransientObjectException;
import org.hibernate.action.spi.AfterTransactionCompletionProcess;
import org.hibernate.cache.spi.NaturalIdCacheKey;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.loading.internal.LoadContexts;
import org.hibernate.engine.spi.AssociationKey;
import org.hibernate.engine.spi.BatchFetchQueue;
import org.hibernate.engine.spi.CachedNaturalIdValueSource;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.EntityUniqueKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.EventSource;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.MarkerObject;
import org.hibernate.internal.util.collections.ConcurrentReferenceHashMap;
import org.hibernate.internal.util.collections.IdentityMap;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.sql.Select;
import org.hibernate.tuple.ElementWrapper;
import org.hibernate.type.CollectionType;

/**
 * A <strong>stateful</strong> implementation of the {@link PersistenceContext} contract meaning that we maintain this
 * state throughout the life of the persistence context.
 * </p>
 * IMPL NOTE: There is meant to be a one-to-one correspondence between a {@link org.hibernate.internal.SessionImpl}
 * and a PersistentContext.  Event listeners and other Session collaborators then use the PersistentContext to drive
 * their processing.
 *
 * @author Steve Ebersole
 */
public class StatefulPersistenceContext implements PersistenceContext {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger( CoreMessageLogger.class, StatefulPersistenceContext.class.getName() );

   private static final boolean tracing = LOG.isTraceEnabled();

	public static final Object NO_ROW = new MarkerObject( "NO_ROW" );

	private static final int INIT_COLL_SIZE = 8;

	private SessionImplementor session;

	// Loaded entity instances, by EntityKey
	private Map<EntityKey, Object> entitiesByKey;

	// Loaded entity instances, by EntityUniqueKey
	private Map<EntityUniqueKey, Object> entitiesByUniqueKey;

	// Identity map of EntityEntry instances, by the entity instance
	private Map<Object,EntityEntry> entityEntries;

	// Entity proxies, by EntityKey
	private Map<EntityKey, Object> proxiesByKey;

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

	private int cascading = 0;
	private int loadCounter = 0;
	private boolean flushing = false;

	private boolean defaultReadOnly = false;
	private boolean hasNonReadOnlyEntities = false;

	private LoadContexts loadContexts;
	private BatchFetchQueue batchFetchQueue;



	/**
	 * Constructs a PersistentContext, bound to the given session.
	 *
	 * @param session The session "owning" this context.
	 */
	public StatefulPersistenceContext(SessionImplementor session) {
		this.session = session;

		entitiesByKey = new HashMap<EntityKey, Object>( INIT_COLL_SIZE );
		entitiesByUniqueKey = new HashMap<EntityUniqueKey, Object>( INIT_COLL_SIZE );
		//noinspection unchecked
		proxiesByKey = new ConcurrentReferenceHashMap<EntityKey, Object>( INIT_COLL_SIZE, .75f, 1, ConcurrentReferenceHashMap.ReferenceType.STRONG, ConcurrentReferenceHashMap.ReferenceType.WEAK, null );
		entitySnapshotsByKey = new HashMap<EntityKey, Object>( INIT_COLL_SIZE );

		entityEntries = IdentityMap.instantiateSequenced( INIT_COLL_SIZE );
		collectionEntries = IdentityMap.instantiateSequenced( INIT_COLL_SIZE );
		parentsByChild = IdentityMap.instantiateSequenced( INIT_COLL_SIZE );

		collectionsByKey = new HashMap<CollectionKey, PersistentCollection>( INIT_COLL_SIZE );
		arrayHolders = new IdentityHashMap<Object, PersistentCollection>( INIT_COLL_SIZE );

		nullifiableEntityKeys = new HashSet<EntityKey>();

		initTransientState();
	}

	private void initTransientState() {
		nullAssociations = new HashSet<AssociationKey>( INIT_COLL_SIZE );
		nonlazyCollections = new ArrayList<PersistentCollection>( INIT_COLL_SIZE );
	}

	@Override
	public boolean isStateless() {
		return false;
	}

	@Override
	public SessionImplementor getSession() {
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
			unownedCollections = new HashMap<CollectionKey,PersistentCollection>(INIT_COLL_SIZE);
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
		for ( Map.Entry<PersistentCollection, CollectionEntry> aCollectionEntryArray : IdentityMap.concurrentEntries( collectionEntries ) ) {
			aCollectionEntryArray.getKey().unsetSession( getSession() );
		}
		arrayHolders.clear();
		entitiesByKey.clear();
		entitiesByUniqueKey.clear();
		entityEntries.clear();
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
		entry.setStatus(status);
		setHasNonReadOnlyEnties(status);
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
		for ( EntityEntry o : entityEntries.values() ) {
			o.setLockMode( LockMode.NONE );
		}
	}

	/**
	 * Get the current state of the entity as known to the underlying
	 * database, or null if there is no corresponding row
	 */
	@Override
	public Object[] getDatabaseSnapshot(Serializable id, EntityPersister persister)
	throws HibernateException {
		final EntityKey key = session.generateEntityKey( id, persister );
		Object cached = entitySnapshotsByKey.get(key);
		if (cached!=null) {
			return cached==NO_ROW ? null : (Object[]) cached;
		}
		else {
			Object[] snapshot = persister.getDatabaseSnapshot( id, session );
			entitySnapshotsByKey.put( key, snapshot==null ? NO_ROW : snapshot );
			return snapshot;
		}
	}

	@Override
	public Object[] getNaturalIdSnapshot(Serializable id, EntityPersister persister)
	throws HibernateException {
		if ( !persister.hasNaturalIdentifier() ) {
			return null;
		}

		persister = locateProperPersister( persister );

		// let's first see if it is part of the natural id cache...
		final Object[] cachedValue = naturalIdHelper.findCachedNaturalId( persister, id );
		if ( cachedValue != null ) {
			return cachedValue;
		}

		// check to see if the natural id is mutable/immutable
		if ( persister.getEntityMetamodel().hasImmutableNaturalId() ) {
			// an immutable natural-id is not retrieved during a normal database-snapshot operation...
			final Object[] dbValue = persister.getNaturalIdentifierSnapshot( id, session );
			naturalIdHelper.cacheNaturalIdCrossReferenceFromLoad(
					persister,
					id,
					dbValue
			);
			return dbValue;
		}
		else {
			// for a mutable natural there is a likelihood that the the information will already be
			// snapshot-cached.
			final int[] props = persister.getNaturalIdentifierProperties();
			final Object[] entitySnapshot = getDatabaseSnapshot( id, persister );
			if ( entitySnapshot == NO_ROW ) {
				return null;
			}

			final Object[] naturalIdSnapshotSubSet = new Object[ props.length ];
			for ( int i = 0; i < props.length; i++ ) {
				naturalIdSnapshotSubSet[i] = entitySnapshot[ props[i] ];
			}
			naturalIdHelper.cacheNaturalIdCrossReferenceFromLoad(
					persister,
					id,
					naturalIdSnapshotSubSet
			);
			return naturalIdSnapshotSubSet;
		}
	}

	private EntityPersister locateProperPersister(EntityPersister persister) {
		return session.getFactory().getEntityPersister( persister.getRootEntityName() );
	}

	/**
	 * Retrieve the cached database snapshot for the requested entity key.
	 * <p/>
	 * This differs from {@link #getDatabaseSnapshot} is two important respects:<ol>
	 * <li>no snapshot is obtained from the database if not already cached</li>
	 * <li>an entry of {@link #NO_ROW} here is interpretet as an exception</li>
	 * </ol>
	 * @param key The entity key for which to retrieve the cached snapshot
	 * @return The cached snapshot
	 * @throws IllegalStateException if the cached snapshot was == {@link #NO_ROW}.
	 */
	@Override
	public Object[] getCachedDatabaseSnapshot(EntityKey key) {
		Object snapshot = entitySnapshotsByKey.get( key );
		if ( snapshot == NO_ROW ) {
			throw new IllegalStateException( "persistence context reported no row snapshot for " + MessageHelper.infoString( key.getEntityName(), key.getIdentifier() ) );
		}
		return ( Object[] ) snapshot;
	}

	@Override
	public void addEntity(EntityKey key, Object entity) {
		entitiesByKey.put(key, entity);
		getBatchFetchQueue().removeBatchLoadableEntityKey(key);
	}

	/**
	 * Get the entity instance associated with the given
	 * <tt>EntityKey</tt>
	 */
	@Override
	public Object getEntity(EntityKey key) {
		return entitiesByKey.get(key);
	}

	@Override
	public boolean containsEntity(EntityKey key) {
		return entitiesByKey.containsKey(key);
	}

	/**
	 * Remove an entity from the session cache, also clear
	 * up other state associated with the entity, all except
	 * for the <tt>EntityEntry</tt>
	 */
	@Override
	public Object removeEntity(EntityKey key) {
		Object entity = entitiesByKey.remove(key);
		Iterator iter = entitiesByUniqueKey.values().iterator();
		while ( iter.hasNext() ) {
			if ( iter.next()==entity ) iter.remove();
		}
		// Clear all parent cache
		parentsByChild.clear();
		entitySnapshotsByKey.remove(key);
		nullifiableEntityKeys.remove(key);
		getBatchFetchQueue().removeBatchLoadableEntityKey(key);
		getBatchFetchQueue().removeSubselect(key);
		return entity;
	}

	/**
	 * Get an entity cached by unique key
	 */
	@Override
	public Object getEntity(EntityUniqueKey euk) {
		return entitiesByUniqueKey.get(euk);
	}

	/**
	 * Add an entity to the cache by unique key
	 */
	@Override
	public void addEntity(EntityUniqueKey euk, Object entity) {
		entitiesByUniqueKey.put(euk, entity);
	}

	/**
	 * Retrieve the EntityEntry representation of the given entity.
	 *
	 * @param entity The entity for which to locate the EntityEntry.
	 * @return The EntityEntry for the given entity.
	 */
	@Override
	public EntityEntry getEntry(Object entity) {
		return entityEntries.get(entity);
	}

	/**
	 * Remove an entity entry from the session cache
	 */
	@Override
	public EntityEntry removeEntry(Object entity) {
		return entityEntries.remove(entity);
	}

	/**
	 * Is there an EntityEntry for this instance?
	 */
	@Override
	public boolean isEntryFor(Object entity) {
		return entityEntries.containsKey(entity);
	}

	/**
	 * Get the collection entry for a persistent collection
	 */
	@Override
	public CollectionEntry getCollectionEntry(PersistentCollection coll) {
		return collectionEntries.get(coll);
	}

	/**
	 * Adds an entity to the internal caches.
	 */
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
			final boolean disableVersionIncrement,
			boolean lazyPropertiesAreUnfetched) {
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
				persister,
				disableVersionIncrement,
				lazyPropertiesAreUnfetched
		);
	}


	/**
	 * Generates an appropriate EntityEntry instance and adds it
	 * to the event source's internal caches.
	 */
	@Override
	public EntityEntry addEntry(
			final Object entity,
			final Status status,
			final Object[] loadedState,
			final Object rowId,
			final Serializable id,
			final Object version,
			final LockMode lockMode,
			final boolean existsInDatabase,
			final EntityPersister persister,
			final boolean disableVersionIncrement,
			boolean lazyPropertiesAreUnfetched) {

		EntityEntry e = new EntityEntry(
				status,
				loadedState,
				rowId,
				id,
				version,
				lockMode,
				existsInDatabase,
				persister,
				persister.getEntityMode(),
				session.getTenantIdentifier(),
				disableVersionIncrement,
				lazyPropertiesAreUnfetched,
				this
		);
		entityEntries.put(entity, e);

		setHasNonReadOnlyEnties(status);
		return e;
	}

	@Override
	public boolean containsCollection(PersistentCollection collection) {
		return collectionEntries.containsKey(collection);
	}

	@Override
	public boolean containsProxy(Object entity) {
		return proxiesByKey.containsValue( entity );
	}

	/**
	 * Takes the given object and, if it represents a proxy, reassociates it with this event source.
	 *
	 * @param value The possible proxy to be reassociated.
	 * @return Whether the passed value represented an actual proxy which got initialized.
	 * @throws MappingException
	 */
	@Override
	public boolean reassociateIfUninitializedProxy(Object value) throws MappingException {
		if ( value instanceof ElementWrapper ) {
			value = ( (ElementWrapper) value ).getElement();
		}

		if ( !Hibernate.isInitialized(value) ) {
			HibernateProxy proxy = (HibernateProxy) value;
			LazyInitializer li = proxy.getHibernateLazyInitializer();
			reassociateProxy(li, proxy);
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * If a deleted entity instance is re-saved, and it has a proxy, we need to
	 * reset the identifier of the proxy
	 */
	@Override
	public void reassociateProxy(Object value, Serializable id) throws MappingException {
		if ( value instanceof ElementWrapper ) {
			value = ( (ElementWrapper) value ).getElement();
		}

		if ( value instanceof HibernateProxy ) {
			LOG.debugf( "Setting proxy identifier: %s", id );
			HibernateProxy proxy = (HibernateProxy) value;
			LazyInitializer li = proxy.getHibernateLazyInitializer();
			li.setIdentifier(id);
			reassociateProxy(li, proxy);
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
			final EntityPersister persister = session.getFactory().getEntityPersister( li.getEntityName() );
			final EntityKey key = session.generateEntityKey( li.getIdentifier(), persister );
		  	// any earlier proxy takes precedence
			if ( !proxiesByKey.containsKey( key ) ) {
				proxiesByKey.put( key, proxy );
			}
			proxy.getHibernateLazyInitializer().setSession( session );
		}
	}

	/**
	 * Get the entity instance underlying the given proxy, throwing
	 * an exception if the proxy is uninitialized. If the given object
	 * is not a proxy, simply return the argument.
	 */
	@Override
	public Object unproxy(Object maybeProxy) throws HibernateException {
		if ( maybeProxy instanceof ElementWrapper ) {
			maybeProxy = ( (ElementWrapper) maybeProxy ).getElement();
		}

		if ( maybeProxy instanceof HibernateProxy ) {
			HibernateProxy proxy = (HibernateProxy) maybeProxy;
			LazyInitializer li = proxy.getHibernateLazyInitializer();
			if ( li.isUninitialized() ) {
				throw new PersistentObjectException(
						"object was an uninitialized proxy for " +
						li.getEntityName()
				);
			}
			return li.getImplementation(); //unwrap the object
		}
		else {
			return maybeProxy;
		}
	}

	/**
	 * Possibly unproxy the given reference and reassociate it with the current session.
	 *
	 * @param maybeProxy The reference to be unproxied if it currently represents a proxy.
	 * @return The unproxied instance.
	 * @throws HibernateException
	 */
	@Override
	public Object unproxyAndReassociate(Object maybeProxy) throws HibernateException {
		if ( maybeProxy instanceof ElementWrapper ) {
			maybeProxy = ( (ElementWrapper) maybeProxy ).getElement();
		}

		if ( maybeProxy instanceof HibernateProxy ) {
			HibernateProxy proxy = (HibernateProxy) maybeProxy;
			LazyInitializer li = proxy.getHibernateLazyInitializer();
			reassociateProxy(li, proxy);
			return li.getImplementation(); //initialize + unwrap the object
		}
		else {
			return maybeProxy;
		}
	}

	/**
	 * Attempts to check whether the given key represents an entity already loaded within the
	 * current session.
	 * @param object The entity reference against which to perform the uniqueness check.
	 * @throws HibernateException
	 */
	@Override
	public void checkUniqueness(EntityKey key, Object object) throws HibernateException {
		Object entity = getEntity(key);
		if ( entity == object ) {
			throw new AssertionFailure( "object already associated, but no entry was found" );
		}
		if ( entity != null ) {
			throw new NonUniqueObjectException( key.getIdentifier(), key.getEntityName() );
		}
	}

	/**
	 * If the existing proxy is insufficiently "narrow" (derived), instantiate a new proxy
	 * and overwrite the registration of the old one. This breaks == and occurs only for
	 * "class" proxies rather than "interface" proxies. Also init the proxy to point to
	 * the given target implementation if necessary.
	 *
	 * @param proxy The proxy instance to be narrowed.
	 * @param persister The persister for the proxied entity.
	 * @param key The internal cache key for the proxied entity.
	 * @param object (optional) the actual proxied entity instance.
	 * @return An appropriately narrowed instance.
	 * @throws HibernateException
	 */
	@Override
	public Object narrowProxy(Object proxy, EntityPersister persister, EntityKey key, Object object)
			throws HibernateException {

		final Class concreteProxyClass = persister.getConcreteProxyClass();
		boolean alreadyNarrow = concreteProxyClass.isAssignableFrom( proxy.getClass() );

		if ( !alreadyNarrow ) {
			LOG.narrowingProxy( concreteProxyClass );

			if ( object != null ) {
				proxiesByKey.remove(key);
				return object; //return the proxied object
			}
			else {
				proxy = persister.createProxy( key.getIdentifier(), session );
				Object proxyOrig = proxiesByKey.put(key, proxy); //overwrite old proxy
				if ( proxyOrig != null ) {
					if ( ! ( proxyOrig instanceof HibernateProxy ) ) {
						throw new AssertionFailure(
								"proxy not of type HibernateProxy; it is " + proxyOrig.getClass()
						);
					}
					// set the read-only/modifiable mode in the new proxy to what it was in the original proxy
					boolean readOnlyOrig = ( ( HibernateProxy ) proxyOrig ).getHibernateLazyInitializer().isReadOnly();
					( ( HibernateProxy ) proxy ).getHibernateLazyInitializer().setReadOnly( readOnlyOrig );
				}
				return proxy;
			}
		}
		else {

			if ( object != null ) {
				LazyInitializer li = ( (HibernateProxy) proxy ).getHibernateLazyInitializer();
				li.setImplementation(object);
			}

			return proxy;

		}

	}

	/**
	 * Return the existing proxy associated with the given <tt>EntityKey</tt>, or the
	 * third argument (the entity associated with the key) if no proxy exists. Init
	 * the proxy to the target implementation, if necessary.
	 */
	@Override
	public Object proxyFor(EntityPersister persister, EntityKey key, Object impl)
	throws HibernateException {
		if ( !persister.hasProxy() ) {
			return impl;
		}
		Object proxy = proxiesByKey.get( key );
		return ( proxy != null ) ? narrowProxy( proxy, persister, key, impl ) : impl;
	}

	/**
	 * Return the existing proxy associated with the given <tt>EntityKey</tt>, or the
	 * argument (the entity associated with the key) if no proxy exists.
	 * (slower than the form above)
	 */
	@Override
	public Object proxyFor(Object impl) throws HibernateException {
		EntityEntry e = getEntry(impl);
		return proxyFor( e.getPersister(), e.getEntityKey(), impl );
	}

	/**
	 * Get the entity that owns this persistent collection
	 */
	@Override
	public Object getCollectionOwner(Serializable key, CollectionPersister collectionPersister) throws MappingException {
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
			final Serializable owenerId = ownerPersister.getIdentifier( key, session );
			if ( owenerId == null ) {
				return null;
			}
			return getEntity( session.generateEntityKey( owenerId, ownerPersister ) );
		}

		final CollectionType collectionType = collectionPersister.getCollectionType();

		//		2) The incoming key is most likely the collection key which we need to resolve to the owner key
		//			find the corresponding owner instance
		//			a) try by EntityUniqueKey
		if ( collectionType.getLHSPropertyName() != null ) {
			Object owner = getEntity(
					new EntityUniqueKey(
							ownerPersister.getEntityName(),
							collectionType.getLHSPropertyName(),
							key,
							collectionPersister.getKeyType(),
							ownerPersister.getEntityMode(),
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
			final Serializable ownerId = ownerPersister.getIdByUniqueKey( key, collectionType.getLHSPropertyName(), session );
			return getEntity( session.generateEntityKey( ownerId, ownerPersister ) );
		}

		// as a last resort this is what the old code did...
		return getEntity( session.generateEntityKey( key, collectionPersister.getOwnerEntityPersister() ) );
	}

	/**
	 * Get the entity that owned this persistent collection when it was loaded
	 *
	 * @param collection The persistent collection
	 * @return the owner, if its entity ID is available from the collection's loaded key
	 * and the owner entity is in the persistence context; otherwise, returns null
	 */
	@Override
	public Object getLoadedCollectionOwnerOrNull(PersistentCollection collection) {
		CollectionEntry ce = getCollectionEntry( collection );
		if ( ce.getLoadedPersister() == null ) {
			return null; // early exit...
		}
		Object loadedOwner = null;
		// TODO: an alternative is to check if the owner has changed; if it hasn't then
		// return collection.getOwner()
		Serializable entityId = getLoadedCollectionOwnerIdOrNull( ce );
		if ( entityId != null ) {
			loadedOwner = getCollectionOwner( entityId, ce.getLoadedPersister() );
		}
		return loadedOwner;
	}

	/**
	 * Get the ID for the entity that owned this persistent collection when it was loaded
	 *
	 * @param collection The persistent collection
	 * @return the owner ID if available from the collection's loaded key; otherwise, returns null
	 */
	@Override
	public Serializable getLoadedCollectionOwnerIdOrNull(PersistentCollection collection) {
		return getLoadedCollectionOwnerIdOrNull( getCollectionEntry( collection ) );
	}

	/**
	 * Get the ID for the entity that owned this persistent collection when it was loaded
	 *
	 * @param ce The collection entry
	 * @return the owner ID if available from the collection's loaded key; otherwise, returns null
	 */
	private Serializable getLoadedCollectionOwnerIdOrNull(CollectionEntry ce) {
		if ( ce == null || ce.getLoadedKey() == null || ce.getLoadedPersister() == null ) {
			return null;
		}
		// TODO: an alternative is to check if the owner has changed; if it hasn't then
		// get the ID from collection.getOwner()
		return ce.getLoadedPersister().getCollectionType().getIdOfOwnerOrNull( ce.getLoadedKey(), session );
	}

	/**
	 * add a collection we just loaded up (still needs initializing)
	 */
	@Override
	public void addUninitializedCollection(CollectionPersister persister, PersistentCollection collection, Serializable id) {
		CollectionEntry ce = new CollectionEntry(collection, persister, id, flushing);
		addCollection(collection, ce, id);
		if ( persister.getBatchSize() > 1 ) {
			getBatchFetchQueue().addBatchLoadableCollection( collection, ce );
		}
	}

	/**
	 * add a detached uninitialized collection
	 */
	@Override
	public void addUninitializedDetachedCollection(CollectionPersister persister, PersistentCollection collection) {
		CollectionEntry ce = new CollectionEntry( persister, collection.getKey() );
		addCollection( collection, ce, collection.getKey() );
		if ( persister.getBatchSize() > 1 ) {
			getBatchFetchQueue().addBatchLoadableCollection( collection, ce );
		}
	}

	/**
	 * Add a new collection (ie. a newly created one, just instantiated by the
	 * application, with no database state or snapshot)
	 * @param collection The collection to be associated with the persistence context
	 */
	@Override
	public void addNewCollection(CollectionPersister persister, PersistentCollection collection)
	throws HibernateException {
		addCollection(collection, persister);
	}

	/**
	 * Add an collection to the cache, with a given collection entry.
	 *
	 * @param coll The collection for which we are adding an entry.
	 * @param entry The entry representing the collection.
	 * @param key The key of the collection's entry.
	 */
	private void addCollection(PersistentCollection coll, CollectionEntry entry, Serializable key) {
		collectionEntries.put( coll, entry );
		CollectionKey collectionKey = new CollectionKey( entry.getLoadedPersister(), key );
		PersistentCollection old = collectionsByKey.put( collectionKey, coll );
		if ( old != null ) {
			if ( old == coll ) {
				throw new AssertionFailure("bug adding collection twice");
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
	 * @param persister The collection persister
	 */
	private void addCollection(PersistentCollection collection, CollectionPersister persister) {
		CollectionEntry ce = new CollectionEntry( persister, collection );
		collectionEntries.put( collection, ce );
	}

	/**
	 * add an (initialized) collection that was created by another session and passed
	 * into update() (ie. one with a snapshot and existing state on the database)
	 */
	@Override
	public void addInitializedDetachedCollection(CollectionPersister collectionPersister, PersistentCollection collection)
	throws HibernateException {
		if ( collection.isUnreferenced() ) {
			//treat it just like a new collection
			addCollection( collection, collectionPersister );
		}
		else {
			CollectionEntry ce = new CollectionEntry( collection, session.getFactory() );
			addCollection( collection, ce, collection.getKey() );
		}
	}

	/**
	 * add a collection we just pulled out of the cache (does not need initializing)
	 */
	@Override
	public CollectionEntry addInitializedCollection(CollectionPersister persister, PersistentCollection collection, Serializable id)
	throws HibernateException {
		CollectionEntry ce = new CollectionEntry(collection, persister, id, flushing);
		ce.postInitialize(collection);
		addCollection(collection, ce, id);
		return ce;
	}

	/**
	 * Get the collection instance associated with the <tt>CollectionKey</tt>
	 */
	@Override
	public PersistentCollection getCollection(CollectionKey collectionKey) {
		return collectionsByKey.get( collectionKey );
	}

	/**
	 * Register a collection for non-lazy loading at the end of the
	 * two-phase load
	 */
	@Override
	public void addNonLazyCollection(PersistentCollection collection) {
		nonlazyCollections.add(collection);
	}

	/**
	 * Force initialization of all non-lazy collections encountered during
	 * the current two-phase load (actually, this is a no-op, unless this
	 * is the "outermost" load)
	 */
	@Override
	public void initializeNonLazyCollections() throws HibernateException {
		if ( loadCounter == 0 ) {
         if (tracing)
			   LOG.trace( "Initializing non-lazy collections" );

			//do this work only at the very highest level of the load
			loadCounter++; //don't let this method be called recursively
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


	/**
	 * Get the <tt>PersistentCollection</tt> object for an array
	 */
	@Override
	public PersistentCollection getCollectionHolder(Object array) {
		return arrayHolders.get(array);
	}

	/**
	 * Register a <tt>PersistentCollection</tt> object for an array.
	 * Associates a holder with an array - MUST be called after loading
	 * array, since the array instance is not created until endLoad().
	 */
	@Override
	public void addCollectionHolder(PersistentCollection holder) {
		//TODO:refactor + make this method private
		arrayHolders.put( holder.getValue(), holder );
	}

	@Override
	public PersistentCollection removeCollectionHolder(Object array) {
		return arrayHolders.remove(array);
	}

	/**
	 * Get the snapshot of the pre-flush collection state
	 */
	@Override
	public Serializable getSnapshot(PersistentCollection coll) {
		return getCollectionEntry(coll).getSnapshot();
	}

	/**
	 * Get the collection entry for a collection passed to filter,
	 * which might be a collection wrapper, an array, or an unwrapped
	 * collection. Return null if there is no entry.
	 */
	@Override
	public CollectionEntry getCollectionEntryOrNull(Object collection) {
		PersistentCollection coll;
		if ( collection instanceof PersistentCollection ) {
			coll = (PersistentCollection) collection;
			//if (collection==null) throw new TransientObjectException("Collection was not yet persistent");
		}
		else {
			coll = getCollectionHolder(collection);
			if ( coll == null ) {
				//it might be an unwrapped collection reference!
				//try to find a wrapper (slowish)
				Iterator<PersistentCollection> wrappers = collectionEntries.keyIterator();
				while ( wrappers.hasNext() ) {
					PersistentCollection pc = wrappers.next();
					if ( pc.isWrapper(collection) ) {
						coll = pc;
						break;
					}
				}
			}
		}

		return (coll == null) ? null : getCollectionEntry(coll);
	}

	/**
	 * Get an existing proxy by key
	 */
	@Override
	public Object getProxy(EntityKey key) {
		return proxiesByKey.get(key);
	}

	/**
	 * Add a proxy to the session cache
	 */
	@Override
	public void addProxy(EntityKey key, Object proxy) {
		proxiesByKey.put(key, proxy);
	}

	/**
	 * Remove a proxy from the session cache.
	 * <p/>
	 * Additionally, ensure that any load optimization references
	 * such as batch or subselect loading get cleaned up as well.
	 *
	 * @param key The key of the entity proxy to be removed
	 * @return The proxy reference.
	 */
	@Override
	public Object removeProxy(EntityKey key) {
		if ( batchFetchQueue != null ) {
			batchFetchQueue.removeBatchLoadableEntityKey( key );
			batchFetchQueue.removeSubselect( key );
		}
		return proxiesByKey.remove( key );
	}

	/**
	 * Retrieve the set of EntityKeys representing nullifiable references
	 */
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
	public Map getEntityEntries() {
		return entityEntries;
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

	/**
	 * Returns a string representation of the object.
	 *
	 * @return a string representation of the object.
	 */
	@Override
    public String toString() {
		return new StringBuilder()
				.append("PersistenceContext[entityKeys=")
				.append(entitiesByKey.keySet())
				.append(",collectionKeys=")
				.append(collectionsByKey.keySet())
				.append("]")
				.toString();
	}

	/**
	 * Search <tt>this</tt> persistence context for an associated entity instance which is considered the "owner" of
	 * the given <tt>childEntity</tt>, and return that owner's id value.  This is performed in the scenario of a
	 * uni-directional, non-inverse one-to-many collection (which means that the collection elements do not maintain
	 * a direct reference to the owner).
	 * <p/>
	 * As such, the processing here is basically to loop over every entity currently associated with this persistence
	 * context and for those of the correct entity (sub) type to extract its collection role property value and see
	 * if the child is contained within that collection.  If so, we have found the owner; if not, we go on.
	 * <p/>
	 * Also need to account for <tt>mergeMap</tt> which acts as a local copy cache managed for the duration of a merge
	 * operation.  It represents a map of the detached entity instances pointing to the corresponding managed instance.
	 *
	 * @param entityName The entity name for the entity type which would own the child
	 * @param propertyName The name of the property on the owning entity type which would name this child association.
	 * @param childEntity The child entity instance for which to locate the owner instance id.
	 * @param mergeMap A map of non-persistent instances from an on-going merge operation (possibly null).
	 *
	 * @return The id of the entityName instance which is said to own the child; null if an appropriate owner not
	 * located.
	 */
	@Override
	public Serializable getOwnerId(String entityName, String propertyName, Object childEntity, Map mergeMap) {
		final String collectionRole = entityName + '.' + propertyName;
		final EntityPersister persister = session.getFactory().getEntityPersister( entityName );
		final CollectionPersister collectionPersister = session.getFactory().getCollectionPersister( collectionRole );

	    // try cache lookup first
		Object parent = parentsByChild.get( childEntity );
		if ( parent != null ) {
			final EntityEntry entityEntry = entityEntries.get( parent );
			//there maybe more than one parent, filter by type
			if ( 	persister.isSubclassEntityName(entityEntry.getEntityName() )
					&& isFoundInParent( propertyName, childEntity, persister, collectionPersister, parent ) ) {
				return getEntry( parent ).getId();
			}
			else {
				parentsByChild.remove( childEntity ); // remove wrong entry
			}
		}

		//not found in case, proceed
		// iterate all the entities currently associated with the persistence context.
		for ( Entry<Object,EntityEntry> me : IdentityMap.concurrentEntries( entityEntries ) ) {
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
					Object unmergedInstance = mergeMap.get( entityEntryInstance );
					Object unmergedChild = mergeMap.get( childEntity );
					if ( unmergedInstance != null && unmergedChild != null ) {
						found = isFoundInParent(
								propertyName,
								unmergedChild,
								persister,
								collectionPersister,
								unmergedInstance
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
					if ( persister.isSubclassEntityName( proxy.getHibernateLazyInitializer().getEntityName() ) ) {
						boolean found = isFoundInParent(
								propertyName,
								childEntity,
								persister,
								collectionPersister,
								mergeMap.get( proxy )
						);
						if ( !found ) {
							found = isFoundInParent(
									propertyName,
									mergeMap.get( childEntity ),
									persister,
									collectionPersister,
									mergeMap.get( proxy )
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
			EntityPersister persister,
			CollectionPersister collectionPersister,
			Object potentialParent) {
		Object collection = persister.getPropertyValue( potentialParent, property );
		return collection != null
				&& Hibernate.isInitialized( collection )
				&& collectionPersister.getCollectionType().contains( collection, childEntity, session );
	}

	/**
	 * Search the persistence context for an index of the child object,
	 * given a collection role
	 */
	@Override
	public Object getIndexInOwner(String entity, String property, Object childEntity, Map mergeMap) {

		EntityPersister persister = session.getFactory()
				.getEntityPersister(entity);
		CollectionPersister cp = session.getFactory()
				.getCollectionPersister(entity + '.' + property);

	    // try cache lookup first
	    Object parent = parentsByChild.get(childEntity);
		if (parent != null) {
			final EntityEntry entityEntry = entityEntries.get(parent);
			//there maybe more than one parent, filter by type
			if ( persister.isSubclassEntityName( entityEntry.getEntityName() ) ) {
				Object index = getIndexInParent(property, childEntity, persister, cp, parent);

				if (index==null && mergeMap!=null) {
					Object unmergedInstance = mergeMap.get(parent);
					Object unmergedChild = mergeMap.get(childEntity);
					if ( unmergedInstance!=null && unmergedChild!=null ) {
						index = getIndexInParent(property, unmergedChild, persister, cp, unmergedInstance);
					}
				}
				if (index!=null) {
					return index;
				}
			}
			else {
				parentsByChild.remove(childEntity); // remove wrong entry
			}
		}

		//Not found in cache, proceed
		for ( Entry<Object, EntityEntry> me : IdentityMap.concurrentEntries( entityEntries ) ) {
			EntityEntry ee = me.getValue();
			if ( persister.isSubclassEntityName( ee.getEntityName() ) ) {
				Object instance = me.getKey();

				Object index = getIndexInParent(property, childEntity, persister, cp, instance);

				if (index==null && mergeMap!=null) {
					Object unmergedInstance = mergeMap.get(instance);
					Object unmergedChild = mergeMap.get(childEntity);
					if ( unmergedInstance!=null && unmergedChild!=null ) {
						index = getIndexInParent(property, unmergedChild, persister, cp, unmergedInstance);
					}
				}

				if (index!=null) return index;
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
		Object collection = persister.getPropertyValue( potentialParent, property );
		if ( collection!=null && Hibernate.isInitialized(collection) ) {
			return collectionPersister.getCollectionType().indexOf(collection, childEntity);
		}
		else {
			return null;
		}
	}

	/**
	 * Record the fact that the association belonging to the keyed
	 * entity is null.
	 */
	@Override
	public void addNullProperty(EntityKey ownerKey, String propertyName) {
		nullAssociations.add( new AssociationKey(ownerKey, propertyName) );
	}

	/**
	 * Is the association property belonging to the keyed entity null?
	 */
	@Override
	public boolean isPropertyNull(EntityKey ownerKey, String propertyName) {
		return nullAssociations.contains( new AssociationKey(ownerKey, propertyName) );
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
			isReadOnly = ( ( HibernateProxy ) entityOrProxy ).getHibernateLazyInitializer().isReadOnly();
		}
		else {
			EntityEntry ee =  getEntry( entityOrProxy );
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
			HibernateProxy proxy = ( HibernateProxy ) object;
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
			Object maybeProxy = getSession().getPersistenceContext().proxyFor( object );
			if ( maybeProxy instanceof HibernateProxy ) {
				setProxyReadOnly( ( HibernateProxy ) maybeProxy, readOnly );
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
		EntityEntry entry = getEntry(entity);
		if (entry == null) {
			throw new TransientObjectException("Instance was not associated with this persistence context" );
		}
		entry.setReadOnly(readOnly, entity );
		hasNonReadOnlyEntities = hasNonReadOnlyEntities || ! readOnly;
	}

	@Override
	public void replaceDelayedEntityIdentityInsertKeys(EntityKey oldKey, Serializable generatedId) {
		Object entity = entitiesByKey.remove( oldKey );
		EntityEntry oldEntry = entityEntries.remove( entity );
		parentsByChild.clear();

		final EntityKey newKey = session.generateEntityKey( generatedId, oldEntry.getPersister() );
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
		        oldEntry.getPersister(),
		        oldEntry.isBeingReplicated(),
		        oldEntry.isLoadedWithLazyPropertiesUnfetched()
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
		if ( tracing ) LOG.trace( "Serializing persistent-context" );

		oos.writeBoolean( defaultReadOnly );
		oos.writeBoolean( hasNonReadOnlyEntities );

		oos.writeInt( entitiesByKey.size() );
		if ( tracing ) LOG.trace("Starting serialization of [" + entitiesByKey.size() + "] entitiesByKey entries");
		Iterator itr = entitiesByKey.entrySet().iterator();
		while ( itr.hasNext() ) {
			Map.Entry entry = ( Map.Entry ) itr.next();
			( ( EntityKey ) entry.getKey() ).serialize( oos );
			oos.writeObject( entry.getValue() );
		}

		oos.writeInt( entitiesByUniqueKey.size() );
		if ( tracing ) LOG.trace("Starting serialization of [" + entitiesByUniqueKey.size() + "] entitiesByUniqueKey entries");
		itr = entitiesByUniqueKey.entrySet().iterator();
		while ( itr.hasNext() ) {
			Map.Entry entry = ( Map.Entry ) itr.next();
			( ( EntityUniqueKey ) entry.getKey() ).serialize( oos );
			oos.writeObject( entry.getValue() );
		}

		oos.writeInt( proxiesByKey.size() );
		if ( tracing ) LOG.trace("Starting serialization of [" + proxiesByKey.size() + "] proxiesByKey entries");
		itr = proxiesByKey.entrySet().iterator();
		while ( itr.hasNext() ) {
			Map.Entry entry = ( Map.Entry ) itr.next();
			( (EntityKey) entry.getKey() ).serialize( oos );
			oos.writeObject( entry.getValue() );
		}

		oos.writeInt( entitySnapshotsByKey.size() );
		if ( tracing ) LOG.trace("Starting serialization of [" + entitySnapshotsByKey.size() + "] entitySnapshotsByKey entries");
		itr = entitySnapshotsByKey.entrySet().iterator();
		while ( itr.hasNext() ) {
			Map.Entry entry = ( Map.Entry ) itr.next();
			( ( EntityKey ) entry.getKey() ).serialize( oos );
			oos.writeObject( entry.getValue() );
		}

		oos.writeInt( entityEntries.size() );
		if ( tracing ) LOG.trace("Starting serialization of [" + entityEntries.size() + "] entityEntries entries");
		itr = entityEntries.entrySet().iterator();
		while ( itr.hasNext() ) {
			Map.Entry entry = ( Map.Entry ) itr.next();
			oos.writeObject( entry.getKey() );
			( ( EntityEntry ) entry.getValue() ).serialize( oos );
		}

		oos.writeInt( collectionsByKey.size() );
		if ( tracing ) LOG.trace("Starting serialization of [" + collectionsByKey.size() + "] collectionsByKey entries");
		itr = collectionsByKey.entrySet().iterator();
		while ( itr.hasNext() ) {
			Map.Entry entry = ( Map.Entry ) itr.next();
			( ( CollectionKey ) entry.getKey() ).serialize( oos );
			oos.writeObject( entry.getValue() );
		}

		oos.writeInt( collectionEntries.size() );
		if ( tracing ) LOG.trace("Starting serialization of [" + collectionEntries.size() + "] collectionEntries entries");
		itr = collectionEntries.entrySet().iterator();
		while ( itr.hasNext() ) {
			Map.Entry entry = ( Map.Entry ) itr.next();
			oos.writeObject( entry.getKey() );
			( ( CollectionEntry ) entry.getValue() ).serialize( oos );
		}

		oos.writeInt( arrayHolders.size() );
		if ( tracing ) LOG.trace("Starting serialization of [" + arrayHolders.size() + "] arrayHolders entries");
		itr = arrayHolders.entrySet().iterator();
		while ( itr.hasNext() ) {
			Map.Entry entry = ( Map.Entry ) itr.next();
			oos.writeObject( entry.getKey() );
			oos.writeObject( entry.getValue() );
		}

		oos.writeInt( nullifiableEntityKeys.size() );
		if ( tracing ) LOG.trace("Starting serialization of [" + nullifiableEntityKeys.size() + "] nullifiableEntityKey entries");
		for ( EntityKey entry : nullifiableEntityKeys ) {
			entry.serialize( oos );
		}
	}

	public static StatefulPersistenceContext deserialize(
			ObjectInputStream ois,
			SessionImplementor session) throws IOException, ClassNotFoundException {
		final boolean tracing = LOG.isTraceEnabled();
		if ( tracing ) LOG.trace("Serializing persistent-context");
		StatefulPersistenceContext rtn = new StatefulPersistenceContext( session );

		// during deserialization, we need to reconnect all proxies and
		// collections to this session, as well as the EntityEntry and
		// CollectionEntry instances; these associations are transient
		// because serialization is used for different things.

		try {
			rtn.defaultReadOnly = ois.readBoolean();
			// todo : we can actually just determine this from the incoming EntityEntry-s
			rtn.hasNonReadOnlyEntities = ois.readBoolean();

			int count = ois.readInt();
			if ( tracing ) LOG.trace("Starting deserialization of [" + count + "] entitiesByKey entries");
			rtn.entitiesByKey = new HashMap<EntityKey,Object>( count < INIT_COLL_SIZE ? INIT_COLL_SIZE : count );
			for ( int i = 0; i < count; i++ ) {
				rtn.entitiesByKey.put( EntityKey.deserialize( ois, session ), ois.readObject() );
			}

			count = ois.readInt();
			if ( tracing ) LOG.trace("Starting deserialization of [" + count + "] entitiesByUniqueKey entries");
			rtn.entitiesByUniqueKey = new HashMap<EntityUniqueKey,Object>( count < INIT_COLL_SIZE ? INIT_COLL_SIZE : count );
			for ( int i = 0; i < count; i++ ) {
				rtn.entitiesByUniqueKey.put( EntityUniqueKey.deserialize( ois, session ), ois.readObject() );
			}

			count = ois.readInt();
			if ( tracing ) LOG.trace("Starting deserialization of [" + count + "] proxiesByKey entries");
			//noinspection unchecked
			rtn.proxiesByKey = new ConcurrentReferenceHashMap<EntityKey, Object>(
					count < INIT_COLL_SIZE ? INIT_COLL_SIZE : count,
					.75f,
					1,
					ConcurrentReferenceHashMap.ReferenceType.STRONG,
					ConcurrentReferenceHashMap.ReferenceType.WEAK,
					null
			);
			for ( int i = 0; i < count; i++ ) {
				EntityKey ek = EntityKey.deserialize( ois, session );
				Object proxy = ois.readObject();
				if ( proxy instanceof HibernateProxy ) {
					( ( HibernateProxy ) proxy ).getHibernateLazyInitializer().setSession( session );
					rtn.proxiesByKey.put( ek, proxy );
				} else {
					if ( tracing ) LOG.trace("Encountered prunded proxy");
				}
				// otherwise, the proxy was pruned during the serialization process
			}

			count = ois.readInt();
			if ( tracing ) LOG.trace("Starting deserialization of [" + count + "] entitySnapshotsByKey entries");
			rtn.entitySnapshotsByKey = new HashMap<EntityKey,Object>( count < INIT_COLL_SIZE ? INIT_COLL_SIZE : count );
			for ( int i = 0; i < count; i++ ) {
				rtn.entitySnapshotsByKey.put( EntityKey.deserialize( ois, session ), ois.readObject() );
			}

			count = ois.readInt();
			if ( tracing ) LOG.trace("Starting deserialization of [" + count + "] entityEntries entries");
			rtn.entityEntries = IdentityMap.instantiateSequenced( count < INIT_COLL_SIZE ? INIT_COLL_SIZE : count );
			for ( int i = 0; i < count; i++ ) {
				Object entity = ois.readObject();
				EntityEntry entry = EntityEntry.deserialize( ois, rtn );
				rtn.entityEntries.put( entity, entry );
			}

			count = ois.readInt();
			if ( tracing ) LOG.trace("Starting deserialization of [" + count + "] collectionsByKey entries");
			rtn.collectionsByKey = new HashMap<CollectionKey,PersistentCollection>( count < INIT_COLL_SIZE ? INIT_COLL_SIZE : count );
			for ( int i = 0; i < count; i++ ) {
				rtn.collectionsByKey.put( CollectionKey.deserialize( ois, session ), (PersistentCollection) ois.readObject() );
			}

			count = ois.readInt();
			if ( tracing ) LOG.trace("Starting deserialization of [" + count + "] collectionEntries entries");
			rtn.collectionEntries = IdentityMap.instantiateSequenced( count < INIT_COLL_SIZE ? INIT_COLL_SIZE : count );
			for ( int i = 0; i < count; i++ ) {
				final PersistentCollection pc = ( PersistentCollection ) ois.readObject();
				final CollectionEntry ce = CollectionEntry.deserialize( ois, session );
				pc.setCurrentSession( session );
				rtn.collectionEntries.put( pc, ce );
			}

			count = ois.readInt();
			if ( tracing ) LOG.trace("Starting deserialization of [" + count + "] arrayHolders entries");
			rtn.arrayHolders = new IdentityHashMap<Object, PersistentCollection>( count < INIT_COLL_SIZE ? INIT_COLL_SIZE : count );
			for ( int i = 0; i < count; i++ ) {
				rtn.arrayHolders.put( ois.readObject(), (PersistentCollection) ois.readObject() );
			}

			count = ois.readInt();
			if ( tracing ) LOG.trace("Starting deserialization of [" + count + "] nullifiableEntityKey entries");
			rtn.nullifiableEntityKeys = new HashSet<EntityKey>();
			for ( int i = 0; i < count; i++ ) {
				rtn.nullifiableEntityKeys.add( EntityKey.deserialize( ois, session ) );
			}

		}
		catch ( HibernateException he ) {
			throw new InvalidObjectException( he.getMessage() );
		}

		return rtn;
	}

	@Override
	public void addChildParent(Object child, Object parent) {
		parentsByChild.put(child, parent);
	}

	@Override
	public void removeChildParent(Object child) {
	   parentsByChild.remove(child);
	}


	// INSERTED KEYS HANDLING ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private HashMap<String,List<Serializable>> insertedKeysMap;

	@Override
	public void registerInsertedKey(EntityPersister persister, Serializable id) {
		// we only are worried about registering these if the persister defines caching
		if ( persister.hasCache() ) {
			if ( insertedKeysMap == null ) {
				insertedKeysMap = new HashMap<String, List<Serializable>>();
			}
			final String rootEntityName = persister.getRootEntityName();
			List<Serializable> insertedEntityIds = insertedKeysMap.get( rootEntityName );
			if ( insertedEntityIds == null ) {
				insertedEntityIds = new ArrayList<Serializable>();
				insertedKeysMap.put( rootEntityName, insertedEntityIds );
			}
			insertedEntityIds.add( id );
		}
	}

	@Override
	public boolean wasInsertedDuringTransaction(EntityPersister persister, Serializable id) {
		// again, we only really care if the entity is cached
		if ( persister.hasCache() ) {
			if ( insertedKeysMap != null ) {
				List<Serializable> insertedEntityIds = insertedKeysMap.get( persister.getRootEntityName() );
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
				EntityPersister persister,
				Serializable id,
				Object[] naturalIdValues) {
			if ( !persister.hasNaturalIdentifier() ) {
				// nothing to do
				return;
			}

			persister = locateProperPersister( persister );

			// 'justAddedLocally' is meant to handle the case where we would get double stats jounaling
			//	from a single load event.  The first put journal would come from the natural id resolution;
			// the second comes from the entity loading.  In this condition, we want to avoid the multiple
			// 'put' stats incrementing.
			boolean justAddedLocally = naturalIdXrefDelegate.cacheNaturalIdCrossReference( persister, id, naturalIdValues );

			if ( justAddedLocally && persister.hasNaturalIdCache() ) {
				managedSharedCacheEntries( persister, id, naturalIdValues, null, CachedNaturalIdValueSource.LOAD );
			}
		}

		@Override
		public void manageLocalNaturalIdCrossReference(
				EntityPersister persister,
				Serializable id,
				Object[] state,
				Object[] previousState,
				CachedNaturalIdValueSource source) {
			if ( !persister.hasNaturalIdentifier() ) {
				// nothing to do
				return;
			}

			persister = locateProperPersister( persister );
			final Object[] naturalIdValues = extractNaturalIdValues( state, persister );

			// cache
			naturalIdXrefDelegate.cacheNaturalIdCrossReference( persister, id, naturalIdValues );
		}

		@Override
		public void manageSharedNaturalIdCrossReference(
				EntityPersister persister,
				final Serializable id,
				Object[] state,
				Object[] previousState,
				CachedNaturalIdValueSource source) {
			if ( !persister.hasNaturalIdentifier() ) {
				// nothing to do
				return;
			}

			if ( !persister.hasNaturalIdCache() ) {
				// nothing to do
				return;
			}

			persister = locateProperPersister( persister );
			final Object[] naturalIdValues = extractNaturalIdValues( state, persister );
			final Object[] previousNaturalIdValues = previousState == null ? null : extractNaturalIdValues( previousState, persister );

			managedSharedCacheEntries( persister, id, naturalIdValues, previousNaturalIdValues, source );
		}

		private void managedSharedCacheEntries(
				EntityPersister persister,
				final Serializable id,
				Object[] naturalIdValues,
				Object[] previousNaturalIdValues,
				CachedNaturalIdValueSource source) {
			final NaturalIdRegionAccessStrategy naturalIdCacheAccessStrategy = persister.getNaturalIdCacheAccessStrategy();
			final NaturalIdCacheKey naturalIdCacheKey = new NaturalIdCacheKey( naturalIdValues, persister, session );

			final SessionFactoryImplementor factory = session.getFactory();

			switch ( source ) {
				case LOAD: {
					if (naturalIdCacheAccessStrategy.get(naturalIdCacheKey, session.getTimestamp()) != null) {
						return; // prevent identical re-cachings
					}
					final boolean put = naturalIdCacheAccessStrategy.putFromLoad(
							naturalIdCacheKey,
							id,
							session.getTimestamp(),
							null
					);

					if ( put && factory.getStatistics().isStatisticsEnabled() ) {
						factory.getStatisticsImplementor().naturalIdCachePut(
								naturalIdCacheAccessStrategy.getRegion()
										.getName()
						);
					}

					break;
				}
				case INSERT: {
					final boolean put = naturalIdCacheAccessStrategy.insert( naturalIdCacheKey, id );
					if ( put && factory.getStatistics().isStatisticsEnabled() ) {
						factory.getStatisticsImplementor()
								.naturalIdCachePut( naturalIdCacheAccessStrategy.getRegion().getName() );
					}

					( (EventSource) session ).getActionQueue().registerProcess(
							new AfterTransactionCompletionProcess() {
								@Override
								public void doAfterTransactionCompletion(boolean success, SessionImplementor session) {
									if (success) {
										final boolean put = naturalIdCacheAccessStrategy.afterInsert( naturalIdCacheKey, id );

										if ( put && factory.getStatistics().isStatisticsEnabled() ) {
											factory.getStatisticsImplementor()
												.naturalIdCachePut( naturalIdCacheAccessStrategy.getRegion().getName() );
										}
									}
									else {
										naturalIdCacheAccessStrategy.remove(naturalIdCacheKey);
									}
								}
							}
					);

					break;
				}
				case UPDATE: {
					final NaturalIdCacheKey previousCacheKey = new NaturalIdCacheKey( previousNaturalIdValues, persister, session );
					if (naturalIdCacheKey.equals(previousCacheKey)) {
						return; // prevent identical re-caching, solves HHH-7309
					}
					final SoftLock removalLock = naturalIdCacheAccessStrategy.lockItem( previousCacheKey, null );
					naturalIdCacheAccessStrategy.remove( previousCacheKey );

					final SoftLock lock = naturalIdCacheAccessStrategy.lockItem( naturalIdCacheKey, null );
					final boolean put = naturalIdCacheAccessStrategy.update( naturalIdCacheKey, id );
					if ( put && factory.getStatistics().isStatisticsEnabled() ) {
						factory.getStatisticsImplementor()
								.naturalIdCachePut( naturalIdCacheAccessStrategy.getRegion().getName() );
					}

					( (EventSource) session ).getActionQueue().registerProcess(
							new AfterTransactionCompletionProcess() {
								@Override
								public void doAfterTransactionCompletion(boolean success, SessionImplementor session) {
									naturalIdCacheAccessStrategy.unlockItem( previousCacheKey, removalLock );
									if (success) {
										final boolean put = naturalIdCacheAccessStrategy.afterUpdate( naturalIdCacheKey, id, lock );

										if ( put && factory.getStatistics().isStatisticsEnabled() ) {
											factory.getStatisticsImplementor()
												.naturalIdCachePut( naturalIdCacheAccessStrategy.getRegion().getName() );
										}
									}
									else {
										naturalIdCacheAccessStrategy.unlockItem( naturalIdCacheKey, lock );
									}
								}
							}
					);

					break;
				}
			}
		}

		@Override
		public Object[] removeLocalNaturalIdCrossReference(EntityPersister persister, Serializable id, Object[] state) {
			if ( !persister.hasNaturalIdentifier() ) {
				// nothing to do
				return null;
			}

			persister = locateProperPersister( persister );
			final Object[] naturalIdValues = getNaturalIdValues( state, persister );

			final Object[] localNaturalIdValues = naturalIdXrefDelegate.removeNaturalIdCrossReference( 
					persister, 
					id, 
					naturalIdValues 
			);

			return localNaturalIdValues != null ? localNaturalIdValues : naturalIdValues;
		}

		@Override
		public void removeSharedNaturalIdCrossReference(EntityPersister persister, Serializable id, Object[] naturalIdValues) {
			if ( !persister.hasNaturalIdentifier() ) {
				// nothing to do
				return;
			}

			if ( ! persister.hasNaturalIdCache() ) {
				// nothing to do
				return;
			}

			// todo : couple of things wrong here:
			//		1) should be using access strategy, not plain evict..
			//		2) should prefer session-cached values if any (requires interaction from removeLocalNaturalIdCrossReference

			persister = locateProperPersister( persister );
			final NaturalIdRegionAccessStrategy naturalIdCacheAccessStrategy = persister.getNaturalIdCacheAccessStrategy();
			final NaturalIdCacheKey naturalIdCacheKey = new NaturalIdCacheKey( naturalIdValues, persister, session );
			naturalIdCacheAccessStrategy.evict( naturalIdCacheKey );

//			if ( sessionCachedNaturalIdValues != null
//					&& !Arrays.equals( sessionCachedNaturalIdValues, deletedNaturalIdValues ) ) {
//				final NaturalIdCacheKey sessionNaturalIdCacheKey = new NaturalIdCacheKey( sessionCachedNaturalIdValues, persister, session );
//				naturalIdCacheAccessStrategy.evict( sessionNaturalIdCacheKey );
//			}
		}

		@Override
		public Object[] findCachedNaturalId(EntityPersister persister, Serializable pk) {
			return naturalIdXrefDelegate.findCachedNaturalId( locateProperPersister( persister ), pk );
		}

		@Override
		public Serializable findCachedNaturalIdResolution(EntityPersister persister, Object[] naturalIdValues) {
			return naturalIdXrefDelegate.findCachedNaturalIdResolution( locateProperPersister( persister ), naturalIdValues );
		}

		@Override
		public Object[] extractNaturalIdValues(Object[] state, EntityPersister persister) {
			final int[] naturalIdPropertyIndexes = persister.getNaturalIdentifierProperties();
			if ( state.length == naturalIdPropertyIndexes.length ) {
				return state;
			}

			final Object[] naturalIdValues = new Object[naturalIdPropertyIndexes.length];
			for ( int i = 0; i < naturalIdPropertyIndexes.length; i++ ) {
				naturalIdValues[i] = state[naturalIdPropertyIndexes[i]];
			}
			return naturalIdValues;
		}

		@Override
		public Object[] extractNaturalIdValues(Object entity, EntityPersister persister) {
			if ( entity == null ) {
				throw new AssertionFailure( "Entity from which to extract natural id value(s) cannot be null" );
			}
			if ( persister == null ) {
				throw new AssertionFailure( "Persister to use in extracting natural id value(s) cannot be null" );
			}

			final int[] naturalIdentifierProperties = persister.getNaturalIdentifierProperties();
			final Object[] naturalIdValues = new Object[naturalIdentifierProperties.length];

			for ( int i = 0; i < naturalIdentifierProperties.length; i++ ) {
				naturalIdValues[i] = persister.getPropertyValue( entity, naturalIdentifierProperties[i] );
			}

			return naturalIdValues;
		}

		@Override
		public Collection<Serializable> getCachedPkResolutions(EntityPersister entityPersister) {
			return naturalIdXrefDelegate.getCachedPkResolutions( entityPersister );
		}

		@Override
		public void handleSynchronization(EntityPersister persister, Serializable pk, Object entity) {
			if ( !persister.hasNaturalIdentifier() ) {
				// nothing to do
				return;
			}

			persister = locateProperPersister( persister );

			final Object[] naturalIdValuesFromCurrentObjectState = extractNaturalIdValues( entity, persister );
			final boolean changed = ! naturalIdXrefDelegate.sameAsCached(
					persister,
					pk,
					naturalIdValuesFromCurrentObjectState
			);

			if ( changed ) {
				final Object[] cachedNaturalIdValues = naturalIdXrefDelegate.findCachedNaturalId( persister, pk );
				naturalIdXrefDelegate.cacheNaturalIdCrossReference( persister, pk, naturalIdValuesFromCurrentObjectState );
				naturalIdXrefDelegate.stashInvalidNaturalIdReference( persister, cachedNaturalIdValues );

				removeSharedNaturalIdCrossReference(
						persister,
						pk,
						cachedNaturalIdValues
				);
			}
		}

		@Override
		public void cleanupFromSynchronizations() {
			naturalIdXrefDelegate.unStashInvalidNaturalIdReferences();
		}
	};

	@Override
	public NaturalIdHelper getNaturalIdHelper() {
		return naturalIdHelper;
	}

	private Object[] getNaturalIdValues(Object[] state, EntityPersister persister) {
		final int[] naturalIdPropertyIndexes = persister.getNaturalIdentifierProperties();
		final Object[] naturalIdValues = new Object[naturalIdPropertyIndexes.length];

		for ( int i = 0; i < naturalIdPropertyIndexes.length; i++ ) {
			naturalIdValues[i] = state[naturalIdPropertyIndexes[i]];
		}

		return naturalIdValues;
	}
}
