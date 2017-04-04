/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import java.io.InputStream;
import java.io.Reader;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;

/**
 * A {@link Session session's} helper for creating LOB data.
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
	 * Create a new {@link java.sql.Clob} from content.
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
