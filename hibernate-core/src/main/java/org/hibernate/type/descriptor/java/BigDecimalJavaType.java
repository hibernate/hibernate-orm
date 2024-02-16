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
	public boolean areEqual(BigDecimal one, BigDecimal another) {
		return one == another
			|| one != null && another != null && one.compareTo( another ) == 0;
	}

	@Override
	public int extractHashCode(BigDecimal value) {
		return value.intValue();
	}

	@SuppressWarnings("unchecked")
	public <X> X unwrap(BigDecimal value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( BigDecimal.class.isAssignableFrom( type ) ) {
			return (X) value;
		}
		if ( BigInteger.class.isAssignableFrom( type ) ) {
			return (X) value.toBigIntegerExact();
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

	public <X> BigDecimal wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( value instanceof BigDecimal ) {
			return (BigDecimal) value;
		}
		if ( value instanceof BigInteger ) {
			return new BigDecimal( (BigInteger) value );
		}
		if ( value instanceof Number ) {
			return BigDecimal.valueOf( ( (Number) value ).doubleValue() );
		}
		if ( value instanceof String ) {
			return new BigDecimal( (String) value );
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
			case "long":
			case "java.lang.Long":
			case "java.math.BigInteger":
				return true;
			default:
				return false;
		}
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
	public <X> BigDecimal coerce(X value, CoercionContext coercionContext) {
		if ( value == null ) {
			return null;
		}

		if ( value instanceof BigDecimal ) {
			return (BigDecimal) value;
		}

		if ( value instanceof Number ) {
			return BigDecimal.valueOf( ( (Number) value ).doubleValue() );
		}

		if ( value instanceof String ) {
			return CoercionHelper.coerceWrappingError(
					() -> BigDecimal.valueOf( Double.parseDouble( (String) value ) )
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
