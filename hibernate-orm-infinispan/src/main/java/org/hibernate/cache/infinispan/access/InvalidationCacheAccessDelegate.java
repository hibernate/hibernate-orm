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
import org.hibernate.cache.infinispan.util.InfinispanMessageLogger;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import org.infinispan.AdvancedCache;

/**
 *
 * @author Brian Stansberry
 * @author Galder Zamarreño
 * @since 3.5
 */
public abstract class InvalidationCacheAccessDelegate implements AccessDelegate {
	protected static final InfinispanMessageLogger log = InfinispanMessageLogger.Provider.getLog( InvalidationCacheAccessDelegate.class );
	protected static final boolean TRACE_ENABLED = log.isTraceEnabled();
	protected final AdvancedCache cache;
	protected final BaseRegion region;
	protected final PutFromLoadValidator putValidator;
	protected final AdvancedCache<Object, Object> writeCache;

   /**
    * Create a new transactional access delegate instance.
    *
    * @param region to control access to
    * @param validator put from load validator
    */
	@SuppressWarnings("unchecked")
	protected InvalidationCacheAccessDelegate(BaseRegion region, PutFromLoadValidator validator) {
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
	@Override
	@SuppressWarnings("UnusedParameters")
	public Object get(SharedSessionContractImplementor session, Object key, long txTimestamp) throws CacheException {
		if ( !region.checkValid() ) {
			return null;
		}
		final Object val = cache.get( key );
		if ( val == null ) {
			putValidator.registerPendingPut(session, key, txTimestamp );
		}
		return val;
	}

	@Override
	public boolean putFromLoad(SharedSessionContractImplementor session, Object key, Object value, long txTimestamp, Object version) {
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
	@Override
	@SuppressWarnings("UnusedParameters")
	public boolean putFromLoad(SharedSessionContractImplementor session, Object key, Object value, long txTimestamp, Object version, boolean minimalPutOverride)
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

	@Override
	public void remove(SharedSessionContractImplementor session, Object key) throws CacheException {
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

	@Override
	public void removeAll() throws CacheException {
		try {
			if (!putValidator.beginInvalidatingRegion()) {
				log.failedInvalidateRegion(region.getName());
			}
			Caches.removeAll(cache);
		}
		finally {
			putValidator.endInvalidatingRegion();
		}
	}

	@Override
	public void evict(Object key) throws CacheException {
		writeCache.remove( key );
	}

	@Override
	public void evictAll() throws CacheException {
		try {
			if (!putValidator.beginInvalidatingRegion()) {
				log.failedInvalidateRegion(region.getName());
			}

			// Invalidate the local region and then go remote
			region.invalidateRegion();
			Caches.broadcastEvictAll(cache);
		}
		finally {
			putValidator.endInvalidatingRegion();
		}
	}

	@Override
	public void unlockItem(SharedSessionContractImplementor session, Object key) throws CacheException {
	}
}
