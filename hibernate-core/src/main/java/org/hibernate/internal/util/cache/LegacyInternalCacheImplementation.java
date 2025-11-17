/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util.cache;

import org.hibernate.internal.util.collections.BoundedConcurrentHashMap;

import java.io.Serializable;
import java.util.function.Function;

/**
 * An implementation of {@link InternalCache} based on the deprecated {@link BoundedConcurrentHashMap}.
 * @param <K>
 * @param <V>
 */
final class LegacyInternalCacheImplementation<K,V> implements InternalCache<K,V>, Serializable {

	private final BoundedConcurrentHashMap<K,V> map;

	public LegacyInternalCacheImplementation(int intendedApproximateSize) {
		map = new BoundedConcurrentHashMap<>(
				intendedApproximateSize, 20, BoundedConcurrentHashMap.Eviction.LIRS );
	}

	@Override
	public int heldElementsEstimate() {
		return map.size();
	}

	@Override
	public V get(K key) {
		return map.get( key );
	}

	@Override
	public void put(K key, V value) {
		map.put( key, value );
	}

	@Override
	public void clear() {
		map.clear();
	}

	@Override
	public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
		return map.computeIfAbsent( key, mappingFunction );
	}

}
