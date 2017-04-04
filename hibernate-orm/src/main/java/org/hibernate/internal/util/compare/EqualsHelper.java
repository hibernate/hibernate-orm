/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.util.compare;

import java.util.Arrays;

/**
 * Helper for equality determination
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public final class EqualsHelper {

	@SuppressWarnings("SimplifiableIfStatement")
	public static boolean equals(final Object x, final Object y) {
		if ( x == y ) {
			return true;
		}

		if ( x == null || y == null ) {
			// One is null, but the other is not (otherwise the `x == y` check would have passed).
			// null can never equal a non-null
			return false;
		}

		return x.equals( y );
	}

	/**
	 * Like the legacy {@link #equals} method, but handles array-specific checks
	 *
	 * @param x One value to check
	 * @param y The other value
	 *
	 * @return {@code true} if the 2 values are equal; {@code false} otherwise.
	 */
	public static boolean areEqual(final Object x, final Object y) {
		if ( x == y ) {
			return true;
		}

		if ( x == null || y == null ) {
			// One is null, but the other is not (otherwise the `x == y` check would have passed).
			// null can never equal a non-null
			return false;
		}

		if ( x.equals( y ) ) {
			return true;
		}

		// Check for possibility of arrays
		final Class xClass = x.getClass();
		final Class yClass = y.getClass();

		if ( xClass.isArray() && yClass.isArray() ) {
			if ( xClass.equals( yClass ) ) {
				if ( x instanceof boolean[] ) {
					return Arrays.equals( (boolean[]) x, (boolean[]) y );
				}
				else if ( x instanceof byte[] ) {
					return Arrays.equals( (byte[]) x, (byte[]) y );
				}
				else if ( x instanceof char[] ) {
					return Arrays.equals( (char[]) x, (char[]) y );
				}
				else  if ( x instanceof short[] ) {
					return Arrays.equals( (short[]) x, (short[]) y );
				}
				else if ( x instanceof int[] ) {
					return Arrays.equals( (int[]) x, (int[]) y );
				}
				else if ( x instanceof long[] ) {
					return Arrays.equals( (long[]) x, (long[]) y );
				}
				else  if ( x instanceof double[] ) {
					return Arrays.equals( (double[]) x, (double[]) y );
				}
				else  if ( x instanceof float[] ) {
					return Arrays.equals( (float[]) x, (float[]) y );
				}
			}
			return Arrays.equals( (Object[]) x, (Object[]) y );
		}

		return false;
	}

	/**
	 * Private ctor - disallow instantiation
	 */
	private EqualsHelper() {
		// disallow instantiation
	}

}
