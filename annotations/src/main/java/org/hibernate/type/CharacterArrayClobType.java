/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.type;

import java.io.CharArrayReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;

import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;
import org.hibernate.util.ArrayHelper;

/**
 * Map a Character[] to a Clob
 * Experimental
 *
 * @author Emmanuel Bernard
 */
public class CharacterArrayClobType implements UserType, Serializable {
	public static final int BUFFER_SIZE = 4096;

	public int[] sqlTypes() {
		return new int[]{Types.CLOB};
	}

	public Class returnedClass() {
		return Character[].class;
	}

	public boolean equals(Object x, Object y) throws HibernateException {
		if ( x == y ) return true;
		if ( x == null || y == null ) return false;
		if ( x instanceof Character[] ) {
			Object[] o1 = (Object[]) x;
			Object[] o2 = (Object[]) y;
			return ArrayHelper.isEquals( o1, o2 );
		}
		else {
			char[] c1 = (char[]) x;
			char[] c2 = (char[]) y;
			return ArrayHelper.isEquals( c1, c2 );
		}
	}

	public int hashCode(Object x) throws HibernateException {
		if ( x instanceof Character[] ) {
			Object[] o = (Object[]) x;
			return ArrayHelper.hash( o );
		}
		else {
			char[] c = (char[]) x;
			return ArrayHelper.hash( c );
		}
	}

	public Object nullSafeGet(ResultSet rs, String[] names, Object owner) throws HibernateException, SQLException {
		Reader reader = rs.getCharacterStream( names[0] );
		if ( reader == null ) return null;
		ArrayList result = new ArrayList();
		try {
			char[] charbuf = new char[BUFFER_SIZE];
			for ( int i = reader.read( charbuf ); i > 0 ; i = reader.read( charbuf ) ) {
				result.ensureCapacity( result.size() + BUFFER_SIZE );
				for ( int charIndex = 0; charIndex < i ; charIndex++ ) {
					result.add( Character.valueOf( charbuf[charIndex] ) );
				}
			}
		}
		catch (IOException e) {
			throw new SQLException( e.getMessage() );
		}
		if ( returnedClass().equals( Character[].class ) ) {
			return result.toArray( new Character[ result.size() ] );
		}
		else {
			//very suboptimal
			int length = result.size();
			char[] chars = new char[length];
			for ( int index = 0; index < length ; index++ ) {
				chars[index] = ( (Character) result.get( index ) ).charValue();
			}
			return chars;
		}
	}

	public void nullSafeSet(PreparedStatement st, Object value, int index) throws HibernateException, SQLException {
		if ( value != null ) {
			char[] chars;
			if ( value instanceof Character[] ) {
				Character[] character = (Character[]) value;
				int length = character.length;
				chars = new char[length];
				for ( int i = 0; i < length ; i++ ) {
					chars[i] = character[i].charValue();
				}
			}
			else {
				chars = (char[]) value;
			}
			CharArrayReader reader = new CharArrayReader( chars );
			st.setCharacterStream( index, reader, chars.length );
		}
		else {
			st.setNull( index, sqlTypes()[0] );
		}
	}

	public Object deepCopy(Object value) throws HibernateException {
		if ( value == null ) return null;
		if ( value instanceof Character[] ) {
			Character[] array = (Character[]) value;
			int length = array.length;
			Character[] copy = new Character[length];
			for ( int index = 0; index < length ; index++ ) {
				copy[index] = Character.valueOf( array[index].charValue() );
			}
			return copy;
		}
		else {
			char[] array = (char[]) value;
			int length = array.length;
			char[] copy = new char[length];
			System.arraycopy( array, 0, copy, 0, length );
			return copy;
		}
	}

	public boolean isMutable() {
		return true;
	}

	public Serializable disassemble(Object value) throws HibernateException {
		return (Serializable) deepCopy( value );
	}

	public Object assemble(Serializable cached, Object owner) throws HibernateException {
		return deepCopy( cached );
	}

	public Object replace(Object original, Object target, Object owner) throws HibernateException {
		return deepCopy( original );
	}
}
