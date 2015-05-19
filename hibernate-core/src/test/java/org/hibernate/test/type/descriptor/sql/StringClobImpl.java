/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.type.descriptor.sql;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.sql.Clob;
import java.sql.SQLException;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class StringClobImpl implements Clob {
	private final String value;

	public StringClobImpl(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public long length() throws SQLException {
		return value.length();
	}

	public String getSubString(long pos, int length) throws SQLException {
		return value.substring( (int)pos, (int)(pos+length) );
	}

	public Reader getCharacterStream() throws SQLException {
		return new StringReader( value );
	}

	public Reader getCharacterStream(long pos, long length) throws SQLException {
		return new StringReader( getSubString( pos, (int)length ) );
	}

	public InputStream getAsciiStream() throws SQLException {
		throw new UnsupportedOperationException( "not supported" );
	}

	public long position(String searchstr, long start) throws SQLException {
		return value.indexOf( searchstr, (int)start );
	}

	public long position(Clob searchstr, long start) throws SQLException {
		throw new UnsupportedOperationException( "not supported" );
	}

	public int setString(long pos, String str) throws SQLException {
		throw new UnsupportedOperationException( "not supported" );
	}

	public int setString(long pos, String str, int offset, int len) throws SQLException {
		throw new UnsupportedOperationException( "not supported" );
	}

	public OutputStream setAsciiStream(long pos) throws SQLException {
		throw new UnsupportedOperationException( "not supported" );
	}

	public Writer setCharacterStream(long pos) throws SQLException {
		throw new UnsupportedOperationException( "not supported" );
	}

	public void truncate(long len) throws SQLException {
	}

	public void free() throws SQLException {
	}
}
