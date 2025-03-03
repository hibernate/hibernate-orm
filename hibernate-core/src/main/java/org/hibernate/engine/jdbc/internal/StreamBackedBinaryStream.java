/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.internal;

import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.BinaryStream;
import org.hibernate.engine.jdbc.LobCreator;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;

/**
 * Implementation of {@link BinaryStream} backed by an {@link InputStream}.
 *
 * @since 7.0
 */
public class StreamBackedBinaryStream implements BinaryStream {

	private final InputStream stream;
	private final long length;
	private byte[] bytes;

	public StreamBackedBinaryStream(InputStream stream, long length) {
		this.stream = stream;
		this.length = length;
	}

	@Override
	public InputStream getInputStream() {
		return stream;
	}

	@Override
	public byte[] getBytes() {
		if ( bytes == null ) {
			try {
				bytes = stream.readAllBytes();
			}
			catch (IOException e) {
				throw new HibernateException( "IOException occurred reading a binary value", e );
			}
		}
		return bytes;
	}

	@Override
	public long getLength() {
		return length;
	}

	@Override
	public Blob asBlob(LobCreator lobCreator) {
		return lobCreator.createBlob( stream, length );
	}

	@Override
	public void release() {
		try {
			stream.close();
		}
		catch (IOException ignore) {
		}
	}
}
