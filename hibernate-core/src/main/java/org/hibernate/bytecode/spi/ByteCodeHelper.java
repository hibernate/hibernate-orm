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
package org.hibernate.bytecode.spi;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipInputStream;

/**
 * A helper for reading byte code from various input sources.
 *
 * @author Steve Ebersole
 */
public class ByteCodeHelper {
	/**
	 * Disallow instantiation (its a helper)
	 */
	private ByteCodeHelper() {
	}

	/**
	 * Reads class byte array info from the given input stream.
	 *
	 * The stream is closed within this method!
	 *
	 * @param inputStream The stream containing the class binary; null will lead to an {@link IOException}
	 *
	 * @return The read bytes
	 *
	 * @throws IOException Indicates a problem accessing the given stream.
	 */
	public static byte[] readByteCode(InputStream inputStream) throws IOException {
		if ( inputStream == null ) {
			throw new IOException( "null input stream" );
		}

		final byte[] buffer = new byte[409600];
		byte[] classBytes = new byte[0];

		try {
			int r = inputStream.read( buffer );
			while ( r >= buffer.length ) {
				final byte[] temp = new byte[ classBytes.length + buffer.length ];
				// copy any previously read bytes into the temp array
				System.arraycopy( classBytes, 0, temp, 0, classBytes.length );
				// copy the just read bytes into the temp array (after the previously read)
				System.arraycopy( buffer, 0, temp, classBytes.length, buffer.length );
				classBytes = temp;
				// read the next set of bytes into buffer
				r = inputStream.read( buffer );
			}
			if ( r != -1 ) {
				final byte[] temp = new byte[ classBytes.length + r ];
				// copy any previously read bytes into the temp array
				System.arraycopy( classBytes, 0, temp, 0, classBytes.length );
				// copy the just read bytes into the temp array (after the previously read)
				System.arraycopy( buffer, 0, temp, classBytes.length, r );
				classBytes = temp;
			}
		}
		finally {
			try {
				inputStream.close();
			}
			catch (IOException ignore) {
				// intentionally empty
			}
		}

		return classBytes;
	}

	/**
	 * Read class definition from a file.
	 *
	 * @param file The file to read.
	 *
	 * @return The class bytes
	 *
	 * @throws IOException Indicates a problem accessing the given stream.
	 */
	public static byte[] readByteCode(File file) throws IOException {
		return ByteCodeHelper.readByteCode( new FileInputStream( file ) );
	}

	/**
	 * Read class definition a zip (jar) file entry.
	 *
	 * @param zip The zip entry stream.
	 *
	 * @return The class bytes
	 *
	 * @throws IOException Indicates a problem accessing the given stream.
	 */
	public static byte[] readByteCode(ZipInputStream zip) throws IOException {
		final ByteArrayOutputStream bout = new ByteArrayOutputStream();
		final InputStream in = new BufferedInputStream( zip );
		int b;
		while ( ( b = in.read() ) != -1 ) {
			bout.write( b );
		}
		return bout.toByteArray();
	}
}
