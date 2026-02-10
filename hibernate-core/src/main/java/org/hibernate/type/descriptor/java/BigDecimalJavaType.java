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
 * Descriptor for {@link BigDecimal} handling.
 *
 * @author Steve Ebersole
 */
public class BigDecimalJavaType extends AbstractClassJavaType<BigDecimal> {
	public static final BigDecimalJavaType INSTANCE = new BigDecimalJavaType();

	public BigDecimalJavaType() {
		super( BigDecimal.class );
	}

	public String toString(BigDecimal value) {
		return value.toString();
	}

	public BigDecimal fromString(CharSequence string) {
		return new BigDecimal( string.toString() );
	}

	@Override
	public boolean isInstance(Object value) {
		return value instanceof BigDecimal;
	}

	@Override
	public BigDecimal cast(Object value) {
		return (BigDecimal) value;
	}

	@Override
	public boolean areEqual(BigDecimal one, BigDecimal another) {
		return one == another
			|| one != null && another != null && one.compareTo( another ) == 0;
	}

	@Override
	public int extractHashCode(BigDecimal value) {
		return value.intValue();
	}

	public <X> X unwrap(BigDecimal value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( BigDecimal.class.isAssignableFrom( type ) ) {
			return type.cast( value );
		}
		if ( BigInteger.class.isAssignableFrom( type ) ) {
			return type.cast( value.toBigIntegerExact() );
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

	public <X> BigDecimal wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( value instanceof BigDecimal bigDecimal ) {
			return bigDecimal;
		}
		if ( value instanceof BigInteger bigInteger ) {
			return new BigDecimal( bigInteger );
		}
		if ( value instanceof Number number ) {
			return BigDecimal.valueOf( number.doubleValue() );
		}
		if ( value instanceof String string ) {
			return new BigDecimal( string );
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
				"java.math.BigInteger" -> true;
			default -> false;
		};
	}

	@Override
	public long getDefaultSqlLength(Dialect dialect, JdbcType jdbcType) {
		return getDefaultSqlPrecision( dialect, jdbcType ) + 2;
	}

	@Override
	public int getDefaultSqlPrecision(Dialect dialect, JdbcType jdbcType) {
		return dialect.getDefaultDecimalPrecision();
	}

	@Override
	public BigDecimal coerce(Object value) {
		if ( value == null ) {
			return null;
		}

		if ( value instanceof BigDecimal bigDecimal ) {
			return bigDecimal;
		}

		if ( value instanceof Number number ) {
			return BigDecimal.valueOf( number.doubleValue() );
		}

		if ( value instanceof String string ) {
			return CoercionHelper.coerceWrappingError(
					() -> BigDecimal.valueOf( Double.parseDouble( string ) )
			);
		}

		throw new CoercionException(
				String.format(
						Locale.ROOT,
						"Unable to coerce value [%s (%s)] to BigDecimal",
						value,
						value.getClass().getName()
				)
		);
	}
}
