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
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * Descriptor for {@link BigInteger} handling.
 *
 * @author Steve Ebersole
 */
public class BigIntegerJavaType extends AbstractClassJavaType<BigInteger> {
	public static final BigIntegerJavaType INSTANCE = new BigIntegerJavaType();

	public BigIntegerJavaType() {
		super( BigInteger.class );
	}

	@Override
	public String toString(BigInteger value) {
		return value.toString();
	}

	@Override
	public BigInteger fromString(CharSequence string) {
		return new BigInteger( string.toString() );
	}

	@Override
	public boolean isInstance(Object value) {
		return value instanceof BigInteger;
	}

	@Override
	public int extractHashCode(BigInteger value) {
		return value.intValue();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> X unwrap(BigInteger value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( BigInteger.class.isAssignableFrom( type ) ) {
			return (X) value;
		}
		if ( BigDecimal.class.isAssignableFrom( type ) ) {
			return (X) new BigDecimal( value );
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
		if ( Double.class.isAssignableFrom( type ) ) {
			return (X) Double.valueOf( value.doubleValue() );
		}
		if ( Float.class.isAssignableFrom( type ) ) {
			return (X) Float.valueOf( value.floatValue() );
		}
		if ( String.class.isAssignableFrom( type ) ) {
			return (X) value.toString();
		}
		throw unknownUnwrap( type );
	}

	@Override
	public <X> BigInteger wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( value instanceof BigInteger bigInteger ) {
			return bigInteger;
		}
		if ( value instanceof BigDecimal bigDecimal ) {
			return bigDecimal.toBigIntegerExact();
		}
		if ( value instanceof Number number ) {
			return BigInteger.valueOf( number.longValue() );
		}
		if ( value instanceof String string ) {
			return new BigInteger( string );
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
				"long", "java.lang.Long" -> true;
			default -> false;
		};
	}

	@Override
	public long getDefaultSqlLength(Dialect dialect, JdbcType jdbcType) {
		return getDefaultSqlPrecision( dialect, jdbcType )+1;
	}

	@Override
	public int getDefaultSqlPrecision(Dialect dialect, JdbcType jdbcType) {
		return dialect.getDefaultDecimalPrecision();
	}

	@Override
	public int getDefaultSqlScale(Dialect dialect, JdbcType jdbcType) {
		return 0;
	}

	@Override
	public <X> BigInteger coerce(X value, CoercionContext coercionContext) {
		if ( value == null ) {
			return null;
		}

		if ( value instanceof BigInteger bigInteger ) {
			return bigInteger;
		}

		if ( value instanceof Byte byteValue ) {
			return BigInteger.valueOf( byteValue );
		}

		if ( value instanceof Short shortValue ) {
			return BigInteger.valueOf( shortValue );
		}

		if ( value instanceof Integer integerValue ) {
			return BigInteger.valueOf( integerValue );
		}

		if ( value instanceof Long longValue ) {
			return BigInteger.valueOf( longValue );
		}

		if ( value instanceof Double doubleValue ) {
			return CoercionHelper.toBigInteger( doubleValue );
		}

		if ( value instanceof Float floatValue ) {
			return CoercionHelper.toBigInteger( floatValue );
		}

		if ( value instanceof BigDecimal bigDecimal ) {
			return CoercionHelper.toBigInteger( bigDecimal );
		}

		if ( value instanceof String string ) {
			return CoercionHelper.coerceWrappingError(
					() -> BigInteger.valueOf( Long.parseLong( string ) )
			);
		}

		throw new CoercionException(
				String.format(
						Locale.ROOT,
						"Unable to coerce value [%s (%s)] to BigInteger",
						value,
						value.getClass().getName()
				)
		);
	}
}
