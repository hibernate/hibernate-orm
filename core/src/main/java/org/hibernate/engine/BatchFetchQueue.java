/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.engine;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.EntityMode;
import org.hibernate.cache.CacheKey;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.util.MarkerObject;

/**
 * Tracks entity and collection keys that are available for batch
 * fetching, and the queries which were used to load entities, which
 * can be re-used as a subquery for loading owned collections.
 *
 * @author Gavin King
 */
public class BatchFetchQueue {

	public static final Object MARKER = new MarkerObject( "MARKER" );

	/**
	 * Defines a sequence of {@link EntityKey} elements that are currently
	 * elegible for batch-fetching.
	 * <p/>
	 * Even though this is a map, we only use the keys.  A map was chosen in
	 * order to utilize a {@link LinkedHashMap} to maintain sequencing
	 * as well as uniqueness.
	 * <p/>
	 * TODO : this would be better as a SequencedReferenceSet, but no such beast exists!
	 */
	private final Map batchLoadableEntityKeys = new LinkedHashMap(8);

	/**
	 * A map of {@link SubselectFetch subselect-fetch descriptors} keyed by the
	 * {@link EntityKey) against which the descriptor is registered.
	 */
	private final Map subselectsByEntityKey = new HashMap(8);

	/**
	 * The owning persistence context.
	 */
	private final PersistenceContext context;

	/**
	 * Constructs a queue for the given context.
	 *
	 * @param context The owning context.
	 */
	public BatchFetchQueue(PersistenceContext context) {
		this.context = context;
	}

	/**
	 * Clears all entries from this fetch queue.
	 */
	public void clear() {
		batchLoadableEntityKeys.clear();
		subselectsByEntityKey.clear();
	}

	/**
	 * Retrieve the fetch descriptor associated with the given entity key.
	 *
	 * @param key The entity key for which to locate any defined subselect fetch.
	 * @return The fetch descriptor; may return null if no subselect fetch queued for
	 * this entity key.
	 */
	public SubselectFetch getSubselect(EntityKey key) {
		return (SubselectFetch) subselectsByEntityKey.get(key);
	}

	/**
	 * Adds a subselect fetch decriptor for the given entity key.
	 *
	 * @param key The entity for which to register the subselect fetch.
	 * @param subquery The fetch descriptor.
	 */
	public void addSubselect(EntityKey key, SubselectFetch subquery) {
		subselectsByEntityKey.put(key, subquery);
	}

	/**
	 * After evicting or deleting an entity, we don't need to
	 * know the query that was used to load it anymore (don't
	 * call this after loading the entity, since we might still
	 * need to load its collections)
	 */
	public void removeSubselect(EntityKey key) {
		subselectsByEntityKey.remove(key);
	}

	/**
	 * Clears all pending subselect fetches from the queue.
	 * <p/>
	 * Called after flushing.
	 */
	public void clearSubselects() {
		subselectsByEntityKey.clear();
	}

	/**
	 * If an EntityKey represents a batch loadable entity, add
	 * it to the queue.
	 * <p/>
	 * Note that the contract here is such that any key passed in should
	 * previously have been been checked for existence within the
	 * {@link PersistenceContext}; failure to do so may cause the
	 * referenced entity to be included in a batch even though it is
	 * already associated with the {@link PersistenceContext}.
	 */
	public void addBatchLoadableEntityKey(EntityKey key) {
		if ( key.isBatchLoadable() ) {
			batchLoadableEntityKeys.put( key, MARKER );
		}
	}

	/**
	 * After evicting or deleting or loading an entity, we don't
	 * need to batch fetch it anymore, remove it from the queue
	 * if necessary
	 */
	public void removeBatchLoadableEntityKey(EntityKey key) {
		if ( key.isBatchLoadable() ) batchLoadableEntityKeys.remove(key);
	}

	/**
	 * Get a batch of uninitialized collection keys for a given role
	 *
	 * @param collectionPersister The persister for the collection role.
	 * @param id A key that must be included in the batch fetch
	 * @param batchSize the maximum number of keys to return
	 * @return an array of collection keys, of length batchSize (padded with nulls)
	 */
	public Serializable[] getCollectionBatch(
			final CollectionPersister collectionPersister,
			final Serializable id,
			final int batchSize,
			final EntityMode entityMode) {
		Serializable[] keys = new Serializable[batchSize];
		keys[0] = id;
		int i = 1;
		//int count = 0;
		int end = -1;
		boolean checkForEnd = false;
		// this only works because collection entries are kept in a sequenced
		// map by persistence context (maybe we should do like entities and
		// keep a separate sequences set...)
		Iterator iter = context.getCollectionEntries().entrySet().iterator(); //TODO: calling entrySet on an IdentityMap is SLOW!!
		while ( iter.hasNext() ) {
			Map.Entry me = (Map.Entry) iter.next();

			CollectionEntry ce = (CollectionEntry) me.getValue();
			PersistentCollection collection = (PersistentCollection) me.getKey();
			if ( !collection.wasInitialized() && ce.getLoadedPersister() == collectionPersister ) {

				if ( checkForEnd && i == end ) {
					return keys; //the first key found after the given key
				}

				//if ( end == -1 && count > batchSize*10 ) return keys; //try out ten batches, max

				final boolean isEqual = collectionPersister.getKeyType().isEqual(
						id,
						ce.getLoadedKey(),
						entityMode,
						collectionPersister.getFactory()
				);

				if ( isEqual ) {
					end = i;
					//checkForEnd = false;
				}
				else if ( !isCached( ce.getLoadedKey(), collectionPersister, entityMode ) ) {
					keys[i++] = ce.getLoadedKey();
					//count++;
				}

				if ( i == batchSize ) {
					i = 1; //end of array, start filling again from start
					if ( end != -1 ) {
						checkForEnd = true;
					}
				}
			}

		}
		return keys; //we ran out of keys to try
	}

	/**
	 * Get a batch of unloaded identifiers for this class, using a slightly
	 * complex algorithm that tries to grab keys registered immediately after
	 * the given key.
	 *
	 * @param persister The persister for the entities being loaded.
	 * @param id The identifier of the entity currently demanding load.
	 * @param batchSize The maximum number of keys to return
	 * @return an array of identifiers, of length batchSize (possibly padded with nulls)
	 */
	public Serializable[] getEntityBatch(
			final EntityPersister persister,
			final Serializable id,
			final int batchSize,
			final EntityMode entityMode) {
		Serializable[] ids = new Serializable[batchSize];
		ids[0] = id; //first element of array is reserved for the actual instance we are loading!
		int i = 1;
		int end = -1;
		boolean checkForEnd = false;

		Iterator iter = batchLoadableEntityKeys.keySet().iterator();
		while ( iter.hasNext() ) {
			EntityKey key = (EntityKey) iter.next();
			if ( key.getEntityName().equals( persister.getEntityName() ) ) { //TODO: this needn't exclude subclasses...
				if ( checkForEnd && i == end ) {
					//the first id found after the given id
					return ids;
				}
				if ( persister.getIdentifierType().isEqual( id, key.getIdentifier(), entityMode ) ) {
					end = i;
				}
				else {
					if ( !isCached( key, persister, entityMode ) ) {
						ids[i++] = key.getIdentifier();
					}
				}
				if ( i == batchSize ) {
					i = 1; //end of array, start filling again from start
					if (end!=-1) checkForEnd = true;
				}
			}
		}
		return ids; //we ran out of ids to try
	}

	private boolean isCached(
			EntityKey entityKey,
			EntityPersister persister,
			EntityMode entityMode) {
		if ( persister.hasCache() ) {
			CacheKey key = new CacheKey(
					entityKey.getIdentifier(),
					persister.getIdentifierType(),
					entityKey.getEntityName(),
					entityMode,
					context.getSession().getFactory()
			);
			return persister.getCacheAccessStrategy().get( key, context.getSession().getTimestamp() ) != null;
		}
		return false;
	}

	private boolean isCached(
			Serializable collectionKey,
			CollectionPersister persister,
			EntityMode entityMode) {
		if ( persister.hasCache() ) {
			CacheKey cacheKey = new CacheKey(
					collectionKey,
			        persister.getKeyType(),
			        persister.getRole(),
			        entityMode,
			        context.getSession().getFactory()
			);
			return persister.getCacheAccessStrategy().get( cacheKey, context.getSession().getTimestamp() ) != null;
		}
		return false;
	}
}
