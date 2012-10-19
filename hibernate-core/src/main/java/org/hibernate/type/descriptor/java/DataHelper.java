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
import java.sql.Clob;
import java.sql.SQLException;

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.internal.BinaryStreamImpl;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.engine.jdbc.BinaryStream;

/**
 * A help for dealing with BLOB and CLOB data
 *
 * @author Steve Ebersole
 */
public class DataHelper {

	/** The size of the buffer we will use to deserialize larger streams */
	private static final int BUFFER_SIZE = 1024 * 4;

	private static final CoreMessageLogger LOG = Logger.getMessageLogger( CoreMessageLogger.class, DataHelper.class.getName() );

	public static boolean isNClob(final Class type) {
		return java.sql.NClob.class.isAssignableFrom( type );
	}

	/**
	 * Extract the contents of the given reader/stream as a string.
	 * The reader will be closed.
	 *
	 * @param reader The reader for the content
	 *
	 * @return The content as string
	 */
	public static String extractString(Reader reader) {
		return extractString( reader, BUFFER_SIZE );
	}

	/**
	 * Extract the contents of the given reader/stream as a string.
	 * The reader will be closed.
	 *
	 * @param reader The reader for the content
	 * @param lengthHint if the length is known in advance the implementation can be slightly more efficient
	 *
	 * @return The content as string
	 */
	public static String extractString(Reader reader, int lengthHint) {
		// read the Reader contents into a buffer and return the complete string
		final int bufferSize = getSuggestedBufferSize( lengthHint );
		final StringBuilder stringBuilder = new StringBuilder( bufferSize );
		try {
			char[] buffer = new char[bufferSize];
			while (true) {
				int amountRead = reader.read( buffer, 0, bufferSize );
				if ( amountRead == -1 ) {
					break;
				}
				stringBuilder.append( buffer, 0, amountRead );
			}
		}
		catch ( IOException ioe ) {
			throw new HibernateException( "IOException occurred reading text", ioe );
		}
		finally {
			try {
				reader.close();
			}
			catch (IOException e) {
				LOG.unableToCloseStream( e );
			}
		}
		return stringBuilder.toString();
	}

	/**
	 * Extracts a portion of the contents of the given reader/stream as a string.
	 *
	 * @param characterStream The reader for the content
	 * @param start The start position/offset (0-based, per general stream/reader contracts).
	 * @param length The amount to extract
	 *
	 * @return The content as string
	 */
	private static String extractString(Reader characterStream, long start, int length) {
		if ( length == 0 ) {
			return "";
		}
		StringBuilder stringBuilder = new StringBuilder( length );
		try {
			long skipped = characterStream.skip( start );
			if ( skipped != start ) {
				throw new HibernateException( "Unable to skip needed bytes" );
			}
			final int bufferSize = getSuggestedBufferSize( length );
			char[] buffer = new char[bufferSize];
			int charsRead = 0;
			while ( true ) {
				int amountRead = characterStream.read( buffer, 0, bufferSize );
				if ( amountRead == -1 ) {
					break;
				}
				stringBuilder.append( buffer, 0, amountRead );
				if ( amountRead < bufferSize ) {
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

	/**
	 * Extract a portion of a reader, wrapping the portion in a new reader.
	 *
	 * @param characterStream The reader for the content
	 * @param start The start position/offset (0-based, per general stream/reader contracts).
	 * @param length The amount to extract
	 *
	 * @return The content portion as a reader
	 */
	public static Object subStream(Reader characterStream, long start, int length) {
		return new StringReader( extractString( characterStream, start, length ) );
	}

	/**
	 * Extract by bytes from the given stream.
	 *
	 * @param inputStream The stream of bytes.
	 *
	 * @return The contents as a {@code byte[]}
	 */
	public static byte[] extractBytes(InputStream inputStream) {
		if ( BinaryStream.class.isInstance( inputStream ) ) {
			return ( (BinaryStream ) inputStream ).getBytes();
		}

		// read the stream contents into a buffer and return the complete byte[]
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(BUFFER_SIZE);
		try {
			byte[] buffer = new byte[BUFFER_SIZE];
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
				LOG.unableToCloseInputStream( e );
			}
			try {
				outputStream.close();
			}
			catch ( IOException e ) {
				LOG.unableToCloseOutputStream( e );
			}
		}
		return outputStream.toByteArray();
	}

	/**
	 * Extract a portion of the bytes from the given stream.
	 *
	 * @param inputStream The stream of bytes.
	 * @param start The start position/offset (0-based, per general stream/reader contracts).
	 * @param length The amount to extract
	 *
	 * @return The extracted bytes
	 */
	public static byte[] extractBytes(InputStream inputStream, long start, int length) {
		if ( BinaryStream.class.isInstance( inputStream ) && Integer.MAX_VALUE > start ) {
			byte[] data = ( (BinaryStream ) inputStream ).getBytes();
			int size = Math.min( length, data.length );
			byte[] result = new byte[size];
			System.arraycopy( data, (int) start, result, 0, size );
			return result;
		}

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream( length );
		try {
			long skipped = inputStream.skip( start );
			if ( skipped != start ) {
				throw new HibernateException( "Unable to skip needed bytes" );
			}
			byte[] buffer = new byte[BUFFER_SIZE];
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

	/**
	 * Extract a portion of the bytes from the given stream., wrapping them in a new stream.
	 *
	 * @param inputStream The stream of bytes.
	 * @param start The start position/offset (0-based, per general stream/reader contracts).
	 * @param length The amount to extract
	 *
	 * @return The extracted bytes as a stream
	 */
	public static InputStream subStream(InputStream inputStream, long start, int length) {
		return new BinaryStreamImpl( extractBytes( inputStream, start, length ) );
	}

	/**
	 * Extract the contents of the given Clob as a string.
	 *
	 * @param reader The reader for the content
	 *
	 * @return The content as string
	 */
	public static String extractString(final Clob value) {
		try {
			Reader characterStream = value.getCharacterStream();
			long length = value.length();
			if ( length > Integer.MAX_VALUE ) {
				return extractString( characterStream, Integer.MAX_VALUE );
			}
			else {
				return extractString( characterStream, (int) length );
			}
		}
		catch ( SQLException e ) {
			throw new HibernateException( "Unable to access lob stream", e );
		}
	}

	/**
	 * Make sure we allocate a buffer sized not bigger than 2048,
	 * not higher than what is actually needed, and at least one.
	 * 
	 * @param lengthHint the expected size of the full value
	 * @return the buffer size
	 */
	private static final int getSuggestedBufferSize(final int lengthHint) {
		return Math.max( 1, Math.min( lengthHint , BUFFER_SIZE ) );
	}
}
