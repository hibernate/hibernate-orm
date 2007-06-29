//$Id: BlobImpl.java 4997 2004-12-18 18:48:20Z oneovthafew $
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






