/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.collection.internal;

import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.collection.BasicCollectionPersister;

/**
 * A persistent wrapper for a <tt>java.util.SortedMap</tt>. Underlying
 * collection is a <tt>TreeMap</tt>.
 *
 * @see java.util.TreeMap
 * @author <a href="mailto:doug.currie@alum.mit.edu">e</a>
 */
public class PersistentSortedMap extends PersistentMap implements SortedMap {

	protected Comparator comparator;

	protected Serializable snapshot(BasicCollectionPersister persister, EntityMode entityMode) throws HibernateException {
		TreeMap clonedMap = new TreeMap(comparator);
		Iterator iter = map.entrySet().iterator();
		while ( iter.hasNext() ) {
			Map.Entry e = (Map.Entry) iter.next();
			clonedMap.put( e.getKey(), persister.getElementType().deepCopy( e.getValue(), persister.getFactory() ) );
		}
		return clonedMap;
	}

	public PersistentSortedMap(SessionImplementor session) {
		super(session);
	}

	public void setComparator(Comparator comparator) {
		this.comparator = comparator;
	}

	public PersistentSortedMap(SessionImplementor session, SortedMap map) {
		super(session, map);
		comparator = map.comparator();
	}

	public PersistentSortedMap() {} //needed for SOAP libraries, etc

	/**
	 * @see PersistentSortedMap#comparator()
	 */
	public Comparator comparator() {
		return comparator;
	}

	/**
	 * @see PersistentSortedMap#subMap(Object, Object)
	 */
	public SortedMap subMap(Object fromKey, Object toKey) {
		read();
		SortedMap m = ( (SortedMap) map ).subMap(fromKey, toKey);
		return new SortedSubMap(m);
	}

	/**
	 * @see PersistentSortedMap#headMap(Object)
	 */
	public SortedMap headMap(Object toKey) {
		read();
		SortedMap m;
		m = ( (SortedMap) map ).headMap(toKey);
		return new SortedSubMap(m);
	}

	/**
	 * @see PersistentSortedMap#tailMap(Object)
	 */
	public SortedMap tailMap(Object fromKey) {
		read();
		SortedMap m;
		m = ( (SortedMap) map ).tailMap(fromKey);
		return new SortedSubMap(m);
	}

	/**
	 * @see PersistentSortedMap#firstKey()
	 */
	public Object firstKey() {
		read();
		return ( (SortedMap) map ).firstKey();
	}

	/**
	 * @see PersistentSortedMap#lastKey()
	 */
	public Object lastKey() {
		read();
		return ( (SortedMap) map ).lastKey();
	}

	class SortedSubMap implements SortedMap {

		SortedMap submap;

		SortedSubMap(SortedMap m) {
			this.submap = m;
		}
		// from Map
		public int size() {
			return submap.size();
		}
		public boolean isEmpty() {
			return submap.isEmpty();
		}
		public boolean containsKey(Object key) {
			return submap.containsKey(key);
		}
		public boolean containsValue(Object key) {
			return submap.containsValue(key) ;
		}
		public Object get(Object key) {
			return submap.get(key);
		}
		public Object put(Object key, Object value) {
			write();
			return submap.put(key,  value);
		}
		public Object remove(Object key) {
			write();
			return submap.remove(key);
		}
		public void putAll(Map other) {
			write();
			submap.putAll(other);
		}
		public void clear() {
			write();
			submap.clear();
		}
		public Set keySet() {
			return new SetProxy( submap.keySet() );
		}
		public Collection values() {
			return new SetProxy( submap.values() );
		}
		public Set entrySet() {
			return new EntrySetProxy( submap.entrySet() );
		}
		// from SortedMap
		public Comparator comparator() {
			return submap.comparator();
		}
		public SortedMap subMap(Object fromKey, Object toKey) {
			SortedMap m;
			m = submap.subMap(fromKey, toKey);
			return new SortedSubMap( m );
		}
		public SortedMap headMap(Object toKey) {
			SortedMap m;
			m = submap.headMap(toKey);
			return new SortedSubMap(m);
		}
		public SortedMap tailMap(Object fromKey) {
			SortedMap m;
			m = submap.tailMap(fromKey);
			return new SortedSubMap(m);
		}
		public Object firstKey() {
			return  submap.firstKey();
		}
		public Object lastKey() {
			return submap.lastKey();
		}

	}

}







