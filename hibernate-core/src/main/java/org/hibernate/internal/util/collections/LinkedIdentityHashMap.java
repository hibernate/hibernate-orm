/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util.collections;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Utility {@link Map} implementation that uses identity (==) for key comparison and
 * preserves insertion order like {@link LinkedHashMap}.
 */
public class LinkedIdentityHashMap<K, V> implements Map<K, V> {
	private final LinkedHashMap<Identity<K>, V> delegate;

	record Identity<K>(K key) {
		@Override
		public int hashCode() {
			return System.identityHashCode( key );
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof Identity<?> that && this.key == that.key;
		}
	}

	public LinkedIdentityHashMap() {
		delegate = new LinkedHashMap<>();
	}

	public LinkedIdentityHashMap(int expectedSize) {
		delegate = new LinkedHashMap<>( expectedSize );
	}

	@Override
	public int size() {
		return delegate.size();
	}

	@Override
	public boolean isEmpty() {
		return delegate.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return delegate.containsKey( new Identity<>( key ) );
	}

	@Override
	public boolean containsValue(Object value) {
		return delegate.containsValue( value );
	}

	@Override
	public V get(Object key) {
		return delegate.get( new Identity<>( key ) );
	}

	@Override
	public V put(K key, V value) {
		return delegate.put( new Identity<>( key ), value );
	}

	@Override
	public V putIfAbsent(K key, V value) {
		return delegate.putIfAbsent( new Identity<>( key ), value );
	}

	@Override
	public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
		return delegate.computeIfAbsent( new Identity<>( key ), k -> mappingFunction.apply( k.key ) );
	}

	@Override
	public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
		return delegate.computeIfPresent( new Identity<>( key ), (k, v) -> remappingFunction.apply( k.key, v ) );
	}

	@Override
	public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
		return delegate.compute( new Identity<>( key ), (k, v) -> remappingFunction.apply( k.key, v ) );
	}

	@Override
	public V remove(Object key) {
		return delegate.remove( new Identity<>( key ) );
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		delegate.putAll( m.entrySet().stream()
				.collect( Collectors.toMap( e -> new Identity<>( e.getKey() ), Entry::getValue ) ) );
	}

	@Override
	public boolean remove(Object key, Object value) {
		return delegate.remove( new Identity<>( key ), value );
	}

	@Override
	public void clear() {
		delegate.clear();
	}

	@Override
	public Set<K> keySet() {
		return delegate.keySet().stream().map( w -> w.key )
				.collect( Collectors.toCollection( LinkedHashSet::new ) );
	}

	@Override
	public Collection<V> values() {
		return delegate.values();
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return delegate.entrySet().stream().map( e -> Map.entry( e.getKey().key, e.getValue() ) )
				.collect( Collectors.toCollection( LinkedHashSet::new ) );
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}
}
