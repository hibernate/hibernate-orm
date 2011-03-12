/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
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
 */
package org.hibernate.cache;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EHCache plugin for Hibernate
 * <p/>
 * EHCache uses a {@link net.sf.ehcache.store.MemoryStore} and a
 * {@link net.sf.ehcache.store.DiskStore}.
 * The {@link net.sf.ehcache.store.DiskStore} requires that both keys and values be {@link java.io.Serializable}.
 * However the MemoryStore does not and in ehcache-1.2 nonSerializable Objects are permitted. They are discarded
 * if an attempt it made to overflow them to Disk or to replicate them to remote cache peers.
 *
 * @author Greg Luck
 * @author Emmanuel Bernard
 */
public class EhCache implements Cache {
	private static final Logger log = LoggerFactory.getLogger( EhCache.class );

	private static final int SIXTY_THOUSAND_MS = 60000;

	private net.sf.ehcache.Ehcache cache;

	/**
	 * Creates a new Hibernate pluggable cache based on a cache name.
	 * <p/>
	 *
	 * @param cache The underlying EhCache instance to use.
	 */
	public EhCache(net.sf.ehcache.Ehcache cache) {
		this.cache = cache;
	}

	/**
	 * Gets a value of an element which matches the given key.
	 *
	 * @param key the key of the element to return.
	 * @return The value placed into the cache with an earlier put, or null if not found or expired
	 * @throws CacheException
	 */
	public Object get(Object key) throws CacheException {
		try {
			if ( log.isDebugEnabled() ) {
				log.debug( "key: " + key );
			}
			if ( key == null ) {
				return null;
			}
			else {
				Element element = cache.get( key );
				if ( element == null ) {
					if ( log.isDebugEnabled() ) {
						log.debug( "Element for " + key + " is null" );
					}
					return null;
				}
				else {
					return element.getObjectValue();
				}
			}
		}
		catch (net.sf.ehcache.CacheException e) {
			throw new CacheException( e );
		}
	}

	public Object read(Object key) throws CacheException {
		return get( key );
	}


	/**
	 * Puts an object into the cache.
	 *
	 * @param key   a key
	 * @param value a value
	 * @throws CacheException if the {@link CacheManager}
	 *                        is shutdown or another {@link Exception} occurs.
	 */
	public void update(Object key, Object value) throws CacheException {
		put( key, value );
	}

	/**
	 * Puts an object into the cache.
	 *
	 * @param key   a key
	 * @param value a value
	 * @throws CacheException if the {@link CacheManager}
	 *                        is shutdown or another {@link Exception} occurs.
	 */
	public void put(Object key, Object value) throws CacheException {
		try {
			Element element = new Element( key, value );
			cache.put( element );
		}
		catch (IllegalArgumentException e) {
			throw new CacheException( e );
		}
		catch (IllegalStateException e) {
			throw new CacheException( e );
		}
		catch (net.sf.ehcache.CacheException e) {
			throw new CacheException( e );
		}

	}

	/**
	 * Removes the element which matches the key.
	 * <p/>
	 * If no element matches, nothing is removed and no Exception is thrown.
	 *
	 * @param key the key of the element to remove
	 * @throws CacheException
	 */
	public void remove(Object key) throws CacheException {
		try {
			cache.remove( key );
		}
		catch (ClassCastException e) {
			throw new CacheException( e );
		}
		catch (IllegalStateException e) {
			throw new CacheException( e );
		}
		catch (net.sf.ehcache.CacheException e) {
			throw new CacheException( e );
		}
	}

	/**
	 * Remove all elements in the cache, but leave the cache
	 * in a useable state.
	 *
	 * @throws CacheException
	 */
	public void clear() throws CacheException {
		try {
			cache.removeAll();
		}
		catch (IllegalStateException e) {
			throw new CacheException( e );
		}
		catch (net.sf.ehcache.CacheException e) {
			throw new CacheException( e );
		}
	}

	/**
	 * Remove the cache and make it unuseable.
	 *
	 * @throws CacheException
	 */
	public void destroy() throws CacheException {
		try {
			cache.getCacheManager().removeCache( cache.getName() );
		}
		catch (IllegalStateException e) {
			throw new CacheException( e );
		}
		catch (net.sf.ehcache.CacheException e) {
			throw new CacheException( e );
		}
	}

	/**
	 * Calls to this method should perform there own synchronization.
	 * It is provided for distributed caches. Because EHCache is not distributed
	 * this method does nothing.
	 */
	public void lock(Object key) throws CacheException {
	}

	/**
	 * Calls to this method should perform there own synchronization.
	 * It is provided for distributed caches. Because EHCache is not distributed
	 * this method does nothing.
	 */
	public void unlock(Object key) throws CacheException {
	}

	/**
	 * Gets the next timestamp;
	 */
	public long nextTimestamp() {
		return Timestamper.next();
	}

	/**
	 * Returns the lock timeout for this cache.
	 */
	public int getTimeout() {
		// 60 second lock timeout
		return Timestamper.ONE_MS * SIXTY_THOUSAND_MS;
	}

	public String getRegionName() {
		return cache.getName();
	}

	/**
	 * Warning: This method can be very expensive to run. Allow approximately 1 second
	 * per 1MB of entries. Running this method could create liveness problems
	 * because the object lock is held for a long period
	 * <p/>
	 *
	 * @return the approximate size of memory ehcache is using for the MemoryStore for this cache
	 */
	public long getSizeInMemory() {
		try {
			return cache.calculateInMemorySize();
		}
		catch (Throwable t) {
			return -1;
		}
	}

	public long getElementCountInMemory() {
		try {
			return cache.getMemoryStoreSize();
		}
		catch (net.sf.ehcache.CacheException ce) {
			throw new CacheException( ce );
		}
	}

	public long getElementCountOnDisk() {
		return cache.getDiskStoreSize();
	}

	public Map toMap() {
		try {
			Map result = new HashMap();
			Iterator iter = cache.getKeys().iterator();
			while ( iter.hasNext() ) {
				Object key = iter.next();
				result.put( key, cache.get( key ).getObjectValue() );
			}
			return result;
		}
		catch (Exception e) {
			throw new CacheException( e );
		}
	}

	public String toString() {
		return "EHCache(" + getRegionName() + ')';
	}

}