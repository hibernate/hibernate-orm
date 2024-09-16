/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc;

import java.io.InputStream;
import java.io.Reader;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;

/**
 * Contract for creating various LOB references.
 *
 * @apiNote This class is not intended to be called directly by the application program.
 *          Instead, use {@link org.hibernate.Session#getLobHelper()}.
 *
 * @author Steve Ebersole
 * @author Gail Badner
 *
 * @see org.hibernate.type.descriptor.WrapperOptions#getLobCreator()
 * @see org.hibernate.engine.jdbc.spi.JdbcServices#getLobCreator(LobCreationContext)
 * @see org.hibernate.LobHelper
 */
public interface LobCreator {
	/**
	 * Wrap the given blob in a serializable wrapper.
	 *
	 * @param blob The blob to be wrapped.
	 * @return The wrapped blob which will be castable to {@link Blob} as well as {@link WrappedBlob}.
	 */
	Blob wrap(Blob blob);

	/**
	 * Wrap the given clob in a serializable wrapper.
	 *
	 * @param clob The clob to be wrapped.
	 * @return The wrapped clob which will be castable to {@link Clob} as well as {@link WrappedClob}.
	 */
	Clob wrap(Clob clob);

	/**
	 * Wrap the given nclob in a serializable wrapper.
	 *
	 * @param nclob The nclob to be wrapped.
	 * @return The wrapped nclob which will be castable to {@link NClob} as well as {@link WrappedNClob}.
	 */
	NClob wrap(NClob nclob);

	/**
	 * Create a BLOB reference encapsulating the given byte array.
	 *
	 * @param bytes The byte array to wrap as a blob.
	 * @return The created blob, castable to {@link Blob} as well as {@link BlobImplementer}
	 */
	Blob createBlob(byte[] bytes);

	/**
	 * Create a BLOB reference encapsulating the given binary stream.
	 *
	 * @param stream The binary stream to wrap as a blob.
	 * @param length The length of the stream.
	 * @return The created blob, castable to {@link Blob} as well as {@link BlobImplementer}
	 */
	Blob createBlob(InputStream stream, long length);

	/**
	 * Create a CLOB reference encapsulating the given String data.
	 *
	 * @param string The String to wrap as a clob.
	 * @return The created clob, castable to {@link Clob} as well as {@link ClobImplementer}
	 */
	Clob createClob(String string);

	/**
	 * Create a CLOB reference encapsulating the given character data.
	 *
	 * @param reader The character data reader.
	 * @param length The length of the reader data.
	 * @return The created clob, castable to {@link Clob} as well as {@link ClobImplementer}
	 */
	Clob createClob(Reader reader, long length);

	/**
	 * Create a NCLOB reference encapsulating the given String data.
	 *
	 * @param string The String to wrap as a NCLOB.
	 * @return The created NCLOB, castable as {@link Clob} as well as {@link NClobImplementer}.  In JDK 1.6
	 * environments, also castable to java.sql.NClob
	 */
	NClob createNClob(String string);

	/**
	 * Create a NCLOB reference encapsulating the given character data.
	 *
	 * @param reader The character data reader.
	 * @param length The length of the reader data.
	 * @return The created NCLOB, castable as {@link Clob} as well as {@link NClobImplementer}.  In JDK 1.6
	 * environments, also castable to java.sql.NClob
	 */
	NClob createNClob(Reader reader, long length);
}
