/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.internal.util;

import java.util.function.Supplier;

/**
 * @author Steve Ebersole
 */
public class NullnessHelper {

	/**
	 * Operates like SQL coalesce expression, except empty strings are treated as null.  Return the first non-empty value
	 *
	 * @param values The list of values.
	 * @param <T> Generic type of values to coalesce
	 *
	 * @return The first non-empty value, or null if all values were empty
	 */
	public static <T> T coalesce(T... values) {
		if ( values == null ) {
			return null;
		}
		for ( T value : values ) {
			if ( value != null ) {
				if ( String.class.isInstance( value ) ) {
					if ( !( (String) value ).isEmpty() ) {
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
	 * Find the first non-null value supplied by the given suppliers
	 */
	public static <T> T coalesceSuppliedValues(Supplier<T>... valueSuppliers) {
		if ( valueSuppliers == null ) {
			return null;
		}

		for ( Supplier<T> valueSupplier : valueSuppliers ) {
			final T value = valueSupplier.get();
			if ( value != null ) {
					return value;
			}
		}

		return null;
	}
}
