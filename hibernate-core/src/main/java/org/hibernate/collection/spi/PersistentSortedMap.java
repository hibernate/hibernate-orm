/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.collection.spi;

import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.BasicCollectionPersister;

/**
 * A persistent wrapper for a {@link java.util.SortedMap}. Underlying
 * collection is a {@code TreeMap}.
 *
 * @apiNote Incubating in terms of making this non-internal.
 *          These contracts will be getting cleaned up in following
 *          releases.
 *
 * @author Doug Currie
 */
@Incubating
public class PersistentSortedMap<K,E> extends PersistentMap<K,E> implements SortedMap<K,E> {
	protected Comparator<? super K> comparator;

	/**
	 * Constructs a PersistentSortedMap.  This form needed for SOAP libraries, etc
	 */
	@SuppressWarnings("unused")
	public PersistentSortedMap() {
	}

	/**
	 * Constructs a PersistentSortedMap.
	 *
	 * @param session The session
	 * @param comparator The sort comparator
	 */
	public PersistentSortedMap(SharedSessionContractImplementor session, Comparator<K> comparator) {
		super( session );
		this.comparator = comparator;
	}

	/**
	 * Constructs a PersistentSortedMap.
	 *
	 * @param session The session
	 * @param map The underlying map data
	 */
	public PersistentSortedMap(SharedSessionContractImplementor session, SortedMap<K,E> map) {
		super( session, map );
		comparator = map.comparator();
	}

	@SuppressWarnings("UnusedParameters")
	protected Serializable snapshot(BasicCollectionPersister persister) throws HibernateException {
		final TreeMap<K,E> clonedMap = new TreeMap<>( comparator );
		for ( Entry<K,E> e : map.entrySet() ) {
			clonedMap.put( e.getKey(), (E) persister.getElementType().deepCopy( e.getValue(), persister.getFactory() ) );
		}
		return clonedMap;
	}

	public void setComparator(Comparator<? super K> comparator) {
		this.comparator = comparator;
	}

	@Override
	public Comparator<? super K> comparator() {
		return comparator;
	}

	@Override
	public SortedMap<K,E> subMap(K fromKey, K toKey) {
		read();
		final SortedMap<K,E> subMap = ( (SortedMap<K,E>) map ).subMap( fromKey, toKey );
		return new SortedSubMap( subMap );
	}

	@Override
	public SortedMap<K,E> headMap(K toKey) {
		read();
		final SortedMap<K,E> headMap = ( (SortedMap<K,E>) map ).headMap( toKey );
		return new SortedSubMap( headMap );
	}

	@Override
	public SortedMap<K,E> tailMap(K fromKey) {
		read();
		final SortedMap<K,E> tailMap = ( (SortedMap<K,E>) map ).tailMap( fromKey );
		return new SortedSubMap( tailMap );
	}

	@Override
	public K firstKey() {
		read();
		return ( (SortedMap<K,E>) map ).firstKey();
	}

	@Override
	public K lastKey() {
		read();
		return ( (SortedMap<K,E>) map ).lastKey();
	}

	class SortedSubMap implements SortedMap<K,E> {
		SortedMap<K,E> subMap;

		SortedSubMap(SortedMap<K,E> subMap) {
			this.subMap = subMap;
		}

		@Override
		public int size() {
			return subMap.size();
		}

		@Override
		public boolean isEmpty() {
			return subMap.isEmpty();
		}

		@Override
		public boolean containsKey(Object key) {
			return subMap.containsKey( key );
		}

		@Override
		public boolean containsValue(Object key) {
			return subMap.containsValue( key ) ;
		}

		@Override
		public E get(Object key) {
			return subMap.get( key );
		}

		@Override
		public E put(K key, E value) {
			write();
			return subMap.put( key,  value );
		}

		@Override
		public E remove(Object key) {
			write();
			return subMap.remove( key );
		}

		@Override
		public void putAll(Map<? extends K,? extends E> other) {
			write();
			subMap.putAll( other );
		}

		@Override
		public void clear() {
			write();
			subMap.clear();
		}

		@Override
		public Set<K> keySet() {
			return new SetProxy<>( subMap.keySet() );
		}

		@Override
		public Collection<E> values() {
			return new SetProxy<>( subMap.values() );
		}

		@Override
		public Set<Entry<K,E>> entrySet() {
			return new EntrySetProxy( subMap.entrySet() );
		}

		@Override
		public Comparator<? super K> comparator() {
			return subMap.comparator();
		}

		@Override
		public SortedMap<K,E> subMap(K fromKey, K toKey) {
			final SortedMap<K,E> subMap = this.subMap.subMap( fromKey, toKey );
			return new SortedSubMap( subMap );
		}

		@Override
		public SortedMap<K,E> headMap(K toKey) {
			final SortedMap<K,E> headMap = subMap.headMap( toKey );
			return new SortedSubMap( headMap );
		}

		@Override
		public SortedMap<K,E> tailMap(K fromKey) {
			final SortedMap<K,E> tailMap = subMap.tailMap( fromKey );
			return new SortedSubMap( tailMap );
		}

		@Override
		public K firstKey() {
			return subMap.firstKey();
		}

		@Override
		public K lastKey() {
			return subMap.lastKey();
		}
	}
}
