/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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
package org.hibernate.engine.jdbc;
import java.io.InputStream;
import java.io.Reader;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;

/**
 * Contract for creating various LOB references.
 * 
 * @author Steve Ebersole
 * @author Gail Badner
 */
public interface LobCreator {
	/**
	 * Wrap the given blob in a serializable wrapper.
	 *
	 * @param blob The blob to be wrapped.
	 * @return The wrapped blob which will be castable to {@link Blob} as well as {@link WrappedBlob}.
	 */
	public Blob wrap(Blob blob);

	/**
	 * Wrap the given clob in a serializable wrapper.
	 *
	 * @param clob The clob to be wrapped.
	 * @return The wrapped clob which will be castable to {@link Clob} as well as {@link WrappedClob}.
	 */
	public Clob wrap(Clob clob);

	/**
	 * Wrap the given nclob in a serializable wrapper.
	 *
	 * @param nclob The nclob to be wrapped.
	 * @return The wrapped nclob which will be castable to {@link NClob} as well as {@link WrappedNClob}.
	 */
	public NClob wrap(NClob nclob);

	/**
	 * Create a BLOB reference encapsulating the given byte array.
	 *
	 * @param bytes The byte array to wrap as a blob.
	 * @return The created blob, castable to {@link Blob} as well as {@link BlobImplementer}
	 */
	public Blob createBlob(byte[] bytes);

	/**
	 * Create a BLOB reference encapsulating the given binary stream.
	 *
	 * @param stream The binary stream to wrap as a blob.
	 * @param length The length of the stream.
	 * @return The created blob, castable to {@link Blob} as well as {@link BlobImplementer}
	 */
	public Blob createBlob(InputStream stream, long length);

	/**
	 * Create a CLOB reference encapsulating the given String data.
	 *
	 * @param string The String to wrap as a clob.
	 * @return The created clob, castable to {@link Clob} as well as {@link ClobImplementer}
	 */
	public Clob createClob(String string);

	/**
	 * Create a CLOB reference encapsulating the given character data.
	 *
	 * @param reader The character data reader.
	 * @param length The length of the reader data.
	 * @return The created clob, castable to {@link Clob} as well as {@link ClobImplementer}
	 */
	public Clob createClob(Reader reader, long length);

	/**
	 * Create a NCLOB reference encapsulating the given String data.
	 *
	 * @param string The String to wrap as a NCLOB.
	 * @return The created NCLOB, castable as {@link Clob} as well as {@link NClobImplementer}.  In JDK 1.6
	 * environments, also castable to java.sql.NClob
	 */
	public NClob createNClob(String string);

	/**
	 * Create a NCLOB reference encapsulating the given character data.
	 *
	 * @param reader The character data reader.
	 * @param length The length of the reader data.
	 * @return The created NCLOB, castable as {@link Clob} as well as {@link NClobImplementer}.  In JDK 1.6
	 * environments, also castable to java.sql.NClob
	 */
	public NClob createNClob(Reader reader, long length);
}
