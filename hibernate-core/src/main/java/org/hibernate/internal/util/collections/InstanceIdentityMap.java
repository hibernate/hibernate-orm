/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util.collections;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.engine.spi.InstanceIdentity;
import org.hibernate.internal.build.AllowReflection;

import java.lang.reflect.Array;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * {@link Map} implementation of based on {@link InstanceIdentity}, similar to {@link InstanceIdentityStore}.
 * This collection also stores values using an array-like structure that automatically grows as needed
 * but, contrary to the store, it initializes {@link Map.Entry}s eagerly to optimize iteration
 * performance and avoid type-pollution issues when checking the type of contained objects.
 * <p>
 * Instance ids are considered to start from 1, and if two instances are found to have the
 * same identifier a {@link java.util.ConcurrentModificationException will be thrown}.
 * <p>
 * Methods accessing / modifying the map with {@link Object} typed parameters will need
 * to type check against the instance identity interface which might be inefficient,
 * so it's recommended to use the position (int) based variant of those methods.
 */
public class InstanceIdentityMap<K extends InstanceIdentity, V> extends AbstractPagedArray<Map.Entry<K, V>>
		implements Map<K, V> {
	private int size;

	@Override
	public int size() {
		return size;
	}

	@Override
	public boolean isEmpty() {
		return size == 0;
	}

	/**
	 * Returns {@code true} if this map contains a mapping for the specified instance id.
	 *
	 * @param instanceId the instance id whose associated value is to be returned
	 * @param key key instance to double-check instance equality
	 * @return {@code true} if this map contains a mapping for the specified instance id
	 * @implNote This method accesses the backing array with the provided instance id, but performs an instance
	 * equality check ({@code ==}) with the provided key to ensure it corresponds to the mapped one
	 */
	public boolean containsKey(int instanceId, Object key) {
		return get( instanceId, key ) != null;
	}

	/**
	 * @inheritDoc
	 * @implNote This only works for {@link InstanceIdentity} keys, and it's inefficient
	 * since we need to do a type check. Prefer using {@link #containsKey(int, Object)}.
	 */
	@Override
	public boolean containsKey(Object key) {
		if ( key instanceof InstanceIdentity instance ) {
			return containsKey( instance.$$_hibernate_getInstanceId(), instance );
		}
		throw new ClassCastException( "Provided key does not support instance identity" );
	}

	@Override
	public boolean containsValue(Object value) {
		for ( V v : values() ) {
			if ( Objects.equals( value, v ) ) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Tests if the specified key-value mapping is in the map.
	 *
	 * @param key possible key, must be an instance of {@link InstanceIdentity}
	 * @param value possible value
	 * @return {@code true} if and only if the specified key-value mapping is in the map
	 */
	private boolean containsMapping(Object key, Object value) {
		if ( key instanceof InstanceIdentity instance ) {
			return get( instance.$$_hibernate_getInstanceId(), instance ) == value;
		}
		throw new ClassCastException( "Provided key does not support instance identity" );
	}

	/**
	 * Returns the value to which the specified instance id is mapped, or {@code null} if this map
	 * contains no mapping for the instance id.
	 *
	 * @param instanceId the instance id whose associated value is to be returned
	 * @param key key instance to double-check instance equality
	 * @return the value to which the specified instance id is mapped,
	 * or {@code null} if this map contains no mapping for the instance id
	 * @implNote This method accesses the backing array with the provided instance id, but performs an instance
	 * equality check ({@code ==}) with the provided key to ensure it corresponds to the mapped one
	 */
	public @Nullable V get(int instanceId, Object key) {
		if ( instanceId <= 0 ) {
			return null;
		}

		final Entry<K, V> entry = get( instanceId - 1 );
		if ( entry != null ) {
			if ( entry.getKey() == key ) {
				return entry.getValue();
			}
			else {
				throw new ConcurrentModificationException(
						"Found a different instance corresponding to instanceId [" + instanceId +
						"], this might indicate a concurrent access to this persistence context."
				);
			}
		}
		return null;
	}

	/**
	 * @inheritDoc
	 * @implNote This only works for {@link InstanceIdentity} keys, and it's inefficient
	 * since we need to do a type check. Prefer using {@link #get(int, Object)}.
	 */
	@Override
	public @Nullable V get(Object key) {
		if ( key instanceof InstanceIdentity instance ) {
			return get( instance.$$_hibernate_getInstanceId(), instance );
		}
		throw new ClassCastException( "Provided key does not support instance identity" );
	}

	@Override
	public @Nullable V put(K key, V value) {
		if ( key == null ) {
			throw new NullPointerException( "This map does not support null keys" );
		}

		final int index = key.$$_hibernate_getInstanceId() - 1;
		if ( index < 0 ) {
			throw new IllegalArgumentException( "Instance ID must be a positive value" );
		}

		final Map.Entry<K, V> old = set( index, new AbstractMap.SimpleImmutableEntry<>( key, value ) );
		if ( old == null ) {
			size++;
			return null;
		}
		else {
			return old.getValue();
		}
	}

	/**
	 * Removes the mapping for an instance id from this map if it is present (optional operation).
	 *
	 * @param instanceId the instance id whose associated value is to be returned
	 * @param key key instance to double-check instance equality
	 * @return the previous value associated with {@code instanceId}, or {@code null} if there was no mapping for it.
	 * @implNote This method accesses the backing array with the provided instance id, but performs an instance
	 * equality check ({@code ==}) with the provided key to ensure it corresponds to the mapped one
	 */
	public @Nullable V remove(int instanceId, Object key) {
		if ( instanceId <= 0 ) {
			return null;
		}

		final int index = instanceId - 1;
		final Page<Map.Entry<K, V>> page = getPage( index );
		if ( page != null ) {
			final int pageOffset = toPageOffset( index );
			final Map.Entry<K, V> entry = page.set( pageOffset, null );
			// Check that the provided instance really matches with the key contained in the map
			if ( entry != null ) {
				if ( entry.getKey() == key ) {
					size--;
					return entry.getValue();
				}
				else {
					throw new ConcurrentModificationException(
							"Found a different instance corresponding to instanceId [" + instanceId +
							"], this might indicate a concurrent access to this persistence context."
					);
				}
			}
		}
		return null;
	}

	/**
	 * @inheritDoc
	 * @implNote This only works for {@link InstanceIdentity} keys, and it's inefficient
	 * since we need to do a type check. Prefer using {@link #remove(int, Object)}.
	 */
	@Override
	public @Nullable V remove(Object key) {
		if ( key instanceof InstanceIdentity instance ) {
			return remove( instance.$$_hibernate_getInstanceId(), instance );
		}
		throw new ClassCastException( "Provided key does not support instance identity" );
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		for ( Entry<? extends K, ? extends V> entry : m.entrySet() ) {
			put( entry.getKey(), entry.getValue() );
		}
	}

	@Override
	public @Nullable V putIfAbsent(K key, V value) {
		V v = get( key.$$_hibernate_getInstanceId(), key );
		if ( v == null ) {
			v = put( key, value );
		}
		return v;
	}

	@Override
	public void clear() {
		super.clear();
		size = 0;
	}

	@Override
	public @NonNull Set<K> keySet() {
		return new KeySet();
	}

	@Override
	public @NonNull Collection<V> values() {
		return new Values();
	}

	@Override
	public @NonNull Set<Map.Entry<K, V>> entrySet() {
		return new EntrySet();
	}

	@Override
	public void forEach(BiConsumer<? super K, ? super V> action) {
		for ( final Page<Map.Entry<K, V>> page : elementPages ) {
			if ( page != null ) {
				for ( int j = 0; j <= page.lastNotEmptyOffset(); j++ ) {
					final Map.Entry<K, V> entry = page.get( j );
					if ( entry != null ) {
						action.accept( entry.getKey(), entry.getValue() );
					}
				}
			}
		}
	}

	public Map.Entry<K, V>[] toArray() {
		//noinspection unchecked
		return entrySet().toArray( new Map.Entry[0] );
	}

	private class KeyIterator extends PagedArrayIterator<K> {
		@Override
		public K next() {
			return get( nextIndex() ).getKey();
		}
	}

	private class ValueIterator extends PagedArrayIterator<V> {
		@Override
		public V next() {
			return get( nextIndex() ).getValue();
		}
	}

	private class EntryIterator extends PagedArrayIterator<Map.Entry<K, V>> {
		@Override
		public Map.Entry<K, V> next() {
			return new Entry( nextIndex() );
		}

		private class Entry implements Map.Entry<K, V> {
			private final int index;

			private Entry(int index) {
				this.index = index;
			}

			@Override
			public K getKey() {
				return get( index ).getKey();
			}

			@Override
			public V getValue() {
				return get( index ).getValue();
			}

			@Override
			public V setValue(V value) {
				throw new UnsupportedOperationException();
			}

			@Override
			public boolean equals(Object o) {
				return o instanceof Map.Entry<?, ?> e
					&& Objects.equals( e.getKey(), getKey() )
					&& Objects.equals( e.getValue(), getValue() );
			}

			@Override
			public int hashCode() {
				return getKey().hashCode() ^ Objects.hashCode( getValue() );
			}

			@Override
			public String toString() {
				return getKey() + "=" + getValue();
			}
		}
	}

	private class KeySet extends AbstractSet<K> {
		@Override
		public @NonNull Iterator<K> iterator() {
			return new KeyIterator();
		}

		@Override
		public int size() {
			return InstanceIdentityMap.this.size();
		}

		@Override
		public boolean contains(Object o) {
			return containsKey( o );
		}
	}

	private class Values extends AbstractCollection<V> {
		@Override
		public @NonNull Iterator<V> iterator() {
			return new ValueIterator();
		}

		@Override
		public int size() {
			return InstanceIdentityMap.this.size();
		}
	}

	private class EntrySet extends AbstractSet<Entry<K, V>> {
		@Override
		public @NonNull Iterator<Entry<K, V>> iterator() {
			return new EntryIterator();
		}

		@Override
		public int size() {
			return InstanceIdentityMap.this.size();
		}

		@Override
		public boolean contains(Object o) {
			return o instanceof Entry<?, ?> entry
				&& containsMapping( entry.getKey(), entry.getValue() );
		}

		@Override
		public @NonNull Object @NonNull [] toArray() {
			return toArray( new Object[0] );
		}

		@Override
		@AllowReflection
		@SuppressWarnings("unchecked")
		public <T> @NonNull T @NonNull [] toArray(T[] a) {
			int size = size();
			if ( a.length < size ) {
				a = (T[]) Array.newInstance( a.getClass().getComponentType(), size );
			}
			int i = 0;
			for ( Page<Entry<K, V>> page : elementPages ) {
				if ( page != null ) {
					for ( int j = 0; j <= page.lastNotEmptyOffset(); j++ ) {
						final Map.Entry<K, V> entry;
						if ( (entry = page.get( j )) != null ) {
							a[i++] = (T) entry;
						}
					}
				}
			}
			// fewer elements than expected or concurrent modification from other thread detected
			if ( i < size ) {
				throw new ConcurrentModificationException();
			}
			if ( i < a.length ) {
				a[i] = null;
			}
			return a;
		}
	}
}
