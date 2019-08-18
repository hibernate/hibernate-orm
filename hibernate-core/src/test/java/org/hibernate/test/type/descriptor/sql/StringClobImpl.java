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

	@Override
	public long length() throws SQLException {
		return value.length();
	}

	@Override
	public String getSubString(long pos, int length) throws SQLException {
		return value.substring( (int)pos, (int)(pos+length) );
	}

	@Override
	public Reader getCharacterStream() throws SQLException {
		return new StringReader( value );
	}

	@Override
	public Reader getCharacterStream(long pos, long length) throws SQLException {
		return new StringReader( getSubString( pos, (int)length ) );
	}

	@Override
	public InputStream getAsciiStream() throws SQLException {
		throw new UnsupportedOperationException( "not supported" );
	}

	@Override
	public long position(String searchstr, long start) throws SQLException {
		return value.indexOf( searchstr, (int)start );
	}

	@Override
	public long position(Clob searchstr, long start) throws SQLException {
		throw new UnsupportedOperationException( "not supported" );
	}

	@Override
	public int setString(long pos, String str) throws SQLException {
		throw new UnsupportedOperationException( "not supported" );
	}

	@Override
	public int setString(long pos, String str, int offset, int len) throws SQLException {
		throw new UnsupportedOperationException( "not supported" );
	}

	@Override
	public OutputStream setAsciiStream(long pos) throws SQLException {
		throw new UnsupportedOperationException( "not supported" );
	}

	@Override
	public Writer setCharacterStream(long pos) throws SQLException {
		throw new UnsupportedOperationException( "not supported" );
	}

	@Override
	public void truncate(long len) throws SQLException {
	}

	@Override
	public void free() throws SQLException {
	}
}
