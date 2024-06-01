/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java;

import java.io.Reader;
import java.io.StringReader;
import java.sql.Clob;
import java.sql.NClob;
import java.util.Arrays;

import org.hibernate.engine.jdbc.CharacterStream;
import org.hibernate.engine.jdbc.internal.CharacterStreamImpl;
import org.hibernate.type.descriptor.WrapperOptions;

/**
 * Descriptor for {@code char[]} handling.
 *
 * @author Steve Ebersole
 */
public class PrimitiveCharacterArrayJavaType extends AbstractClassJavaType<char[]> {
	public static final PrimitiveCharacterArrayJavaType INSTANCE = new PrimitiveCharacterArrayJavaType();

	@SuppressWarnings("unchecked")
	protected PrimitiveCharacterArrayJavaType() {
		super( char[].class, ArrayMutabilityPlan.INSTANCE, IncomparableComparator.INSTANCE );
	}

	public String toString(char[] value) {
		return new String( value );
	}

	public char[] fromString(CharSequence string) {
		return string.toString().toCharArray();
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

	@SuppressWarnings("unchecked")
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
		if ( NClob.class.isAssignableFrom( type ) ) {
			return (X) options.getLobCreator().createNClob( new String( value ) );
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
		if (value instanceof char[]) {
			return (char[]) value;
		}
		if (value instanceof String) {
			return ( (String) value ).toCharArray();
		}
		if (value instanceof Clob) {
			return DataHelper.extractString( ( (Clob) value ) ).toCharArray();
		}
		if (value instanceof Reader) {
			return DataHelper.extractString( ( (Reader) value ) ).toCharArray();
		}
		else if ( value instanceof Character ) {
			// Support binding a single element as parameter value
			return new char[]{ (char) value };
		}
		throw unknownWrap( value.getClass() );
	}

	@Override
	public <X> char[] coerce(X value, CoercionContext coercionContext) {
		return wrap( value, null );
	}
}
