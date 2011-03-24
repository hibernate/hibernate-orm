/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
public class StreamCopier {
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

