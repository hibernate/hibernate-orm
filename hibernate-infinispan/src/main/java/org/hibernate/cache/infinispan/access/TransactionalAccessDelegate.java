/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007, Red Hat, Inc. and/or it's affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc. and/or it's affiliates.
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
package org.hibernate.cache.infinispan.access;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.infinispan.impl.BaseRegion;
import org.hibernate.cache.infinispan.util.Caches;

import org.infinispan.AdvancedCache;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * Defines the strategy for transactional access to entity or collection data in a Infinispan instance.
 * <p/>
 * The intent of this class is to encapsulate common code and serve as a delegate for
 * {@link org.hibernate.cache.spi.access.EntityRegionAccessStrategy}
 * and {@link org.hibernate.cache.spi.access.CollectionRegionAccessStrategy} implementations.
 *
 * @author Brian Stansberry
 * @author Galder Zamarreño
 * @since 3.5
 */
public class TransactionalAccessDelegate {
	private static final Log log = LogFactory.getLog( TransactionalAccessDelegate.class );
	private static final boolean TRACE_ENABLED = log.isTraceEnabled();
	private final AdvancedCache cache;
	private final BaseRegion region;
	private final PutFromLoadValidator putValidator;
	private final AdvancedCache<Object, Object> writeCache;

   /**
    * Create a new transactional access delegate instance.
    *
    * @param region to control access to
    * @param validator put from load validator
    */
	@SuppressWarnings("unchecked")
	public TransactionalAccessDelegate(BaseRegion region, PutFromLoadValidator validator) {
		this.region = region;
		this.cache = region.getCache();
		this.putValidator = validator;
		this.writeCache = Caches.ignoreReturnValuesCache( cache );
	}

   /**
    * Attempt to retrieve an object from the cache.
    *
    * @param key The key of the item to be retrieved
    * @param txTimestamp a timestamp prior to the transaction start time
    * @return the cached object or <tt>null</tt>
    * @throws CacheException if the cache retrieval failed
    */
	@SuppressWarnings("UnusedParameters")
	public Object get(Object key, long txTimestamp) throws CacheException {
		if ( !region.checkValid() ) {
			return null;
		}
		final Object val = cache.get( key );
		if ( val == null ) {
			putValidator.registerPendingPut( key );
		}
		return val;
	}

   /**
    * Attempt to cache an object, after loading from the database.
    *
    * @param key The item key
    * @param value The item
    * @param txTimestamp a timestamp prior to the transaction start time
    * @param version the item version number
    * @return <tt>true</tt> if the object was successfully cached
    */
	public boolean putFromLoad(Object key, Object value, long txTimestamp, Object version) {
		return putFromLoad( key, value, txTimestamp, version, false );
	}

   /**
    * Attempt to cache an object, after loading from the database, explicitly
    * specifying the minimalPut behavior.
    *
    * @param key The item key
    * @param value The item
    * @param txTimestamp a timestamp prior to the transaction start time
    * @param version the item version number
    * @param minimalPutOverride Explicit minimalPut flag
    * @return <tt>true</tt> if the object was successfully cached
    * @throws CacheException if storing the object failed
    */
	@SuppressWarnings("UnusedParameters")
	public boolean putFromLoad(Object key, Object value, long txTimestamp, Object version, boolean minimalPutOverride)
			throws CacheException {
		if ( !region.checkValid() ) {
			if ( TRACE_ENABLED ) {
				log.tracef( "Region %s not valid", region.getName() );
			}
			return false;
		}

		// In theory, since putForExternalRead is already as minimal as it can
		// get, we shouldn't be need this check. However, without the check and
		// without https://issues.jboss.org/browse/ISPN-1986, it's impossible to
		// know whether the put actually occurred. Knowing this is crucial so
		// that Hibernate can expose accurate statistics.
		if ( minimalPutOverride && cache.containsKey( key ) ) {
			return false;
		}

		if ( !putValidator.acquirePutFromLoadLock( key ) ) {
			if ( TRACE_ENABLED ) {
				log.tracef( "Put from load lock not acquired for key %s", key );
			}
			return false;
		}

		try {
			// Conditional put/putForExternalRead. If the region has been
			// evicted in the current transaction, do a put instead of a
			// putForExternalRead to make it stick, otherwise the current
			// transaction won't see it.
			if ( region.isRegionInvalidatedInCurrentTx() )
				writeCache.put( key, value );
			else
				writeCache.putForExternalRead( key, value );
		}
		finally {
			putValidator.releasePutFromLoadLock( key );
		}

		return true;
	}

   /**
    * Called after an item has been inserted (before the transaction completes),
    * instead of calling evict().
    *
    * @param key The item key
    * @param value The item
    * @param version The item's version value
    * @return Were the contents of the cache actual changed by this operation?
    * @throws CacheException if the insert fails
    */
	@SuppressWarnings("UnusedParameters")
	public boolean insert(Object key, Object value, Object version) throws CacheException {
		if ( !region.checkValid() ) {
			return false;
		}

		writeCache.put( key, value );
		return true;
	}

   /**
    * Called after an item has been updated (before the transaction completes),
    * instead of calling evict().
    *
    * @param key The item key
    * @param value The item
    * @param currentVersion The item's current version value
    * @param previousVersion The item's previous version value
    * @return Whether the contents of the cache actual changed by this operation
    * @throws CacheException if the update fails
    */
	@SuppressWarnings("UnusedParameters")
	public boolean update(Object key, Object value, Object currentVersion, Object previousVersion)
			throws CacheException {
		// We update whether or not the region is valid. Other nodes
		// may have already restored the region so they need to
		// be informed of the change.
		writeCache.put( key, value );
		return true;
	}

   /**
    * Called after an item has become stale (before the transaction completes).
    *
    * @param key The key of the item to remove
    * @throws CacheException if removing the cached item fails
    */
	public void remove(Object key) throws CacheException {
		if ( !putValidator.invalidateKey( key ) ) {
			throw new CacheException(
					"Failed to invalidate pending putFromLoad calls for key " + key + " from region " + region.getName()
			);
		}
		// We update whether or not the region is valid. Other nodes
		// may have already restored the region so they need to
		// be informed of the change.
		writeCache.remove( key );
	}

   /**
    * Called to evict data from the entire region
    *
    * @throws CacheException if eviction the region fails
    */
	public void removeAll() throws CacheException {
		if ( !putValidator.invalidateRegion() ) {
			throw new CacheException( "Failed to invalidate pending putFromLoad calls for region " + region.getName() );
		}
		cache.clear();
	}

   /**
    * Forcibly evict an item from the cache immediately without regard for transaction
    * isolation.
    *
    * @param key The key of the item to remove
    * @throws CacheException if evicting the item fails
    */
	public void evict(Object key) throws CacheException {
		if ( !putValidator.invalidateKey( key ) ) {
			throw new CacheException(
					"Failed to invalidate pending putFromLoad calls for key " + key + " from region " + region.getName()
			);
		}
		writeCache.remove( key );
	}

   /**
    * Forcibly evict all items from the cache immediately without regard for transaction
    * isolation.
    *
    * @throws CacheException if evicting items fails
    */
	public void evictAll() throws CacheException {
		if ( !putValidator.invalidateRegion() ) {
			throw new CacheException( "Failed to invalidate pending putFromLoad calls for region " + region.getName() );
		}

		// Invalidate the local region and then go remote
		region.invalidateRegion();
		Caches.broadcastEvictAll( cache );
	}

}
