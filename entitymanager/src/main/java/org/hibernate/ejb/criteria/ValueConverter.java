/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.ejb.criteria;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Helper for generically converting a values into another type.
 *
 * @author Steve Ebersole
 */
public class ValueConverter {
	private ValueConverter() {
	}

	public static interface Conversion<T> {
		public T apply(Object value);
	}

	public static class ByteConversion implements Conversion<Byte> {
		public static final ByteConversion INSTANCE = new ByteConversion();
		@SuppressWarnings({ "UnnecessaryBoxing" })
		public Byte apply(Object value) {
			if ( value == null ) {
				return null;
			}
			if ( Number.class.isInstance( value ) ) {
				return Byte.valueOf( ( (Number) value ).byteValue() );
			}
			else if ( String.class.isInstance( value ) ) {
				return Byte.valueOf( ( (String) value ) );
			}
			throw unknownConversion( value, Byte.class );
		}
	}

	public static class ShortConversion implements Conversion<Short> {
		public static final ShortConversion INSTANCE = new ShortConversion();
		@SuppressWarnings({ "UnnecessaryBoxing" })
		public Short apply(Object value) {
			if ( value == null ) {
				return null;
			}
			if ( Number.class.isInstance( value ) ) {
				return Short.valueOf( ( (Number) value ).shortValue() );
			}
			else if ( String.class.isInstance( value ) ) {
				return Short.valueOf( ( (String) value ) );
			}
			throw unknownConversion( value, Short.class );
		}
	}

	public static class IntegerConversion implements Conversion<Integer> {
		public static final IntegerConversion INSTANCE = new IntegerConversion();
		@SuppressWarnings({ "UnnecessaryBoxing" })
		public Integer apply(Object value) {
			if ( value == null ) {
				return null;
			}
			if ( Number.class.isInstance( value ) ) {
				return Integer.valueOf( ( (Number) value ).intValue() );
			}
			else if ( String.class.isInstance( value ) ) {
				return Integer.valueOf( ( (String) value ) );
			}
			throw unknownConversion( value, Integer.class );
		}
	}

	public static class LongConversion implements Conversion<Long> {
		public static final LongConversion INSTANCE = new LongConversion();
		@SuppressWarnings({ "UnnecessaryBoxing" })
		public Long apply(Object value) {
			if ( value == null ) {
				return null;
			}
			if ( Number.class.isInstance( value ) ) {
				return Long.valueOf( ( (Number) value ).longValue() );
			}
			else if ( String.class.isInstance( value ) ) {
				return Long.valueOf( ( (String) value ) );
			}
			throw unknownConversion( value, Long.class );
		}
	}

	public static class FloatConversion implements Conversion<Float> {
		public static final FloatConversion INSTANCE = new FloatConversion();
		@SuppressWarnings({ "UnnecessaryBoxing" })
		public Float apply(Object value) {
			if ( value == null ) {
				return null;
			}
			if ( Number.class.isInstance( value ) ) {
				return Float.valueOf( ( (Number) value ).floatValue() );
			}
			else if ( String.class.isInstance( value ) ) {
				return Float.valueOf( ( (String) value ) );
			}
			throw unknownConversion( value, Float.class );
		}
	}

	public static class DoubleConversion implements Conversion<Double> {
		public static final DoubleConversion INSTANCE = new DoubleConversion();
		@SuppressWarnings({ "UnnecessaryBoxing" })
		public Double apply(Object value) {
			if ( value == null ) {
				return null;
			}
			if ( Number.class.isInstance( value ) ) {
				return Double.valueOf( ( (Number) value ).doubleValue() );
			}
			else if ( String.class.isInstance( value ) ) {
				return Double.valueOf( ( (String) value ) );
			}
			throw unknownConversion( value, Double.class );
		}
	}

	public static class BigIntegerConversion implements Conversion<BigInteger> {
		public static final BigIntegerConversion INSTANCE = new BigIntegerConversion();
		public BigInteger apply(Object value) {
			if ( value == null ) {
				return null;
			}
			if ( Number.class.isInstance( value ) ) {
				return BigInteger.valueOf( ( (Number) value ).longValue() );
			}
			else if ( String.class.isInstance( value ) ) {
				return new BigInteger( (String) value );
			}
			throw unknownConversion( value, BigInteger.class );
		}
	}

	public static class BigDecimalConversion implements Conversion<BigDecimal> {
		public static final BigDecimalConversion INSTANCE = new BigDecimalConversion();
		public BigDecimal apply(Object value) {
			if ( value == null ) {
				return null;
			}
			if ( BigInteger.class.isInstance( value ) ) {
				return new BigDecimal( (BigInteger) value );
			}
			else if ( Number.class.isInstance( value ) ) {
				return BigDecimal.valueOf( ( (Number) value ).doubleValue() );
			}
			else if ( String.class.isInstance( value ) ) {
				return new BigDecimal( (String) value );
			}
			throw unknownConversion( value, BigDecimal.class );
		}
	}

	public static class StringConversion implements Conversion<String> {
		public static final StringConversion INSTANCE = new StringConversion();
		public String apply(Object value) {
			return value == null ? null : value.toString();
		}
	}

	private static IllegalArgumentException unknownConversion(Object value, Class type) {
		return new IllegalArgumentException(
				"Unaware how to convert value [" + value + "] to requested type [" + type.getName() + "]"
		);
	}

	/**
	 * Convert the given value into the specified target type.
	 *
	 * @param value The value to convert
	 * @param targetType The type to which it should be converted
	 *
	 * @return The converted value.
	 */
	@SuppressWarnings({ "unchecked" })
	public static <T> T convert(Object value, Class<T> targetType) {
		if ( value == null ) {
			return null;
		}
		if ( targetType.equals( value.getClass() ) ) {
			return (T) value;
		}

		Conversion<T> conversion = determineAppropriateConversion( targetType );
		if ( conversion == null ) {
			throw unknownConversion( value, targetType );
		}
		return conversion.apply( value );
	}

	/**
	 * Determine the appropriate {@link Conversion} strategy for converting a value
	 * to the given target type
	 *
	 * @param targetType The target type (to which we want to convert values).
	 * @param <T> parameterized type for the target type.
	 * @return The conversion
	 */
	@SuppressWarnings({ "unchecked" })
	public static <T> Conversion<T> determineAppropriateConversion(Class<T> targetType) {
		if ( String.class.equals( targetType ) ) {
			return (Conversion<T>) StringConversion.INSTANCE;
		}
		if ( Byte.class.equals( targetType ) ) {
			return (Conversion<T>) ByteConversion.INSTANCE;
		}
		if ( Short.class.equals( targetType ) ) {
			return (Conversion<T>) ShortConversion.INSTANCE;
		}
		if ( Integer.class.equals( targetType ) ) {
			return (Conversion<T>) IntegerConversion.INSTANCE;
		}
		if ( Long.class.equals( targetType ) ) {
			return (Conversion<T>) LongConversion.INSTANCE;
		}
		if ( Float.class.equals( targetType ) ) {
			return (Conversion<T>) FloatConversion.INSTANCE;
		}
		if ( Double.class.equals( targetType ) ) {
			return (Conversion<T>) DoubleConversion.INSTANCE;
		}
		if ( BigInteger.class.equals( targetType ) ) {
			return (Conversion<T>) BigIntegerConversion.INSTANCE;
		}
		if ( BigDecimal.class.equals( targetType ) ) {
			return (Conversion<T>) BigDecimalConversion.INSTANCE;
		}
		return null;
	}
}
