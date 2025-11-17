/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util.collections;

import java.util.Map;

/**
 * For efficient lookup based on Class types as key,
 * a ClassValue should be used; however it requires
 * lazy association of values; this helper wraps
 * a plain HashMap but optimises lookups via the ClassValue.
 * N.B. there is a cost in memory and in terms of weak references,
 * so let's use this only where proven that a simple Map lookup
 * is otherwise too costly.
 * @param <V> the type of the values stored in the Maps.
 * @author Sanne Grinovero
 * @since 6.2
 */
public final class MapBackedClassValue<V> implements ReadOnlyMap<Class<?>,V> {

	private volatile Map<Class<?>, V> map;

	private final ClassValue<V> classValue = new ClassValue<>() {
		@Override
		protected V computeValue(final Class<?> type) {
			final var m = map;
			if ( m == null ) {
				throw new IllegalStateException( "This MapBackedClassValue has been disposed" );
			}
			else {
				return map.get( type );
			}
		}
	};

	public MapBackedClassValue(final Map<Class<?>, V> map) {
		//Defensive copy, and implicit null check.
		//Choose the Map.copyOf implementation as it has a compact layout;
		//it doesn't have great get() performance, but it's acceptable since we're performing that at most
		//once per key before caching it via the ClassValue.
		this.map = Map.copyOf( map );
	}

	@Override
	public V get(Class<?> key) {
		return classValue.get( key );
	}

	/**
	 * Use this to wipe the backing map, important
	 * to avoid classloader leaks.
	 */
	@Override
	public void dispose() {
		final var existing = map;
		map = null;
		if ( existing != null ) {
			for ( var entry : existing.entrySet() ) {
				classValue.remove( entry.getKey() );
			}
		}
	}

}
