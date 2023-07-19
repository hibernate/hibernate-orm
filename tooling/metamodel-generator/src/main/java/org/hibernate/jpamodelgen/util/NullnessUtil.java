/*
 * Checker Framework utilities
 * Copyright 2004-present by the Checker Framework developers
 *
 * MIT License:
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.hibernate.jpamodelgen.util;

import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.AnnotatedFor;

/**
 * Utility class for the Nullness Checker.
 *
 * <p>To avoid the need to write the NullnessUtil class name, do:
 *
 * <pre>import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;</pre>
 * <p>
 * or
 *
 * <pre>import static org.checkerframework.checker.nullness.util.NullnessUtil.*;</pre>
 *
 * <p><b>Runtime Dependency</b>: If you use this class, you must distribute (or link to) {@code
 * checker-qual.jar}, along with your binaries. Or, you can copy this class into your own project.
 */
@SuppressWarnings({
		"nullness", // Nullness utilities are trusted regarding nullness.
		"cast" // Casts look redundant if Nullness Checker is not run.
})
@AnnotatedFor("nullness")
public final class NullnessUtil {

	private NullnessUtil() {
		throw new AssertionError( "shouldn't be instantiated" );
	}

	/**
	 * A method that suppresses warnings from the Nullness Checker.
	 *
	 * <p>The method takes a possibly-null reference, unsafely casts it to have the @NonNull type
	 * qualifier, and returns it. The Nullness Checker considers both the return value, and also the
	 * argument, to be non-null after the method call. Therefore, the {@code castNonNull} method can
	 * be used either as a cast expression or as a statement. The Nullness Checker issues no warnings
	 * in any of the following code:
	 *
	 * <pre><code>
	 *   // one way to use as a cast:
	 *  {@literal @}NonNull String s = castNonNull(possiblyNull1);
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
	 * prove that the value is not null but wishes to have an earlier indication if it is. See the
	 * Checker Framework Manual for further discussion.
	 *
	 * <p>The method throws {@link AssertionError} if Java assertions are enabled and the argument is
	 * {@code null}. If the exception is ever thrown, then that indicates that the programmer misused
	 * the method by using it in a circumstance where its argument can be null.
	 *
	 * @param <T> the type of the reference
	 * @param ref a reference of @Nullable type, that is non-null at run time
	 *
	 * @return the argument, cast to have the type qualifier @NonNull
	 */
	@EnsuresNonNull("#1")
	public static <T extends @Nullable Object> @NonNull T castNonNull(@Nullable T ref) {
		assert ref != null : "Misuse of castNonNull: called with a null argument";
		return ref;
	}

	/**
	 * Suppress warnings from the Nullness Checker, with a custom error message. See {@link
	 * #castNonNull(Object)} for documentation.
	 *
	 * @param <T> the type of the reference
	 * @param ref a reference of @Nullable type, that is non-null at run time
	 * @param message text to include if this method is misused
	 *
	 * @return the argument, cast to have the type qualifier @NonNull
	 *
	 * @see #castNonNull(Object)
	 */
	public static @EnsuresNonNull("#1") <T extends @Nullable Object> @NonNull T castNonNull(
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
	 * @return the argument, cast to have the type qualifier @NonNull at all levels
	 *
	 * @see #castNonNull(Object)
	 */
	@EnsuresNonNull("#1")
	public static <T extends @Nullable Object> @NonNull T @NonNull [] castNonNullDeep(
			T @Nullable [] arr) {
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
	 * @return the argument, cast to have the type qualifier @NonNull at all levels
	 *
	 * @see #castNonNull(Object)
	 */
	@EnsuresNonNull("#1")
	public static <T extends @Nullable Object> @NonNull T @NonNull [] castNonNullDeep(
			T @Nullable [] arr, String message) {
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
	 * @return the argument, cast to have the type qualifier @NonNull at all levels
	 *
	 * @see #castNonNull(Object)
	 */
	@EnsuresNonNull("#1")
	public static <T extends @Nullable Object> @NonNull T @NonNull [][] castNonNullDeep(
			T @Nullable [] @Nullable [] arr) {
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
	 * @return the argument, cast to have the type qualifier @NonNull at all levels
	 *
	 * @see #castNonNull(Object)
	 */
	@EnsuresNonNull("#1")
	public static <T extends @Nullable Object> @NonNull T @NonNull [][] castNonNullDeep(
			T @Nullable [] @Nullable [] arr, String message) {
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
	 * @return the argument, cast to have the type qualifier @NonNull at all levels
	 *
	 * @see #castNonNull(Object)
	 */
	@EnsuresNonNull("#1")
	public static <T extends @Nullable Object> @NonNull T @NonNull [][][] castNonNullDeep(
			T @Nullable [] @Nullable [] @Nullable [] arr) {
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
	 * @return the argument, cast to have the type qualifier @NonNull at all levels
	 *
	 * @see #castNonNull(Object)
	 */
	@EnsuresNonNull("#1")
	public static <T extends @Nullable Object> @NonNull T @NonNull [][][] castNonNullDeep(
			T @Nullable [] @Nullable [] @Nullable [] arr, String message) {
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
	 * @return the argument, cast to have the type qualifier @NonNull at all levels
	 *
	 * @see #castNonNull(Object)
	 */
	@EnsuresNonNull("#1")
	public static <T extends @Nullable Object> @NonNull T @NonNull [][][][] castNonNullDeep(
			T @Nullable [] @Nullable [] @Nullable [] @Nullable [] arr) {
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
	 * @return the argument, cast to have the type qualifier @NonNull at all levels
	 *
	 * @see #castNonNull(Object)
	 */
	@EnsuresNonNull("#1")
	public static <T extends @Nullable Object> @NonNull T @NonNull [][][][] castNonNullDeep(
			T @Nullable [] @Nullable [] @Nullable [] @Nullable [] arr, String message) {
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
	 * @return the argument, cast to have the type qualifier @NonNull at all levels
	 *
	 * @see #castNonNull(Object)
	 */
	@EnsuresNonNull("#1")
	public static <T extends @Nullable Object> @NonNull T @NonNull [][][][][] castNonNullDeep(
			T @Nullable [] @Nullable [] @Nullable [] @Nullable [] @Nullable [] arr) {
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
	 * @return the argument, cast to have the type qualifier @NonNull at all levels
	 *
	 * @see #castNonNull(Object)
	 */
	@EnsuresNonNull("#1")
	public static <T extends @Nullable Object> @NonNull T @NonNull [][][][][] castNonNullDeep(
			T @Nullable [] @Nullable [] @Nullable [] @Nullable [] @Nullable [] arr, String message) {
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
	 * @return the argument, cast to have the type qualifier @NonNull at all levels
	 */
	private static <T extends @Nullable Object> @NonNull T @NonNull [] castNonNullArray(
			T @Nullable [] arr, @Nullable String message) {
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
	private static void checkIfArray(@NonNull Object ref, @Nullable String message) {
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
