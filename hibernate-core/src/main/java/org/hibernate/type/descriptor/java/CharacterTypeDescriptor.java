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
import org.hibernate.HibernateException;
import org.hibernate.type.descriptor.WrapperOptions;

/**
 * Descriptor for {@link Character} handling.
 *
 * @author Steve Ebersole
 */
public class CharacterTypeDescriptor extends AbstractTypeDescriptor<Character> {
	public static final CharacterTypeDescriptor INSTANCE = new CharacterTypeDescriptor();

	public CharacterTypeDescriptor() {
		super( Character.class );
	}
	@Override
	public String toString(Character value) {
		return value.toString();
	}
	@Override
	public Character fromString(String string) {
		if ( string.length() != 1 ) {
			throw new HibernateException( "multiple or zero characters found parsing string" );
		}
		return string.charAt( 0 );
	}

	@SuppressWarnings({ "unchecked" })
	@Override
	public <X> X unwrap(Character value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( Character.class.isAssignableFrom( type ) ) {
			return (X) value;
		}
		if ( String.class.isAssignableFrom( type ) ) {
			return (X) value.toString();
		}
		if ( Number.class.isAssignableFrom( type ) ) {
			return (X) Short.valueOf( (short)value.charValue() );
		}
		throw unknownUnwrap( type );
	}
	@Override
	public <X> Character wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( Character.class.isInstance( value ) ) {
			return (Character) value;
		}
		if ( String.class.isInstance( value ) ) {
			final String str = (String) value;
			return str.charAt( 0 );
		}
		if ( Number.class.isInstance( value ) ) {
			final Number nbr = (Number) value;
			return (char) nbr.shortValue();
		}
		throw unknownWrap( value.getClass() );
	}
}
