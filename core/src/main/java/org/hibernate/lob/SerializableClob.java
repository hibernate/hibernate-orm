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
