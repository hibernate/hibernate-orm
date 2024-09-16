/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
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
	InputStream getInputStream();

	/**
	 * Access to the bytes.
	 *
	 * @return The bytes.
	 */
	byte[] getBytes();

	/**
	 * Retrieve the length of the input stream
	 *
	 * @return The input stream length
	 */
	long getLength();

	/**
	 * Release any underlying resources.
	 */
	void release();
}
