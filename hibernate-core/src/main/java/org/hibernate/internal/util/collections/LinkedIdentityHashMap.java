/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.internal.util.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility {@link IdentityHashMap} implementation that maintains predictable iteration order,
 * with special care for keys iteration efficiency, and uses reference equality
 * (i.e. {@code ==}) in place of object-equality when comparing keys.
 * <p>
 * Note that the {@link #keySet()}, {@link #values()} and {@link #entrySet()} methods for this class
 * return unmodifiable collections and are only meant for iteration.
 *
 * @author Marco Belladelli
 */
public class LinkedIdentityHashMap<K, V> extends IdentityHashMap<K, V> {
	private final ArraySet<K> orderedKeys = new ArraySet<>();

	@Override
	public V put(K key, V value) {
		orderedKeys.add( key );
		return super.put( key, value );
	}

	@Override
	public V remove(Object key) {
		orderedKeys.remove( key );
		return super.remove( key );
	}

	@Override
	public boolean remove(Object key, Object value) {
		final boolean removed = super.remove( key, value );
		if ( removed ) {
			orderedKeys.remove( key );
		}
		return removed;
	}

	@Override
	public void clear() {
		orderedKeys.clear();
		super.clear();
	}

	@Override
	public Set<K> keySet() {
		return Collections.unmodifiableSet( orderedKeys );
	}

	@Override
	public Collection<V> values() {
		return orderedKeys.stream().map( this::get ).collect( Collectors.toUnmodifiableList() );
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return Collections.unmodifiableSet(
				(Set<? extends Entry<K, V>>) orderedKeys.stream()
						.map( key -> Map.entry( key, get( key ) ) )
						.collect( Collectors.toCollection( LinkedHashSet::new ) )
		);
	}

	@Override
	public Object clone() {
		throw new UnsupportedOperationException();
	}

	private static final class ArraySet<E> extends ArrayList<E> implements Set<E> {
	}
}
