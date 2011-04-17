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
package org.hibernate;

import java.io.InputStream;
import java.io.Reader;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;

/**
 * A {@link Session session's} helper for creating LOB data
 *
 * @author Steve Ebersole
 */
public interface LobHelper {

	/**
	 * Create a new {@link Blob} from bytes.
	 *
	 * @param bytes a byte array
	 *
	 * @return the created Blob
	 */
	public Blob createBlob(byte[] bytes);

	/**
	 * Create a new {@link Blob} from stream data.
	 *
	 * @param stream a binary stream
	 * @param length the number of bytes in the stream

	 * @return the create Blob
	 */
	public Blob createBlob(InputStream stream, long length);

	/**
	 * Create a new {@link java.sql.Clob} from content
	 *
	 * @param string The string data
	 *
	 * @return The created {@link java.sql.Clob}
	 */
	public Clob createClob(String string);

	/**
	 * Create a new {@link Clob} from character reader.
	 *
	 * @param reader a character stream
	 * @param length the number of characters in the stream
	 *
	 * @return The created {@link Clob}
	 */
	public Clob createClob(Reader reader, long length);

	/**
	 * Create a new {@link NClob} from content.
	 *
	 * @param string The string data
	 *
	 * @return The created {@link NClob}
	 */
	public NClob createNClob(String string);

	/**
	 * Create a new {@link NClob} from character reader.
	 *
	 * @param reader a character stream
	 * @param length the number of characters in the stream
	 *
	 * @return The created {@link NClob}
	 */
	public NClob createNClob(Reader reader, long length);
}
