/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Locale;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.spi.PrimitiveJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * Descriptor for {@link Integer} handling.
 *
 * @author Steve Ebersole
 */
public class IntegerJavaType extends AbstractClassJavaType<Integer>
		implements PrimitiveJavaType<Integer>, VersionJavaType<Integer> {

	public static final Integer ZERO = 0;
	public static final IntegerJavaType INSTANCE = new IntegerJavaType();

	public IntegerJavaType() {
		super( Integer.class );
	}

	@Override
	public boolean useObjectEqualsHashCode() {
		return true;
	}

	@Override
	public String toString(Integer value) {
		return value == null ? null : value.toString();
	}

	@Override
	public Integer fromString(CharSequence string) {
		return string == null ? null : Integer.valueOf( string.toString() );
	}

	@Override
	public boolean isInstance(Object value) {
		return value instanceof Integer;
	}

	@Override
	public Integer cast(Object value) {
		return (Integer) value;
	}

	@Override
	public <X> X unwrap(Integer value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( Integer.class.isAssignableFrom( type ) || type == Object.class ) {
			return type.cast( value );
		}
		if ( Byte.class.isAssignableFrom( type ) ) {
			return type.cast( value.byteValue() );
		}
		if ( Short.class.isAssignableFrom( type ) ) {
			return type.cast( value.shortValue() );
		}
		if ( Long.class.isAssignableFrom( type ) ) {
			return type.cast( value.longValue() );
		}
		if ( Double.class.isAssignableFrom( type ) ) {
			return type.cast( value.doubleValue() );
		}
		if ( Float.class.isAssignableFrom( type ) ) {
			return type.cast( value.floatValue() );
		}
		if ( BigInteger.class.isAssignableFrom( type ) ) {
			return type.cast( BigInteger.valueOf( value ) );
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
	public <X> Integer wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if (value instanceof Integer integer) {
			return integer;
		}
		if (value instanceof Number number) {
			return number.intValue();
		}
		if (value instanceof String string) {
			return Integer.valueOf( string );
		}
		throw unknownWrap( value.getClass() );
	}

	@Override
	public boolean isWider(JavaType<?> javaType) {
		return switch ( javaType.getTypeName() ) {
			case
				"byte", "java.lang.Byte",
				"short", "java.lang.Short" -> true;
			default -> false;
		};
	}

	@Override
	public Class<?> getPrimitiveClass() {
		return int.class;
	}

	@Override
	public Class<Integer[]> getArrayClass() {
		return Integer[].class;
	}

	@Override
	public Class<?> getPrimitiveArrayClass() {
		return int[].class;
	}

	@Override
	public Integer getDefaultValue() {
		return 0;
	}

	@Override
	public long getDefaultSqlLength(Dialect dialect, JdbcType jdbcType) {
		return getDefaultSqlPrecision( dialect, jdbcType )+1;
	}

	@Override
	public int getDefaultSqlPrecision(Dialect dialect, JdbcType jdbcType) {
		return 10;
	}

	@Override
	public int getDefaultSqlScale(Dialect dialect, JdbcType jdbcType) {
		return 0;
	}

	@Override
	public Integer coerce(Object value) {
		if ( value == null ) {
			return null;
		}

		if ( value instanceof Integer integer ) {
			return integer;
		}

		if ( value instanceof Short shortValue ) {
			return CoercionHelper.toInteger( shortValue );
		}

		if ( value instanceof Byte byteValue ) {
			return CoercionHelper.toInteger( byteValue );
		}

		if ( value instanceof Long longValue ) {
			return CoercionHelper.toInteger( longValue );
		}

		if ( value instanceof Double doubleValue ) {
			return CoercionHelper.toInteger( doubleValue );
		}

		if ( value instanceof Float floatValue ) {
			return CoercionHelper.toInteger( floatValue );
		}

		if ( value instanceof BigInteger bigInteger ) {
			return CoercionHelper.toInteger( bigInteger );
		}

		if ( value instanceof BigDecimal bigDecimal ) {
			return CoercionHelper.toInteger( bigDecimal );
		}

		if ( value instanceof String string ) {
			return CoercionHelper.coerceWrappingError(
					() -> Integer.parseInt( string )
			);
		}

		throw new CoercionException(
				String.format(
						Locale.ROOT,
						"Cannot coerce value '%s' [%s] to Integer",
						value,
						value.getClass().getName()
				)
		);
	}

	@Override
	public Integer seed(Long length, Integer precision, Integer scale, SharedSessionContractImplementor session) {
		return ZERO;
	}

	@Override
	public Integer next(
			Integer current,
			Long length,
			Integer precision,
			Integer scale,
			SharedSessionContractImplementor session) {
		return current + 1;
	}

}
