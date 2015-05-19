/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.util.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Various help for handling collections.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public final class CollectionHelper {
	public static final int MINIMUM_INITIAL_CAPACITY = 16;
	public static final float LOAD_FACTOR = 0.75f;

	/**
	 * @deprecated use  {@link java.util.Collections#EMPTY_LIST} or {@link java.util.Collections#emptyList()}  instead
	 */
	@Deprecated
	public static final List EMPTY_LIST = Collections.EMPTY_LIST;
	/**
	 * @deprecated use {@link java.util.Collections#EMPTY_LIST} or {@link java.util.Collections#emptyList()}  instead
	 */
	@Deprecated
	public static final Collection EMPTY_COLLECTION = Collections.EMPTY_LIST;
	/**
	 * @deprecated use {@link java.util.Collections#EMPTY_MAP} or {@link java.util.Collections#emptyMap()}  instead
	 */
	@Deprecated
	public static final Map EMPTY_MAP = Collections.EMPTY_MAP;

	private CollectionHelper() {
	}

	/**
	 * Build a properly sized map, especially handling load size and load factor to prevent immediate resizing.
	 * <p/>
	 * Especially helpful for copy map contents.
	 *
	 * @param size The size to make the map.
	 *
	 * @return The sized map.
	 */
	public static <K, V> Map<K, V> mapOfSize(int size) {
		return new HashMap<K, V>( determineProperSizing( size ), LOAD_FACTOR );
	}

	/**
	 * Given a map, determine the proper initial size for a new Map to hold the same number of values.
	 * Specifically we want to account for load size and load factor to prevent immediate resizing.
	 *
	 * @param original The original map
	 *
	 * @return The proper size.
	 */
	public static int determineProperSizing(Map original) {
		return determineProperSizing( original.size() );
	}

	/**
	 * Given a set, determine the proper initial size for a new set to hold the same number of values.
	 * Specifically we want to account for load size and load factor to prevent immediate resizing.
	 *
	 * @param original The original set
	 *
	 * @return The proper size.
	 */
	public static int determineProperSizing(Set original) {
		return determineProperSizing( original.size() );
	}

	/**
	 * Determine the proper initial size for a new collection in order for it to hold the given a number of elements.
	 * Specifically we want to account for load size and load factor to prevent immediate resizing.
	 *
	 * @param numberOfElements The number of elements to be stored.
	 *
	 * @return The proper size.
	 */
	public static int determineProperSizing(int numberOfElements) {
		int actual = ( (int) ( numberOfElements / LOAD_FACTOR ) ) + 1;
		return Math.max( actual, MINIMUM_INITIAL_CAPACITY );
	}

	/**
	 * Create a properly sized {@link ConcurrentHashMap} based on the given expected number of elements.
	 *
	 * @param expectedNumberOfElements The expected number of elements for the created map
	 * @param <K> The map key type
	 * @param <V> The map value type
	 *
	 * @return The created map.
	 */
	public static <K, V> ConcurrentHashMap<K, V> concurrentMap(int expectedNumberOfElements) {
		return concurrentMap( expectedNumberOfElements, LOAD_FACTOR );
	}

	/**
	 * Create a properly sized {@link ConcurrentHashMap} based on the given expected number of elements and an
	 * explicit load factor
	 *
	 * @param expectedNumberOfElements The expected number of elements for the created map
	 * @param loadFactor The collection load factor
	 * @param <K> The map key type
	 * @param <V> The map value type
	 *
	 * @return The created map.
	 */
	public static <K, V> ConcurrentHashMap<K, V> concurrentMap(int expectedNumberOfElements, float loadFactor) {
		final int size = expectedNumberOfElements + 1 + (int) ( expectedNumberOfElements * loadFactor );
		return new ConcurrentHashMap<K, V>( size, loadFactor );
	}

	public static <T> ArrayList<T> arrayList(int anticipatedSize) {
		return new ArrayList<T>( anticipatedSize );
	}

	public static <T> Set<T> makeCopy(Set<T> source) {
		if ( source == null ) {
			return null;
		}

		final int size = source.size();
		final Set<T> copy = new HashSet<T>( size + 1 );
		copy.addAll( source );
		return copy;
	}

	public static boolean isEmpty(Collection collection) {
		return collection == null || collection.isEmpty();
	}

	public static boolean isEmpty(Map map) {
		return map == null || map.isEmpty();
	}

	public static boolean isNotEmpty(Collection collection) {
		return !isEmpty( collection );
	}

	public static boolean isNotEmpty(Map map) {
		return !isEmpty( map );
	}

	public static boolean isEmpty(Object[] objects) {
		return objects == null || objects.length == 0;
	}

	public static <X, Y> Map<X, Y> makeCopy(Map<X, Y> map) {
		final Map<X, Y> copy = mapOfSize( map.size() + 1 );
		copy.putAll( map );
		return copy;
	}
}
