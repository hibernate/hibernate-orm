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

import net.sf.swarmcache.ObjectCache;

import java.io.Serializable;
import java.util.Map;

/**
 * @author Jason Carreira, Gavin King
 */
public class SwarmCache implements Cache {
	
    private final ObjectCache cache;
    private final String regionName;
    
    public SwarmCache(ObjectCache cache, String regionName) {
        this.cache = cache;
        this.regionName = regionName;
    }

    /**
     * Get an item from the cache
     * @param key
     * @return the cached object or <tt>null</tt>
     * @throws CacheException
     */
    public Object get(Object key) throws CacheException {
        if (key instanceof Serializable) {
            return cache.get( (Serializable) key );
        } 
        else {
            throw new CacheException("Keys must implement Serializable");
        }
    }

    public Object read(Object key) throws CacheException {
		return get(key);
    }
	
    /**
     * Add an item to the cache
     * @param key
     * @param value
     * @throws CacheException
     */
    public void update(Object key, Object value) throws CacheException {
		put(key, value);
	}
	
    /**
     * Add an item to the cache
     * @param key
     * @param value
     * @throws CacheException
     */
	public void put(Object key, Object value) throws CacheException {
        if (key instanceof Serializable) {
            cache.put( (Serializable) key, value );
        } 
        else {
            throw new CacheException("Keys must implement Serializable");
        }
    }

    /**
     * Remove an item from the cache
     */
    public void remove(Object key) throws CacheException {
        if (key instanceof Serializable) {
            cache.clear( (Serializable) key );
        } 
        else {
            throw new CacheException("Keys must implement Serializable");
        }
    }

    /**
     * Clear the cache
     */
    public void clear() throws CacheException {
        cache.clearAll();
    }

    /**
     * Clean up
     */
    public void destroy() throws CacheException {
        cache.clearAll();
    }

    /**
     * If this is a clustered cache, lock the item
     */
    public void lock(Object key) throws CacheException {
        throw new UnsupportedOperationException("SwarmCache does not support locking (use nonstrict-read-write)");
    }

    /**
     * If this is a clustered cache, unlock the item
     */
    public void unlock(Object key) throws CacheException {
		throw new UnsupportedOperationException("SwarmCache does not support locking (use nonstrict-read-write)");
    }

    /**
     * Generate a (coarse) timestamp
     */
    public long nextTimestamp() {
    	return System.currentTimeMillis() / 100;
    }

    /**
     * Get a reasonable "lock timeout"
     */
    public int getTimeout() {
		return 600;
    }

	public String getRegionName() {
		return regionName;
	}

	public long getSizeInMemory() {
		return -1;
	}

	public long getElementCountInMemory() {
		return -1;
	}

	public long getElementCountOnDisk() {
		return -1;
	}
	
	public Map toMap() {
		throw new UnsupportedOperationException();
	}

	public String toString() {
		return "SwarmCache(" + regionName + ')';
	}

}
