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
 * Descriptor for {@link Byte} handling.
 *
 * @author Steve Ebersole
 * @author Lukasz Antoniak
 */
public class ByteJavaType extends AbstractClassJavaType<Byte>
		implements PrimitiveJavaType<Byte>, VersionJavaType<Byte> {

	private static final Byte ZERO = (byte) 0;
	public static final ByteJavaType INSTANCE = new ByteJavaType();

	public ByteJavaType() {
		super( Byte.class );
	}

	@Override
	public boolean useObjectEqualsHashCode() {
		return true;
	}

	@Override
	public String toString(Byte value) {
		return value == null ? null : value.toString();
	}

	@Override
	public Byte fromString(CharSequence string) {
		return Byte.valueOf( string.toString() );
	}

	@Override
	public boolean isInstance(Object value) {
		return value instanceof Byte;
	}

	@Override
	public Byte cast(Object value) {
		return (Byte) value;
	}

	@Override
	public <X> X unwrap(Byte value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( Byte.class.isAssignableFrom( type ) || type == Object.class ) {
			return type.cast( value );
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
		if ( Double.class.isAssignableFrom( type ) ) {
			return type.cast( value.doubleValue() );
		}
		if ( Float.class.isAssignableFrom( type ) ) {
			return type.cast( value.floatValue() );
		}
		if ( String.class.isAssignableFrom( type ) ) {
			return type.cast( value.toString() );
		}
		throw unknownUnwrap( type );
	}

	@Override
	public <X> Byte wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( value instanceof Byte byteValue ) {
			return byteValue;
		}
		if ( value instanceof Number number ) {
			return number.byteValue();
		}
		if ( value instanceof String string ) {
			return Byte.valueOf( string );
		}
		throw unknownWrap( value.getClass() );
	}

	@Override
	public Class<Byte> getPrimitiveClass() {
		return byte.class;
	}

	@Override
	public Class<Byte[]> getArrayClass() {
		return Byte[].class;
	}

	@Override
	public Class<?> getPrimitiveArrayClass() {
		return byte[].class;
	}

	@Override
	public Byte getDefaultValue() {
		return 0;
	}

	@Override
	public long getDefaultSqlLength(Dialect dialect, JdbcType jdbcType) {
		return 1;
	}

	@Override
	public int getDefaultSqlPrecision(Dialect dialect, JdbcType jdbcType) {
		return 3;
	}

	@Override
	public int getDefaultSqlScale(Dialect dialect, JdbcType jdbcType) {
		return 0;
	}

	@Override
	public Byte coerce(Object value) {
		if ( value == null ) {
			return null;
		}

		if ( value instanceof Byte byteValue ) {
			return byteValue;
		}

		if ( value instanceof Short shotValue ) {
			return CoercionHelper.toByte( shotValue );
		}

		if ( value instanceof Integer integerValue ) {
			return CoercionHelper.toByte( integerValue );
		}

		if ( value instanceof Long longValue ) {
			return CoercionHelper.toByte( longValue );
		}

		if ( value instanceof Double doubleValue ) {
			return CoercionHelper.toByte( doubleValue );
		}

		if ( value instanceof Float floatValue ) {
			return CoercionHelper.toByte( floatValue );
		}

		if ( value instanceof BigInteger bigInteger ) {
			return CoercionHelper.toByte( bigInteger );
		}

		if ( value instanceof BigDecimal bigDecimal ) {
			return CoercionHelper.toByte( bigDecimal );
		}

		if ( value instanceof String string ) {
			return CoercionHelper.coerceWrappingError(
					() -> Byte.parseByte( string )
			);
		}

		throw new CoercionException(
				String.format(
						Locale.ROOT,
						"Cannot coerce value '%s' [%s] to Byte",
						value,
						value.getClass().getName()
				)
		);
	}

	@Override
	public Byte next(
			Byte current,
			Long length,
			Integer precision,
			Integer scale,
			SharedSessionContractImplementor session) {
		return (byte) ( current + 1 );
	}

	@Override
	public Byte seed(
			Long length,
			Integer precision,
			Integer scale,
			SharedSessionContractImplementor session) {
		return ZERO;
	}

}
