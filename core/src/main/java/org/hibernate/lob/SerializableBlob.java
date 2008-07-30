/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.lob;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.sql.Blob;
import java.sql.SQLException;

/**
 * @author Gavin King
 */
public class SerializableBlob implements Serializable, Blob {
	
	private transient final Blob blob;
	
	public SerializableBlob(Blob blob) {
		this.blob = blob;
	}

	public Blob getWrappedBlob() {
		if ( blob==null ) {
			throw new IllegalStateException("Blobs may not be accessed after serialization");
		}
		else {
			return blob;
		}
	}
	
	public long length() throws SQLException {
		return getWrappedBlob().length();
	}

	public byte[] getBytes(long pos, int length) throws SQLException {
		return getWrappedBlob().getBytes(pos, length);
	}

	public InputStream getBinaryStream() throws SQLException {
		return getWrappedBlob().getBinaryStream();
	}

	public long position(byte[] pattern, long start) throws SQLException {
		return getWrappedBlob().position(pattern, start);
	}

	public long position(Blob pattern, long start) throws SQLException {
		return getWrappedBlob().position(pattern, start);
	}

	public int setBytes(long pos, byte[] bytes) throws SQLException {
		return getWrappedBlob().setBytes(pos, bytes);
	}

	public int setBytes(long pos, byte[] bytes, int offset, int len) throws SQLException {
		return getWrappedBlob().setBytes(pos, bytes, offset, len);
	}

	public OutputStream setBinaryStream(long pos) throws SQLException {
		return getWrappedBlob().setBinaryStream(pos);
	}

	public void truncate(long len) throws SQLException {
		getWrappedBlob().truncate(len);
	}

}
