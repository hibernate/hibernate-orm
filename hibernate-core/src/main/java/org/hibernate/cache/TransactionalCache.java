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
package org.hibernate.cache;

import java.util.Comparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.cache.access.SoftLock;

/**
 * Support for fully transactional cache implementations like
 * JBoss TreeCache. Note that this might be a less scalable
 * concurrency strategy than <tt>ReadWriteCache</tt>. This is
 * a "synchronous" concurrency strategy.
 *
 * @author Gavin King
 */
public class TransactionalCache implements CacheConcurrencyStrategy {

	private static final Logger log = LoggerFactory.getLogger( TransactionalCache.class );

	private Cache cache;

	public String getRegionName() {
		return cache.getRegionName();
	}

	public Object get(Object key, long txTimestamp) throws CacheException {
		if ( log.isDebugEnabled() ) {
			log.debug( "cache lookup: " + key );
		}
		Object result = cache.read( key );
		if ( log.isDebugEnabled() ) {
			log.debug( result == null ? "cache miss" : "cache hit" );
		}
		return result;
	}

	public boolean put(
			Object key,
	        Object value,
	        long txTimestamp,
	        Object version,
	        Comparator versionComparator,
	        boolean minimalPut) throws CacheException {
		if ( minimalPut && cache.read( key ) != null ) {
			if ( log.isDebugEnabled() ) {
				log.debug( "item already cached: " + key );
			}
			return false;
		}
		if ( log.isDebugEnabled() ) {
			log.debug( "caching: " + key );
		}
		if ( cache instanceof OptimisticCache ) {
			( ( OptimisticCache ) cache ).writeLoad( key, value, version );
		}
		else {
			cache.put( key, value );
		}
		return true;
	}

	/**
	 * Do nothing, returning null.
	 */
	public SoftLock lock(Object key, Object version) throws CacheException {
		//noop
		return null;
	}

	/**
	 * Do nothing.
	 */
	public void release(Object key, SoftLock clientLock) throws CacheException {
		//noop
	}

	public boolean update(
			Object key,
	        Object value,
	        Object currentVersion,
	        Object previousVersion) throws CacheException {
		if ( log.isDebugEnabled() ) {
			log.debug( "updating: " + key );
		}
		if ( cache instanceof OptimisticCache ) {
			( ( OptimisticCache ) cache ).writeUpdate( key, value, currentVersion, previousVersion );
		}
		else {
			cache.update( key, value );
		}
		return true;
	}

	public boolean insert(
			Object key,
	        Object value,
	        Object currentVersion) throws CacheException {
		if ( log.isDebugEnabled() ) {
			log.debug( "inserting: " + key );
		}
		if ( cache instanceof OptimisticCache ) {
			( ( OptimisticCache ) cache ).writeInsert( key, value, currentVersion );
		}
		else {
			cache.update( key, value );
		}
		return true;
	}

	public void evict(Object key) throws CacheException {
		cache.remove( key );
	}

	public void remove(Object key) throws CacheException {
		if ( log.isDebugEnabled() ) {
			log.debug( "removing: " + key );
		}
		cache.remove( key );
	}

	public void clear() throws CacheException {
		log.debug( "clearing" );
		cache.clear();
	}

	public void destroy() {
		try {
			cache.destroy();
		}
		catch ( Exception e ) {
			log.warn( "could not destroy cache", e );
		}
	}

	public void setCache(Cache cache) {
		this.cache = cache;
	}

	public Cache getCache() {
		return cache;
	}

	/**
	 * Do nothing.
	 */
	public boolean afterInsert(
			Object key,
	        Object value,
	        Object version) throws CacheException {
		return false;
	}

	/**
	 * Do nothing.
	 */
	public boolean afterUpdate(
			Object key,
	        Object value,
	        Object version,
	        SoftLock clientLock) throws CacheException {
		return false;
	}

	public String toString() {
		return cache + "(transactional)";
	}

}
