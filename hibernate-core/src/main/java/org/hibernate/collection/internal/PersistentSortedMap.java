/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.collection.internal;

import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.RepresentationMode;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;

/**
 * A persistent wrapper for a <tt>java.util.SortedMap</tt>. Underlying
 * collection is a <tt>TreeMap</tt>.
 *
 * @see java.util.TreeMap
 * @author <a href="mailto:doug.currie@alum.mit.edu">e</a>
 */
public class PersistentSortedMap<K,V> extends PersistentMap<K,V> implements SortedMap<K,V> {
	protected Comparator comparator;

	/**
	 * Constructs a PersistentSortedMap.  This form needed for SOAP libraries, etc
	 */
	@SuppressWarnings("UnusedDeclaration")
	protected PersistentSortedMap() {
	}

	/**
	 * Constructs a PersistentSortedMap.
	 *
	 * @param session The session
	 */
	public PersistentSortedMap(
			SharedSessionContractImplementor session,
			PersistentCollectionDescriptor<?,?,V> descriptor) {
		super( session, descriptor );
		this.comparator = descriptor.getSortingComparator();
	}

	/**
	 * Constructs a PersistentSortedMap.
	 *
	 * @param session The session
	 * @param map The underlying map data
	 */
	public PersistentSortedMap(
			SharedSessionContractImplementor session,
			PersistentCollectionDescriptor<?,?,V> descriptor,
			SortedMap map) {
		super( session, descriptor );
		comparator = map.comparator();
	}

	/**
	 * Constructs a PersistentSortedMap.
	 */
	public PersistentSortedMap(
			SharedSessionContractImplementor session,
			PersistentCollectionDescriptor<?,?,V> descriptor,
			Object key) {
		super( session, descriptor, key );
		this.comparator = descriptor.getSortingComparator();
	}

	@SuppressWarnings({"unchecked", "UnusedParameters"})
	protected Serializable snapshot(PersistentCollectionDescriptor<?,?,V> descriptor, RepresentationMode entityMode) throws HibernateException {
		final TreeMap clonedMap = new TreeMap( comparator );
		for ( Entry<K, V> entry : map.entrySet() ) {
			clonedMap.put(
					entry.getKey(),
					descriptor.getElementDescriptor().getJavaTypeDescriptor().getMutabilityPlan().deepCopy( entry.getValue() )
			);
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
	public SortedMap<K,V> headMap(K toKey) {
		read();
		final SortedMap<K,V> headMap = map().headMap( toKey );
		return new SortedSubMap( headMap );
	}

	@Override
	@SuppressWarnings("unchecked")
	public SortedMap<K,V> tailMap(K fromKey) {
		read();
		final SortedMap<K,V> tailMap = map().tailMap( fromKey );
		return new SortedSubMap( tailMap );
	}

	@Override
	@SuppressWarnings("unchecked")
	public K firstKey() {
		read();
		return map().firstKey();
	}

	private SortedMap<K,V> map() {
		return (SortedMap<K, V>) map;
	}

	@Override
	@SuppressWarnings("unchecked")
	public K lastKey() {
		read();
		return map().lastKey();
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
