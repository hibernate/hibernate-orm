/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.util;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Utility class for nullness assertions.
 */
@SuppressWarnings({
		"nullness", // Nullness utilities are trusted regarding nullness.
		"cast" // Casts look redundant if static nullness analysis is not run.
})
public final class NullnessUtil {

	private NullnessUtil() {
		throw new AssertionError( "shouldn't be instantiated" );
	}

	/**
	 * A method that suppresses warnings from static nullness analysis.
	 *
	 * <p>The method takes a possibly-null reference, unsafely casts it to have the @Nonnull type
	 * qualifier, and returns it. Static nullness analysis considers both the return value, and also the
	 * argument, to be non-null after the method call. Therefore, the {@code castNonNull} method can
	 * be used either as a cast expression or as a statement. Static nullness analysis issues no warnings
	 * in any of the following code:
	 *
	 * <pre><code>
	 *   // one way to use as a cast:
	 *  {@literal @}Nonnull String s = castNonNull(possiblyNull1);
	 *
	 *   // another way to use as a cast:
	 *   castNonNull(possiblyNull2).toString();
	 *
	 *   // one way to use as a statement:
	 *   castNonNull(possiblyNull3);
	 *   possiblyNull3.toString();`
	 * </code></pre>
	 * <p>
	 * The {@code castNonNull} method is intended to be used in situations where the programmer
	 * definitively knows that a given reference is not null, but the type system is unable to make
	 * this deduction. It is not intended for defensive programming, in which a programmer cannot
	 * prove that the value is not null but wishes to have an earlier indication if it is.
	 *
	 * <p>The method throws {@link AssertionError} if Java assertions are enabled and the argument is
	 * {@code null}. If the exception is ever thrown, then that indicates that the programmer misused
	 * the method by using it in a circumstance where its argument can be null.
	 *
	 * @param <T> the type of the reference
	 * @param ref a reference of @Nullable type, that is non-null at run time
	 *
	 * @return the argument, cast to have the type qualifier @Nonnull
	 */
	public static <T extends Object> @Nonnull T castNonNull(@Nullable T ref) {
		assert ref != null : "Misuse of castNonNull: called with a null argument";
		return ref;
	}

	/**
	 * Suppress warnings from static nullness analysis, with a custom error message. See {@link
	 * #castNonNull(Object)} for documentation.
	 *
	 * @param <T> the type of the reference
	 * @param ref a reference of @Nullable type, that is non-null at run time
	 * @param message text to include if this method is misused
	 *
	 * @return the argument, cast to have the type qualifier @Nonnull
	 *
	 * @see #castNonNull(Object)
	 */
	public static <T extends Object> @Nonnull T castNonNull(
			@Nullable T ref, String message) {
		assert ref != null : "Misuse of castNonNull: called with a null argument: " + message;
		return ref;
	}

	/**
	 * Like castNonNull, but whereas that method only checks and casts the reference itself, this
	 * traverses all levels of the argument array. The array is recursively checked to ensure that all
	 * elements at every array level are non-null.
	 *
	 * @param <T> the component type of the array
	 * @param arr an array all of whose elements, and their elements recursively, are non-null at run
	 * time
	 *
	 * @return the argument, cast to have the type qualifier @Nonnull at all levels
	 *
	 * @see #castNonNull(Object)
	 */
	public static <T extends Object> @Nonnull T[] castNonNullDeep(
			@Nullable T[] arr) {
		return castNonNullArray( arr, null );
	}

	/**
	 * Like castNonNull, but whereas that method only checks and casts the reference itself, this
	 * traverses all levels of the argument array. The array is recursively checked to ensure that all
	 * elements at every array level are non-null.
	 *
	 * @param <T> the component type of the array
	 * @param arr an array all of whose elements, and their elements recursively, are non-null at run
	 * time
	 * @param message text to include if this method is misused
	 *
	 * @return the argument, cast to have the type qualifier @Nonnull at all levels
	 *
	 * @see #castNonNull(Object)
	 */
	public static <T extends Object> @Nonnull T[] castNonNullDeep(
			@Nullable T[] arr, String message) {
		return castNonNullArray( arr, message );
	}

	/**
	 * Like castNonNull, but whereas that method only checks and casts the reference itself, this
	 * traverses all levels of the argument array. The array is recursively checked to ensure that all
	 * elements at every array level are non-null.
	 *
	 * @param <T> the component type of the component type of the array
	 * @param arr an array all of whose elements, and their elements recursively, are non-null at run
	 * time
	 *
	 * @return the argument, cast to have the type qualifier @Nonnull at all levels
	 *
	 * @see #castNonNull(Object)
	 */
	public static <T extends Object> @Nonnull T[][] castNonNullDeep(
			@Nullable T[][] arr) {
		return castNonNullArray( arr, null );
	}

	/**
	 * Like castNonNull, but whereas that method only checks and casts the reference itself, this
	 * traverses all levels of the argument array. The array is recursively checked to ensure that all
	 * elements at every array level are non-null.
	 *
	 * @param <T> the component type of the component type of the array
	 * @param arr an array all of whose elements, and their elements recursively, are non-null at run
	 * time
	 * @param message text to include if this method is misused
	 *
	 * @return the argument, cast to have the type qualifier @Nonnull at all levels
	 *
	 * @see #castNonNull(Object)
	 */
	public static <T extends Object> @Nonnull T[][] castNonNullDeep(
			@Nullable T[][] arr, String message) {
		return castNonNullArray( arr, message );
	}

	/**
	 * Like castNonNull, but whereas that method only checks and casts the reference itself, this
	 * traverses all levels of the argument array. The array is recursively checked to ensure that all
	 * elements at every array level are non-null.
	 *
	 * @param <T> the component type (three levels in) of the array
	 * @param arr an array all of whose elements, and their elements recursively, are non-null at run
	 * time
	 *
	 * @return the argument, cast to have the type qualifier @Nonnull at all levels
	 *
	 * @see #castNonNull(Object)
	 */
	public static <T extends Object> @Nonnull T[][][] castNonNullDeep(
			@Nullable T[][][] arr) {
		return castNonNullArray( arr, null );
	}

	/**
	 * Like castNonNull, but whereas that method only checks and casts the reference itself, this
	 * traverses all levels of the argument array. The array is recursively checked to ensure that all
	 * elements at every array level are non-null.
	 *
	 * @param <T> the component type (three levels in) of the array
	 * @param arr an array all of whose elements, and their elements recursively, are non-null at run
	 * time
	 * @param message text to include if this method is misused
	 *
	 * @return the argument, cast to have the type qualifier @Nonnull at all levels
	 *
	 * @see #castNonNull(Object)
	 */
	public static <T extends Object> @Nonnull T[][][] castNonNullDeep(
			@Nullable T[][][] arr, String message) {
		return castNonNullArray( arr, message );
	}

	/**
	 * Like castNonNull, but whereas that method only checks and casts the reference itself, this
	 * traverses all levels of the argument array. The array is recursively checked to ensure that all
	 * elements at every array level are non-null.
	 *
	 * @param <T> the component type of the array
	 * @param arr an array all of whose elements, and their elements recursively, are non-null at run
	 * time
	 *
	 * @return the argument, cast to have the type qualifier @Nonnull at all levels
	 *
	 * @see #castNonNull(Object)
	 */
	public static <T extends Object> @Nonnull T[][][][] castNonNullDeep(
			@Nullable T[][][][] arr) {
		return castNonNullArray( arr, null );
	}

	/**
	 * Like castNonNull, but whereas that method only checks and casts the reference itself, this
	 * traverses all levels of the argument array. The array is recursively checked to ensure that all
	 * elements at every array level are non-null.
	 *
	 * @param <T> the component type (four levels in) of the array
	 * @param arr an array all of whose elements, and their elements recursively, are non-null at run
	 * time
	 * @param message text to include if this method is misused
	 *
	 * @return the argument, cast to have the type qualifier @Nonnull at all levels
	 *
	 * @see #castNonNull(Object)
	 */
	public static <T extends Object> @Nonnull T[][][][] castNonNullDeep(
			@Nullable T[][][][] arr, String message) {
		return castNonNullArray( arr, message );
	}

	/**
	 * Like castNonNull, but whereas that method only checks and casts the reference itself, this
	 * traverses all levels of the argument array. The array is recursively checked to ensure that all
	 * elements at every array level are non-null.
	 *
	 * @param <T> the component type (four levels in) of the array
	 * @param arr an array all of whose elements, and their elements recursively, are non-null at run
	 * time
	 *
	 * @return the argument, cast to have the type qualifier @Nonnull at all levels
	 *
	 * @see #castNonNull(Object)
	 */
	public static <T extends Object> @Nonnull T[][][][][] castNonNullDeep(
			@Nullable T[][][][][] arr) {
		return castNonNullArray( arr, null );
	}

	/**
	 * Like castNonNull, but whereas that method only checks and casts the reference itself, this
	 * traverses all levels of the argument array. The array is recursively checked to ensure that all
	 * elements at every array level are non-null.
	 *
	 * @param <T> the component type (five levels in) of the array
	 * @param arr an array all of whose elements, and their elements recursively, are non-null at run
	 * time
	 * @param message text to include if this method is misused
	 *
	 * @return the argument, cast to have the type qualifier @Nonnull at all levels
	 *
	 * @see #castNonNull(Object)
	 */
	public static <T extends Object> @Nonnull T[][][][][] castNonNullDeep(
			@Nullable T[][][][][] arr, String message) {
		return castNonNullArray( arr, message );
	}

	/**
	 * The implementation of castNonNullDeep.
	 *
	 * @param <T> the component type (five levels in) of the array
	 * @param arr an array all of whose elements, and their elements recursively, are non-null at run
	 * time
	 * @param message text to include if there is a non-null value, or null to use uncustomized
	 * message
	 *
	 * @return the argument, cast to have the type qualifier @Nonnull at all levels
	 */
	private static <T extends Object> @Nonnull T[] castNonNullArray(
			@Nullable T[] arr, @Nullable String message) {
		assert arr != null
				: "Misuse of castNonNullArray: called with a null array argument"
				+ ( message == null ? "" : ": " + message );
		for ( int i = 0; i < arr.length; ++i ) {
			assert arr[i] != null
					: "Misuse of castNonNull: called with a null array element"
					+ ( message == null ? "" : ": " + message );
			checkIfArray( arr[i], message );
		}
		return arr;
	}

	/**
	 * If the argument is an array, requires it to be non-null at all levels.
	 *
	 * @param ref a value; if an array, all of its elements, and their elements recursively, are
	 * non-null at run time
	 * @param message text to include if there is a non-null value, or null to use uncustomized
	 * message
	 */
	private static void checkIfArray(@Nonnull Object ref, @Nullable String message) {
		assert ref != null
				: "Misuse of checkIfArray: called with a null argument"
				+ ( ( message == null ) ? "" : ( ": " + message ) );
		Class<?> comp = ref.getClass().getComponentType();
		if ( comp != null ) {
			// comp is non-null for arrays, otherwise null.
			if ( comp.isPrimitive() ) {
				// Nothing to do for arrays of primitive type: primitives are
				// never null.
			}
			else {
				castNonNullArray( (Object[]) ref, message );
			}
		}
	}
}
