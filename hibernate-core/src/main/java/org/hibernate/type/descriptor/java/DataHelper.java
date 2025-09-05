/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Clob;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

import org.hibernate.HibernateException;
import org.hibernate.Internal;
import org.hibernate.engine.jdbc.BinaryStream;
import org.hibernate.engine.jdbc.internal.ArrayBackedBinaryStream;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;


/**
 * A helper for dealing with {@code BLOB} and {@code CLOB} data
 *
 * @author Steve Ebersole
 */
@Internal
public final class DataHelper {
	private DataHelper() {
	}

	/** The size of the buffer we will use to deserialize larger streams */
	private static final int BUFFER_SIZE = 1024 * 4;

	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( DataHelper.class );

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
			final char[] buffer = new char[bufferSize];
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
		final StringBuilder stringBuilder = new StringBuilder( length );
		try {
			final long skipped = characterStream.skip( start );
			if ( skipped != start ) {
				throw new HibernateException( "Unable to skip needed bytes" );
			}
			final int bufferSize = getSuggestedBufferSize( length );
			final char[] buffer = new char[bufferSize];
			int charsRead = 0;
			while ( true ) {
				final int amountRead = characterStream.read( buffer, 0, bufferSize );
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
	public static Reader subStream(Reader characterStream, long start, int length) {
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
		if ( inputStream instanceof BinaryStream binaryStream ) {
			return binaryStream.getBytes();
		}

		// read the stream contents into a buffer and return the complete byte[]
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(BUFFER_SIZE);
		try {
			final byte[] buffer = new byte[BUFFER_SIZE];
			while (true) {
				final int amountRead = inputStream.read( buffer );
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
		if ( inputStream instanceof BinaryStream binaryStream
				&& Integer.MAX_VALUE > start ) {
			final byte[] data = binaryStream.getBytes();
			final int size = Math.min( length, data.length );
			final byte[] result = new byte[size];
			System.arraycopy( data, (int) start, result, 0, size );
			return result;
		}

		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream( length );
		try {
			final long skipped = inputStream.skip( start );
			if ( skipped != start ) {
				throw new HibernateException( "Unable to skip needed bytes" );
			}
			final byte[] buffer = new byte[BUFFER_SIZE];
			int bytesRead = 0;
			while ( true ) {
				final int amountRead = inputStream.read( buffer );
				if ( amountRead == -1 ) {
					break;
				}
				outputStream.write( buffer, 0, amountRead );
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
	 * Extract a portion of the bytes from the given stream, wrapping them in a new stream.
	 *
	 * @param inputStream The stream of bytes.
	 * @param start The start position/offset (0-based, per general stream/reader contracts).
	 * @param length The amount to extract
	 *
	 * @return The extracted bytes as a stream
	 */
	public static InputStream subStream(InputStream inputStream, long start, int length) {
		return new ArrayBackedBinaryStream( extractBytes( inputStream, start, length ) );
	}

	/**
	 * Extract the contents of the given Clob as a string.
	 *
	 * @param value The clob to to be extracted from
	 *
	 * @return The content as string
	 */
	public static String extractString(final Clob value) {
		try {
			final Reader characterStream = value.getCharacterStream();
			final long length = determineLengthForBufferSizing( value );
			return length > Integer.MAX_VALUE
					? extractString( characterStream, Integer.MAX_VALUE )
					: extractString( characterStream, (int) length );
		}
		catch ( SQLException e ) {
			throw new HibernateException( "Unable to access lob stream", e );
		}
	}

	/**
	 * Determine a buffer size for reading the underlying character stream.
	 *
	 * @param value The Clob value
	 *
	 * @return The appropriate buffer size ({@link Clob#length()} by default.
	 *
	 */
	private static long determineLengthForBufferSizing(Clob value) throws SQLException {
		try {
			return value.length();
		}
		catch ( SQLFeatureNotSupportedException e ) {
			return BUFFER_SIZE;
		}
	}

	/**
	 * Make sure we allocate a buffer sized not bigger than 2048,
	 * not higher than what is actually needed, and at least one.
	 *
	 * @param lengthHint the expected size of the full value
	 * @return the buffer size
	 */
	private static int getSuggestedBufferSize(final int lengthHint) {
		return Math.max( 1, Math.min( lengthHint , BUFFER_SIZE ) );
	}
}
