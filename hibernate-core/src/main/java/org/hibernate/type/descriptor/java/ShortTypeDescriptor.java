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
import org.hibernate.type.descriptor.WrapperOptions;

/**
 * Descriptor for {@link Short} handling.
 *
 * @author Steve Ebersole
 */
public class ShortTypeDescriptor extends AbstractTypeDescriptor<Short> {
	public static final ShortTypeDescriptor INSTANCE = new ShortTypeDescriptor();

	public ShortTypeDescriptor() {
		super( Short.class );
	}

	public String toString(Short value) {
		return value == null ? null : value.toString();
	}

	public Short fromString(String string) {
		return Short.valueOf( string );
	}

	@SuppressWarnings({ "unchecked" })
	public <X> X unwrap(Short value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( Short.class.isAssignableFrom( type ) ) {
			return (X) value;
		}
		if ( Byte.class.isAssignableFrom( type ) ) {
			return (X) Byte.valueOf( value.byteValue() );
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

	@SuppressWarnings({ "UnnecessaryBoxing" })
	public <X> Short wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( Short.class.isInstance( value ) ) {
			return (Short) value;
		}
		if ( Number.class.isInstance( value ) ) {
			return Short.valueOf( ( (Number) value ).shortValue() );
		}
		if ( String.class.isInstance( value ) ) {
			return Short.valueOf( ( (String) value ) );
		}
		throw unknownWrap( value.getClass() );
	}
}
