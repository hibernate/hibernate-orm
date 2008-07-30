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

import java.util.Collections;
import java.util.Hashtable;

import java.util.Map;

/**
 * A lightweight implementation of the <tt>Cache</tt> interface
 * @author Gavin King
 */
public class HashtableCache implements Cache {
	
	private final Map hashtable = new Hashtable();
	private final String regionName;
	
	public HashtableCache(String regionName) {
		this.regionName = regionName;
	}

	public String getRegionName() {
		return regionName;
	}

	public Object read(Object key) throws CacheException {
		return hashtable.get(key);
	}

	public Object get(Object key) throws CacheException {
		return hashtable.get(key);
	}

	public void update(Object key, Object value) throws CacheException {
		put(key, value);
	}
	
	public void put(Object key, Object value) throws CacheException {
		hashtable.put(key, value);
	}

	public void remove(Object key) throws CacheException {
		hashtable.remove(key);
	}

	public void clear() throws CacheException {
		hashtable.clear();
	}

	public void destroy() throws CacheException {

	}

	public void lock(Object key) throws CacheException {
		// local cache, so we use synchronization
	}

	public void unlock(Object key) throws CacheException {
		// local cache, so we use synchronization
	}

	public long nextTimestamp() {
		return Timestamper.next();
	}

	public int getTimeout() {
		return Timestamper.ONE_MS * 60000; //ie. 60 seconds
	}

	public long getSizeInMemory() {
		return -1;
	}

	public long getElementCountInMemory() {
		return hashtable.size();
	}

	public long getElementCountOnDisk() {
		return 0;
	}
	
	public Map toMap() {
		return Collections.unmodifiableMap(hashtable);
	}

	public String toString() {
		return "HashtableCache(" + regionName + ')';
	}

}
