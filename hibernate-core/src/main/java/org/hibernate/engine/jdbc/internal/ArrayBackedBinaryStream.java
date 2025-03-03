/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.internal;

import org.hibernate.engine.jdbc.BinaryStream;
import org.hibernate.engine.jdbc.LobCreator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;

/**
 * Implementation of {@link BinaryStream} backed by a {@code byte[]} array.
 *
 * @author Steve Ebersole
 */
public class ArrayBackedBinaryStream extends ByteArrayInputStream implements BinaryStream {
	private final int length;

	/**
	 * Constructs a ArrayBackedBinaryStream
	 *
	 * @param bytes The bytes to use backing the stream
	 */
	public ArrayBackedBinaryStream(byte[] bytes) {
		super( bytes );
		this.length = bytes.length;
	}

	@Override
	public InputStream getInputStream() {
		return this;
	}

	@Override
	public byte[] getBytes() {
		// from ByteArrayInputStream
		return buf;
	}

	@Override
	public long getLength() {
		return length;
	}

	@Override
	public Blob asBlob(LobCreator lobCreator) {
		return lobCreator.createBlob( buf );
	}

	@Override
	public void release() {
		try {
			super.close();
		}
		catch (IOException ignore) {
		}
	}
}
