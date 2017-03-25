/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.envers.enhanced.SequenceIdRevisionEntity;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

import static org.junit.Assert.assertEquals;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class TestTools {
	public static <T> Set<T> makeSet(T... objects) {
		final Set<T> ret = new HashSet<T>();
		//noinspection ManualArrayToCollectionCopy
		for ( T o : objects ) {
			ret.add( o );
		}
		return ret;
	}

	public static <T> List<T> makeList(T... objects) {
		return Arrays.asList( objects );
	}

	public static Map<Object, Object> makeMap(Object... objects) {
		final Map<Object, Object> ret = new HashMap<Object, Object>();
		// The number of objects must be divisable by 2.
		//noinspection ManualArrayToCollectionCopy
		for ( int i = 0; i < objects.length; i += 2 ) {
			ret.put( objects[i], objects[i + 1] );
		}
		return ret;
	}

	public static <T> boolean checkCollection(Collection<T> list, T... objects) {
		if ( list.size() != objects.length ) {
			return false;
		}
		for ( T obj : objects ) {
			if ( !list.contains( obj ) ) {
				return false;
			}
		}
		return true;
	}

	public static List<Integer> extractRevisionNumbers(List queryResults) {
		final List<Integer> result = new ArrayList<Integer>();
		for ( Object queryResult : queryResults ) {
			result.add( ((SequenceIdRevisionEntity) ((Object[]) queryResult)[1]).getId() );
		}
		return result;
	}

	public static Set<String> extractModProperties(PersistentClass persistentClass) {
		return extractModProperties( persistentClass, "_MOD" );
	}

	public static Set<String> extractModProperties(PersistentClass persistentClass, String suffix) {
		final Set<String> result = new HashSet<String>();
		final Iterator iterator = persistentClass.getPropertyIterator();
		while ( iterator.hasNext() ) {
			final Property property = (Property) iterator.next();
			final String propertyName = property.getName();
			if ( propertyName.endsWith( suffix ) ) {
				result.add( propertyName );
			}
		}
		return result;
	}

	public static <T> void assertCollectionsEqual(Collection<T> expected, Collection<T> actual) {
		assertEquals( expected.size(), actual.size() );
		Iterator<T> it = actual.iterator();
		for ( T obj : actual ) {
			assertEquals( obj, it.next() );
		}
	}

	public static <K,E> MapBuilder<K,E> mapBuilder() {
		return new MashMapBuilderImpl<K,E>();
	}

	public interface MapBuilder<K,E> {
		MapBuilder<K,E> add(K key, E value);
		Set<Map.Entry<K,E>> entries();
		Map<K,E> map();
	}

	private static class MashMapBuilderImpl<K,E> implements MapBuilder<K,E> {
		private final Map<K,E> hashMap = new HashMap<K, E>();

		@Override
		public MapBuilder<K, E> add(K key, E value) {
			hashMap.put( key, value );
			return this;
		}

		@Override
		public Set<Map.Entry<K, E>> entries() {
			return hashMap.entrySet();
		}

		@Override
		public Map<K, E> map() {
			return hashMap;
		}
	}
}
