/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.hibernate.type.descriptor.WrapperOptions;

/**
 * Descriptor for {@link Float} handling.
 *
 * @author Steve Ebersole
 */
public class FloatTypeDescriptor extends AbstractTypeDescriptor<Float> {
	public static final FloatTypeDescriptor INSTANCE = new FloatTypeDescriptor();

	public FloatTypeDescriptor() {
		super( Float.class );
	}
	@Override
	public String toString(Float value) {
		return value == null ? null : value.toString();
	}
	@Override
	public Float fromString(String string) {
		return Float.valueOf( string );
	}

	@SuppressWarnings({ "unchecked" })
	@Override
	public <X> X unwrap(Float value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( Float.class.isAssignableFrom( type ) ) {
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
		if ( Long.class.isAssignableFrom( type ) ) {
			return (X) Long.valueOf( value.longValue() );
		}
		if ( Double.class.isAssignableFrom( type ) ) {
			return (X) Double.valueOf( value.doubleValue() );
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
	public <X> Float wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( Float.class.isInstance( value ) ) {
			return (Float) value;
		}
		if ( Number.class.isInstance( value ) ) {
			return ( (Number) value ).floatValue();
		}
		else if ( String.class.isInstance( value ) ) {
			return Float.valueOf( ( (String) value ) );
		}
		throw unknownWrap( value.getClass() );
	}
}
