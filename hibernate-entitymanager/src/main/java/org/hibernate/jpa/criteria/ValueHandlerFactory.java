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
package org.hibernate.jpa.criteria;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Helper for generically dealing with literal values.
 *
 * @author Steve Ebersole
 */
public class ValueHandlerFactory {
	private ValueHandlerFactory() {
	}

	public static interface ValueHandler<T> {
		public T convert(Object value);
		public String render(T value);
	}

	public static abstract class BaseValueHandler<T> implements ValueHandler<T>, Serializable {
		public String render(T value) {
			return value.toString();
		}
	}

	public static class NoOpValueHandler<T> extends BaseValueHandler<T> {
		@SuppressWarnings({ "unchecked" })
		public T convert(Object value) {
			return (T) value;
		}
	}

	public static boolean isCharacter(Class type) {
		return String.class.isAssignableFrom( type )
				|| Character.class.isAssignableFrom( type )
				|| Character.TYPE.equals( type );
	}

	public static boolean isCharacter(Object value) {
		return String.class.isInstance( value )
				|| Character.class.isInstance( value )
				|| Character.TYPE.isInstance( value );
	}

	public static boolean isNumeric(Class type) {
		return Number.class.isAssignableFrom( type )
				|| Byte.TYPE.equals( type )
				|| Short.TYPE.equals( type )
				|| Integer.TYPE.equals( type )
				|| Long.TYPE.equals( type )
				|| Float.TYPE.equals( type )
				|| Double.TYPE.equals( type );
	}

	public static boolean isNumeric(Object value) {
		return Number.class.isInstance( value )
				|| Byte.TYPE.isInstance( value )
				|| Short.TYPE.isInstance( value )
				|| Integer.TYPE.isInstance( value )
				|| Long.TYPE.isInstance( value )
				|| Float.TYPE.isInstance( value )
				|| Double.TYPE.isInstance( value );
	}

	public static class ByteValueHandler extends BaseValueHandler<Byte> implements Serializable {
		public static final ByteValueHandler INSTANCE = new ByteValueHandler();
		@SuppressWarnings({ "UnnecessaryBoxing" })
		public Byte convert(Object value) {
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

	public static class ShortValueHandler extends BaseValueHandler<Short> implements Serializable {
		public static final ShortValueHandler INSTANCE = new ShortValueHandler();
		@SuppressWarnings({ "UnnecessaryBoxing" })
		public Short convert(Object value) {
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

	public static class IntegerValueHandler extends BaseValueHandler<Integer> implements Serializable {
		public static final IntegerValueHandler INSTANCE = new IntegerValueHandler();
		@SuppressWarnings({ "UnnecessaryBoxing" })
		public Integer convert(Object value) {
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

	public static class LongValueHandler extends BaseValueHandler<Long> implements Serializable {
		public static final LongValueHandler INSTANCE = new LongValueHandler();
		@SuppressWarnings({ "UnnecessaryBoxing" })
		public Long convert(Object value) {
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

		@Override
		public String render(Long value) {
			return value.toString() + 'L';
		}
	}

	public static class FloatValueHandler extends BaseValueHandler<Float> implements Serializable {
		public static final FloatValueHandler INSTANCE = new FloatValueHandler();
		@SuppressWarnings({ "UnnecessaryBoxing" })
		public Float convert(Object value) {
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

		@Override
		public String render(Float value) {
			return value.toString() + 'F';
		}
	}

	public static class DoubleValueHandler extends BaseValueHandler<Double> implements Serializable {
		public static final DoubleValueHandler INSTANCE = new DoubleValueHandler();
		@SuppressWarnings({ "UnnecessaryBoxing" })
		public Double convert(Object value) {
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

		@Override
		public String render(Double value) {
			return value.toString() + 'D';
		}
	}

	public static class BigIntegerValueHandler extends BaseValueHandler<BigInteger> implements Serializable {
		public static final BigIntegerValueHandler INSTANCE = new BigIntegerValueHandler();
		public BigInteger convert(Object value) {
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

		@Override
		public String render(BigInteger value) {
			return value.toString() + "BI";
		}
	}

	public static class BigDecimalValueHandler extends BaseValueHandler<BigDecimal> implements Serializable {
		public static final BigDecimalValueHandler INSTANCE = new BigDecimalValueHandler();
		public BigDecimal convert(Object value) {
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

		@Override
		public String render(BigDecimal value) {
			return value.toString() + "BD";
		}
	}

	public static class StringValueHandler extends BaseValueHandler<String> implements Serializable {
		public static final StringValueHandler INSTANCE = new StringValueHandler();
		public String convert(Object value) {
			return value == null ? null : value.toString();
		}
	}

	private static IllegalArgumentException unknownConversion(Object value, Class type) {
		return new IllegalArgumentException(
				"Unaware how to convert value [" + value + " : " + typeName( value ) + "] to requested type [" + type.getName() + "]"
		);
	}

	private static String typeName(Object value) {
		return value == null ? "???" : value.getClass().getName();
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

		ValueHandler<T> valueHandler = determineAppropriateHandler( targetType );
		if ( valueHandler == null ) {
			throw unknownConversion( value, targetType );
		}
		return valueHandler.convert( value );
	}

	/**
	 * Determine the appropriate {@link ValueHandlerFactory.ValueHandler} strategy for converting a value
	 * to the given target type
	 *
	 * @param targetType The target type (to which we want to convert values).
	 * @param <T> parameterized type for the target type.
	 * @return The conversion
	 */
	@SuppressWarnings({ "unchecked" })
	public static <T> ValueHandler<T> determineAppropriateHandler(Class<T> targetType) {
		if ( String.class.equals( targetType ) ) {
			return (ValueHandler<T>) StringValueHandler.INSTANCE;
		}
		if ( Byte.class.equals( targetType ) || Byte.TYPE.equals( targetType ) ) {
			return (ValueHandler<T>) ByteValueHandler.INSTANCE;
		}
		if ( Short.class.equals( targetType ) || Short.TYPE.equals( targetType ) ) {
			return (ValueHandler<T>) ShortValueHandler.INSTANCE;
		}
		if ( Integer.class.equals( targetType ) || Integer.TYPE.equals( targetType ) ) {
			return (ValueHandler<T>) IntegerValueHandler.INSTANCE;
		}
		if ( Long.class.equals( targetType ) || Long.TYPE.equals( targetType ) ) {
			return (ValueHandler<T>) LongValueHandler.INSTANCE;
		}
		if ( Float.class.equals( targetType ) || Float.TYPE.equals( targetType ) ) {
			return (ValueHandler<T>) FloatValueHandler.INSTANCE;
		}
		if ( Double.class.equals( targetType ) || Double.TYPE.equals( targetType ) ) {
			return (ValueHandler<T>) DoubleValueHandler.INSTANCE;
		}
		if ( BigInteger.class.equals( targetType ) ) {
			return (ValueHandler<T>) BigIntegerValueHandler.INSTANCE;
		}
		if ( BigDecimal.class.equals( targetType ) ) {
			return (ValueHandler<T>) BigDecimalValueHandler.INSTANCE;
		}
		return null;
	}
}
