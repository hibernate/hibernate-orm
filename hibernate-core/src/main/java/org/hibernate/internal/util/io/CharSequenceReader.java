/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.internal.util.io;

import java.io.IOException;
import java.io.Reader;

/**
 * @author Steve Ebersole
 */
public class CharSequenceReader extends Reader {
	private CharSequence charSequence;

	private int length;
	private int position = 0;
	private int mark = 0;

	public CharSequenceReader(CharSequence charSequence) {
		this.charSequence = charSequence;
		this.length = charSequence.length();
	}

	@Override
	public boolean markSupported() {
		return true;
	}

	@Override
	public void mark(int readAheadLimit) throws IOException {
		this.mark = this.position;
	}

	public int read() throws IOException {
		verifyOpen();

		if ( this.position >= this.length ) {
			return -1;
		}

		return this.charSequence.charAt( this.position++ );
	}

	protected void verifyOpen() throws IOException {
		if ( charSequence == null ) {
			throw new IOException( "Stream closed" );
		}
	}

	@Override
	public int read(char[] dest, int off, int len) throws IOException {
		verifyOpen();

		if ( this.position >= this.length ) {
			return -1;
		}

		if ((off < 0) || (off > dest.length) || (len < 0) ||
				((off + len) > dest.length) || ((off + len) < 0)) {
			throw new IndexOutOfBoundsException();
		}
		else if (len == 0) {
			return 0;
		}

		int n = Math.min( length - position, len );

		for ( int i = 0; i < n; i++ ) {
			dest[i] = charSequence.charAt( n );
		}

		position += n;

		return n;
	}

	public void reset() {
		this.position = this.mark;
	}

	public long skip(long n) {
		if ( n < 0L ) {
			throw new IllegalArgumentException( "Number of characters to skip must be greater than zero: " + n );
		}

		if ( this.position >= length ) {
			return -1L;
		}
		else {
			int dest = (int)Math.min((long)this.charSequence.length(), (long)this.position + n);
			int count = dest - this.position;
			this.position = dest;
			return (long)count;
		}
	}

	@Override
	public void close() throws IOException {
		charSequence = null;
		mark = 0;
		position = 0;
	}
}
