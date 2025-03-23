/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util.collections;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import org.hibernate.Internal;

/**
 * Wraps a ConcurrentHashMap having all keys as Strings
 * and ensures all keys are lowercased.
 * It does assume keys and arguments are never null, preferring to throw a NPE
 * over adding unnecessary checks.
 * The public exposed methods are similar to the ones on Map, but
 * not all Map methods are exposed - only a selection we actually need; this
 * implies it doesn't implement Map; nothing stops us to make it implement Map
 * but at time of writing it seems unnecessary for our purposes.
 * @param <V> the type for the stored values.
 */
@Internal
public final class CaseInsensitiveDictionary<V> {

	private final Map<String, V> map = new ConcurrentHashMap<>();

	public V get(final String key) {
		return map.get( trueKey( key ) );
	}

	/**
	 * Contrary to traditional Map, we make the return unmodifiable.
	 * @return the map's keySet
	 */
	public Set<String> unmodifiableKeySet() {
		return Collections.unmodifiableSet( map.keySet() );
	}

	/**
	 * Contrary to traditional Map, we make the return unmodifiable.
	 * @return the map's entrySet
	 */
	public Set<Map.Entry<String, V>> unmodifiableEntrySet() {
		return Collections.unmodifiableSet( map.entrySet() );
	}

	public V put(final String key, V value) {
		return map.put( trueKey( key ), value );
	}

	public V remove(final String key) {
		return map.remove( trueKey( key ) );
	}

	public boolean containsKey(final String key) {
		return map.containsKey( trueKey( key ) );
	}

	private static String trueKey(final String key) {
		return key.toLowerCase( Locale.ROOT );
	}

	public void clear() {
		map.clear();
	}

	public void forEach(final BiConsumer<? super String, ? super V> action) {
		map.forEach( action );
	}

}
