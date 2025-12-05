/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Locale;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.spi.PrimitiveJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

/**
 * Descriptor for {@link Float} handling.
 *
 * @author Steve Ebersole
 */
public class FloatJavaType extends AbstractClassJavaType<Float> implements PrimitiveJavaType<Float> {
	public static final FloatJavaType INSTANCE = new FloatJavaType();

	public FloatJavaType() {
		super( Float.class );
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators indicators) {
		return indicators.getJdbcType( SqlTypes.FLOAT );
	}

	@Override
	public boolean useObjectEqualsHashCode() {
		return true;
	}

	@Override
	public String toString(Float value) {
		return value == null ? null : value.toString();
	}

	@Override
	public Float fromString(CharSequence string) {
		return Float.valueOf( string.toString() );
	}

	@Override
	public boolean isInstance(Object value) {
		return value instanceof Float;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <X> X unwrap(Float value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( Float.class.isAssignableFrom( type ) || type == Object.class ) {
			return (X) value;
		}
		if ( Double.class.isAssignableFrom( type ) ) {
			return (X) Double.valueOf( value.doubleValue() );
		}
		if ( Byte.class.isAssignableFrom( type ) ) {
			return (X) Byte.valueOf( value.byteValue() );
		}
		if ( Short.class.isAssignableFrom( type ) ) {
			return (X) Short.valueOf( value.shortValue() );
		}
		if ( Integer.class.isAssignableFrom( type ) ) {
			return (X) Integer.valueOf( value.intValue() );
		}
		if ( Long.class.isAssignableFrom( type ) ) {
			return (X) Long.valueOf( value.longValue() );
		}
		if ( BigInteger.class.isAssignableFrom( type ) ) {
			return (X) BigInteger.valueOf( value.longValue() );
		}
		if ( BigDecimal.class.isAssignableFrom( type ) ) {
			return (X) BigDecimal.valueOf( value );
		}
		if ( String.class.isAssignableFrom( type ) ) {
			return (X) value.toString();
		}
		throw unknownUnwrap( type );
	}

	@Override
	public <X> Float wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if (value instanceof Float floatValue) {
			return floatValue;
		}
		if (value instanceof Number number) {
			return number.floatValue();
		}
		else if (value instanceof String string) {
			return Float.valueOf( string );
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
				"java.math.BigInteger",
				"java.math.BigDecimal" -> true;
			default -> false;
		};
	}

	@Override
	public Class<?> getPrimitiveClass() {
		return float.class;
	}

	@Override
	public Class<Float[]> getArrayClass() {
		return Float[].class;
	}

	@Override
	public Class<?> getPrimitiveArrayClass() {
		return float[].class;
	}

	@Override
	public Float getDefaultValue() {
		return 0F;
	}

	@Override
	public long getDefaultSqlLength(Dialect dialect, JdbcType jdbcType) {
		//this is the number of decimal digits
		// + sign + decimal point
		// + space for "E+nn"
		return 1+8+1+4;
	}

	@Override
	public int getDefaultSqlPrecision(Dialect dialect, JdbcType jdbcType) {
		return jdbcType.isFloat()
				// this is usually the number of *binary* digits
				// in a single-precision FP number
				? dialect.getFloatPrecision()
				// this is the number of decimal digits in a Java float
				: 8;
	}

	@Override
	public <X> Float coerce(X value, CoercionContext coercionContext) {
		if ( value == null ) {
			return null;
		}

		if ( value instanceof Float floatValue ) {
			return floatValue;
		}

		if ( value instanceof Number number ) {
			return number.floatValue();
		}

		if ( value instanceof String ) {
			return CoercionHelper.coerceWrappingError(
					() -> Float.parseFloat( (String) value )
			);
		}

		throw new CoercionException(
				String.format(
						Locale.ROOT,
						"Cannot coerce value '%s' [%s] to Float",
						value,
						value.getClass().getName()
				)
		);
	}

}
