/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.util;

/**
 * @author Vlad Mihalcea
 * @author Adrodoc
 */
public final class MathHelper {

	private MathHelper() { /* static methods only - hide constructor */
	}

	/**
	 * Returns the smallest power of two number that is greater than or equal to {@code value}.
	 *
	 * @param value reference number
	 * @return smallest power of two number
	 */
	public static int ceilingPowerOfTwo(int value) {
		int result = 1 << -Integer.numberOfLeadingZeros(value - 1);
		if ( result < value ) { // Overflow
			return Integer.MAX_VALUE;
		}
		return result;
	}

	/**
	 * Returns the result of dividing a positive {@code numerator} by a positive {@code denominator} rounded up. For
	 * example dividing 5 by 2 would give a result of 3.
	 */
	public static int divideRoundingUp(int numerator, int denominator) {
		if ( numerator == 0 ) {
			return 0;
		}
		return ( ( numerator - 1 ) / denominator ) + 1;
	}
}
