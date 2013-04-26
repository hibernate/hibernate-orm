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
package org.hibernate.engine.spi;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.loading.internal.LoadContexts;
import org.hibernate.internal.util.MarkerObject;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Represents the state of "stuff" Hibernate is tracking, including (not exhaustive):
 * <ul>
 *     <li>entities</li>
 *     <li>collections</li>
 *     <li>snapshots</li>
 *     <li>proxies</li>
 * </ul>
 * <p/>
 * Often referred to as the "first level cache".
 * 
 * @author Gavin King
 * @author Steve Ebersole
 */
@SuppressWarnings( {"JavaDoc"})
public interface PersistenceContext {
	/**
	 * Marker object used to indicate (via reference checking) that no row was returned.
	 */
	public static final Object NO_ROW = new MarkerObject( "NO_ROW" );

	@SuppressWarnings( {"UnusedDeclaration"})
	public boolean isStateless();

	/**
	 * Get the session to which this persistence context is bound.
	 *
	 * @return The session.
	 */
	public SessionImplementor getSession();

	/**
	 * Retrieve this persistence context's managed load context.
	 *
	 * @return The load context
	 */
	public LoadContexts getLoadContexts();

	/**
	 * Add a collection which has no owner loaded
	 *
	 * @param key The collection key under which to add the collection
	 * @param collection The collection to add
	 */
	public void addUnownedCollection(CollectionKey key, PersistentCollection collection);

	/**
	 * Take ownership of a previously unowned collection, if one.  This method returns {@code null} if no such
	 * collection was previous added () or was previously removed.
	 * <p/>
	 * This should indicate the owner is being loaded and we are ready to "link" them.
	 *
	 * @param key The collection key for which to locate a collection collection
	 *
	 * @return The unowned collection, or {@code null}
	 */
	public PersistentCollection useUnownedCollection(CollectionKey key);

	/**
	 * Get the {@link BatchFetchQueue}, instantiating one if necessary.
	 *
	 * @return The batch fetch queue in effect for this persistence context
	 */
	public BatchFetchQueue getBatchFetchQueue();
	
	/**
	 * Clear the state of the persistence context
	 */
	public void clear();

	/**
	 * @return false if we know for certain that all the entities are read-only
	 */
	@SuppressWarnings( {"UnusedDeclaration"})
	public boolean hasNonReadOnlyEntities();

	/**
	 * Set the status of an entry
	 *
	 * @param entry The entry for which to set the status
	 * @param status The new status
	 */
	public void setEntryStatus(EntityEntry entry, Status status);

	/**
	 * Called after transactions end
	 */
	public void afterTransactionCompletion();

	/**
	 * Get the current state of the entity as known to the underlying database, or null if there is no
	 * corresponding row
	 *
	 * @param id The identifier of the entity for which to grab a snapshot
	 * @param persister The persister of the entity.
	 *
	 * @return The entity's (non-cached) snapshot
	 *
	 * @see #getCachedDatabaseSnapshot
	 */
	public Object[] getDatabaseSnapshot(Serializable id, EntityPersister persister);

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
	public Object[] getCachedDatabaseSnapshot(EntityKey key);

	/**
	 * Get the values of the natural id fields as known to the underlying database, or null if the entity has no
	 * natural id or there is no corresponding row.
	 *
	 * @param id The identifier of the entity for which to grab a snapshot
	 * @param persister The persister of the entity.
	 *
	 * @return The current (non-cached) snapshot of the entity's natural id state.
	 */
	public Object[] getNaturalIdSnapshot(Serializable id, EntityPersister persister);

	/**
	 * Add a canonical mapping from entity key to entity instance
	 *
	 * @param key The key under which to add an entity
	 * @param entity The entity instance to add
	 */
	public void addEntity(EntityKey key, Object entity);

	/**
	 * Get the entity instance associated with the given key
	 *
	 * @param key The key under which to look for an entity
	 *
	 * @return The matching entity, or {@code null}
	 */
	public Object getEntity(EntityKey key);

	/**
	 * Is there an entity with the given key in the persistence context
	 *
	 * @param key The key under which to look for an entity
	 *
	 * @return {@code true} indicates an entity was found; otherwise {@code false}
	 */
	public boolean containsEntity(EntityKey key);

	/**
	 * Remove an entity.  Also clears up all other state associated with the entity aside from the {@link EntityEntry}
	 *
	 * @param key The key whose matching entity should be removed
	 *
	 * @return The matching entity
	 */
	public Object removeEntity(EntityKey key);

	/**
	 * Add an entity to the cache by unique key
	 *
	 * @param euk The unique (non-primary) key under which to add an entity
	 * @param entity The entity instance
	 */
	public void addEntity(EntityUniqueKey euk, Object entity);

	/**
	 * Get an entity cached by unique key
	 *
	 * @param euk The unique (non-primary) key under which to look for an entity
	 *
	 * @return The located entity
	 */
	public Object getEntity(EntityUniqueKey euk);

	/**
	 * Retrieve the {@link EntityEntry} representation of the given entity.
	 *
	 * @param entity The entity instance for which to locate the corresponding entry
	 * @return The entry
	 */
	public EntityEntry getEntry(Object entity);

	/**
	 * Remove an entity entry from the session cache
	 *
	 * @param entity The entity instance for which to remove the corresponding entry
	 * @return The matching entry
	 */
	public EntityEntry removeEntry(Object entity);

	/**
	 * Is there an {@link EntityEntry} registration for this entity instance?
	 *
	 * @param entity The entity instance for which to check for an entry
	 *
	 * @return {@code true} indicates a matching entry was found.
	 */
	public boolean isEntryFor(Object entity);

	/**
	 * Get the collection entry for a persistent collection
	 *
	 * @param coll The persistent collection instance for which to locate the collection entry
	 *
	 * @return The matching collection entry
	 */
	public CollectionEntry getCollectionEntry(PersistentCollection coll);

	/**
	 * Adds an entity to the internal caches.
	 */
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
			boolean lazyPropertiesAreUnfetched);

	/**
	 * Generates an appropriate EntityEntry instance and adds it 
	 * to the event source's internal caches.
	 */
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
			boolean lazyPropertiesAreUnfetched);

	/**
	 * Is the given collection associated with this persistence context?
	 */
	public boolean containsCollection(PersistentCollection collection);
	
	/**
	 * Is the given proxy associated with this persistence context?
	 */
	public boolean containsProxy(Object proxy);

	/**
	 * Takes the given object and, if it represents a proxy, reassociates it with this event source.
	 *
	 * @param value The possible proxy to be reassociated.
	 * @return Whether the passed value represented an actual proxy which got initialized.
	 * @throws MappingException
	 */
	public boolean reassociateIfUninitializedProxy(Object value) throws MappingException;

	/**
	 * If a deleted entity instance is re-saved, and it has a proxy, we need to
	 * reset the identifier of the proxy 
	 */
	public void reassociateProxy(Object value, Serializable id) throws MappingException;

	/**
	 * Get the entity instance underlying the given proxy, throwing
	 * an exception if the proxy is uninitialized. If the given object
	 * is not a proxy, simply return the argument.
	 */
	public Object unproxy(Object maybeProxy) throws HibernateException;

	/**
	 * Possibly unproxy the given reference and reassociate it with the current session.
	 *
	 * @param maybeProxy The reference to be unproxied if it currently represents a proxy.
	 * @return The unproxied instance.
	 * @throws HibernateException
	 */
	public Object unproxyAndReassociate(Object maybeProxy) throws HibernateException;

	/**
	 * Attempts to check whether the given key represents an entity already loaded within the
	 * current session.
	 *
	 * @param object The entity reference against which to perform the uniqueness check.
	 *
	 * @throws HibernateException
	 */
	public void checkUniqueness(EntityKey key, Object object) throws HibernateException;

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
	public Object narrowProxy(Object proxy, EntityPersister persister, EntityKey key, Object object)
			throws HibernateException;

	/**
	 * Return the existing proxy associated with the given <tt>EntityKey</tt>, or the
	 * third argument (the entity associated with the key) if no proxy exists. Init
	 * the proxy to the target implementation, if necessary.
	 */
	public Object proxyFor(EntityPersister persister, EntityKey key, Object impl)
			throws HibernateException;

	/**
	 * Return the existing proxy associated with the given <tt>EntityKey</tt>, or the
	 * argument (the entity associated with the key) if no proxy exists.
	 * (slower than the form above)
	 */
	public Object proxyFor(Object impl) throws HibernateException;

	/**
	 * Get the entity that owns this persistent collection
	 */
	public Object getCollectionOwner(Serializable key, CollectionPersister collectionPersister)
			throws MappingException;

	/**
	 * Get the entity that owned this persistent collection when it was loaded
	 *
	 * @param collection The persistent collection
	 * @return the owner if its entity ID is available from the collection's loaded key
	 * and the owner entity is in the persistence context; otherwise, returns null
	 */
	Object getLoadedCollectionOwnerOrNull(PersistentCollection collection);

	/**
	 * Get the ID for the entity that owned this persistent collection when it was loaded
	 *
	 * @param collection The persistent collection
	 * @return the owner ID if available from the collection's loaded key; otherwise, returns null
	 */
	public Serializable getLoadedCollectionOwnerIdOrNull(PersistentCollection collection);

	/**
	 * add a collection we just loaded up (still needs initializing)
	 */
	public void addUninitializedCollection(CollectionPersister persister,
			PersistentCollection collection, Serializable id);

	/**
	 * add a detached uninitialized collection
	 */
	public void addUninitializedDetachedCollection(CollectionPersister persister,
			PersistentCollection collection);

	/**
	 * Add a new collection (ie. a newly created one, just instantiated by the
	 * application, with no database state or snapshot)
	 * @param collection The collection to be associated with the persistence context
	 */
	public void addNewCollection(CollectionPersister persister, PersistentCollection collection)
			throws HibernateException;

	/**
	 * add an (initialized) collection that was created by another session and passed
	 * into update() (ie. one with a snapshot and existing state on the database)
	 */
	public void addInitializedDetachedCollection(CollectionPersister collectionPersister,
			PersistentCollection collection) throws HibernateException;

	/**
	 * add a collection we just pulled out of the cache (does not need initializing)
	 */
	public CollectionEntry addInitializedCollection(CollectionPersister persister,
			PersistentCollection collection, Serializable id) throws HibernateException;

	/**
	 * Get the collection instance associated with the <tt>CollectionKey</tt>
	 */
	public PersistentCollection getCollection(CollectionKey collectionKey);

	/**
	 * Register a collection for non-lazy loading at the end of the
	 * two-phase load
	 */
	public void addNonLazyCollection(PersistentCollection collection);

	/**
	 * Force initialization of all non-lazy collections encountered during
	 * the current two-phase load (actually, this is a no-op, unless this
	 * is the "outermost" load)
	 */
	public void initializeNonLazyCollections() throws HibernateException;

	/**
	 * Get the <tt>PersistentCollection</tt> object for an array
	 */
	public PersistentCollection getCollectionHolder(Object array);

	/**
	 * Register a <tt>PersistentCollection</tt> object for an array.
	 * Associates a holder with an array - MUST be called after loading 
	 * array, since the array instance is not created until endLoad().
	 */
	public void addCollectionHolder(PersistentCollection holder);
	
	/**
	 * Remove the mapping of collection to holder during eviction
	 * of the owning entity
	 */
	public PersistentCollection removeCollectionHolder(Object array);

	/**
	 * Get the snapshot of the pre-flush collection state
	 */
	public Serializable getSnapshot(PersistentCollection coll);

	/**
	 * Get the collection entry for a collection passed to filter,
	 * which might be a collection wrapper, an array, or an unwrapped
	 * collection. Return null if there is no entry.
	 */
	public CollectionEntry getCollectionEntryOrNull(Object collection);

	/**
	 * Get an existing proxy by key
	 */
	public Object getProxy(EntityKey key);

	/**
	 * Add a proxy to the session cache
	 */
	public void addProxy(EntityKey key, Object proxy);

	/**
	 * Remove a proxy from the session cache.
	 * <p/>
	 * Additionally, ensure that any load optimization references
	 * such as batch or subselect loading get cleaned up as well.
	 *
	 * @param key The key of the entity proxy to be removed
	 * @return The proxy reference.
	 */
	public Object removeProxy(EntityKey key);

	/** 
	 * Retrieve the set of EntityKeys representing nullifiable references
	 */
	public HashSet getNullifiableEntityKeys();

	/**
	 * Get the mapping from key value to entity instance
	 */
	public Map getEntitiesByKey();

	/**
	 * Provides access to the entity/EntityEntry combos associated with the persistence context in a manner that
	 * is safe from reentrant access.  Specifically, it is safe from additions/removals while iterating.
	 *
	 * @return
	 */
	public Map.Entry<Object,EntityEntry>[] reentrantSafeEntityEntries();

	/**
	 * Get the mapping from entity instance to entity entry
	 *
	 * @deprecated Due to the introduction of EntityEntryContext and bytecode enhancement; only valid really for
	 * sizing, see {@link #getNumberOfManagedEntities}.  For iterating the entity/EntityEntry combos, see
	 * {@link #reentrantSafeEntityEntries}
	 */
	@Deprecated
	public Map getEntityEntries();

	public int getNumberOfManagedEntities();

	/**
	 * Get the mapping from collection instance to collection entry
	 */
	public Map getCollectionEntries();

	/**
	 * Get the mapping from collection key to collection instance
	 */
	public Map getCollectionsByKey();

	/**
	 * How deep are we cascaded?
	 */
	public int getCascadeLevel();
	
	/**
	 * Called before cascading
	 */
	public int incrementCascadeLevel();

	/**
	 * Called after cascading
	 */
	public int decrementCascadeLevel();

	/**
	 * Is a flush cycle currently in process?
	 */
	@SuppressWarnings( {"UnusedDeclaration"})
	public boolean isFlushing();
	
	/**
	 * Called before and after the flushcycle
	 */
	public void setFlushing(boolean flushing);

	/**
	 * Call this before begining a two-phase load
	 */
	public void beforeLoad();

	/**
	 * Call this after finishing a two-phase load
	 */
	public void afterLoad();
	
	/**
	 * Is in a two-phase load? 
	 */
	public boolean isLoadFinished();
	/**
	 * Returns a string representation of the object.
	 *
	 * @return a string representation of the object.
	 */
	public String toString();

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
	public Serializable getOwnerId(String entityName, String propertyName, Object childEntity, Map mergeMap);

	/**
	 * Search the persistence context for an index of the child object,
	 * given a collection role
	 */
	public Object getIndexInOwner(String entity, String property, Object childObject, Map mergeMap);

	/**
	 * Record the fact that the association belonging to the keyed
	 * entity is null.
	 */
	public void addNullProperty(EntityKey ownerKey, String propertyName);

	/**
	 * Is the association property belonging to the keyed entity null?
	 */
	public boolean isPropertyNull(EntityKey ownerKey, String propertyName);

	/**
	 * Will entities and proxies that are loaded into this persistence
	 * context be made read-only by default?
	 *
	 * To determine the read-only/modifiable setting for a particular entity
	 * or proxy:
	 * @see PersistenceContext#isReadOnly(Object)
	 * @see org.hibernate.Session#isReadOnly(Object) 
	 *
	 * @return true, loaded entities/proxies will be made read-only by default;
	 *         false, loaded entities/proxies will be made modifiable by default.
	 *
	 * @see org.hibernate.Session#isDefaultReadOnly() 
	 */
	public boolean isDefaultReadOnly();

	/**
	 * Change the default for entities and proxies loaded into this persistence
	 * context from modifiable to read-only mode, or from modifiable to read-only
	 * mode.
	 *
	 * Read-only entities are not dirty-checked and snapshots of persistent
	 * state are not maintained. Read-only entities can be modified, but
	 * changes are not persisted.
	 *
	 * When a proxy is initialized, the loaded entity will have the same
	 * read-only/modifiable setting as the uninitialized
	 * proxy has, regardless of the persistence context's current setting.
	 *
	 * To change the read-only/modifiable setting for a particular entity
	 * or proxy that is already in this session:
+	 * @see PersistenceContext#setReadOnly(Object,boolean)
	 * @see org.hibernate.Session#setReadOnly(Object, boolean)
	 *
	 * To override this session's read-only/modifiable setting for entities
	 * and proxies loaded by a Query:
	 * @see org.hibernate.Query#setReadOnly(boolean)
	 *
	 * @param readOnly true, the default for loaded entities/proxies is read-only;
	 *                 false, the default for loaded entities/proxies is modifiable
	 *
	 * @see org.hibernate.Session#setDefaultReadOnly(boolean)
	 */
	public void setDefaultReadOnly(boolean readOnly);

	/**
	 * Is the entity or proxy read-only?
	 * <p/>
	 * To determine the default read-only/modifiable setting used for entities and proxies that are loaded into the
	 * session use {@link org.hibernate.Session#isDefaultReadOnly}
	 *
	 * @param entityOrProxy an entity or proxy
	 *
	 * @return {@code true} if the object is read-only; otherwise {@code false} to indicate that the object is
	 * modifiable.
	 */
	public boolean isReadOnly(Object entityOrProxy);

	/**
	 * Set an unmodified persistent object to read-only mode, or a read-only
	 * object to modifiable mode.
	 *
	 * Read-only entities are not dirty-checked and snapshots of persistent
	 * state are not maintained. Read-only entities can be modified, but
	 * changes are not persisted.
	 *
	 * When a proxy is initialized, the loaded entity will have the same
	 * read-only/modifiable setting as the uninitialized
	 * proxy has, regardless of the session's current setting.
	 *
	 * If the entity or proxy already has the specified read-only/modifiable
	 * setting, then this method does nothing.
	 *
	 * @param entityOrProxy an entity or proxy
	 * @param readOnly if {@code true}, the entity or proxy is made read-only; otherwise, the entity or proxy is made
	 * modifiable.
	 *
	 * @see org.hibernate.Session#setDefaultReadOnly
	 * @see org.hibernate.Session#setReadOnly
	 * @see org.hibernate.Query#setReadOnly
	 */
	public void setReadOnly(Object entityOrProxy, boolean readOnly);

	void replaceDelayedEntityIdentityInsertKeys(EntityKey oldKey, Serializable generatedId);

	/**
	 * Add a child/parent relation to cache for cascading op
	 *
	 * @param child The child of the relationship
	 * @param parent The parent of the relationship
	 */
	public void addChildParent(Object child, Object parent);

	/**
	 * Remove child/parent relation from cache
	 *
	 * @param child The child to be removed.
	 */
	public void removeChildParent(Object child);

	/**
	 * Register keys inserted during the current transaction
	 *
	 * @param persister The entity persister
	 * @param id The id
	 */
	public void registerInsertedKey(EntityPersister persister, Serializable id);

	/**
	 * Allows callers to check to see if the identified entity was inserted during the current transaction.
	 *
	 * @param persister The entity persister
	 * @param id The id
	 *
	 * @return True if inserted during this transaction, false otherwise.
	 */
	public boolean wasInsertedDuringTransaction(EntityPersister persister, Serializable id);

	/**
	 * Provides centralized access to natural-id-related functionality.
	 */
	public static interface NaturalIdHelper {
		public static final Serializable INVALID_NATURAL_ID_REFERENCE = new Serializable() {};

		/**
		 * Given an array of "full entity state", extract the portions that represent the natural id
		 * 
		 * @param state The attribute state array
		 * @param persister The persister representing the entity type.
		 * 
		 * @return The extracted natural id values
		 */
		public Object[] extractNaturalIdValues(Object[] state, EntityPersister persister);

		/**
		 * Given an entity instance, extract the values that represent the natural id
		 *
		 * @param entity The entity instance
		 * @param persister The persister representing the entity type.
		 *
		 * @return The extracted natural id values
		 */
		public Object[] extractNaturalIdValues(Object entity, EntityPersister persister);

		/**
		 * Performs processing related to creating natural-id cross-reference entries on load.
		 * Handles both the local (transactional) and shared (second-level) caches.
		 *
		 * @param persister The persister representing the entity type.
		 * @param id The primary key value
		 * @param naturalIdValues The natural id values
		 */
		public void cacheNaturalIdCrossReferenceFromLoad(
				EntityPersister persister, 
				Serializable id, 
				Object[] naturalIdValues);

		/**
		 * Creates necessary local cross-reference entries.
		 *
		 * @param persister The persister representing the entity type.
		 * @param id The primary key value
		 * @param state Generally the "full entity state array", though could also be the natural id values array
		 * @param previousState Generally the "full entity state array", though could also be the natural id values array.  
		 * 		Specifically represents the previous values on update, and so is only used with {@link CachedNaturalIdValueSource#UPDATE}
		 * @param source Enumeration representing how these values are coming into cache.
		 */
		public void manageLocalNaturalIdCrossReference(
				EntityPersister persister,
				Serializable id,
				Object[] state,
				Object[] previousState,
				CachedNaturalIdValueSource source);

		/**
		 * Cleans up local cross-reference entries.
		 * 
		 * @param persister The persister representing the entity type.
		 * @param id The primary key value
		 * @param state Generally the "full entity state array", though could also be the natural id values array
		 * 
		 * @return The local cached natural id values (could be different from given values).
		 */
		public Object[] removeLocalNaturalIdCrossReference(EntityPersister persister, Serializable id, Object[] state);

		/**
		 * Creates necessary shared (second level cache) cross-reference entries.
		 *
		 * @param persister The persister representing the entity type.
		 * @param id The primary key value
		 * @param state Generally the "full entity state array", though could also be the natural id values array
		 * @param previousState Generally the "full entity state array", though could also be the natural id values array.  
		 * 		Specifically represents the previous values on update, and so is only used with {@link CachedNaturalIdValueSource#UPDATE}
		 * @param source Enumeration representing how these values are coming into cache.
		 */
		public void manageSharedNaturalIdCrossReference(
				EntityPersister persister,
				Serializable id,
				Object[] state,
				Object[] previousState,
				CachedNaturalIdValueSource source);

		/**
		 * Cleans up local cross-reference entries.
		 *
		 * @param persister The persister representing the entity type.
		 * @param id The primary key value
		 * @param naturalIdValues The natural id values array
		 */
		public void removeSharedNaturalIdCrossReference(EntityPersister persister, Serializable id, Object[] naturalIdValues);

		/**
		 * Given a persister and primary key, find the corresponding cross-referenced natural id values.
		 *
		 * @param persister The persister representing the entity type.
		 * @param pk The primary key value
		 * 
		 * @return The cross-referenced natural-id values, or {@code null}
		 */
		public Object[] findCachedNaturalId(EntityPersister persister, Serializable pk);

		/**
		 * Given a persister and natural-id values, find the corresponding cross-referenced primary key. Will return
		 * {@link PersistenceContext.NaturalIdHelper#INVALID_NATURAL_ID_REFERENCE} if the given natural ids are known to
		 * be invalid.
		 *
		 * @param persister The persister representing the entity type.
		 * @param naturalIdValues The natural id value(s)
		 *
		 * @return The corresponding cross-referenced primary key, 
		 * 		{@link PersistenceContext.NaturalIdHelper#INVALID_NATURAL_ID_REFERENCE},
		 * 		or {@code null}. 
		 */
		public Serializable findCachedNaturalIdResolution(EntityPersister persister, Object[] naturalIdValues);

		/**
		 * Find all the locally cached primary key cross-reference entries for the given persister.
		 *
		 * @param persister The persister representing the entity type.
		 * 
		 * @return The primary keys
		 */
		public Collection<Serializable> getCachedPkResolutions(EntityPersister persister);

		/**
		 * Part of the "load synchronization process".  Responsible for maintaining cross-reference entries
		 * when natural-id values were found to have changed.  Also responsible for tracking the old values 
		 * as no longer valid until the next flush because otherwise going to the database would just re-pull
		 * the old values as valid.  In this last responsibility, {@link #cleanupFromSynchronizations} is
		 * the inverse process called after flush to clean up those entries.
		 *
		 * @param persister The persister representing the entity type.
		 * @param pk The primary key
		 * @param entity The entity instance
		 * 
		 * @see #cleanupFromSynchronizations
		 */
		public void handleSynchronization(EntityPersister persister, Serializable pk, Object entity);

		/**
		 * The clean up process of {@link #handleSynchronization}.  Responsible for cleaning up the tracking
		 * of old values as no longer valid.
		 */
		public void cleanupFromSynchronizations();

		/**
		 * Called on {@link org.hibernate.Session#evict} to give a chance to clean up natural-id cross refs.
		 *
		 * @param object The entity instance.
		 * @param persister The entity persister
		 * @param identifier The entity identifier
		 */
		public void handleEviction(Object object, EntityPersister persister, Serializable identifier);
	}

	/**
	 * Access to the natural-id helper for this persistence context
	 * 
	 * @return This persistence context's natural-id helper
	 */
	public NaturalIdHelper getNaturalIdHelper();
}
