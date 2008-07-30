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
package org.hibernate.util;

import org.apache.commons.collections.map.ReferenceMap;
import org.apache.commons.collections.map.LRUMap;

import java.io.Serializable;
import java.io.IOException;

/**
 * Cache following a "Most Recently Used" (MRY) algorithm for maintaining a
 * bounded in-memory size; the "Least Recently Used" (LRU) entry is the first
 * available for removal from the cache.
 * <p/>
 * This implementation uses a "soft limit" to the in-memory size of the cache,
 * meaning that all cache entries are kept within a completely
 * {@link java.lang.ref.SoftReference}-based map with the most recently utilized
 * entries additionally kept in a hard-reference manner to prevent those cache
 * entries soft references from becoming enqueued by the garbage collector.
 * Thus the actual size of this cache impl can actually grow beyond the stated
 * max size bound as long as GC is not actively seeking soft references for
 * enqueuement.
 *
 * @author Steve Ebersole
 */
public class SoftLimitMRUCache implements Serializable {

	public static final int DEFAULT_STRONG_REF_COUNT = 128;

	private final int strongReferenceCount;

	// actual cache of the entries.  soft references are used for both the keys and the
	// values here since the values pertaining to the MRU entries are kept in a
	// seperate hard reference cache (to avoid their enqueuement/garbage-collection).
	private transient ReferenceMap softReferenceCache = new ReferenceMap( ReferenceMap.SOFT, ReferenceMap.SOFT );
	// the MRU cache used to keep hard references to the most recently used query plans;
	// note : LRU here is a bit of a misnomer, it indicates that LRU entries are removed, the
	// actual kept entries are the MRU entries
	private transient LRUMap strongReferenceCache;

	public SoftLimitMRUCache() {
		this( DEFAULT_STRONG_REF_COUNT );
	}

	public SoftLimitMRUCache(int strongRefCount) {
		this.strongReferenceCount = strongRefCount;
		init();
	}

	public synchronized Object get(Object key) {
		Object result = softReferenceCache.get( key );
		if ( result != null ) {
			strongReferenceCache.put( key, result );
		}
		return result;
	}

	public synchronized Object put(Object key, Object value) {
		softReferenceCache.put( key, value );
		return strongReferenceCache.put( key, value );
	}

	public synchronized int size() {
		return strongReferenceCache.size();
	}

	public synchronized int softSize() {
		return softReferenceCache.size();
	}

	private void init() {
		strongReferenceCache = new LRUMap( strongReferenceCount );
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		init();
	}

	public synchronized void clear() {
		strongReferenceCache.clear();
		softReferenceCache.clear();
	}
}
