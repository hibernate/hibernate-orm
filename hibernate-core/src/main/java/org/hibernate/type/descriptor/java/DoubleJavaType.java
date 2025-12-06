/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Locale;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.spi.PrimitiveJavaType;
import org.hibernate.type.descriptor.jdbc.DoubleJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

/**
 * Descriptor for {@link Double} handling.
 *
 * @author Steve Ebersole
 */
public class DoubleJavaType extends AbstractClassJavaType<Double> implements
		PrimitiveJavaType<Double> {
	public static final DoubleJavaType INSTANCE = new DoubleJavaType();

	public DoubleJavaType() {
		super( Double.class );
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators indicators) {
		return DoubleJdbcType.INSTANCE;
	}

	@Override
	public boolean useObjectEqualsHashCode() {
		return true;
	}

	@Override
	public String toString(Double value) {
		return value == null ? null : value.toString();
	}

	@Override
	public Double fromString(CharSequence string) {
		return Double.valueOf( string.toString() );
	}

	@Override
	public boolean isInstance(Object value) {
		return value instanceof Double;
	}

	@Override
	public Double cast(Object value) {
		return (Double) value;
	}

	@Override
	public <X> X unwrap(Double value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( Double.class.isAssignableFrom( type ) || type == Object.class ) {
			return type.cast( value );
		}
		if ( Float.class.isAssignableFrom( type ) ) {
			return type.cast( value.floatValue() );
		}
		if ( Byte.class.isAssignableFrom( type ) ) {
			return type.cast( value.byteValue() );
		}
		if ( Short.class.isAssignableFrom( type ) ) {
			return type.cast( value.shortValue() );
		}
		if ( Integer.class.isAssignableFrom( type ) ) {
			return type.cast( value.intValue() );
		}
		if ( Long.class.isAssignableFrom( type ) ) {
			return type.cast( value.longValue() );
		}
		if ( BigInteger.class.isAssignableFrom( type ) ) {
			return type.cast( BigInteger.valueOf( value.longValue() ) );
		}
		if ( BigDecimal.class.isAssignableFrom( type ) ) {
			return type.cast( BigDecimal.valueOf( value ) );
		}
		if ( String.class.isAssignableFrom( type ) ) {
			return type.cast( value.toString() );
		}
		throw unknownUnwrap( type );
	}

	@Override
	public <X> Double wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( value instanceof Double doubleValue ) {
			return doubleValue;
		}
		if ( value instanceof Number number ) {
			return number.doubleValue();
		}
		else if ( value instanceof String string ) {
			return Double.valueOf( string );
		}
		throw unknownWrap( value.getClass() );
	}

	@Override
	public boolean isWider(JavaType<?> javaType) {
		return switch ( javaType.getTypeName() ) {
			case
				"byte", "java.lang.Byte",
				"short", "java.lang.Short",
				"int", "java.lang.Integer",
				"long", "java.lang.Long",
				"float", "java.lang.Float",
				"java.math.BigInteger",
				"java.math.BigDecimal" -> true;
			default -> false;
		};
	}

	@Override
	public Class<?> getPrimitiveClass() {
		return double.class;
	}

	@Override
	public Class<Double[]> getArrayClass() {
		return Double[].class;
	}

	@Override
	public Class<?> getPrimitiveArrayClass() {
		return double[].class;
	}

	@Override
	public Double getDefaultValue() {
		return 0.0;
	}

	@Override
	public long getDefaultSqlLength(Dialect dialect, JdbcType jdbcType) {
		//this is the number of decimal digits
		// + sign + decimal point
		// + space for "E+nnn"
		return 1+17+1+5;
	}

	@Override
	public int getDefaultSqlPrecision(Dialect dialect, JdbcType jdbcType) {
		return jdbcType.isFloat()
				// this is usually the number of *binary* digits
				// in a double-precision FP number
				? dialect.getDoublePrecision()
				// this is the number of decimal digits in a Java double
				: 17;
	}

	@Override
	public <X> Double coerce(X value, CoercionContext coercionContext) {
		if ( value == null ) {
			return null;
		}

		if ( value instanceof Double doubleValue ) {
			return doubleValue;
		}

		if ( value instanceof Float floatValue ) {
			return CoercionHelper.toDouble( floatValue );
		}

		if ( value instanceof BigInteger bigInteger ) {
			return CoercionHelper.toDouble( bigInteger );
		}

		if ( value instanceof BigDecimal bigDecimal ) {
			return CoercionHelper.toDouble( bigDecimal );
		}

		if ( value instanceof Number number ) {
			return number.doubleValue();
		}

		if ( value instanceof String string ) {
			return CoercionHelper.coerceWrappingError(
					() -> Double.parseDouble( string )
			);
		}

		throw new CoercionException(
				String.format(
						Locale.ROOT,
						"Cannot coerce value '%s' [%s] to Double",
						value,
						value.getClass().getName()
				)
		);
	}

}
