/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import org.hibernate.AssertionFailure;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;

import org.jboss.logging.Logger;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.engine.internal.CacheHelper.fromSharedCache;
import static org.hibernate.internal.util.collections.CollectionHelper.linkedMapOfSize;
import static org.hibernate.internal.util.collections.CollectionHelper.linkedSetOfSize;
import static org.hibernate.internal.util.collections.CollectionHelper.mapOfSize;

/**
 * Keeps track of:<ul>
 *     <li>entity and collection keys that are available for batch fetching</li>
 *     <li>details related to queries which load entities with sub-select-fetchable collections</li>
 * </ul>
 *
 * @author Gavin King
 * @author Steve Ebersole
 * @author Guenther Demetz
 */
public class BatchFetchQueue {
	private static final Logger LOG = CoreLogging.logger( BatchFetchQueue.class );

	private final PersistenceContext context;

	/**
	 * A map of {@link SubselectFetch subselect-fetch descriptors} keyed by the
	 * {@link EntityKey} against which the descriptor is registered.
	 */
	private @Nullable Map<EntityKey, SubselectFetch> subselectsByEntityKey;

	/**
	 * Used to hold information about the entities that are currently eligible for batch-fetching. Ultimately
	 * used by {@link #getBatchLoadableEntityIds} to build entity load batches.
	 * <p>
	 * A Map structure is used to segment the keys by entity type since loading can only be done for a particular entity
	 * type at a time.
	 */
	private @Nullable Map <String,LinkedHashSet<EntityKey>> batchLoadableEntityKeys;

	/**
	 * Used to hold information about the collections that are currently eligible for batch-fetching. Ultimately
	 * used by {@link #getCollectionBatch} to build collection load batches.
	 */
	private @Nullable Map<String, LinkedHashMap<CollectionEntry, PersistentCollection<?>>> batchLoadableCollections;

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
	 * <p>
	 * Called after flushing or clearing the session.
	 */
	public void clear() {
		batchLoadableEntityKeys = null;
		batchLoadableCollections = null;
		subselectsByEntityKey = null;
	}


	// sub-select support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Retrieve the fetch descriptor associated with the given entity key.
	 *
	 * @param key The entity key for which to locate any defined subselect fetch.
	 * @return The fetch descriptor; may return null if no subselect fetch queued for
	 * this entity key.
	 */
	public @Nullable SubselectFetch getSubselect(EntityKey key) {
		return subselectsByEntityKey == null ? null : subselectsByEntityKey.get( key );
	}

	/**
	 * Adds a subselect fetch descriptor for the given entity key.
	 *
	 * @param key The entity for which to register the subselect fetch.
	 * @param subquery The fetch descriptor.
	 */
	public void addSubselect(EntityKey key, SubselectFetch subquery) {
		if ( subselectsByEntityKey == null ) {
			subselectsByEntityKey = mapOfSize( 12 );
		}

		final var previous = subselectsByEntityKey.put( key, subquery );
		if ( previous != null && LOG.isDebugEnabled() ) {
			LOG.debugf(
					"SubselectFetch previously registered with BatchFetchQueue for `%s#s`",
					key.getEntityName(),
					key.getIdentifier()
			);
		}
	}

	/**
	 * After evicting or deleting an entity, we don't need to
	 * know the query that was used to load it anymore (don't
	 * call this after loading the entity, since we might still
	 * need to load its collections)
	 */
	public void removeSubselect(EntityKey key) {
		if ( subselectsByEntityKey != null ) {
			subselectsByEntityKey.remove( key );
		}
	}

	// entity batch support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * If an EntityKey represents a batch loadable entity, add
	 * it to the queue.
	 * <p>
	 * Note that the contract here is such that any key passed in should
	 * previously have been been checked for existence within the
	 * {@link PersistenceContext}; failure to do so may cause the
	 * referenced entity to be included in a batch even though it is
	 * already associated with the {@link PersistenceContext}.
	 */
	public void addBatchLoadableEntityKey(EntityKey key) {
		if ( key.isBatchLoadable( context.getSession().getLoadQueryInfluencers() ) ) {
			if ( batchLoadableEntityKeys == null ) {
				batchLoadableEntityKeys = mapOfSize( 12 );
			}
			batchLoadableEntityKeys.computeIfAbsent( key.getEntityName(), k -> linkedSetOfSize( 8 ) )
					.add( key );
		}
	}


	/**
	 * After evicting or deleting or loading an entity, we don't
	 * need to batch fetch it anymore, remove it from the queue
	 * if necessary
	 */
	public void removeBatchLoadableEntityKey(EntityKey key) {
		if ( key.isBatchLoadable( context.getSession().getLoadQueryInfluencers() )
				&& batchLoadableEntityKeys != null ) {
			final var entityKeys = batchLoadableEntityKeys.get( key.getEntityName() );
			if ( entityKeys != null ) {
				entityKeys.remove( key );
			}
		}
	}

	/**
	 * Intended for test usage. Really has no use-case in Hibernate proper.
	 */
	public boolean containsEntityKey(EntityKey key) {
		if ( key.isBatchLoadable( context.getSession().getLoadQueryInfluencers() )
				&& batchLoadableEntityKeys != null ) {
			final var entityKeys = batchLoadableEntityKeys.get( key.getEntityName() );
			if ( entityKeys != null ) {
				return entityKeys.contains( key );
			}
		}
		return false;
	}

	/**
	 * A "collector" form of {@link #getBatchLoadableEntityIds}. Useful
	 * in cases where we want a specially created array/container - allows
	 * creation of concretely typed array for ARRAY param binding to ensure
	 * the driver does not need to cast/copy the values array.
	 */
	public <T> void collectBatchLoadableEntityIds(
			final int domainBatchSize,
			IndexedConsumer<T> collector,
			final @NonNull T loadingId,
			final EntityMappingType entityDescriptor) {
		// make sure we load the id being loaded in the batch!
		collector.accept( 0, loadingId );

		if ( batchLoadableEntityKeys != null ) {
			final var entityKeys = batchLoadableEntityKeys.get( entityDescriptor.getEntityName() );
			if ( entityKeys != null ) {
				final var identifierMapping = entityDescriptor.getIdentifierMapping();
				int batchPosition = 1;
				int end = -1;
				boolean checkForEnd = false;
				for ( var entityKey : entityKeys ) {
					if ( checkForEnd && batchPosition == end ) {
						// the first id found after the given id
						return;
					}
					else if ( identifierMapping.areEqual( loadingId, entityKey.getIdentifier(),
							context.getSession() ) ) {
						end = batchPosition;
					}
					else if ( !isCached( entityKey, entityDescriptor.getEntityPersister() ) ) {
						//noinspection unchecked
						collector.accept( batchPosition++, (T) entityKey.getIdentifier() );
					}

					if ( batchPosition == domainBatchSize ) {
						// end of array, start filling again from start
						batchPosition = 1;
						if ( end != -1 ) {
							checkForEnd = true;
						}
					}
				}
			}
		}
	}

	/**
	 * Get a batch of unloaded identifiers for this class, using a slightly
	 * complex algorithm that tries to grab keys registered immediately after
	 * the given key.
	 */
	public Object [] getBatchLoadableEntityIds(
			final EntityMappingType entityDescriptor,
			final Object loadingId,
			final int maxBatchSize) {

		final Object[] ids = new Object[maxBatchSize];
		// make sure we load the id being loaded in the batch!
		ids[0] = loadingId;

		if ( batchLoadableEntityKeys != null ) {
			int i = 1;
			int end = -1;
			boolean checkForEnd = false;
			// TODO: this needn't exclude subclasses...
			final var entityKeys = batchLoadableEntityKeys.get( entityDescriptor.getEntityName() );
			if ( entityKeys != null ) {
				final var entityPersister = entityDescriptor.getEntityPersister();
				final var identifierType = entityPersister.getIdentifierType();
				for ( var entityKey : entityKeys ) {
					if ( checkForEnd && i == end ) {
						// the first id found after the given id
						return ids;
					}
					else if ( identifierType.isEqual( loadingId, entityKey.getIdentifier() ) ) {
						end = i;
					}
					else if ( !isCached( entityKey, entityPersister ) ) {
						ids[i++] = entityKey.getIdentifier();
					}

					if ( i == maxBatchSize ) {
						i = 1; // end of array, start filling again from start
						if ( end != -1 ) {
							checkForEnd = true;
						}
					}
				}
			}
			//we ran out of ids to try
		}
		return ids;
	}


	// collection batch support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * If a CollectionEntry represents a batch loadable collection, add
	 * it to the queue.
	 */
	public void addBatchLoadableCollection(PersistentCollection<?> collection, CollectionEntry ce) {
		final var persister = ce.getLoadedPersister();
		assert persister != null : "@AssumeAssertion(nullness)";
		if ( batchLoadableCollections == null ) {
			batchLoadableCollections = mapOfSize( 12 );
		}
		batchLoadableCollections.computeIfAbsent( persister.getRole(), k -> linkedMapOfSize( 16 ) )
				.put( ce, collection );
	}

	/**
	 * After a collection was initialized or evicted, we don't
	 * need to batch fetch it anymore, remove it from the queue
	 * if necessary
	 */
	public void removeBatchLoadableCollection(CollectionEntry ce) {
		final var persister = ce.getLoadedPersister();
		assert persister != null : "@AssumeAssertion(nullness)";
		if ( batchLoadableCollections != null ) {
			final var map = batchLoadableCollections.get( persister.getRole() );
			if ( map != null ) {
				map.remove( ce );
			}
		}
	}


	/**
	 * A "collector" form of {@link #getCollectionBatch}.
	 * Useful in cases where we want a specially created array/container.
	 * Allows creation of a concretely typed array for ARRAY param binding to
	 * ensure the driver does not need to cast or copy the values array.
	 */
	public <T> void collectBatchLoadableCollectionKeys(
			int batchSize,
			IndexedConsumer<T> collector,
			@NonNull T keyBeingLoaded,
			PluralAttributeMapping pluralAttributeMapping) {
		collector.accept( 0, keyBeingLoaded );

		if ( batchLoadableCollections != null ) {
			final var map = batchLoadableCollections.get( pluralAttributeMapping.getNavigableRole().getFullPath() );
			if ( map != null ) {
				int i = 1;
				int end = -1;
				boolean checkForEnd = false;
				for ( var me : map.entrySet() ) {
					final CollectionEntry ce = me.getKey();
					final Object loadedKey = ce.getLoadedKey();
					final PersistentCollection<?> collection = me.getValue();

					// the loadedKey of the collectionEntry might be null as it might have been reset to null
					// (see for example Collections.processDereferencedCollection()
					// and CollectionEntry.afterAction())
					// though we clear the queue on flush, it seems like a good idea to guard
					// against potentially null loadedKeys (which leads to various NPEs as demonstrated in HHH-7821).

					if ( loadedKey != null ) {
						if ( collection.wasInitialized() ) {
							throw new AssertionFailure( "Encountered initialized collection in BatchFetchQueue" );
						}
						else if ( checkForEnd && i == end ) {
							// the first key found after the given key
							return;
						}
						else {
							final boolean isEqual =
									pluralAttributeMapping.getKeyDescriptor()
											.areEqual( keyBeingLoaded, loadedKey, context.getSession() );
							if ( isEqual ) {
								end = i;
							}
							else if ( !isCached( loadedKey, pluralAttributeMapping.getCollectionDescriptor() ) ) {
								//noinspection unchecked
								collector.accept( i++, (T) loadedKey );
							}

							if ( i == batchSize ) {
								//end of array, start filling again from start
								i = 1;
								if ( end != -1 ) {
									checkForEnd = true;
								}
							}
						}
					}
				}
				//we ran out of keys to try
			}
		}
	}

	/**
	 * Get a batch of uninitialized collection keys for a given role
	 *
	 * @param collectionPersister The persister for the collection role.
	 * @param id A key that must be included in the batch fetch
	 * @param batchSize the maximum number of keys to return
	 * @return an array of collection keys, of length batchSize (padded with nulls)
	 */
	public Object [] getCollectionBatch(
			final CollectionPersister collectionPersister,
			final Object id,
			final int batchSize) {

		final Object[] keys = new Object[batchSize];
		keys[0] = id;

		if ( batchLoadableCollections != null ) {
			int i = 1;
			int end = -1;
			boolean checkForEnd = false;

			final var map = batchLoadableCollections.get( collectionPersister.getRole() );
			if ( map != null ) {
				for ( var me : map.entrySet() ) {
					final CollectionEntry ce = me.getKey();
					final Object loadedKey = ce.getLoadedKey();
					final PersistentCollection<?> collection = me.getValue();

					// the loadedKey of the collectionEntry might be null as it might have been reset to null
					// (see for example Collections.processDereferencedCollection()
					// and CollectionEntry.afterAction())
					// though we clear the queue on flush, it seems like a good idea to guard
					// against potentially null loadedKeys (which leads to various NPEs as demonstrated in HHH-7821).

					if ( loadedKey != null ) {
						if ( collection.wasInitialized() ) {
							throw new AssertionFailure( "Encountered initialized collection in BatchFetchQueue" );
						}
						else if ( checkForEnd && i == end ) {
							return keys; //the first key found after the given key
						}
						else {
							final boolean isEqual =
									collectionPersister.getKeyType()
											.isEqual( id, loadedKey, collectionPersister.getFactory() );
							if ( isEqual ) {
								end = i;
								//checkForEnd = false;
							}
							else if ( !isCached( loadedKey, collectionPersister ) ) {
								keys[i++] = loadedKey;
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
				}
			}
			//we ran out of keys to try
		}
		return keys;
	}

	public SharedSessionContractImplementor getSession() {
		return context.getSession();
	}

	private boolean isCached(Object collectionKey, CollectionPersister persister) {
		final var session = getSession();
		if ( session.getCacheMode().isGetEnabled() && persister.hasCache() ) {
			final var cache = persister.getCacheAccessStrategy();
			final Object cacheKey =
					cache.generateCacheKey( collectionKey, persister,
							session.getFactory(), session.getTenantIdentifier() );
			return fromSharedCache( session, cacheKey, persister, cache ) != null;
		}
		else {
			return false;
		}
	}

	private boolean isCached(EntityKey entityKey, EntityPersister persister) {
		final var session = getSession();
		if ( session.getCacheMode().isGetEnabled() && persister.canReadFromCache() ) {
			final var cache = persister.getCacheAccessStrategy();
			final Object key =
					cache.generateCacheKey( entityKey.getIdentifier(), persister,
							session.getFactory(), session.getTenantIdentifier() );
			return fromSharedCache( session, key, persister, cache ) != null;
		}
		else {
			return false;
		}
	}
}
