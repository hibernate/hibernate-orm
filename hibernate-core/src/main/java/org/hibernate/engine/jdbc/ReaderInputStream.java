/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;

/**
 * Exposes a {@link Reader} as an {@link InputStream}.
 *
 * @author Gavin King
 */
public class ReaderInputStream extends InputStream {
	private Reader reader;
	private CharBuffer encoderIn;
	private ByteBuffer encoderOut;
	private CharsetEncoder encoder;

	/**
	 * Constructs a ReaderInputStream from a Reader
	 *
	 * @param reader The reader to expose as an InputStream
	 */
	public ReaderInputStream(Reader reader) {
		this.reader = reader;
		this.encoder = Charset.forName( "UTF-8" ) // TODO Charset should be configurable
				.newEncoder()
				.onMalformedInput( CodingErrorAction.REPLACE )
				.onUnmappableCharacter( CodingErrorAction.REPLACE );
		encoderIn = CharBuffer.allocate( 512 );
		encoderOut = ByteBuffer.allocate( 1024 );
		encoderIn.flip();
		encoderOut.flip();
	}

	private void fillEncoderOut() throws IOException {
		boolean endOfInput = false;
		encoderIn.compact();
		int cnt = reader.read( encoderIn );
		if ( cnt == -1 ) {
			endOfInput = true;
		}
		encoderIn.flip();

		encoderOut.compact();
		encoder.encode( encoderIn, encoderOut, endOfInput );
		encoderOut.flip();
	}

	@Override
	public int read() throws IOException {
		if ( !encoderOut.hasRemaining() ) {
			fillEncoderOut();
			if ( !encoderOut.hasRemaining() ) {
				return -1;
			}
		}
		return 0xff & encoderOut.get();
	}
}
