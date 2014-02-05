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

import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;

import org.hibernate.cfg.Environment;

/**
 * Cache following a "Most Recently Used" (MRU) algorithm for maintaining a
 * bounded in-memory size; the "Least Recently Used" (LRU) entry is the first
 * available for removal from the cache.
 * <p/>
 * This implementation uses a "soft limit" to the in-memory size of the cache,
 * meaning that all cache entries are kept within a completely
 * {@link java.lang.ref.SoftReference}-based map with the most recently utilized
 * entries additionally kept in a hard-reference manner to prevent those cache
 * entries soft references from becoming enqueued by the garbage collector. Thus
 * the actual size of this cache impl can actually grow beyond the stated max
 * size bound as long as GC is not actively seeking soft references for
 * enqueuement.
 * <p/>
 * The soft-size is bounded and configurable. This allows controlling memory
 * usage which can grow out of control under some circumstances, especially when
 * very large heaps are in use. Although memory usage per se should not be a
 * problem with soft references, which are cleared when necessary, this can
 * trigger extremely slow stop-the-world GC pauses when nearing full heap usage,
 * even with CMS concurrent GC (i.e. concurrent mode failure). This is most
 * evident when ad-hoc HQL queries are produced by the application, leading to
 * poor soft-cache hit ratios. This can also occur with heavy use of SQL IN
 * clauses, which will generate multiples SQL queries (even if parameterized),
 * one for each collection/array size passed to the IN clause. Many slightly
 * different queries will eventually fill the heap and trigger a full GC to
 * reclaim space, leading to unacceptable pauses in some cases.
 * <p/>
 * <strong>Note:</strong> This class is serializable, however all entries are
 * discarded on serialization.
 * 
 * @see Environment#QUERY_PLAN_CACHE_MAX_STRONG_REFERENCES
 * @see Environment#QUERY_PLAN_CACHE_MAX_SOFT_REFERENCES
 * 
 * @author Steve Ebersole
 * @author Manuel Dominguez Sarmiento
 */
public class SoftLimitMRUCache implements Serializable {

	// Constants

	/**
	 * The default strong reference count.
	 */
	public static final int DEFAULT_STRONG_REF_COUNT = 128;

	/**
	 * The default soft reference count.
	 */
	public static final int DEFAULT_SOFT_REF_COUNT = 2048;

	// Fields

	private final int strongRefCount;

	private final int softRefCount;

	private transient LRUMap strongRefCache;

	private transient LRUMap softRefCache;

	private transient ReferenceQueue referenceQueue;

	// Constructors

	/**
	 * Constructs a cache with the default settings.
	 * 
	 * @see #DEFAULT_STRONG_REF_COUNT
	 * @see #DEFAULT_SOFT_REF_COUNT
	 */
	public SoftLimitMRUCache() {
		this(DEFAULT_STRONG_REF_COUNT, DEFAULT_SOFT_REF_COUNT);
	}

	/**
	 * Constructs a cache with the specified settings.
	 * 
	 * @param strongRefCount
	 *            the strong reference count.
	 * @param softRefCount
	 *            the soft reference count.
	 * @throws IllegalArgumentException
	 *             if either of the arguments is less than one, or if the strong
	 *             reference count is higher than the soft reference count.
	 */
	public SoftLimitMRUCache(int strongRefCount, int softRefCount) {

		if (strongRefCount < 1 || softRefCount < 1) {
			throw new IllegalArgumentException();
		}

		if (strongRefCount > softRefCount) {
			throw new IllegalArgumentException();
		}

		this.strongRefCount = strongRefCount;
		this.softRefCount = softRefCount;
		init();
	}

	// Cache methods

	/**
	 * Gets an object from the cache.
	 * 
	 * @param key
	 *            the cache key.
	 * @return the stored value, or <code>null</code> if no entry exists.
	 */
	public synchronized Object get(Object key) {

		if (key == null) {
			throw new NullPointerException();
		}

		clearObsoleteReferences();

		SoftReference ref = (SoftReference) softRefCache.get(key);
		if (ref != null) {
			Object refValue = ref.get();
			if (refValue != null) {
				// This ensures recently used entries are strongly-reachable
				strongRefCache.put(key, refValue);
				return refValue;
			}
		}

		return null;
	}

	/**
	 * Puts a value in the cache.
	 * 
	 * @param key
	 *            the key.
	 * @param value
	 *            the value.
	 * @return the previous value stored in the cache, if any.
	 */
	public synchronized Object put(Object key, Object value) {

		if (key == null || value == null) {
			throw new NullPointerException();
		}

		clearObsoleteReferences();

		strongRefCache.put(key, value);
		SoftReference ref = (SoftReference) softRefCache.put(key,
				new KeyedSoftReference(key, value, referenceQueue));

		return (ref != null) ? ref.get() : null;
	}

	/**
	 * Gets the strong reference cache size.
	 * 
	 * @return the strong reference cache size.
	 */
	public synchronized int size() {
		clearObsoleteReferences();
		return strongRefCache.size();
	}

	/**
	 * Gets the soft reference cache size.
	 * 
	 * @return the soft reference cache size.
	 */
	public synchronized int softSize() {
		clearObsoleteReferences();
		return softRefCache.size();
	}

	/**
	 * Clears the cache.
	 */
	public synchronized void clear() {
		strongRefCache.clear();
		softRefCache.clear();
	}

	// Private helper methods

	private void init() {
		this.strongRefCache = new LRUMap(strongRefCount);
		this.softRefCache = new LRUMap(softRefCount);
		this.referenceQueue = new ReferenceQueue();
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException,
			ClassNotFoundException {

		in.defaultReadObject();
		init();
	}

	private void clearObsoleteReferences() {

		// Clear entries for soft references
		// removed by garbage collector
		KeyedSoftReference obsoleteRef;
		while ((obsoleteRef = (KeyedSoftReference) referenceQueue.poll()) != null) {
			Object key = obsoleteRef.getKey();
			softRefCache.remove(key);
		}
	}

	// Private static class

	private static class KeyedSoftReference extends SoftReference {

		private final Object key;

		private KeyedSoftReference(Object key, Object value, ReferenceQueue q) {
			super(value, q);
			this.key = key;
		}

		private Object getKey() {
			return key;
		}
	}
}
