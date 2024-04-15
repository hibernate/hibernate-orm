/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.stat.internal;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import org.hibernate.internal.util.collections.BoundedConcurrentHashMap;

/**
 * Decorates a ConcurrentHashMap implementation to make sure the methods are being
 * used correctly for the purpose of Hibernate's statistics.
 * In particular, we do like the semantics of ConcurrentHashMap#computeIfAbsent
 * but not how it performs.
 * See also:
 *  - http://jsr166-concurrency.10961.n7.nabble.com/Re-ConcurrentHashMap-computeIfAbsent-td11687.html
 *  - https://hibernate.atlassian.net/browse/HHH-13527
 *
 * @author Sanne Grinovero
 */
final class StatsNamedContainer<V> {

	private final ConcurrentMap<String,Object> map;
	private final static Object NULL_TOKEN = new Object();

	/**
	 * Creates a bounded container - based on BoundedConcurrentHashMap
	 */
	StatsNamedContainer(int capacity, int concurrencyLevel) {
		this.map = new BoundedConcurrentHashMap( capacity, concurrencyLevel, BoundedConcurrentHashMap.Eviction.LRU );
	}

	/**
	 * Creates an unbounded container - based on ConcurrentHashMap
	 */
	StatsNamedContainer() {
		this.map = new ConcurrentHashMap<>(  );
	}

	public void clear() {
		map.clear();
	}

	/**
	 * This method is inherently racy and expensive. Only use on non-hot paths, and
	 * only to get a recent snapshot.
	 * @return all keys in string form.
	 */
	public String[] keysAsArray() {
		return map.keySet().toArray( new String[0] );
	}

	/**
	 * Similar semantics as you'd get by invoking {@link java.util.concurrent.ConcurrentMap#computeIfAbsent(Object, Function)},
	 * but we check for the key existence first.
	 * This is technically a redundant check, but it has been shown to perform better when the key existing is very likely,
	 * as in our case.
	 * Most notably, the ConcurrentHashMap implementation might block other accesses for the sake of making
	 * sure the function is invoked at most once: we don't need this guarantee, and prefer to reduce risk of blocking.
	 */
	public V getOrCompute(final String key, final Function<String, V> function) {
		final Object v1 = map.get( key );
		if ( v1 != null ) {
			if ( v1 == NULL_TOKEN ) {
				return null;
			}
			return (V) v1;
		}
		else {
			final V v2 = function.apply( key );
			if ( v2 == null ) {
				map.put( key, NULL_TOKEN );
				return null;
			}
			else {
				final Object v3 = map.putIfAbsent( key, v2 );
				if ( v3 == null ) {
					return v2;
				}
				else {
					return (V) v3;
				}
			}
		}
	}

	public V get(final String key) {
		final Object o = map.get( key );
		if ( o == NULL_TOKEN) {
			return null;
		}
		else {
			return (V) o;
		}
	}

}
