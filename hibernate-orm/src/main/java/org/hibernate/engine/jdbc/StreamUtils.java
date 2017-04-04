/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

/**
 * Stream copying utilities
 *
 * @author Steve Ebersole
 */
public class StreamUtils {
	/**
	 * Default size to use for reading buffers.
	 */
	public static final int DEFAULT_CHUNK_SIZE = 1024;

	/**
	 * Copy the inputStream to the outputStream.  Uses a buffer of the default size ({@link #DEFAULT_CHUNK_SIZE}).
	 *
	 * @param inputStream The input stream to read
	 * @param outputStream The output stream to write to
	 *
	 * @return The number of bytes read
	 *
	 * @throws IOException If a problem occurred accessing either stream
	 */
	public static long copy(InputStream inputStream, OutputStream outputStream) throws IOException {
		return copy( inputStream, outputStream, DEFAULT_CHUNK_SIZE );
	}

	/**
	 * Copy the inputStream to the outputStream using a buffer of the specified size
	 *
	 * @param inputStream The input stream to read
	 * @param outputStream The output stream to write to
	 * @param bufferSize The size of the buffer to use for reading
	 *
	 * @return The number of bytes read
	 *
	 * @throws IOException If a problem occurred accessing either stream
	 */
	public static long copy(InputStream inputStream, OutputStream outputStream, int bufferSize) throws IOException {
		final byte[] buffer = new byte[bufferSize];
		long count = 0;
		int n;
		while ( -1 != ( n = inputStream.read( buffer ) ) ) {
			outputStream.write( buffer, 0, n );
			count += n;
		}
		return count;
	}

	/**
	 * Copy the reader to the writer.  Uses a buffer of the default size ({@link #DEFAULT_CHUNK_SIZE}).
	 *
	 * @param reader The reader to read from
	 * @param writer The writer to write to
	 *
	 * @return The number of bytes read
	 *
	 * @throws IOException If a problem occurred accessing reader or writer
	 */
	public static long copy(Reader reader, Writer writer) throws IOException {
		return copy( reader, writer, DEFAULT_CHUNK_SIZE );
	}

	/**
	 * Copy the reader to the writer using a buffer of the specified size
	 *
	 * @param reader The reader to read from
	 * @param writer The writer to write to
	 * @param bufferSize The size of the buffer to use for reading
	 *
	 * @return The number of bytes read
	 *
	 * @throws IOException If a problem occurred accessing either stream
	 */
	public static long copy(Reader reader, Writer writer, int bufferSize) throws IOException {
		final char[] buffer = new char[bufferSize];
		long count = 0;
		int n;
		while ( -1 != ( n = reader.read( buffer ) ) ) {
			writer.write( buffer, 0, n );
			count += n;
		}
		return count;
	}

	private StreamUtils() {
	}
}
