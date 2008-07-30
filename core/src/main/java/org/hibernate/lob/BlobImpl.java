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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.SQLException;

/**
 * A dummy implementation of <tt>java.sql.Blob</tt> that
 * may be used to insert new data into a BLOB.
 * @author Gavin King
 */
public class BlobImpl implements Blob {

	private InputStream stream;
	private int length;
	private boolean needsReset = false;

	public BlobImpl(byte[] bytes) {
		this.stream = new ByteArrayInputStream(bytes);
		this.length = bytes.length;
	}

	public BlobImpl(InputStream stream, int length) {
		this.stream = stream;
		this.length = length;
	}

	/**
	 * @see java.sql.Blob#length()
	 */
	public long length() throws SQLException {
		return length;
	}

	/**
	 * @see java.sql.Blob#truncate(long)
	 */
	public void truncate(long pos) throws SQLException {
		excep();
	}

	/**
	 * @see java.sql.Blob#getBytes(long, int)
	 */
	public byte[] getBytes(long pos, int len) throws SQLException {
		excep(); return null;
	}

	/**
	 * @see java.sql.Blob#setBytes(long, byte[])
	 */
	public int setBytes(long pos, byte[] bytes) throws SQLException {
		excep(); return 0;
	}

	/**
	 * @see java.sql.Blob#setBytes(long, byte[], int, int)
	 */
	public int setBytes(long pos, byte[] bytes, int i, int j)
	throws SQLException {
		excep(); return 0;
	}

	/**
	 * @see java.sql.Blob#position(byte[], long)
	 */
	public long position(byte[] bytes, long pos) throws SQLException {
		excep(); return 0;
	}

	/**
	 * @see java.sql.Blob#getBinaryStream()
	 */
	public InputStream getBinaryStream() throws SQLException {
		try {
			if (needsReset) stream.reset();
		}
		catch (IOException ioe) {
			throw new SQLException("could not reset reader");
		}
		needsReset = true;
		return stream;
	}

	/**
	 * @see java.sql.Blob#setBinaryStream(long)
	 */
	public OutputStream setBinaryStream(long pos) throws SQLException {
		excep(); return null;
	}

	/**
	 * @see java.sql.Blob#position(Blob, long)
	 */
	public long position(Blob blob, long pos) throws SQLException {
		excep(); return 0;
	}

	private static void excep() {
		throw new UnsupportedOperationException("Blob may not be manipulated from creating session");
	}

}






