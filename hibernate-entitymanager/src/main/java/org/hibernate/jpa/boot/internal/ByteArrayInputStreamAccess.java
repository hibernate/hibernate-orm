/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.jpa.boot.internal;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.hibernate.jpa.boot.spi.InputStreamAccess;
import org.hibernate.jpa.boot.spi.NamedInputStream;

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

	@Override
	public NamedInputStream asNamedInputStream() {
		return new NamedInputStream( getStreamName(), accessInputStream() );
	}
}
