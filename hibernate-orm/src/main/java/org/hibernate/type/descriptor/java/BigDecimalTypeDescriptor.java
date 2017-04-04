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
 * Descriptor for {@link BigDecimal} handling.
 *
 * @author Steve Ebersole
 */
public class BigDecimalTypeDescriptor extends AbstractTypeDescriptor<BigDecimal> {
	public static final BigDecimalTypeDescriptor INSTANCE = new BigDecimalTypeDescriptor();

	public BigDecimalTypeDescriptor() {
		super( BigDecimal.class );
	}

	public String toString(BigDecimal value) {
		return value.toString();
	}

	public BigDecimal fromString(String string) {
		return new BigDecimal( string );
	}

	@Override
	public boolean areEqual(BigDecimal one, BigDecimal another) {
		return one == another || ( one != null && another != null && one.compareTo( another ) == 0 );
	}

	@Override
	public int extractHashCode(BigDecimal value) {
		return value.intValue();
	}

	@SuppressWarnings({ "unchecked" })
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
		throw unknownUnwrap( type );
	}

	public <X> BigDecimal wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( BigDecimal.class.isInstance( value ) ) {
			return (BigDecimal) value;
		}
		if ( BigInteger.class.isInstance( value ) ) {
			return new BigDecimal( (BigInteger) value );
		}
		if ( Number.class.isInstance( value ) ) {
			return BigDecimal.valueOf( ( (Number) value ).doubleValue() );
		}
		throw unknownWrap( value.getClass() );
	}
}
