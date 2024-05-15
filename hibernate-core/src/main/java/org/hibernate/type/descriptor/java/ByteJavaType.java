/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
	public String toString(Byte value) {
		return value == null ? null : value.toString();
	}

	@Override
	public Byte fromString(CharSequence string) {
		return Byte.valueOf( string.toString() );
	}

	@SuppressWarnings("unchecked")
	@Override
	public <X> X unwrap(Byte value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( Byte.class.isAssignableFrom( type ) || type == Object.class ) {
			return (X) value;
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
	public <X> Byte wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( value instanceof Byte ) {
			return (Byte) value;
		}
		if ( value instanceof Number ) {
			return ( (Number) value ).byteValue();
		}
		if ( value instanceof String ) {
			return Byte.valueOf( ( (String) value ) );
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
	public <X> Byte coerce(X value, CoercionContext coercionContext) {
		if ( value == null ) {
			return null;
		}

		if ( value instanceof Byte ) {
			return (Byte) value;
		}

		if ( value instanceof Short ) {
			return CoercionHelper.toByte( (Short) value );
		}

		if ( value instanceof Integer ) {
			return CoercionHelper.toByte( (Integer) value );
		}

		if ( value instanceof Long ) {
			return CoercionHelper.toByte( (Long) value );
		}

		if ( value instanceof Double ) {
			return CoercionHelper.toByte( (Double) value );
		}

		if ( value instanceof Float ) {
			return CoercionHelper.toByte( (Float) value );
		}

		if ( value instanceof BigInteger ) {
			return CoercionHelper.toByte( (BigInteger) value );
		}

		if ( value instanceof BigDecimal ) {
			return CoercionHelper.toByte( (BigDecimal) value );
		}

		if ( value instanceof String ) {
			return CoercionHelper.coerceWrappingError(
					() -> Byte.parseByte( (String) value )
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
