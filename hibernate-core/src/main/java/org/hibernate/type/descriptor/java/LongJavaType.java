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
 * Descriptor for {@link Long} handling.
 *
 * @author Steve Ebersole
 */
public class LongJavaType extends AbstractClassJavaType<Long>
		implements PrimitiveJavaType<Long>, VersionJavaType<Long> {

	private static final Long ZERO = (long) 0;
	public static final LongJavaType INSTANCE = new LongJavaType();

	public LongJavaType() {
		super( Long.class );
	}

	@Override
	public String toString(Long value) {
		return value == null ? null : value.toString();
	}

	@Override
	public Long fromString(CharSequence string) {
		return Long.valueOf( string.toString() );
	}

	@SuppressWarnings("unchecked")
	@Override
	public <X> X unwrap(Long value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( Long.class.isAssignableFrom( type ) || type == Object.class ) {
			return (X) value;
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
		if ( Double.class.isAssignableFrom( type ) ) {
			return (X) Double.valueOf( value.doubleValue() );
		}
		if ( Float.class.isAssignableFrom( type ) ) {
			return (X) Float.valueOf( value.floatValue() );
		}
		if ( BigInteger.class.isAssignableFrom( type ) ) {
			return (X) BigInteger.valueOf( value );
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
	public <X> Long wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( value instanceof Long ) {
			return (Long) value;
		}
		if ( value instanceof Number ) {
			return ( (Number) value ).longValue();
		}
		else if ( value instanceof String ) {
			return Long.valueOf( ( (String) value ) );
		}
		throw unknownWrap( value.getClass() );
	}

	@Override
	public boolean isWider(JavaType<?> javaType) {
		switch ( javaType.getTypeName() ) {
			case "byte":
			case "java.lang.Byte":
			case "short":
			case "java.lang.Short":
			case "int":
			case "java.lang.Integer":
				return true;
			default:
				return false;
		}
	}

	@Override
	public <X> Long coerce(X value, CoercionContext coercionContext) {
		if ( value == null ) {
			return null;
		}

		if ( value instanceof Long ) {
			return ( (Long) value );
		}

		if ( value instanceof Byte ) {
			return CoercionHelper.toLong( (Byte) value );
		}

		if ( value instanceof Short ) {
			return CoercionHelper.toLong( (Short) value );
		}

		if ( value instanceof Integer ) {
			return CoercionHelper.toLong( (Integer) value );
		}

		if ( value instanceof Double ) {
			return CoercionHelper.toLong( (Double) value );
		}

		if ( value instanceof Float ) {
			return CoercionHelper.toLong( (Float) value );
		}

		if ( value instanceof BigInteger ) {
			return CoercionHelper.toLong( (BigInteger) value );
		}

		if ( value instanceof BigDecimal ) {
			return CoercionHelper.toLong( (BigDecimal) value );
		}

		if ( value instanceof String ) {
			return CoercionHelper.coerceWrappingError(
					() -> Long.parseLong( (String) value )
			);
		}

		throw new CoercionException(
				String.format(
						Locale.ROOT,
						"Cannot coerce value '%s' [%s] to Long",
						value,
						value.getClass().getName()
				)
		);
	}

	@Override
	public Class<?> getPrimitiveClass() {
		return long.class;
	}

	@Override
	public Class<Long[]> getArrayClass() {
		return Long[].class;
	}

	@Override
	public Class<?> getPrimitiveArrayClass() {
		return long[].class;
	}

	@Override
	public Long getDefaultValue() {
		return 0L;
	}

	@Override
	public long getDefaultSqlLength(Dialect dialect, JdbcType jdbcType) {
		return getDefaultSqlPrecision( dialect, jdbcType )+1;
	}

	@Override
	public int getDefaultSqlPrecision(Dialect dialect, JdbcType jdbcType) {
		return 19;
	}

	@Override
	public int getDefaultSqlScale(Dialect dialect, JdbcType jdbcType) {
		return 0;
	}

	@Override
	public Long next(
			Long current,
			Long length,
			Integer precision,
			Integer scale,
			SharedSessionContractImplementor session) {
		return current + 1L;
	}

	@Override
	public Long seed(
			Long length,
			Integer precision,
			Integer scale,
			SharedSessionContractImplementor session) {
		return ZERO;
	}

}
