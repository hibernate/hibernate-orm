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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.sql.Clob;
import java.sql.SQLException;

/**
 * A dummy implementation of <tt>java.sql.Clob</tt> that
 * may be used to insert new data into a CLOB.
 * @author Gavin King
 */
public class ClobImpl implements Clob {

	private Reader reader;
	private int length;
	private boolean needsReset = false;

	public ClobImpl(String string) {
		reader = new StringReader(string);
		length = string.length();
	}

	public ClobImpl(Reader reader, int length) {
		this.reader = reader;
		this.length = length;
	}

	/**
	 * @see java.sql.Clob#length()
	 */
	public long length() throws SQLException {
		return length;
	}

	/**
	 * @see java.sql.Clob#truncate(long)
	 */
	public void truncate(long pos) throws SQLException {
		excep();
	}

	/**
	 * @see java.sql.Clob#getAsciiStream()
	 */
	public InputStream getAsciiStream() throws SQLException {
		try {
			if (needsReset) reader.reset();
		}
		catch (IOException ioe) {
			throw new SQLException("could not reset reader");
		}
		needsReset = true;
		return new ReaderInputStream(reader);
	}

	/**
	 * @see java.sql.Clob#setAsciiStream(long)
	 */
	public OutputStream setAsciiStream(long pos) throws SQLException {
		excep(); return null;
	}

	/**
	 * @see java.sql.Clob#getCharacterStream()
	 */
	public Reader getCharacterStream() throws SQLException {
		try {
			if (needsReset) reader.reset();
		}
		catch (IOException ioe) {
			throw new SQLException("could not reset reader");
		}
		needsReset = true;
		return reader;
	}

	/**
	 * @see java.sql.Clob#setCharacterStream(long)
	 */
	public Writer setCharacterStream(long pos) throws SQLException {
		excep(); return null;
	}

	/**
	 * @see java.sql.Clob#getSubString(long, int)
	 */
	public String getSubString(long pos, int len) throws SQLException {
		excep(); return null;
	}

	/**
	 * @see java.sql.Clob#setString(long, String)
	 */
	public int setString(long pos, String string) throws SQLException {
		excep(); return 0;
	}

	/**
	 * @see java.sql.Clob#setString(long, String, int, int)
	 */
	public int setString(long pos, String string, int i, int j)
	throws SQLException {
		excep(); return 0;
	}

	/**
	 * @see java.sql.Clob#position(String, long)
	 */
	public long position(String string, long pos) throws SQLException {
		excep(); return 0;
	}

	/**
	 * @see java.sql.Clob#position(Clob, long)
	 */
	public long position(Clob colb, long pos) throws SQLException {
		excep(); return 0;
	}


	private static void excep() {
		throw new UnsupportedOperationException("Blob may not be manipulated from creating session");
	}

}






