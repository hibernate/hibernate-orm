//$Id: SerializableClob.java 5986 2005-03-02 11:43:36Z oneovthafew $
package org.hibernate.lob;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.sql.Clob;
import java.sql.SQLException;

/**
 * @author Gavin King
 */
public class SerializableClob implements Serializable, Clob {

	private transient final Clob clob;
	
	public SerializableClob(Clob blob) {
		this.clob = blob;
	}

	public Clob getWrappedClob() {
		if ( clob==null ) {
			throw new IllegalStateException("Clobs may not be accessed after serialization");
		}
		else {
			return clob;
		}
	}
	
	public long length() throws SQLException {
		return getWrappedClob().length();
	}

	public String getSubString(long pos, int length) throws SQLException {
		return getWrappedClob().getSubString(pos, length);
	}

	public Reader getCharacterStream() throws SQLException {
		return getWrappedClob().getCharacterStream();
	}

	public InputStream getAsciiStream() throws SQLException {
		return getWrappedClob().getAsciiStream();
	}

	public long position(String searchstr, long start) throws SQLException {
		return getWrappedClob().position(searchstr, start);
	}

	public long position(Clob searchstr, long start) throws SQLException {
		return getWrappedClob().position(searchstr, start);
	}

	public int setString(long pos, String str) throws SQLException {
		return getWrappedClob().setString(pos, str);
	}

	public int setString(long pos, String str, int offset, int len) throws SQLException {
		return getWrappedClob().setString(pos, str, offset, len);
	}

	public OutputStream setAsciiStream(long pos) throws SQLException {
		return getWrappedClob().setAsciiStream(pos);
	}

	public Writer setCharacterStream(long pos) throws SQLException {
		return getWrappedClob().setCharacterStream(pos);
	}

	public void truncate(long len) throws SQLException {
		getWrappedClob().truncate(len);
	}

}
