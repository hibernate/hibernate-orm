/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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
package org.hibernate.engine.jdbc;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/**
 * Stream copying utilities
 *
 * @author Steve Ebersole
 */
public class StreamUtils {
	public static final int DEFAULT_CHUNK_SIZE = 1024;

	public static long copy(InputStream inputStream, OutputStream outputStream) throws IOException {
		return copy( inputStream, outputStream, DEFAULT_CHUNK_SIZE );
	}

	public static long copy(InputStream inputStream, OutputStream outputStream, int bufferSize) throws IOException {
		byte[] buffer = new byte[bufferSize];
		long count = 0;
		int n;
		while ( -1 != ( n = inputStream.read( buffer ) ) ) {
			outputStream.write( buffer, 0, n );
			count += n;
		}
		return count;

	}

	public static long copy(Reader reader, Writer writer) throws IOException {
		return copy( reader, writer, DEFAULT_CHUNK_SIZE );
	}

	public static long copy(Reader reader, Writer writer, int bufferSize) throws IOException {
		char[] buffer = new char[bufferSize];
		long count = 0;
		int n;
		while ( -1 != ( n = reader.read( buffer ) ) ) {
			writer.write( buffer, 0, n );
			count += n;
		}
		return count;

	}
}
