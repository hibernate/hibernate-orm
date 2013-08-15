/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
