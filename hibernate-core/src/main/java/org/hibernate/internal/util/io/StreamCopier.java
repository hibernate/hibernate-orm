/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.util.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.hibernate.HibernateException;

/**
 * Utilities for copying I/O streams.
 *
 * @author Steve Ebersole
 */
public final class StreamCopier {
	private StreamCopier() {
	}

	public static final int BUFFER_SIZE = 1024 * 4;
	public static final byte[] BUFFER = new byte[ BUFFER_SIZE ];

	public static long copy(InputStream from, OutputStream into) {
		try {
			long totalRead = 0;
			while ( true ) {
				synchronized ( BUFFER ) {
					int amountRead = from.read( BUFFER );
					if ( amountRead == -1 ) {
						break;
					}
					into.write( BUFFER, 0, amountRead );
					totalRead += amountRead;
					if ( amountRead < BUFFER_SIZE ) {
						// should mean there is no more data in the stream, no need for next read
						break;
					}
				}
			}
			return totalRead;
		}
		catch (IOException e ) {
			throw new HibernateException( "Unable to copy stream content", e );
		}
	}
}
