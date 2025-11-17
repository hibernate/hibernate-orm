/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util.collections;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static java.lang.System.identityHashCode;
import static org.hibernate.internal.util.collections.CollectionHelper.setOfSize;

/**
 * A {@link Map} where keys are compared by object identity,
 * rather than using {@link #equals(Object)}.
 */
public final class IdentityMap<K,V> implements Map<K,V> {

	private final LinkedHashMap<IdentityKey<K>,V> map;

	private transient Entry<K,V>[] entryArray = null;

	/**
	 * Return a new instance of this class, with iteration
	 * order defined as the order in which entries were added
	 *
	 * @param size The size of the map to create
	 * @return The map
	 */
	public static <K,V> IdentityMap<K,V> instantiateSequenced(int size) {
		return new IdentityMap<>( new LinkedHashMap<>( size << 1, 0.6f ) );
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
	 * Return the map entries (as instances of {@code Map.Entry} in a collection that
	 * is safe from concurrent modification). That is, we may safely add new instances
	 * to the underlying {@code Map} during iteration of the {@code entries()}.
	 *
	 * @param map The map of entries
	 */
	public static <K,V> Entry<K,V>[] concurrentEntries(Map<K,V> map) {
		return ( (IdentityMap<K,V>) map ).entryArray();
	}

	public static <K,V> void onEachKey(Map<K,V> map, Consumer<K> consumer) {
		final IdentityMap<K, V> identityMap = (IdentityMap<K, V>) map;
		identityMap.map.forEach( (kIdentityKey, v) -> consumer.accept( kIdentityKey.key ) );
	}

	/**
	 * Overrides {@link Map#forEach(BiConsumer)} with a more efficient implementation.
	 * @param action the operation to apply to each element
	 */
	@Override
	public void forEach(BiConsumer<? super K, ? super V> action) {
		map.forEach( (k,v) -> action.accept( k.key, v ) );
	}

	public Iterator<K> keyIterator() {
		return new KeyIterator<>( map.keySet().iterator() );
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
	public boolean containsKey(Object key) {
		return map.containsKey( new IdentityKey<>( key ) );
	}

	@Override
	public boolean containsValue(Object val) {
		throw new UnsupportedOperationException( "Avoid this operation: does not perform well" );
		//return map.containsValue( val );
	}

	@Override
	public V get(Object key) {
		return map.get( new IdentityKey<>( key ) );
	}

	@Override
	public V put(K key, V value) {
		this.entryArray = null;
		return map.put( new IdentityKey<>( key ), value );
	}

	@Override
	public V remove(Object key) {
		this.entryArray = null;
		return map.remove( new IdentityKey<>( key ) );
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> otherMap) {
		for ( var entry : otherMap.entrySet() ) {
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
		final Set<Entry<K,V>> set = setOfSize( map.size() );
		for ( var entry : map.entrySet() ) {
			set.add( new IdentityMapEntry<>( entry.getKey().key, entry.getValue() ) );
		}
		return set;
	}

	public Entry<K,V>[] entryArray() {
		if ( entryArray == null ) {
			//noinspection unchecked
			entryArray = new Entry[ map.size() ];
			final var iterator = map.entrySet().iterator();
			int i = 0;
			while ( iterator.hasNext() ) {
				final var entry = iterator.next();
				entryArray[i++] = new IdentityMapEntry<>( entry.getKey().key, entry.getValue() );
			}
		}
		return entryArray;
	}

	@Override
	public String toString() {
		return map.toString();
	}

	private record KeyIterator<K>(Iterator<IdentityKey<K>> identityKeyIterator)
			implements Iterator<K> {
		@Override
		public boolean hasNext() {
			return identityKeyIterator.hasNext();
		}
		@Override
		public K next() {
			return identityKeyIterator.next().key;
		}
		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	private record IdentityMapEntry<K, V>(K key, V value)
			implements Entry<K, V> {
		@Override
		public K getKey() {
			return key;
		}
		@Override
		public V getValue() {
			return value;
		}
		@Override
		public V setValue(final V value) {
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * We need to base the identity on {@link System#identityHashCode(Object)}
	 */
	private record IdentityKey<K>(K key)
			implements Serializable {
		@Override
		public boolean equals(Object other) {
			return other instanceof IdentityKey<?> that
				&& this.key == that.key;
		}
		@Override
		public int hashCode() {
			return identityHashCode( key );
		}
		@Override
		public String toString() {
			return key.toString();
		}
	}

}
