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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.HibernateException;
import org.hibernate.type.descriptor.BinaryStream;
import org.hibernate.util.ReflectHelper;

/**
 * A help for dealing with BLOB and CLOB data
 *
 * @author Steve Ebersole
 */
public class DataHelper {
	private static final Logger log = LoggerFactory.getLogger( DataHelper.class );

	private static Class nClobClass;
	static {
		try {
			// NClobs are only JDBC 4 (JDK 1.6) and higher
			nClobClass = ReflectHelper.classForName( "java.sql.NClob", DataHelper.class );
		}
		catch ( ClassNotFoundException e ) {
			log.info( "Could not locate 'java.sql.NClob' class; assuming JDBC 3" );
		}
	}

	public static boolean isNClob(Class type) {
		return nClobClass != null && nClobClass.isAssignableFrom( type );
	}

	public static String extractString(Reader reader) {
		// read the Reader contents into a buffer and return the complete string
		final StringBuilder stringBuilder = new StringBuilder();
		try {
			char[] buffer = new char[2048];
			while (true) {
				int amountRead = reader.read( buffer, 0, buffer.length );
				if ( amountRead == -1 ) {
					break;
				}
				stringBuilder.append( buffer, 0, amountRead );
			}
		}
		catch ( IOException ioe) {
			throw new HibernateException( "IOException occurred reading text", ioe );
		}
		finally {
			try {
				reader.close();
			}
			catch (IOException e) {
				log.warn( "IOException occurred closing stream", e );
			}
		}
		return stringBuilder.toString();
	}

	private static String extractString(Reader characterStream, long start, int length) {
		StringBuilder stringBuilder = new StringBuilder( length );
		try {
			long skipped = characterStream.skip( start - 1 );
			if ( skipped != start - 1 ) {
				throw new HibernateException( "Unable to skip needed bytes" );
			}
			char[] buffer = new char[2048];
			int charsRead = 0;
			while ( true ) {
				int amountRead = characterStream.read( buffer, 0, buffer.length );
				if ( amountRead == -1 ) {
					break;
				}
				stringBuilder.append( buffer, 0, amountRead );
				if ( amountRead < buffer.length ) {
					// we have read up to the end of stream
					break;
				}
				charsRead += amountRead;
				if ( charsRead >= length ) {
					break;
				}
			}
		}
		catch ( IOException ioe ) {
			throw new HibernateException( "IOException occurred reading a binary value", ioe );
		}
		return stringBuilder.toString();
	}

	public static Object subStream(Reader characterStream, long start, int length) {
		return new StringReader( extractString( characterStream, start, length ) );
	}

	public static byte[] extractBytes(InputStream inputStream) {
		if ( BinaryStream.class.isInstance( inputStream ) ) {
			return ( (BinaryStream ) inputStream ).getBytes();
		}

		// read the stream contents into a buffer and return the complete byte[]
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(2048);
		try {
			byte[] buffer = new byte[2048];
			while (true) {
				int amountRead = inputStream.read( buffer );
				if ( amountRead == -1 ) {
					break;
				}
				outputStream.write( buffer, 0, amountRead );
			}
		}
		catch ( IOException ioe ) {
			throw new HibernateException( "IOException occurred reading a binary value", ioe );
		}
		finally {
			try {
				inputStream.close();
			}
			catch ( IOException e ) {
				log.warn( "IOException occurred closing input stream", e );
			}
			try {
				outputStream.close();
			}
			catch ( IOException e ) {
				log.warn( "IOException occurred closing output stream", e );
			}
		}
		return outputStream.toByteArray();
	}

	public static byte[] extractBytes(InputStream inputStream, long position, int length) {
		if ( BinaryStream.class.isInstance( inputStream ) && Integer.MAX_VALUE > position ) {
			byte[] data = ( (BinaryStream ) inputStream ).getBytes();
			int size = Math.min( length, data.length );
			byte[] result = new byte[size];
			System.arraycopy( data, (int) position, result, 0, size );
			return result;
		}

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream( length );
		try {
			long skipped = inputStream.skip( position - 1 );
			if ( skipped != position - 1 ) {
				throw new HibernateException( "Unable to skip needed bytes" );
			}
			byte[] buffer = new byte[2048];
			int bytesRead = 0;
			while ( true ) {
				int amountRead = inputStream.read( buffer );
				if ( amountRead == -1 ) {
					break;
				}
				outputStream.write( buffer, 0, amountRead );
				if ( amountRead < buffer.length ) {
					// we have read up to the end of stream
					break;
				}
				bytesRead += amountRead;
				if ( bytesRead >= length ) {
					break;
				}
			}
		}
		catch ( IOException ioe ) {
			throw new HibernateException( "IOException occurred reading a binary value", ioe );
		}
		return outputStream.toByteArray();
	}

	public static InputStream subStream(InputStream inputStream, long start, int length) {
		return new BinaryStreamImpl( extractBytes( inputStream, start, length ) );
	}
}
