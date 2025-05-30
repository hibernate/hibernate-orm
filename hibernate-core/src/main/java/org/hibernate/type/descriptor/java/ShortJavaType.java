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
 * Descriptor for {@link Short} handling.
 *
 * @author Steve Ebersole
 */
public class ShortJavaType extends AbstractClassJavaType<Short>
		implements PrimitiveJavaType<Short>, VersionJavaType<Short> {

	private static final Short ZERO = (short) 0;
	public static final ShortJavaType INSTANCE = new ShortJavaType();

	public ShortJavaType() {
		super( Short.class );
	}

	@Override
	public boolean useObjectEqualsHashCode() {
		return true;
	}

	@Override
	public String toString(Short value) {
		return value == null ? null : value.toString();
	}

	@Override
	public Short fromString(CharSequence string) {
		return Short.valueOf( string.toString() );
	}

	@Override
	public boolean isInstance(Object value) {
		return value instanceof Short;
	}

	@Override
	public boolean isWider(JavaType<?> javaType) {
		return switch ( javaType.getTypeName() ) {
			case "byte", "java.lang.Byte" -> true;
			default -> false;
		};
	}

	@SuppressWarnings("unchecked")
	@Override
	public <X> X unwrap(Short value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( Short.class.isAssignableFrom( type ) || type == Object.class ) {
			return (X) value;
		}
		if ( Byte.class.isAssignableFrom( type ) ) {
			return (X) Byte.valueOf( value.byteValue() );
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
	public <X> Short wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if (value instanceof Short shortValue) {
			return shortValue;
		}
		if (value instanceof Number number) {
			return number.shortValue();
		}
		if (value instanceof String string) {
			return Short.valueOf( string );
		}
		throw unknownWrap( value.getClass() );
	}

	@Override
	public Class<Short> getPrimitiveClass() {
		return short.class;
	}

	@Override
	public Class<Short[]> getArrayClass() {
		return Short[].class;
	}

	@Override
	public Class<?> getPrimitiveArrayClass() {
		return short[].class;
	}

	@Override
	public Short getDefaultValue() {
		return 0;
	}

	@Override
	public long getDefaultSqlLength(Dialect dialect, JdbcType jdbcType) {
		return getDefaultSqlPrecision( dialect, jdbcType )+1;
	}

	@Override
	public int getDefaultSqlPrecision(Dialect dialect, JdbcType jdbcType) {
		return 5;
	}

	@Override
	public int getDefaultSqlScale(Dialect dialect, JdbcType jdbcType) {
		return 0;
	}

	@Override
	public Short coerce(Object value, CoercionContext coercionContext) {
		if ( value == null ) {
			return null;
		}

		if ( value instanceof Short shortValue ) {
			return shortValue;
		}

		if ( value instanceof Byte byteValue ) {
			return CoercionHelper.toShort( byteValue );
		}

		if ( value instanceof Integer integerValue ) {
			return CoercionHelper.toShort( integerValue );
		}

		if ( value instanceof Long longValue ) {
			return CoercionHelper.toShort( longValue );
		}

		if ( value instanceof Double doubleValue ) {
			return CoercionHelper.toShort( doubleValue );
		}

		if ( value instanceof Float floatValue ) {
			return CoercionHelper.toShort( floatValue );
		}

		if ( value instanceof BigInteger bigInteger ) {
			return CoercionHelper.toShort( bigInteger );
		}

		if ( value instanceof BigDecimal bigDecimal ) {
			return CoercionHelper.toShort( bigDecimal );
		}

		if ( value instanceof String string ) {
			return CoercionHelper.coerceWrappingError(
					() -> Short.parseShort( string )
			);
		}

		throw new CoercionException(
				String.format(
						Locale.ROOT,
						"Cannot coerce value '%s' [%s] to Short",
						value,
						value.getClass().getName()
				)
		);
	}
	@Override
	public Short seed(
			Long length,
			Integer precision,
			Integer scale,
			SharedSessionContractImplementor session) {
		return ZERO;
	}

	@Override
	public Short next(
			Short current,
			Long length,
			Integer precision,
			Integer scale,
			SharedSessionContractImplementor session) {
		return (short) ( current + 1 );
	}

}
