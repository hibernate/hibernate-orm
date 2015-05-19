/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.tools;

import java.util.Map;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public abstract class ArraysTools {
	public static <T> boolean arrayIncludesInstanceOf(T[] array, Class<?> cls) {
		for ( T obj : array ) {
			if ( cls.isAssignableFrom( obj.getClass() ) ) {
				return true;
			}
		}

		return false;
	}

	public static boolean arraysEqual(Object[] array1, Object[] array2) {
		if ( array1 == null ) {
			return array2 == null;
		}
		if ( array2 == null || array1.length != array2.length ) {
			return false;
		}
		for ( int i = 0; i < array1.length; ++i ) {
			if ( array1[i] != null ? !array1[i].equals( array2[i] ) : array2[i] != null ) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Converts map's value set to an array. {@code keys} parameter specifies requested elements and their order.
	 *
	 * @param data Source map.
	 * @param keys Array of keys that represent requested map values.
	 *
	 * @return Array of values stored in the map under specified keys. If map does not contain requested key,
	 *         {@code null} is inserted.
	 */
	public static Object[] mapToArray(Map<String, Object> data, String[] keys) {
		final Object[] ret = new Object[keys.length];
		for ( int i = 0; i < keys.length; ++i ) {
			ret[i] = data.get( keys[i] );
		}
		return ret;
	}
}
