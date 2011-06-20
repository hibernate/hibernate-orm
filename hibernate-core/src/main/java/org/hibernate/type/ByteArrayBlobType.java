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
package org.hibernate.type;

import java.io.ByteArrayInputStream;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;

import org.dom4j.Node;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.util.collections.ArrayHelper;

/**
 * Map a Byte[] into a Blob
 * Experimental
 *
 * @author Emmanuel Bernard
 * @deprecated replaced by {@link org.hibernate.type.WrappedMaterializedBlobType}
 */
@Deprecated
public class ByteArrayBlobType extends AbstractLobType {
	private static final int[] TYPES = new int[] { Types.BLOB };

	public int[] sqlTypes(Mapping mapping) {
		return TYPES;
	}

	@Override
	public boolean isEqual(Object x, Object y, SessionFactoryImplementor factory) {
		if ( x == y ) return true;
		if ( x == null || y == null ) return false;
		if ( x instanceof Byte[] ) {
			Object[] o1 = (Object[]) x;
			Object[] o2 = (Object[]) y;
			return ArrayHelper.isEquals( o1, o2 );
		}
		else {
			byte[] c1 = (byte[]) x;
			byte[] c2 = (byte[]) y;
			return ArrayHelper.isEquals( c1, c2 );
		}
	}

	public int getHashCode(Object x, SessionFactoryImplementor factory) {
		if ( x instanceof Character[] ) {
			Object[] o = (Object[]) x;
			return ArrayHelper.hash( o );
		}
		else {
			byte[] c = (byte[]) x;
			return ArrayHelper.hash( c );
		}
	}

	public Object deepCopy(Object value, SessionFactoryImplementor factory)
			throws HibernateException {
		if ( value == null ) return null;
		if ( value instanceof Byte[] ) {
			Byte[] array = (Byte[]) value;
			int length = array.length;
			Byte[] copy = new Byte[length];
			for ( int index = 0; index < length ; index++ ) {
				copy[index] = Byte.valueOf( array[index].byteValue() );
			}
			return copy;
		}
		else {
			byte[] array = (byte[]) value;
			int length = array.length;
			byte[] copy = new byte[length];
			System.arraycopy( array, 0, copy, 0, length );
			return copy;
		}
	}

	public Class getReturnedClass() {
		return Byte[].class;
	}

	protected Object get(ResultSet rs, String name) throws SQLException {
		Blob blob = rs.getBlob( name );
		if ( rs.wasNull() ) return null;
		int length = (int) blob.length();
		byte[] primaryResult = blob.getBytes( 1, length );
		return wrap( primaryResult );
	}

	protected void set(PreparedStatement st, Object value, int index, SessionImplementor session) throws SQLException {
		if ( value == null ) {
			st.setNull( index, sqlTypes( null )[0] );
		}
		else {
			byte[] toSet = unWrap( value );
			final boolean useInputStream = session.getFactory().getDialect().useInputStreamToInsertBlob();

			if ( useInputStream ) {
				st.setBinaryStream( index, new ByteArrayInputStream( toSet ), toSet.length );
			}
			else {
				st.setBlob( index, Hibernate.getLobCreator( session ).createBlob( toSet ) );
			}
		}
	}

	public void setToXMLNode(Node node, Object value, SessionFactoryImplementor factory) throws HibernateException {
		node.setText( toString( value ) );
	}

	public String toString(Object val) {
		byte[] bytes = unWrap( val );
		StringBuilder buf = new StringBuilder( 2 * bytes.length );
		for ( int i = 0; i < bytes.length ; i++ ) {
			String hexStr = Integer.toHexString( bytes[i] - Byte.MIN_VALUE );
			if ( hexStr.length() == 1 ) buf.append( '0' );
			buf.append( hexStr );
		}
		return buf.toString();
	}

	public String toLoggableString(Object value, SessionFactoryImplementor factory) {
		return value == null ? "null" : toString( value );
	}

	public Object fromXMLNode(Node xml, Mapping factory) throws HibernateException {
		String xmlText = xml.getText();
		return xmlText == null || xmlText.length() == 0 ? null : fromString( xmlText );
	}

	private Object fromString(String xmlText) {
		if ( xmlText == null ) {
			return null;
		}
		if ( xmlText.length() % 2 != 0 ) {
			throw new IllegalArgumentException( "The string is not a valid xml representation of a binary content." );
		}
		byte[] bytes = new byte[xmlText.length() / 2];
		for ( int i = 0; i < bytes.length ; i++ ) {
			String hexStr = xmlText.substring( i * 2, ( i + 1 ) * 2 );
			bytes[i] = (byte) ( Integer.parseInt( hexStr, 16 ) + Byte.MIN_VALUE );
		}
		return wrap( bytes );
	}

	protected Object wrap(byte[] bytes) {
		return wrapPrimitive( bytes );
	}

	protected byte[] unWrap(Object bytes) {
		return unwrapNonPrimitive( (Byte[]) bytes );
	}

	private byte[] unwrapNonPrimitive(Byte[] bytes) {
		int length = bytes.length;
		byte[] result = new byte[length];
		for ( int i = 0; i < length ; i++ ) {
			result[i] = bytes[i].byteValue();
		}
		return result;
	}

	private Byte[] wrapPrimitive(byte[] bytes) {
		int length = bytes.length;
		Byte[] result = new Byte[length];
		for ( int index = 0; index < length ; index++ ) {
			result[index] = Byte.valueOf( bytes[index] );
		}
		return result;
	}

	public boolean isMutable() {
		return true;
	}

	public Object replace(
			Object original,
			Object target,
			SessionImplementor session,
			Object owner,
			Map copyCache
	)
			throws HibernateException {
		if ( isEqual( original, target ) ) return original;
		return deepCopy( original, session.getFactory() );
	}

	public boolean[] toColumnNullness(Object value, Mapping mapping) {
		return value == null ? ArrayHelper.FALSE : ArrayHelper.TRUE;
	}
}
