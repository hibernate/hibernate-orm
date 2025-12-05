/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util.collections;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.internal.build.AllowReflection;
import org.hibernate.type.Type;

import static java.lang.reflect.Array.get;
import static java.lang.reflect.Array.getLength;
import static java.util.Arrays.asList;

public final class ArrayHelper {

	public static <T> boolean contains(T[] array, T object) {
		return indexOf( array, object ) > -1;
	}

	public static <T> boolean containsAll(T[] array, T[] elements) {
		for ( T element : elements ) {
			if ( !contains( array, element ) ) {
				return false;
			}
		}
		return true;
	}

	public static boolean contains(int[] array, int value) {
		//noinspection ForLoopReplaceableByForEach
		for ( int i = 0; i < array.length; i++ ) {
			if ( array[i] == value ) {
				return true;
			}
		}
		return false;
	}

	public static int indexOf(Object[] array, Object object) {
		return indexOf( array, array.length, object );
	}

	public static int indexOf(Object[] array, int end, Object object) {
		for ( int i = 0; i < end; i++ ) {
			if ( object.equals( array[i] ) ) {
				return i;
			}
		}
		return -1;
	}

	@SuppressWarnings("unchecked")
	@AllowReflection
	public static <T> T[] filledArray(T value, Class<T> valueJavaType, int size) {
		final T[] array = (T[]) Array.newInstance( valueJavaType, size );
		Arrays.fill( array, value );
		return array;
	}

	public static String[] toStringArray(Object[] objects) {
		int length = objects.length;
		String[] result = new String[length];
		for ( int i = 0; i < length; i++ ) {
			result[i] = objects[i].toString();
		}
		return result;
	}

	public static String[] toStringArray(Collection<String> coll) {
		return coll.toArray( EMPTY_STRING_ARRAY );
	}

	public static Object[] toObjectArray(Collection<Object> coll) {
		return coll.toArray( EMPTY_OBJECT_ARRAY );
	}

	public static String[][] to2DStringArray(Collection<String[]> coll) {
		return coll.toArray( new String[0][] );
	}

	public static int[][] to2DIntArray(Collection<int[]> coll) {
		return coll.toArray( new int[0][] );
	}

	public static Type[] toTypeArray(Collection<Type> coll) {
		return coll.toArray( EMPTY_TYPE_ARRAY );
	}

	public static int[] toIntArray(Collection<Integer> coll) {
		final var iter = coll.iterator();
		final int[] arr = new int[coll.size()];
		int i = 0;
		while ( iter.hasNext() ) {
			arr[i++] = iter.next();
		}
		return arr;
	}

	public static boolean[] toBooleanArray(Collection<Boolean> coll) {
		final var iter = coll.iterator();
		final boolean[] arr = new boolean[coll.size()];
		int i = 0;
		while ( iter.hasNext() ) {
			arr[i++] = iter.next();
		}
		return arr;
	}

	public static String[] slice(String[] strings, int begin, int length) {
		final var result = new String[length];
		System.arraycopy( strings, begin, result, 0, length );
		return result;
	}

	public static Object[] slice(Object[] objects, int begin, int length) {
		final var result = new Object[length];
		System.arraycopy( objects, begin, result, 0, length );
		return result;
	}

	public static String[] join(String[] x, String[] y) {
		final var result = new String[x.length + y.length];
		System.arraycopy( x, 0, result, 0, x.length );
		System.arraycopy( y, 0, result, x.length, y.length );
		return result;
	}

	public static String[] join(String[] x, String[] y, boolean[] use) {
		final var result = new String[x.length + countTrue( use )];
		System.arraycopy( x, 0, result, 0, x.length );
		int k = x.length;
		for ( int i = 0; i < y.length; i++ ) {
			if ( use[i] ) {
				result[k++] = y[i];
			}
		}
		return result;
	}

	public static int[] join(int[] x, int[] y) {
		final var result = new int[x.length + y.length];
		System.arraycopy( x, 0, result, 0, x.length );
		System.arraycopy( y, 0, result, x.length, y.length );
		return result;
	}

	@SuppressWarnings("unchecked")
	@AllowReflection
	public static <T> T[] join(T[] x, T... y) {
		final T[] result = (T[]) Array.newInstance( x.getClass().getComponentType(), x.length + y.length );
		System.arraycopy( x, 0, result, 0, x.length );
		System.arraycopy( y, 0, result, x.length, y.length );
		return result;
	}

	@SuppressWarnings("unchecked")
	@AllowReflection
	public static <T> T[] add(T[] x, T y) {
		final T[] result = (T[]) Array.newInstance( x.getClass().getComponentType(), x.length + 1 );
		System.arraycopy( x, 0, result, 0, x.length );
		result[x.length] = y;
		return result;
	}

	public static final boolean[] TRUE = {true};
	public static final boolean[] FALSE = {false};

	private ArrayHelper() {
	}

	public static String toString(Object[] array) {
		final var string = new StringBuilder();
		string.append( "[" );
		for ( int i = 0; i < array.length; i++ ) {
			string.append( array[i] );
			if ( i < array.length - 1 ) {
				string.append( "," );
			}
		}
		string.append( "]" );
		return string.toString();
	}

	public static boolean isAllNegative(int[] array) {
		for ( int element : array ) {
			if ( element >= 0 ) {
				return false;
			}
		}
		return true;
	}

	public static boolean isAllTrue(boolean... array) {
		for ( boolean element : array ) {
			if ( !element ) {
				return false;
			}
		}
		return true;
	}

	public static int countTrue(boolean... array) {
		int result = 0;
		for ( boolean element : array ) {
			if ( element ) {
				result++;
			}
		}
		return result;
	}

	public static boolean isAllFalse(boolean... array) {
		for ( boolean element : array ) {
			if ( element ) {
				return false;
			}
		}
		return true;
	}

	public static boolean isAnyTrue(boolean... values) {
		for ( boolean value : values ) {
			if ( value ) {
				return true;
			}
		}
		return false;
	}

	public static boolean[] negate(boolean[] valueNullness) {
		final var result = new boolean[valueNullness.length];
		for (int i = 0; i < valueNullness.length; i++) {
			result[i] = !valueNullness[i];
		}
		return result;
	}


	public static <T> void addAll(Collection<T> collection, T[] array) {
		collection.addAll( asList( array ) );
	}

	public static final String[] EMPTY_STRING_ARRAY = {};
	public static final int[] EMPTY_INT_ARRAY = {};
	public static final boolean[] EMPTY_BOOLEAN_ARRAY = {};
	public static final Class<?>[] EMPTY_CLASS_ARRAY = {};
	public static final Object[] EMPTY_OBJECT_ARRAY = {};
	public static final Type[] EMPTY_TYPE_ARRAY = {};
	public static final byte[] EMPTY_BYTE_ARRAY = {};


	/**
	 * Reverse the elements of the incoming array
	 *
	 * @return New array with all elements in reversed order
	 */
	public static String[] reverse(String[] source) {
		final int length = source.length;
		final var destination = new String[length];
		for ( int i = 0; i < length; i++ ) {
			destination[length - i - 1] = source[i];
		}
		return destination;
	}

	/**
	 * Reverse the first n elements of the incoming array
	 *
	 * @return New array with the first n elements in reversed order
	 */
	public static String[] reverseFirst(String[] objects, int n) {
		final int length = objects.length;
		final var destination = new String[length];
		for ( int i = 0; i < n; i++ ) {
			destination[i] = objects[n - i - 1];
		}
		for ( int i = n; i < length; i++ ) {
			destination[i] = objects[i];
		}
		return destination;
	}

	/**
	 * Reverse the first n elements of the incoming array
	 *
	 * @return New array with the first n elements in reversed order
	 */
	public static String[][] reverseFirst(String[][] objects, int n) {
		final int length = objects.length;
		final var destination = new String[length][];
		for ( int i = 0; i < n; i++ ) {
			destination[i] = objects[n - i - 1];
		}
		for ( int i = n; i < length; i++ ) {
			destination[i] = objects[i];
		}
		return destination;
	}

	public static int[] trim(int[] from, int length) {
		final var trimmed = new int[length];
		System.arraycopy( from, 0, trimmed, 0, length );
		return trimmed;
	}

	public static Object[] toObjectArray(Object array) {
		if ( array instanceof Object[] objects ) {
			return objects;
		}
		else {
			final int arrayLength = getLength( array );
			final var result = new Object[arrayLength];
			for ( int i = 0; i < arrayLength; ++i ) {
				result[i] = get( array, i );
			}
			return result;
		}
	}

	public static <T> List<T> toExpandableList(T[] values) {
		return values == null ? new ArrayList<>() : asList( values );
	}

	public static boolean isEmpty(Object[] array) {
		return array == null || array.length == 0;
	}

	public static <T> int size(T[] array) {
		return array == null ? 0 : array.length;
	}

	public static <T> void forEach(T[] array, Consumer<T> consumer) {
		if ( array != null ) {
			//noinspection ForLoopReplaceableByForEach
			for ( int i = 0; i < array.length; i++ ) {
				consumer.accept( array[i] );
			}
		}
	}

	/**
	 * @deprecated Use {@link Array#newInstance(Class, int)} instead.
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	@AllowReflection
	public static <T> T[] newInstance(Class<T> elementType, int length) {
		return (T[]) Array.newInstance( elementType, length );
	}
}
