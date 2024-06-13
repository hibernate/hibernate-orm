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
	public String toString(Double value) {
		return value == null ? null : value.toString();
	}

	@Override
	public Double fromString(CharSequence string) {
		return Double.valueOf( string.toString() );
	}

	@SuppressWarnings("unchecked")
	@Override
	public <X> X unwrap(Double value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( Double.class.isAssignableFrom( type ) || type == Object.class ) {
			return (X) value;
		}
		if ( Float.class.isAssignableFrom( type ) ) {
			return (X) Float.valueOf( value.floatValue() );
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
	public <X> Double wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( value instanceof Double ) {
			return (Double) value;
		}
		if ( value instanceof Number ) {
			return ( (Number) value ).doubleValue();
		}
		else if ( value instanceof String ) {
			return Double.valueOf( ( (String) value ) );
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
			case "float":
			case "java.lang.Float":
			case "java.math.BigInteger":
			case "java.math.BigDecimal":
				return true;
			default:
				return false;
		}
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
		//this is the number of *binary* digits
		//in a double-precision FP number
		return dialect.getDoublePrecision();
	}


	@Override
	public <X> Double coerce(X value, CoercionContext coercionContext) {
		if ( value == null ) {
			return null;
		}

		if ( value instanceof Double ) {
			return ( (Double) value );
		}

		if ( value instanceof Byte ) {
			return ( (Byte) value ).doubleValue();
		}

		if ( value instanceof Short ) {
			return ( (Short) value ).doubleValue();
		}

		if ( value instanceof Integer ) {
			return ( (Integer) value ).doubleValue();
		}

		if ( value instanceof Long ) {
			return ( (Long) value ).doubleValue();
		}

		if ( value instanceof Float ) {
			return CoercionHelper.toDouble( (Float) value );
		}

		if ( value instanceof BigInteger ) {
			return CoercionHelper.toDouble( (BigInteger) value );
		}

		if ( value instanceof BigDecimal ) {
			return CoercionHelper.toDouble( (BigDecimal) value );
		}

		if ( value instanceof String ) {
			return CoercionHelper.coerceWrappingError(
					() -> Double.parseDouble( (String) value )
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
