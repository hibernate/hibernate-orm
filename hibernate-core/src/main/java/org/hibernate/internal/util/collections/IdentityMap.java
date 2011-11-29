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
package org.hibernate.internal.util.collections;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A <tt>Map</tt> where keys are compared by object identity,
 * rather than <tt>equals()</tt>.
 */
public final class IdentityMap<K,V> implements Map<K,V> {

	private final Map<IdentityKey<K>,V> map;
	@SuppressWarnings( {"unchecked"})
	private transient Entry<IdentityKey<K>,V>[] entryArray = new Entry[0];
	private transient boolean dirty = false;

	/**
	 * Return a new instance of this class, with an undefined
	 * iteration order.
	 *
	 * @param size The size of the map
	 * @return Map
	 */
	public static <K,V> Map<K,V> instantiate(int size) {
		return new java.util.IdentityHashMap<K, V>( size );
	}

	/**
	 * Return a new instance of this class, with iteration
	 * order defined as the order in which entries were added
	 *
	 * @param size The size of the map to create
	 * @return The map
	 */
	public static <K,V> IdentityMap<K,V> instantiateSequenced(int size) {
		return new IdentityMap<K,V>( new LinkedHashMap<IdentityKey<K>,V>( size ) );
	}

	/**
	 * Private ctor used in serialization.
	 *
	 * @param underlyingMap The delegate map.
	 */
	private IdentityMap(Map<IdentityKey<K>,V> underlyingMap) {
		map = underlyingMap;
		dirty = true;
	}

	/**
	 * Return the map entries (as instances of <tt>Map.Entry</tt> in a collection that
	 * is safe from concurrent modification). ie. we may safely add new instances to
	 * the underlying <tt>Map</tt> during iteration of the <tt>entries()</tt>.
	 *
	 * @param map The map of entries
	 * @return Collection
	 */
	public static <K,V> Map.Entry<K,V>[] concurrentEntries(Map<K,V> map) {
		return ( (IdentityMap<K,V>) map ).entryArray();
	}

	public static <K,V> Iterator<K> keyIterator(Map<K,V> map) {
		return ( (IdentityMap<K,V>) map ).keyIterator();
	}

	public Iterator keyIterator() {
		return new KeyIterator( map.keySet().iterator() );
	}

	public static final class IdentityMapEntry<K,V> implements java.util.Map.Entry<K,V> {
		private K key;
		private V value;

		IdentityMapEntry(K key, V value) {
			this.key=key;
			this.value=value;
		}

		public K getKey() {
			return key;
		}

		public V getValue() {
			return value;
		}

		public V setValue(V value) {
			V result = this.value;
			this.value = value;
			return result;
		}
	}

	public static final class IdentityKey<K> implements Serializable {
		private K key;

		IdentityKey(K key) {
			this.key=key;
		}

		@SuppressWarnings( {"EqualsWhichDoesntCheckParameterClass"})
		@Override
        public boolean equals(Object other) {
			return key == ( (IdentityKey) other ).key;
		}

		@Override
        public int hashCode() {
			return System.identityHashCode(key);
		}

		@Override
        public String toString() {
			return key.toString();
		}

		public K getRealKey() {
			return key;
		}
	}

	public int size() {
		return map.size();
	}

	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public boolean containsKey(Object key) {
		IdentityKey k = new IdentityKey(key);
		return map.containsKey(k);
	}

	@Override
	public boolean containsValue(Object val) {
		return map.containsValue(val);
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public V get(Object key) {
		return map.get( new IdentityKey(key) );
	}

	@Override
	public V put(K key, V value) {
		dirty = true;
		return map.put( new IdentityKey<K>(key), value );
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public V remove(Object key) {
		dirty = true;
		return map.remove( new IdentityKey(key) );
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> otherMap) {
		for ( Entry<? extends K, ? extends V> entry : otherMap.entrySet() ) {
			put( entry.getKey(), entry.getValue() );
		}
	}

	@Override
	public void clear() {
		dirty = true;
		entryArray = null;
		map.clear();
	}

	@Override
	public Set<K> keySet() {
		// would need an IdentitySet for this!
		throw new UnsupportedOperationException();
	}

	@Override
	public Collection<V> values() {
		return map.values();
	}

	@Override
	public Set<Entry<K,V>> entrySet() {
		Set<Entry<K,V>> set = new HashSet<Entry<K,V>>( map.size() );
		for ( Entry<IdentityKey<K>, V> entry : map.entrySet() ) {
			set.add( new IdentityMapEntry<K,V>( entry.getKey().getRealKey(), entry.getValue() ) );
		}
		return set;
	}

	public List<Entry<K,V>> entryList() {
		ArrayList<Entry<K,V>> list = new ArrayList<Entry<K,V>>( map.size() );
		for ( Entry<IdentityKey<K>, V> entry : map.entrySet() ) {
			list.add( new IdentityMapEntry<K,V>( entry.getKey().getRealKey(), entry.getValue() ) );
		}
		return list;
	}

	@SuppressWarnings( {"unchecked"})
	public Map.Entry[] entryArray() {
		if (dirty) {
			entryArray = new Map.Entry[ map.size() ];
			Iterator itr = map.entrySet().iterator();
			int i=0;
			while ( itr.hasNext() ) {
				Map.Entry me = (Map.Entry) itr.next();
				entryArray[i++] = new IdentityMapEntry( ( (IdentityKey) me.getKey() ).key, me.getValue() );
			}
			dirty = false;
		}
		return entryArray;
	}

	/**
	 * Workaround for a JDK 1.4.1 bug where <tt>IdentityHashMap</tt>s are not
	 * correctly deserialized.
	 *
	 * @param map The map to serialize
	 * @return Object
	 */
	public static Object serialize(Map map) {
		return ( (IdentityMap) map ).map;
	}

	/**
	 * Workaround for a JDK 1.4.1 bug where <tt>IdentityHashMap</tt>s are not
	 * correctly deserialized.
	 *
	 * @param o the serialized map data
	 * @return The deserialized map
	 */
	@SuppressWarnings( {"unchecked"})
	public static <K,V> Map<K,V> deserialize(Object o) {
		return new IdentityMap<K,V>( (Map<IdentityKey<K>,V>) o );
	}
	
	@Override
    public String toString() {
		return map.toString();
	}

	public static <K,V> Map<V,K> invert(Map<K,V> map) {
		Map<V,K> result = new IdentityHashMap( map.size() );
		for ( Entry<K, V> entry : map.entrySet() ) {
			result.put( entry.getValue(), entry.getKey() );
		}
		return result;
	}

	static final class KeyIterator<K> implements Iterator<K> {
		private final Iterator<IdentityKey<K>> identityKeyIterator;

		private KeyIterator(Iterator<IdentityKey<K>> iterator) {
			identityKeyIterator = iterator;
		}

		public boolean hasNext() {
			return identityKeyIterator.hasNext();
		}

		public K next() {
			return identityKeyIterator.next().getRealKey();
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

	}

}