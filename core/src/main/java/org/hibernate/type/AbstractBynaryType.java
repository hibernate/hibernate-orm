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
 *
 */
package org.hibernate.type;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Types;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Comparator;

import org.hibernate.HibernateException;
import org.hibernate.EntityMode;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.cfg.Environment;

/**
 * Logic to bind stream of byte into a VARBINARY
 *
 * @author Gavin King
 * @author Emmanuel Bernard
 */
public abstract class AbstractBynaryType extends MutableType implements VersionType, Comparator {

	/**
	 * Convert the byte[] into the expected object type
	 */
	abstract protected Object toExternalFormat(byte[] bytes);

	/**
	 * Convert the object into the internal byte[] representation
	 */
	abstract protected byte[] toInternalFormat(Object bytes);

	public void set(PreparedStatement st, Object value, int index) throws HibernateException, SQLException {
		byte[] internalValue = toInternalFormat( value );
		if ( Environment.useStreamsForBinary() ) {
			st.setBinaryStream( index, new ByteArrayInputStream( internalValue ), internalValue.length );
		}
		else {
			st.setBytes( index, internalValue );
		}
	}

	public Object get(ResultSet rs, String name) throws HibernateException, SQLException {

		if ( Environment.useStreamsForBinary() ) {

			InputStream inputStream = rs.getBinaryStream(name);

			if (inputStream==null) return toExternalFormat( null ); // is this really necessary?

			ByteArrayOutputStream outputStream = new ByteArrayOutputStream(2048);
			byte[] buffer = new byte[2048];

			try {
				while (true) {
					int amountRead = inputStream.read(buffer);
					if (amountRead == -1) {
						break;
					}
					outputStream.write(buffer, 0, amountRead);
				}

				inputStream.close();
				outputStream.close();
			}
			catch (IOException ioe) {
				throw new HibernateException( "IOException occurred reading a binary value", ioe );
			}

			return toExternalFormat( outputStream.toByteArray() );

		}
		else {
			return toExternalFormat( rs.getBytes(name) );
		}
	}

	public int sqlType() {
		return Types.VARBINARY;
	}

	// VersionType impl ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	//      Note : simply returns null for seed() and next() as the only known
	//      application of binary types for versioning is for use with the
	//      TIMESTAMP datatype supported by Sybase and SQL Server, which
	//      are completely db-generated values...
	public Object seed(SessionImplementor session) {
		return null;
	}

	public Object next(Object current, SessionImplementor session) {
		return current;
	}

	public Comparator getComparator() {
		return this;
	}

	public int compare(Object o1, Object o2) {
		return compare( o1, o2, null );
	}
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public boolean isEqual(Object x, Object y) {
		return x==y || ( x!=null && y!=null && java.util.Arrays.equals( toInternalFormat(x), toInternalFormat(y) ) );
	}

	public int getHashCode(Object x, EntityMode entityMode) {
		byte[] bytes = toInternalFormat(x);
		int hashCode = 1;
		for ( int j=0; j<bytes.length; j++ ) {
			hashCode = 31 * hashCode + bytes[j];
		}
		return hashCode;
	}

	public int compare(Object x, Object y, EntityMode entityMode) {
		byte[] xbytes = toInternalFormat(x);
		byte[] ybytes = toInternalFormat(y);
		if ( xbytes.length < ybytes.length ) return -1;
		if ( xbytes.length > ybytes.length ) return 1;
		for ( int i=0; i<xbytes.length; i++ ) {
			if ( xbytes[i] < ybytes[i] ) return -1;
			if ( xbytes[i] > ybytes[i] ) return 1;
		}
		return 0;
	}

	public abstract String getName();

	public String toString(Object val) {
		byte[] bytes = toInternalFormat(val);
		StringBuffer buf = new StringBuffer();
		for ( int i=0; i<bytes.length; i++ ) {
			String hexStr = Integer.toHexString( bytes[i] - Byte.MIN_VALUE );
			if ( hexStr.length()==1 ) buf.append('0');
			buf.append(hexStr);
		}
		return buf.toString();
	}

	public Object deepCopyNotNull(Object value) {
		byte[] bytes = toInternalFormat(value);
		byte[] result = new byte[bytes.length];
		System.arraycopy(bytes, 0, result, 0, bytes.length);
		return toExternalFormat(result);
	}

	public Object fromStringValue(String xml) throws HibernateException {
		if (xml == null)
			return null;
		if (xml.length() % 2 != 0)
			throw new IllegalArgumentException("The string is not a valid xml representation of a binary content.");
		byte[] bytes = new byte[xml.length() / 2];
		for (int i = 0; i < bytes.length; i++) {
			String hexStr = xml.substring(i * 2, (i + 1) * 2);
			bytes[i] = (byte) (Integer.parseInt(hexStr, 16) + Byte.MIN_VALUE);
		}
		return toExternalFormat(bytes);
	}

}
