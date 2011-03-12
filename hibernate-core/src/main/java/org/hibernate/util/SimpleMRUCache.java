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

import org.apache.commons.collections.map.LRUMap;

import java.io.Serializable;
import java.io.IOException;

/**
 * Cache following a "Most Recently Used" (MRU) algorithm for maintaining a
 * bounded in-memory size; the "Least Recently Used" (LRU) entry is the first
 * available for removal from the cache.
 * <p/>
 * This implementation uses a bounded MRU Map to limit the in-memory size of
 * the cache.  Thus the size of this cache never grows beyond the stated size.
 *
 * @author Steve Ebersole
 */
public class SimpleMRUCache implements Serializable {

	public static final int DEFAULT_STRONG_REF_COUNT = 128;

	private final int strongReferenceCount;
	private transient LRUMap cache;

	public SimpleMRUCache() {
		this( DEFAULT_STRONG_REF_COUNT );
	}

	public SimpleMRUCache(int strongReferenceCount) {
		this.strongReferenceCount = strongReferenceCount;
		init();
	}

	public synchronized Object get(Object key) {
		return cache.get( key );
	}

	public synchronized Object put(Object key, Object value) {
		return cache.put( key, value );
	}

	public synchronized int size() {
		return cache.size();
	}

	private void init() {
		cache = new LRUMap( strongReferenceCount );
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		init();
	}

	public synchronized void clear() {
		cache.clear();
	}
}
