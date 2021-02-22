/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.spi.Primitive;

/**
 * Descriptor for {@link Integer} handling.
 *
 * @author Steve Ebersole
 */
public class IntegerTypeDescriptor extends AbstractClassTypeDescriptor<Integer> implements Primitive<Integer> {
	public static final IntegerTypeDescriptor INSTANCE = new IntegerTypeDescriptor();

	public IntegerTypeDescriptor() {
		super( Integer.class );
	}
	@Override
	public String toString(Integer value) {
		return value == null ? null : value.toString();
	}
	@Override
	public Integer fromString(String string) {
		return string == null ? null : Integer.valueOf( string );
	}

	@SuppressWarnings({ "unchecked" })
	@Override
	public <X> X unwrap(Integer value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( Integer.class.isAssignableFrom( type ) ) {
			return (X) value;
		}
		if ( Byte.class.isAssignableFrom( type ) ) {
			return (X) Byte.valueOf( value.byteValue() );
		}
		if ( Short.class.isAssignableFrom( type ) ) {
			return (X) Short.valueOf( value.shortValue() );
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
	public <X> Integer wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( Integer.class.isInstance( value ) ) {
			return (Integer) value;
		}
		if ( Number.class.isInstance( value ) ) {
			return ( (Number) value ).intValue();
		}
		if ( String.class.isInstance( value ) ) {
			return Integer.valueOf( ( (String) value ) );
		}
		throw unknownWrap( value.getClass() );
	}

	@Override
	public Class getPrimitiveClass() {
		return int.class;
	}

	@Override
	public Integer getDefaultValue() {
		return 0;
	}

	@Override
	public long getDefaultSqlLength(Dialect dialect) {
		return getDefaultSqlPrecision(dialect)+1;
	}

	@Override
	public int getDefaultSqlPrecision(Dialect dialect) {
		return 10;
	}

	@Override
	public int getDefaultSqlScale() {
		return 0;
	}
}
