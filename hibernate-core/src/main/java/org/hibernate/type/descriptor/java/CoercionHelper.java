/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Locale;

/**
 * Helper for type coercions.  Mainly used for narrowing coercions which
 * might lead to under/over-flow problems
 *
 * @author Steve Ebersole
 */
public class CoercionHelper {
	private CoercionHelper() {
		// disallow direct instantiation
	}

	public static Byte toByte(Short value) {
		if ( value > Byte.MAX_VALUE ) {
			throw new CoercionException(
					String.format(
							Locale.ROOT,
							"Cannot coerce Short value `%s` to Byte : overflow",
							value
					)
			);
		}

		if ( value < Byte.MIN_VALUE ) {
			throw new CoercionException(
					String.format(
							Locale.ROOT,
							"Cannot coerce Short value `%s` to Byte : underflow",
							value
					)
			);
		}

		return value.byteValue();
	}

	public static Byte toByte(Integer value) {
		if ( value > Byte.MAX_VALUE ) {
			throw new CoercionException(
					String.format(
							Locale.ROOT,
							"Cannot coerce Integer value `%s` to Byte : overflow",
							value
					)
			);
		}

		if ( value < Byte.MIN_VALUE ) {
			throw new CoercionException(
					String.format(
							Locale.ROOT,
							"Cannot coerce Integer value `%s` to Byte : underflow",
							value
					)
			);
		}

		return value.byteValue();
	}

	public static Byte toByte(Long value) {
		if ( value > Byte.MAX_VALUE ) {
			throw new CoercionException(
					String.format(
							Locale.ROOT,
							"Cannot coerce Long value `%s` to Byte : overflow",
							value
					)
			);
		}

		if ( value < Byte.MIN_VALUE ) {
			throw new CoercionException(
					String.format(
							Locale.ROOT,
							"Cannot coerce Long value `%s` to Byte : underflow",
							value
					)
			);
		}

		return value.byteValue();
	}

	public static Byte toByte(Double value) {
		if ( ! isWholeNumber( value ) ) {
			throw new CoercionException(
					String.format(
							Locale.ROOT,
							"Cannot coerce Double value `%s` to Byte : not a whole number",
							value
					)
			);
		}

		if ( value > Byte.MAX_VALUE ) {
			throw new CoercionException(
					String.format(
							Locale.ROOT,
							"Cannot coerce Double value `%s` to Byte : overflow",
							value
					)
			);
		}

		if ( value < Byte.MIN_VALUE ) {
			throw new CoercionException(
					String.format(
							Locale.ROOT,
							"Cannot coerce Double value `%s` to Byte : underflow",
							value
					)
			);
		}

		return value.byteValue();
	}

	public static Byte toByte(Float value) {
		if ( ! isWholeNumber( value ) ) {
			throw new CoercionException(
					String.format(
							Locale.ROOT,
							"Cannot coerce Float value `%s` to Byte : not a whole number",
							value
					)
			);
		}

		if ( value > Byte.MAX_VALUE ) {
			throw new CoercionException(
					String.format(
							Locale.ROOT,
							"Cannot coerce Float value `%s` to Byte : overflow",
							value
					)
			);
		}

		if ( value < Byte.MIN_VALUE ) {
			throw new CoercionException(
					String.format(
							Locale.ROOT,
							"Cannot coerce Float value `%s` to Byte : underflow",
							value
					)
			);
		}

		return value.byteValue();
	}

	public static Byte toByte(BigInteger value) {
		return coerceWrappingError( value::byteValueExact );
	}

	public static Byte toByte(BigDecimal value) {
		return coerceWrappingError( value::byteValueExact );
	}

	public static Short toShort(Byte value) {
		return value.shortValue();
	}

	public static Short toShort(Integer value) {
		if ( value > Short.MAX_VALUE ) {
			throw new CoercionException( "Cannot coerce Integer value `" + value + "` as Short : overflow" );
		}

		if ( value < Short.MIN_VALUE ) {
			throw new CoercionException( "Cannot coerce Integer value `" + value + "` as Short : underflow" );
		}

		return value.shortValue();
	}

	public static Short toShort(Long value) {
		if ( value > Short.MAX_VALUE ) {
			throw new CoercionException( "Cannot coerce Long value `" + value + "` as Short : overflow" );
		}

		if ( value < Short.MIN_VALUE ) {
			throw new CoercionException( "Cannot coerce Long value `" + value + "` as Short : underflow" );
		}

		return value.shortValue();
	}

	public static Short toShort(Double doubleValue) {
		if ( ! isWholeNumber( doubleValue ) ) {
			throw new CoercionException( "Cannot coerce Double value `" + doubleValue + "` as Short : not a whole number" );
		}
		return toShort( doubleValue.longValue() );
	}

	public static Short toShort(Float floatValue) {
		if ( ! isWholeNumber( floatValue ) ) {
			throw new CoercionException( "Cannot coerce Float value `" + floatValue + "` as Short : not a whole number" );
		}
		return toShort( floatValue.longValue() );
	}

	public static Short toShort(BigInteger value) {
		return coerceWrappingError( value::shortValueExact );
	}

	public static Short toShort(BigDecimal value) {
		return coerceWrappingError( value::shortValueExact );
	}

	public static Integer toInteger(Byte value) {
		return value.intValue();
	}

	public static Integer toInteger(Short value) {
		return value.intValue();
	}

	public static Integer toInteger(Long value) {
		return coerceWrappingError( () -> Math.toIntExact( value ) );
	}

	public static Integer toInteger(Double doubleValue) {
		if ( ! isWholeNumber( doubleValue ) ) {
			throw new CoercionException(
					String.format(
							Locale.ROOT,
							"Unable to coerce Double value `%s` to Integer: not a whole number",
							doubleValue
					)
			);
		}

		return toInteger( doubleValue.longValue() );
	}

	public static Integer toInteger(Float floatValue) {
		if ( ! isWholeNumber( floatValue ) ) {
			throw new CoercionException(
					String.format(
							Locale.ROOT,
							"Unable to coerce Float value `%s` to Integer: not a whole number",
							floatValue
					)
			);
		}

		return toInteger( floatValue.longValue() );
	}

	public static Integer toInteger(BigInteger value) {
		return coerceWrappingError( value::intValueExact );
	}

	public static Integer toInteger(BigDecimal value) {
		return coerceWrappingError( value::intValueExact );
	}

	public static Long toLong(Byte value) {
		return value.longValue();
	}

	public static Long toLong(Short value) {
		return value.longValue();
	}

	public static Long toLong(Integer value) {
		return value.longValue();
	}

	public static Long toLong(Double doubleValue) {
		if ( ! isWholeNumber( doubleValue ) ) {
			throw new CoercionException(
					String.format(
							Locale.ROOT,
							"Unable to coerce Double value `%s` as Integer: not a whole number",
							doubleValue
					)
			);
		}
		return doubleValue.longValue();
	}

	public static Long toLong(Float floatValue) {
		if ( ! isWholeNumber( floatValue ) ) {
			throw new CoercionException(
					String.format(
							Locale.ROOT,
							"Unable to coerce Float value `%s` as Integer: not a whole number",
							floatValue
					)
			);
		}
		return floatValue.longValue();
	}

	public static Long toLong(BigInteger value) {
		return coerceWrappingError( value::longValueExact );
	}

	public static Long toLong(BigDecimal value) {
		return coerceWrappingError( value::longValueExact );
	}

	public static BigInteger toBigInteger(Double doubleValue) {
		if ( ! isWholeNumber( doubleValue ) ) {
			throw new CoercionException(
					String.format(
							Locale.ROOT,
							"Unable to coerce Double value `%s` as BigInteger: not a whole number",
							doubleValue
					)
			);
		}
		return BigInteger.valueOf( doubleValue.longValue() );
	}

	public static BigInteger toBigInteger(Float floatValue) {
		if ( ! isWholeNumber( floatValue ) ) {
			throw new CoercionException(
					String.format(
							Locale.ROOT,
							"Unable to coerce Double Float `%s` as BigInteger: not a whole number",
							floatValue
					)
			);
		}
		return BigInteger.valueOf( floatValue.longValue() );
	}

	public static BigInteger toBigInteger(BigDecimal value) {
		return coerceWrappingError( value::toBigIntegerExact );
	}

	public static Double toDouble(Float floatValue) {
		if ( floatValue > (float) Double.MAX_VALUE ) {
			throw new CoercionException(
					String.format(
							Locale.ROOT,
							"Cannot coerce Float value `%s` to Double : overflow",
							floatValue
					)
			);
		}

		if ( floatValue < (float) Double.MIN_VALUE ) {
			throw new CoercionException(
					String.format(
							Locale.ROOT,
							"Cannot coerce Float value `%s` to Double : underflow",
							floatValue
					)
			);
		}

		return (double) floatValue;
	}

	public static Double toDouble(BigInteger value) {
		return coerceWrappingError( value::doubleValue );
	}

	public static Double toDouble(BigDecimal value) {
		return coerceWrappingError( value::doubleValue );
	}

	public static boolean isWholeNumber(double doubleValue) {
		return doubleValue % 1 == 0;
	}

	public static boolean isWholeNumber(float floatValue) {
		return floatValue == ( (float) (long) floatValue );
	}

	@FunctionalInterface
	public interface Coercer<T> {
		T doCoercion();
	}

	public static <T> T coerceWrappingError(Coercer<T> coercer) {
		try {
			return coercer.doCoercion();
		}
		catch (ArithmeticException | NumberFormatException e) {
			throw new CoercionException( "Error coercing value", e );
		}
	}
}
