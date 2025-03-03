/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util;

/**
 * @author Vlad Mihalcea
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
		return 1 << -Integer.numberOfLeadingZeros(value - 1);
	}

	/**
	 * Returns the result of dividing a positive {@code numerator} by a positive {@code denominator} rounded up.
	 * <p>
	 * For example dividing 5 by 2 ist 2.5, which (when rounded up) gives a result of 3.
	 */
	public static int divideRoundingUp(int numerator, int denominator) {
		return ( numerator + denominator - 1 ) / denominator;
	}
}
