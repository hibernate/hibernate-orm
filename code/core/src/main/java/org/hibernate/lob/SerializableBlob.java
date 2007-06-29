//$Id: SerializableBlob.java 5986 2005-03-02 11:43:36Z oneovthafew $
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
