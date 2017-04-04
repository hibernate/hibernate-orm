/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc;

import java.io.InputStream;

/**
 * Wraps a binary stream to also provide the length which is needed when binding.
 *
 * @author Steve Ebersole
 */
public interface BinaryStream {
	/**
	 * Retrieve the input stream.
	 *
	 * @return The input stream
	 */
	public InputStream getInputStream();

	/**
	 * Access to the bytes.
	 *
	 * @return The bytes.
	 */
	public byte[] getBytes();

	/**
	 * Retrieve the length of the input stream
	 *
	 * @return The input stream length
	 */
	public long getLength();

	/**
	 * Release any underlying resources.
	 */
	public void release();
}
