/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.access;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.infinispan.impl.BaseRegion;
import org.hibernate.cache.infinispan.util.Caches;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SessionImplementor;
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
public abstract class TransactionalAccessDelegate {
	protected static final Log log = LogFactory.getLog( TransactionalAccessDelegate.class );
	protected static final boolean TRACE_ENABLED = log.isTraceEnabled();
	protected final AdvancedCache cache;
	protected final BaseRegion region;
	protected final PutFromLoadValidator putValidator;
	protected final AdvancedCache<Object, Object> writeCache;

	public static TransactionalAccessDelegate create(BaseRegion region, PutFromLoadValidator validator) {
		if (region.getCache().getCacheConfiguration().transaction().transactionMode().isTransactional()) {
			return new TxTransactionalAccessDelegate(region, validator);
		}
		else {
			return new NonTxTransactionalAccessDelegate(region, validator);
		}
	}

   /**
    * Create a new transactional access delegate instance.
    *
    * @param region to control access to
    * @param validator put from load validator
    */
	@SuppressWarnings("unchecked")
	protected TransactionalAccessDelegate(BaseRegion region, PutFromLoadValidator validator) {
		this.region = region;
		this.cache = region.getCache();
		this.putValidator = validator;
		this.writeCache = Caches.ignoreReturnValuesCache( cache );
	}

   /**
    * Attempt to retrieve an object from the cache.
    *
    *
	 * @param session
	 * @param key The key of the item to be retrieved
    * @param txTimestamp a timestamp prior to the transaction start time
    * @return the cached object or <tt>null</tt>
    * @throws CacheException if the cache retrieval failed
    */
	@SuppressWarnings("UnusedParameters")
	public Object get(SessionImplementor session, Object key, long txTimestamp) throws CacheException {
		if ( !region.checkValid() ) {
			return null;
		}
		final Object val = cache.get( key );
		if ( val == null ) {
			putValidator.registerPendingPut(session, key, txTimestamp );
		}
		return val;
	}

   /**
    * Attempt to cache an object, after loading from the database.
    *
	 * @param session Current session
	 * @param key The item key
    * @param value The item
    * @param txTimestamp a timestamp prior to the transaction start time
    * @param version the item version number
    * @return <tt>true</tt> if the object was successfully cached
    */
	public boolean putFromLoad(SessionImplementor session, Object key, Object value, long txTimestamp, Object version) {
		return putFromLoad(session, key, value, txTimestamp, version, false );
	}

   /**
    * Attempt to cache an object, after loading from the database, explicitly
    * specifying the minimalPut behavior.
    *
	 * @param session Current session
	 * @param key The item key
    * @param value The item
    * @param txTimestamp a timestamp prior to the transaction start time
    * @param version the item version number
    * @param minimalPutOverride Explicit minimalPut flag
    * @return <tt>true</tt> if the object was successfully cached
    * @throws CacheException if storing the object failed
    */
	@SuppressWarnings("UnusedParameters")
	public boolean putFromLoad(SessionImplementor session, Object key, Object value, long txTimestamp, Object version, boolean minimalPutOverride)
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

		PutFromLoadValidator.Lock lock = putValidator.acquirePutFromLoadLock(session, key, txTimestamp);
		if ( lock == null) {
			if ( TRACE_ENABLED ) {
				log.tracef( "Put from load lock not acquired for key %s", key );
			}
			return false;
		}

		try {
			writeCache.putForExternalRead( key, value );
		}
		finally {
			putValidator.releasePutFromLoadLock( key, lock);
		}

		return true;
	}

	/**
	 * Called after an item has been inserted (before the transaction completes),
	 * instead of calling evict().
	 *
	 * @param session Current session
	 * @param key The item key
	 * @param value The item
	 * @param version The item's version value
	 * @return Were the contents of the cache actual changed by this operation?
	 * @throws CacheException if the insert fails
	 */
	public abstract boolean insert(SessionImplementor session, Object key, Object value, Object version) throws CacheException;

	/**
	 * Called after an item has been updated (before the transaction completes),
	 * instead of calling evict().
	 *
	 * @param session Current session
	 * @param key The item key
	 * @param value The item
	 * @param currentVersion The item's current version value
	 * @param previousVersion The item's previous version value
	 * @return Whether the contents of the cache actual changed by this operation
	 * @throws CacheException if the update fails
	 */
	public abstract boolean update(SessionImplementor session, Object key, Object value, Object currentVersion, Object previousVersion)
			throws CacheException;

	/**
    * Called after an item has become stale (before the transaction completes).
    *
	 * @param session Current session
	 * @param key The key of the item to remove
    * @throws CacheException if removing the cached item fails
    */
	public void remove(SessionImplementor session, Object key) throws CacheException {
		if ( !putValidator.beginInvalidatingKey(session, key)) {
			throw new CacheException(
					"Failed to invalidate pending putFromLoad calls for key " + key + " from region " + region.getName()
			);
		}
		putValidator.setCurrentSession(session);
		try {
			// We update whether or not the region is valid. Other nodes
			// may have already restored the region so they need to
			// be informed of the change.
			writeCache.remove(key);
		}
		finally {
			putValidator.resetCurrentSession();
		}
	}

   /**
    * Called to evict data from the entire region
    *
    * @throws CacheException if eviction the region fails
    */
	public void removeAll() throws CacheException {
		try {
			if (!putValidator.beginInvalidatingRegion()) {
				throw new CacheException("Failed to invalidate pending putFromLoad calls for region " + region.getName());
			}
			Caches.removeAll(cache);
		}
		finally {
			putValidator.endInvalidatingRegion();
		}
	}

   /**
    * Forcibly evict an item from the cache immediately without regard for transaction
    * isolation.
    *
    * @param key The key of the item to remove
    * @throws CacheException if evicting the item fails
    */
	public void evict(Object key) throws CacheException {
		writeCache.remove( key );
	}

   /**
    * Forcibly evict all items from the cache immediately without regard for transaction
    * isolation.
    *
    * @throws CacheException if evicting items fails
    */
	public void evictAll() throws CacheException {
		try {
			if (!putValidator.beginInvalidatingRegion()) {
				throw new CacheException("Failed to invalidate pending putFromLoad calls for region " + region.getName());
			}

			// Invalidate the local region and then go remote
			region.invalidateRegion();
			Caches.broadcastEvictAll(cache);
		}
		finally {
			putValidator.endInvalidatingRegion();
		}
	}

	/**
	 * Called when we have finished the attempted update/delete (which may or
	 * may not have been successful), after transaction completion.  This method
	 * is used by "asynchronous" concurrency strategies.
	 *
	 *
	 * @param session
	 * @param key The item key
	 * @throws org.hibernate.cache.CacheException Propogated from underlying {@link org.hibernate.cache.spi.Region}
	 */
	public void unlockItem(SessionImplementor session, Object key) throws CacheException {
		if ( !putValidator.endInvalidatingKey(session, key) ) {
			// TODO: localization
			log.warn("Failed to end invalidating pending putFromLoad calls for key " + key + " from region "
					+ region.getName() + "; the key won't be cached until invalidation expires.");
		}
	}

	/**
	 * Called after an item has been inserted (after the transaction completes),
	 * instead of calling release().
	 * This method is used by "asynchronous" concurrency strategies.
	 *
	 *
	 * @param session
	 * @param key The item key
	 * @param value The item
	 * @param version The item's version value
	 * @return Were the contents of the cache actual changed by this operation?
	 * @throws CacheException Propagated from underlying {@link org.hibernate.cache.spi.Region}
	 */
	public boolean afterInsert(SessionImplementor session, Object key, Object value, Object version) {
		if ( !putValidator.endInvalidatingKey(session, key) ) {
			// TODO: localization
			log.warn("Failed to end invalidating pending putFromLoad calls for key " + key + " from region "
					+ region.getName() + "; the key won't be cached until invalidation expires.");
		}
		return false;
	}

	/**
	 * Called after an item has been updated (after the transaction completes),
	 * instead of calling release().  This method is used by "asynchronous"
	 * concurrency strategies.
	 *
	 *
	 * @param session
	 * @param key The item key
	 * @param value The item
	 * @param currentVersion The item's current version value
	 * @param previousVersion The item's previous version value
	 * @param lock The lock previously obtained from {@link #lockItem}
	 * @return Were the contents of the cache actual changed by this operation?
	 * @throws CacheException Propagated from underlying {@link org.hibernate.cache.spi.Region}
	 */
	public boolean afterUpdate(SessionImplementor session, Object key, Object value, Object currentVersion, Object previousVersion, SoftLock lock) {
		if ( !putValidator.endInvalidatingKey(session, key) ) {
			// TODO: localization
			log.warn("Failed to end invalidating pending putFromLoad calls for key " + key + " from region "
					+ region.getName() + "; the key won't be cached until invalidation expires.");
		}
		return false;
	}
}
