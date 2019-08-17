/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.util.collections;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A <tt>Map</tt> where keys are compared by object identity,
 * rather than <tt>equals()</tt>.
 */
public final class IdentityMap<K,V> implements Map<K,V> {

	private final LinkedHashMap<IdentityKey<K>,V> map;
	@SuppressWarnings( {"unchecked"})
	private transient Map.Entry<IdentityKey<K>,V>[] entryArray = null;

	/**
	 * Return a new instance of this class, with iteration
	 * order defined as the order in which entries were added
	 *
	 * @param size The size of the map to create
	 * @return The map
	 */
	public static <K,V> IdentityMap<K,V> instantiateSequenced(int size) {
		return new IdentityMap<K,V>( new LinkedHashMap<>( size << 1, 0.6f ) );
	}

	/**
	 * Private ctor used in serialization.
	 *
	 * @param underlyingMap The delegate map.
	 */
	private IdentityMap(LinkedHashMap<IdentityKey<K>,V> underlyingMap) {
		map = underlyingMap;
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

	public static <K,V> void onEachKey(Map<K,V> map, Consumer<K> consumer) {
		final IdentityMap<K, V> identityMap = (IdentityMap<K, V>) map;
		identityMap.map.forEach( (kIdentityKey, v) -> consumer.accept( kIdentityKey.key ) );
	}

	/**
	 * Override Map{@link #forEach(BiConsumer)} to provide a more efficient implementation
	 * @param action the operation to apply to each element
	 */
	@Override
	public void forEach(BiConsumer<? super K, ? super V> action) {
		map.forEach( (k,v) -> action.accept( k.key, v ) );
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
		throw new UnsupportedOperationException( "Avoid this operation: does not perform well" );
		//return map.containsValue( val );
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public V get(Object key) {
		return map.get( new IdentityKey( key ) );
	}

	@Override
	public V put(K key, V value) {
		this.entryArray = null;
		return map.put( new IdentityKey<K>( key ), value );
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public V remove(Object key) {
		this.entryArray = null;
		return map.remove( new IdentityKey( key ) );
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> otherMap) {
		for ( Entry<? extends K, ? extends V> entry : otherMap.entrySet() ) {
			put( entry.getKey(), entry.getValue() );
		}
	}

	@Override
	public void clear() {
		entryArray = null;
		map.clear();
	}

	@Override
	public Set<K> keySet() {
		// would need an IdentitySet for this!
		// (and we just don't use this method so it's ok)
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
			set.add( new IdentityMapEntry<K,V>( entry.getKey().key, entry.getValue() ) );
		}
		return set;
	}

	@SuppressWarnings( {"unchecked"})
	public Map.Entry[] entryArray() {
		if ( entryArray == null ) {
			entryArray = new Map.Entry[ map.size() ];
			final Iterator<Entry<IdentityKey<K>, V>> itr = map.entrySet().iterator();
			int i = 0;
			while ( itr.hasNext() ) {
				final Entry<IdentityKey<K>, V> me = itr.next();
				entryArray[i++] = new IdentityMapEntry( me.getKey().key, me.getValue() );
			}
		}
		return entryArray;
	}

	@Override
	public String toString() {
		return map.toString();
	}

	private static final class KeyIterator<K> implements Iterator<K> {
		private final Iterator<IdentityKey<K>> identityKeyIterator;

		private KeyIterator(Iterator<IdentityKey<K>> iterator) {
			identityKeyIterator = iterator;
		}

		public boolean hasNext() {
			return identityKeyIterator.hasNext();
		}

		public K next() {
			return identityKeyIterator.next().key;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

	}

	private static final class IdentityMapEntry<K,V> implements java.util.Map.Entry<K,V> {

		private final K key;
		private final V value;

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
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * We need to base the identity on {@link System#identityHashCode(Object)}
	 */
	private static final class IdentityKey<K> implements Serializable {

		private final K key;

		IdentityKey(K key) {
			this.key = key;
		}

		@SuppressWarnings( {"EqualsWhichDoesntCheckParameterClass"})
		@Override
		public boolean equals(Object other) {
			return other != null && key == ( (IdentityKey) other ).key;
		}

		@Override
		public int hashCode() {
			return System.identityHashCode( key );
		}

		@Override
		public String toString() {
			return key.toString();
		}

	}

}
