/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.util;

import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

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
	public static <T> T castNonNull(@Nullable T ref) {
		return requireNonNull( ref, "Misuse of castNonNull: called with a null argument" );
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
	public static <T> T castNonNull(@Nullable T ref, String message) {
		return requireNonNull( ref, "Misuse of castNonNull: called with a null argument: " + message );
	}

	// removed unused code and very deeply arrays
}
