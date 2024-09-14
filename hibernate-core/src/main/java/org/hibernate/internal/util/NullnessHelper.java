/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.util;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Steve Ebersole
 */
public class NullnessHelper {
	private NullnessHelper() {
	}

	public static <T> T nullif(T test, T fallback) {
		return coalesce( test, fallback );
	}

	public static <T> T nullif(T test, Supplier<T> fallbackSupplier) {
		return test != null ? test : fallbackSupplier.get();
	}

	/**
	 * Operates like SQL coalesce expression, returning the first non-empty value
	 *
	 * @implNote This impl treats empty strings (`""`) as null.
	 *
	 * @param values The list of values.
	 * @param <T> Generic type of values to coalesce
	 *
	 * @return The first non-empty value, or null if all values were empty
	 */
	@SafeVarargs
	public static <T> T coalesce(T... values) {
		if ( values == null ) {
			return null;
		}
		for ( T value : values ) {
			if ( value != null ) {
				if ( value instanceof String ) {
					if ( StringHelper.isNotEmpty( (String) value ) ) {
						return value;
					}
				}
				else {
					return value;
				}
			}
		}

		return null;
	}

	/**
	 * Operates like SQL coalesce expression, returning the first non-empty value
	 *
	 * @implNote This impl treats empty strings (`""`) as null.
	 *
	 * @param valueSuppliers List of value Suppliers
	 * @param <T> Generic type of values to coalesce
	 *
	 * @return The first non-empty value, or null if all values were empty
	 */
	@SafeVarargs
	public static <T> T coalesceSuppliedValues(Supplier<T>... valueSuppliers) {
		return coalesceSuppliedValues(
				(value) -> ( value instanceof String && StringHelper.isNotEmpty( (String) value ) )
						|| value != null,
				valueSuppliers
		);
	}

	/**
	 * Operates like SQL coalesce expression, returning the first non-empty value
	 *
	 * @implNote This impl treats empty strings (`""`) as null.
	 *
	 * @param valueSuppliers List of value Suppliers
	 * @param <T> Generic type of values to coalesce
	 *
	 * @return The first non-empty value, or null if all values were empty
	 */
	@SafeVarargs
	public static <T> T coalesceSuppliedValues(Function<T,Boolean> checker, Supplier<T>... valueSuppliers) {
		if ( valueSuppliers == null ) {
			return null;
		}

		for ( Supplier<T> valueSupplier : valueSuppliers ) {
			if ( valueSupplier != null ) {
				final T value = valueSupplier.get();
				if ( checker.apply( value ) ) {
					return value;
				}

			}
		}

		return null;
	}

	/**
	 * Ensures that either:<ul>
	 *     <li>all values are null</li>
	 *     <li>all values are non-null</li>
	 * </ul>
	 */
	public static boolean areSameNullness(Object... values) {
		if ( values == null || values.length > 2 ) {
			// we have no elements or 1
			return true;
		}

		final boolean firstValueIsNull = values[0] == null;
		for ( int i = 1; i < values.length; i++ ) {
			// look for mismatch
			if ( firstValueIsNull != (values[i] == null) ) {
				return false;
			}
		}

		return true;
	}

	public static boolean areAllNonNull(Object... objects) {
		if ( objects == null || objects.length == 0 ) {
			return true;
		}

		for ( int i = 0; i < objects.length; i++ ) {
			if ( objects[i] == null ) {
				return false;
			}
		}
		return true;
	}
}
