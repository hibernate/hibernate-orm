package org.hibernate.bytecode.util;

import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.io.BufferedInputStream;
import java.util.zip.ZipInputStream;

/**
 * A helper for reading byte code from various input sources.
 *
 * @author Steve Ebersole
 */
public class ByteCodeHelper {
	private ByteCodeHelper() {
	}

	/**
	 * Reads class byte array info from the given input stream.
	 * <p/>
	 * The stream is closed within this method!
	 *
	 * @param inputStream
	 * @return
	 * @throws IOException
	 */
	public static byte[] readByteCode(InputStream inputStream) throws IOException {
		if ( inputStream == null ) {
			throw new IOException( "null input stream" );
		}

		byte[] buffer = new byte[409600];
		byte[] classBytes = new byte[0];
		int r = 0;

		try {
			r = inputStream.read( buffer );
			while ( r >= buffer.length ) {
				byte[] temp = new byte[ classBytes.length + buffer.length ];
				System.arraycopy( classBytes, 0, temp, 0, classBytes.length );
				System.arraycopy( buffer, 0, temp, classBytes.length, buffer.length );
				classBytes = temp;
			}
			if ( r != -1 ) {
				byte[] temp = new byte[ classBytes.length + r ];
				System.arraycopy( classBytes, 0, temp, 0, classBytes.length );
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

	public static byte[] readByteCode(File file) throws IOException {
		return ByteCodeHelper.readByteCode( new FileInputStream( file ) );
	}

	public static byte[] readByteCode(ZipInputStream zip) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        InputStream in = new BufferedInputStream( zip );
        int b;
        while ( ( b = in.read() ) != -1 ) {
            bout.write( b );
        }
        return bout.toByteArray();
    }
}
