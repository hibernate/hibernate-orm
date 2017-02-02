/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.archive.internal;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.hibernate.boot.archive.spi.InputStreamAccess;

/**
 * An InputStreamAccess implementation based on a byte array
 *
 * @author Steve Ebersole
 */
public class ByteArrayInputStreamAccess implements InputStreamAccess {
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
