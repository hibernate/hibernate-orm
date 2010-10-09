/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.type.descriptor.java;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.hibernate.type.descriptor.WrapperOptions;

/**
 * Descriptor for {@link Integer} handling.
 *
 * @author Steve Ebersole
 */
public class IntegerTypeDescriptor extends AbstractTypeDescriptor<Integer> {
	public static final IntegerTypeDescriptor INSTANCE = new IntegerTypeDescriptor();

	public IntegerTypeDescriptor() {
		super( Integer.class );
	}

	public String toString(Integer value) {
		return value == null ? null : value.toString();
	}

	public Integer fromString(String string) {
		return string == null ? null : Integer.valueOf( string );
	}

	@SuppressWarnings({ "unchecked" })
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

	@SuppressWarnings({ "UnnecessaryBoxing" })
	public <X> Integer wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( Integer.class.isInstance( value ) ) {
			return (Integer) value;
		}
		if ( Number.class.isInstance( value ) ) {
			return Integer.valueOf( ( (Number) value ).intValue() );
		}
		if ( String.class.isInstance( value ) ) {
			return Integer.valueOf( ( (String) value ) );
		}
		throw unknownWrap( value.getClass() );
	}
}
