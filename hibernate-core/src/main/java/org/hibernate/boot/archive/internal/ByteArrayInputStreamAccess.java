/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.archive.internal;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;

import org.hibernate.boot.archive.spi.InputStreamAccess;

/**
 * An InputStreamAccess implementation based on a byte array
 *
 * @author Steve Ebersole
 */
public class ByteArrayInputStreamAccess implements InputStreamAccess, Serializable {
	private final String name;
	private final byte[] bytes;

	public ByteArrayInputStreamAccess(String name, byte[] bytes) {
		this.name = name;
		this.bytes = bytes;
	}

	@Override
	public String getStreamName() {
		return name;
	}

	@Override
	public InputStream accessInputStream() {
		return new ByteArrayInputStream( bytes );
	}
}
