/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util.collections;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.internal.build.AllowReflection;
import org.hibernate.type.Type;

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

	public static String[] fillArray(String value, int length) {
		String[] result = new String[length];
		Arrays.fill( result, value );
		return result;
	}

	public static int[] fillArray(int value, int length) {
		int[] result = new int[length];
		Arrays.fill( result, value );
		return result;
	}

	public static LockMode[] fillArray(LockMode lockMode, int length) {
		LockMode[] array = new LockMode[length];
		Arrays.fill( array, lockMode );
		return array;
	}

	public static LockOptions[] fillArray(LockOptions lockOptions, int length) {
		LockOptions[] array = new LockOptions[length];
		Arrays.fill( array, lockOptions );
		return array;
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
		Iterator<Integer> iter = coll.iterator();
		int[] arr = new int[coll.size()];
		int i = 0;
		while ( iter.hasNext() ) {
			arr[i++] = iter.next();
		}
		return arr;
	}

	public static boolean[] toBooleanArray(Collection<Boolean> coll) {
		Iterator<Boolean> iter = coll.iterator();
		boolean[] arr = new boolean[coll.size()];
		int i = 0;
		while ( iter.hasNext() ) {
			arr[i++] = iter.next();
		}
		return arr;
	}

	public static Object[] typecast(Object[] array, Object[] to) {
		return Arrays.asList( array ).toArray( to );
	}

	//Arrays.asList doesn't do primitive arrays
//	public static List toList(Object array) {
//		if ( array instanceof Object[] ) {
//			return Arrays.asList( (Object[]) array ); //faster?
//		}
//		int size = Array.getLength( array );
//		ArrayList<Object> list = new ArrayList<>( size );
//		for ( int i = 0; i < size; i++ ) {
//			list.add( Array.get( array, i ) );
//		}
//		return list;
//	}

	public static String[] slice(String[] strings, int begin, int length) {
		String[] result = new String[length];
		System.arraycopy( strings, begin, result, 0, length );
		return result;
	}

	public static Object[] slice(Object[] objects, int begin, int length) {
		Object[] result = new Object[length];
		System.arraycopy( objects, begin, result, 0, length );
		return result;
	}

	public static <T> List<T> toList(Iterator<T> iter) {
		List<T> list = new ArrayList<>();
		while ( iter.hasNext() ) {
			list.add( iter.next() );
		}
		return list;
	}

	public static String[] join(String[] x, String[] y) {
		String[] result = new String[x.length + y.length];
		System.arraycopy( x, 0, result, 0, x.length );
		System.arraycopy( y, 0, result, x.length, y.length );
		return result;
	}

	public static String[] join(String[] x, String[] y, boolean[] use) {
		String[] result = new String[x.length + countTrue( use )];
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
		int[] result = new int[x.length + y.length];
		System.arraycopy( x, 0, result, 0, x.length );
		System.arraycopy( y, 0, result, x.length, y.length );
		return result;
	}

	@SuppressWarnings("unchecked")
	@AllowReflection
	public static <T> T[] join(T[] x, T... y) {
		T[] result = (T[]) Array.newInstance( x.getClass().getComponentType(), x.length + y.length );
		System.arraycopy( x, 0, result, 0, x.length );
		System.arraycopy( y, 0, result, x.length, y.length );
		return result;
	}

	@SuppressWarnings("unchecked")
	@AllowReflection
	public static <T> T[] add(T[] x, T y) {
		T[] result = (T[]) Array.newInstance( x.getClass().getComponentType(), x.length + 1 );
		System.arraycopy( x, 0, result, 0, x.length );
		result[x.length] = y;
		return result;
	}

	public static final boolean[] TRUE = {true};
	public static final boolean[] FALSE = {false};

	private ArrayHelper() {
	}

	public static String toString(Object[] array) {
		StringBuilder sb = new StringBuilder();
		sb.append( "[" );
		for ( int i = 0; i < array.length; i++ ) {
			sb.append( array[i] );
			if ( i < array.length - 1 ) {
				sb.append( "," );
			}
		}
		sb.append( "]" );
		return sb.toString();
	}

	public static boolean isAllNegative(int[] array) {
		for ( int anArray : array ) {
			if ( anArray >= 0 ) {
				return false;
			}
		}
		return true;
	}

	public static boolean isAllTrue(boolean... array) {
		for ( boolean anArray : array ) {
			if ( !anArray ) {
				return false;
			}
		}
		return true;
	}

	public static int countTrue(boolean... array) {
		int result = 0;
		for ( boolean anArray : array ) {
			if ( anArray ) {
				result++;
			}
		}
		return result;
	}

	public static boolean isAllFalse(boolean... array) {
		for ( boolean anArray : array ) {
			if ( anArray ) {
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
		boolean[] result = new boolean[valueNullness.length];
		for (int i = 0; i < valueNullness.length; i++) {
			result[i] = !valueNullness[i];
		}
		return result;
	}


	public static <T> void addAll(Collection<T> collection, T[] array) {
		collection.addAll( Arrays.asList( array ) );
	}

	public static final String[] EMPTY_STRING_ARRAY = {};
	public static final int[] EMPTY_INT_ARRAY = {};
	public static final boolean[] EMPTY_BOOLEAN_ARRAY = {};
	public static final Class[] EMPTY_CLASS_ARRAY = {};
	public static final Object[] EMPTY_OBJECT_ARRAY = {};
	public static final Type[] EMPTY_TYPE_ARRAY = {};
	public static final byte[] EMPTY_BYTE_ARRAY = {};

	/**
	 * Calculate the batch partitions needed to handle the {@code mappedBatchSize}.
	 *
	 * @param mappedBatchSize The {@link org.hibernate.annotations.BatchSize batch-size}.  Internally
	 * this is capped at {@code 256}
	 *
	 * @implNote The max batch size is capped at {@code 256}
	 *
	 * @return The upper bound for the partitions
	 */
	public static int[] calculateBatchPartitions(int mappedBatchSize) {
		final SortedSet<Integer> partitionSizes = new TreeSet<>( Integer::compareTo );
		int batchSize = Math.min( mappedBatchSize, 256 );
		while ( batchSize > 1 ) {
			partitionSizes.add( batchSize );
			batchSize = calculateNextBatchPartitionLimit( batchSize );
		}

		return ArrayHelper.toIntArray( partitionSizes );
	}

	private static int calculateNextBatchPartitionLimit(int batchSize) {
		if ( batchSize <= 10 ) {
			return batchSize - 1; //allow 9,8,7,6,5,4,3,2,1
		}
		else if ( batchSize / 2 < 10 ) {
			return 10;
		}
		else {
			return batchSize / 2;
		}
	}

	public static int[] getBatchSizes(int maxBatchSize) {
		int batchSize = maxBatchSize;
		int n = 1;
		while ( batchSize > 1 ) {
			batchSize = getNextBatchSize( batchSize );
			n++;
		}
		int[] result = new int[n];
		batchSize = maxBatchSize;
		for ( int i = 0; i < n; i++ ) {
			result[i] = batchSize;
			batchSize = getNextBatchSize( batchSize );
		}
		return result;
	}

	private static int getNextBatchSize(int batchSize) {
		if ( batchSize <= 10 ) {
			return batchSize - 1; //allow 9,8,7,6,5,4,3,2,1
		}
		else if ( batchSize / 2 < 10 ) {
			return 10;
		}
		else {
			return batchSize / 2;
		}
	}

	private static final int SEED = 23;
	private static final int PRIME_NUMBER = 37;

	/**
	 * calculate the array hash (only the first level)
	 */
	public static int hash(Object[] array) {
		int seed = SEED;
		for ( Object anArray : array ) {
			seed = hash( seed, anArray == null ? 0 : anArray.hashCode() );
		}
		return seed;
	}

	/**
	 * calculate the array hash (only the first level)
	 */
	public static int hash(char[] array) {
		int seed = SEED;
		for ( char anArray : array ) {
			seed = hash( seed, anArray );
		}
		return seed;
	}

	/**
	 * calculate the array hash (only the first level)
	 */
	public static int hash(byte[] bytes) {
		int seed = SEED;
		for ( byte aByte : bytes ) {
			seed = hash( seed, aByte );
		}
		return seed;
	}

	private static int hash(int seed, int i) {
		return PRIME_NUMBER * seed + i;
	}

	public static Serializable[] extractNonNull(Serializable[] array) {
		final int nonNullCount = countNonNull( array );
		final Serializable[] result = new Serializable[nonNullCount];
		int i = 0;
		for ( Serializable element : array ) {
			if ( element != null ) {
				result[i++] = element;
			}
		}
		if ( i != nonNullCount ) {
			throw new HibernateException( "Number of non-null elements varied between iterations" );
		}
		return result;
	}

	public static int countNonNull(Serializable[] array) {
		int i = 0;
		for ( Serializable element : array ) {
			if ( element != null ) {
				i++;
			}
		}
		return i;
	}

	public static int countNonNull(Object[] array) {
		int i = 0;
		for ( Object element : array ) {
			if ( element != null ) {
				i++;
			}
		}
		return i;
	}

	/**
	 * Reverse the elements of the incoming array
	 *
	 * @return New array with all elements in reversed order
	 */
	public static String[] reverse(String[] source) {
		final int length = source.length;
		final String[] destination = new String[length];
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
		final String[] destination = new String[length];
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
		final String[][] destination = new String[length][];
		for ( int i = 0; i < n; i++ ) {
			destination[i] = objects[n - i - 1];
		}
		for ( int i = n; i < length; i++ ) {
			destination[i] = objects[i];
		}
		return destination;
	}

	public static int[] trim(int[] from, int length) {
		int[] trimmed = new int[length];
		System.arraycopy( from, 0, trimmed, 0, length );
		return trimmed;
	}

	public static Object[] toObjectArray(Object array) {
		if ( array instanceof Object[] objects ) {
			return objects;
		}
		final int arrayLength = Array.getLength( array );
		final Object[] outputArray = new Object[ arrayLength ];
		for ( int i = 0; i < arrayLength; ++i ) {
			outputArray[ i ] = Array.get( array, i );
		}
		return outputArray;
	}

	public static <T> List<T> toExpandableList(T[] values) {
		if ( values == null ) {
			return new ArrayList<>();
		}
		return Arrays.asList( values );
	}

	public static boolean isEmpty(Object[] array) {
		return array == null || array.length == 0;
	}

	public static <T> void forEach(T[] array, Consumer<T> consumer) {
		if ( array == null ) {
			return;
		}

		//noinspection ForLoopReplaceableByForEach
		for ( int i = 0; i < array.length; i++ ) {
			consumer.accept( array[ i ] );
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

	public static <T> int size(T[] array) {
		return array == null ? 0 : array.length;
	}
}
