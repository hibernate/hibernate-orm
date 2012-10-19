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
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class CharacterArrayTypeDescriptor extends AbstractTypeDescriptor<Character[]> {
	public static final CharacterArrayTypeDescriptor INSTANCE = new CharacterArrayTypeDescriptor();

	@SuppressWarnings({ "unchecked" })
	public CharacterArrayTypeDescriptor() {
		super( Character[].class, ArrayMutabilityPlan.INSTANCE );
	}

	public String toString(Character[] value) {
		return new String( unwrapChars( value ) );
	}

	public Character[] fromString(String string) {
		return wrapChars( string.toCharArray() );
	}

	@Override
	public boolean areEqual(Character[] one, Character[] another) {
		return one == another
				|| ( one != null && another != null && Arrays.equals( one, another ) );
	}

	@Override
	public int extractHashCode(Character[] chars) {
		int hashCode = 1;
		for ( Character aChar : chars ) {
			hashCode = 31 * hashCode + aChar;
		}
		return hashCode;
	}

	@SuppressWarnings({ "unchecked" })
	public <X> X unwrap(Character[] value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( Character[].class.isAssignableFrom( type ) ) {
			return (X) value;
		}
		if ( String.class.isAssignableFrom( type ) ) {
			return (X) new String( unwrapChars( value ) );
		}
		if ( Clob.class.isAssignableFrom( type ) ) {
			return (X) options.getLobCreator().createClob( new String( unwrapChars( value ) ) );
		}
		if ( Reader.class.isAssignableFrom( type ) ) {
			return (X) new StringReader( new String( unwrapChars( value ) ) );
		}
		if ( CharacterStream.class.isAssignableFrom( type ) ) {
			return (X) new CharacterStreamImpl( new String( unwrapChars( value ) ) );
		}
		throw unknownUnwrap( type );
	}

	public <X> Character[] wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( Character[].class.isInstance( value ) ) {
			return (Character[]) value;
		}
		if ( String.class.isInstance( value ) ) {
			return wrapChars( ( (String) value ).toCharArray() );
		}
		if ( Clob.class.isInstance( value ) ) {
			return wrapChars( DataHelper.extractString( ( (Clob) value ) ).toCharArray() );
		}
		if ( Reader.class.isInstance( value ) ) {
			return wrapChars( DataHelper.extractString( (Reader) value ).toCharArray() );
		}
		throw unknownWrap( value.getClass() );
	}

	@SuppressWarnings({ "UnnecessaryBoxing" })
	private Character[] wrapChars(char[] chars) {
		if ( chars == null ) {
			return null;
		}
		final Character[] result = new Character[chars.length];
		for ( int i = 0; i < chars.length; i++ ) {
			result[i] = Character.valueOf( chars[i] );
		}
		return result;
	}

	@SuppressWarnings({ "UnnecessaryUnboxing" })
	private char[] unwrapChars(Character[] chars) {
		if ( chars == null ) {
			return null;
		}
		final char[] result = new char[chars.length];
		for ( int i = 0; i < chars.length; i++ ) {
			result[i] = chars[i].charValue();
		}
		return result;
	}
}
