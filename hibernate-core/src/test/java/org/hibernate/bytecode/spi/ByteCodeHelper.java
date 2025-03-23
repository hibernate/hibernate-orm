/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.spi;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.hibernate.internal.util.collections.ArrayHelper;

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

		final byte[] buffer = new byte[8_000]; //reasonably large enough for most classes
		byte[] classBytes = ArrayHelper.EMPTY_BYTE_ARRAY;

		try {
			int r = inputStream.read( buffer );
			while ( r != -1 ) {
				final byte[] temp = Arrays.copyOf( classBytes, classBytes.length + r );
				// copy the just read bytes into the temp array (after the previously read)
				System.arraycopy( buffer, 0, temp, classBytes.length, r );
				classBytes = temp;
				// read the next set of bytes into buffer
				r = inputStream.read( buffer );
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

}
