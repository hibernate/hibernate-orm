/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.hibernate.engine.jdbc.BinaryStream;

/**
 * Implementation of {@link BinaryStream}
 *
 * @author Steve Ebersole
 */
public final class BinaryStreamImpl extends ByteArrayInputStream implements BinaryStream {
	private final int length;

	/**
	 * Constructs a BinaryStreamImpl
	 *
	 * @param bytes The bytes to use backing the stream
	 */
	public BinaryStreamImpl(byte[] bytes) {
		super( bytes );
		this.length = bytes.length;
	}

	public InputStream getInputStream() {
		return this;
	}

	public byte[] getBytes() {
		// from ByteArrayInputStream
		return buf;
	}

	public long getLength() {
		return length;
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
