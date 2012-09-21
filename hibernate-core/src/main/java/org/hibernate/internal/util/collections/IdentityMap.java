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
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
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

	public Iterator<K> keyIterator() {
		return new KeyIterator<K>( map.keySet().iterator() );
	}

	@Override
	public int size() {
		return map.size();
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public boolean containsKey(Object key) {
		return map.containsKey( new IdentityKey( key ) );
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

	@Override
    public String toString() {
		return map.toString();
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
		public static final class IdentityMapEntry<K,V> implements java.util.Map.Entry<K,V> {
		private final K key;
		private V value;

		IdentityMapEntry(final K key, final V value) {
			this.key=key;
			this.value=value;
		}

		public K getKey() {
			return key;
		}

		public V getValue() {
			return value;
		}

		public V setValue(final V value) {
			V result = this.value;
			this.value = value;
			return result;
		}
	}

	/**
	 * We need to base the identity on {@link System#identityHashCode(Object)} but
	 * attempt to lazily initialize and cache this value: being a native invocation
	 * it is an expensive value to retrieve.
	 */
	public static final class IdentityKey<K> implements Serializable {

		private final K key;
		private int hash = 0;

		IdentityKey(K key) {
			this.key = key;
		}

		@SuppressWarnings( {"EqualsWhichDoesntCheckParameterClass"})
		@Override
		public boolean equals(Object other) {
			return key == ( (IdentityKey) other ).key;
		}

		@Override
		public int hashCode() {
			if ( this.hash == 0 ) {
				//We consider "zero" as non-initialized value
				final int newHash = System.identityHashCode( key );
				if ( newHash == 0 ) {
					//So make sure we don't store zeros as it would trigger initialization again:
					//any value is fine as long as we're deterministic.
					this.hash = -1;
					return -1;
				}
				else {
					this.hash = newHash;
					return newHash;
				}
			}
			return hash;
		}

		@Override
		public String toString() {
			return key.toString();
		}

		public K getRealKey() {
			return key;
		}
	}

}