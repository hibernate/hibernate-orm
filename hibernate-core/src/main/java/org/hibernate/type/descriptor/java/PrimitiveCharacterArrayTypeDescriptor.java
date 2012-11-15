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

import java.io.Reader;
import java.io.StringReader;
import java.sql.Clob;
import java.util.Arrays;

import org.hibernate.engine.jdbc.CharacterStream;
import org.hibernate.engine.jdbc.internal.CharacterStreamImpl;
import org.hibernate.type.descriptor.WrapperOptions;

/**
 * Descriptor for {@code char[]} handling.
 *
 * @author Steve Ebersole
 */
public class PrimitiveCharacterArrayTypeDescriptor extends AbstractTypeDescriptor<char[]> {
	public static final PrimitiveCharacterArrayTypeDescriptor INSTANCE = new PrimitiveCharacterArrayTypeDescriptor();

	@SuppressWarnings({ "unchecked" })
	protected PrimitiveCharacterArrayTypeDescriptor() {
		super( char[].class, ArrayMutabilityPlan.INSTANCE );
	}

	public String toString(char[] value) {
		return new String( value );
	}

	public char[] fromString(String string) {
		return string.toCharArray();
	}

	@Override
	public boolean areEqual(char[] one, char[] another) {
		return one == another
				|| ( one != null && another != null && Arrays.equals( one, another ) );
	}

	@Override
	public int extractHashCode(char[] chars) {
		int hashCode = 1;
		for ( char aChar : chars ) {
			hashCode = 31 * hashCode + aChar;
		}
		return hashCode;
	}

	@SuppressWarnings({ "unchecked" })
	public <X> X unwrap(char[] value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( char[].class.isAssignableFrom( type ) ) {
			return (X) value;
		}
		if ( String.class.isAssignableFrom( type ) ) {
			return (X) new String( value );
		}
		if ( Clob.class.isAssignableFrom( type ) ) {
			return (X) options.getLobCreator().createClob( new String( value ) );
		}
		if ( Reader.class.isAssignableFrom( type ) ) {
			return (X) new StringReader( new String( value ) );
		}
		if ( CharacterStream.class.isAssignableFrom( type ) ) {
			return (X) new CharacterStreamImpl( new String( value ) );
		}
		throw unknownUnwrap( type );
	}

	public <X> char[] wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( char[].class.isInstance( value ) ) {
			return (char[]) value;
		}
		if ( String.class.isInstance( value ) ) {
			return ( (String) value ).toCharArray();
		}
		if ( Clob.class.isInstance( value ) ) {
			return DataHelper.extractString( ( (Clob) value ) ).toCharArray();
		}
		if ( Reader.class.isInstance( value ) ) {
			return DataHelper.extractString( ( (Reader) value ) ).toCharArray();
		}
		throw unknownWrap( value.getClass() );
	}
}
