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

	/**
	 * Constructs a PersistentSortedMap.  This form needed for SOAP libraries, etc
	 */
	@SuppressWarnings("UnusedDeclaration")
	public PersistentSortedMap() {
	}

	/**
	 * Constructs a PersistentSortedMap.
	 *
	 * @param session The session
	 */
	public PersistentSortedMap(SessionImplementor session) {
		super( session );
	}

	/**
	 * Constructs a PersistentSortedMap.
	 *
	 * @param session The session
	 * @param map The underlying map data
	 */
	public PersistentSortedMap(SessionImplementor session, SortedMap map) {
		super( session, map );
		comparator = map.comparator();
	}

	@SuppressWarnings({"unchecked", "UnusedParameters"})
	protected Serializable snapshot(BasicCollectionPersister persister, EntityMode entityMode) throws HibernateException {
		final TreeMap clonedMap = new TreeMap( comparator );
		for ( Object o : map.entrySet() ) {
			final Entry e = (Entry) o;
			clonedMap.put( e.getKey(), persister.getElementType().deepCopy( e.getValue(), persister.getFactory() ) );
		}
		return clonedMap;
	}

	public void setComparator(Comparator comparator) {
		this.comparator = comparator;
	}

	@Override
	public Comparator comparator() {
		return comparator;
	}

	@Override
	@SuppressWarnings("unchecked")
	public SortedMap subMap(Object fromKey, Object toKey) {
		read();
		final SortedMap subMap = ( (SortedMap) map ).subMap( fromKey, toKey );
		return new SortedSubMap( subMap );
	}

	@Override
	@SuppressWarnings("unchecked")
	public SortedMap headMap(Object toKey) {
		read();
		final SortedMap headMap = ( (SortedMap) map ).headMap( toKey );
		return new SortedSubMap( headMap );
	}

	@Override
	@SuppressWarnings("unchecked")
	public SortedMap tailMap(Object fromKey) {
		read();
		final SortedMap tailMap = ( (SortedMap) map ).tailMap( fromKey );
		return new SortedSubMap( tailMap );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object firstKey() {
		read();
		return ( (SortedMap) map ).firstKey();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object lastKey() {
		read();
		return ( (SortedMap) map ).lastKey();
	}

	class SortedSubMap implements SortedMap {
		SortedMap subMap;

		SortedSubMap(SortedMap subMap) {
			this.subMap = subMap;
		}

		@Override
		@SuppressWarnings("unchecked")
		public int size() {
			return subMap.size();
		}

		@Override
		@SuppressWarnings("unchecked")
		public boolean isEmpty() {
			return subMap.isEmpty();
		}

		@Override
		@SuppressWarnings("unchecked")
		public boolean containsKey(Object key) {
			return subMap.containsKey( key );
		}

		@Override
		@SuppressWarnings("unchecked")
		public boolean containsValue(Object key) {
			return subMap.containsValue( key ) ;
		}

		@Override
		@SuppressWarnings("unchecked")
		public Object get(Object key) {
			return subMap.get( key );
		}

		@Override
		@SuppressWarnings("unchecked")
		public Object put(Object key, Object value) {
			write();
			return subMap.put( key,  value );
		}

		@Override
		@SuppressWarnings("unchecked")
		public Object remove(Object key) {
			write();
			return subMap.remove( key );
		}

		@Override
		@SuppressWarnings("unchecked")
		public void putAll(Map other) {
			write();
			subMap.putAll( other );
		}

		@Override
		@SuppressWarnings("unchecked")
		public void clear() {
			write();
			subMap.clear();
		}

		@Override
		@SuppressWarnings("unchecked")
		public Set keySet() {
			return new SetProxy( subMap.keySet() );
		}

		@Override
		@SuppressWarnings("unchecked")
		public Collection values() {
			return new SetProxy( subMap.values() );
		}

		@Override
		@SuppressWarnings("unchecked")
		public Set entrySet() {
			return new EntrySetProxy( subMap.entrySet() );
		}

		@Override
		@SuppressWarnings("unchecked")
		public Comparator comparator() {
			return subMap.comparator();
		}

		@Override
		@SuppressWarnings("unchecked")
		public SortedMap subMap(Object fromKey, Object toKey) {
			final SortedMap subMap = this.subMap.subMap( fromKey, toKey );
			return new SortedSubMap( subMap );
		}

		@Override
		@SuppressWarnings("unchecked")
		public SortedMap headMap(Object toKey) {
			final SortedMap headMap = subMap.headMap( toKey );
			return new SortedSubMap( headMap );
		}

		@Override
		@SuppressWarnings("unchecked")
		public SortedMap tailMap(Object fromKey) {
			final SortedMap tailMap = subMap.tailMap( fromKey );
			return new SortedSubMap( tailMap );
		}

		@Override
		@SuppressWarnings("unchecked")
		public Object firstKey() {
			return subMap.firstKey();
		}

		@Override
		@SuppressWarnings("unchecked")
		public Object lastKey() {
			return subMap.lastKey();
		}
	}
}
