/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.support.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.sql.Clob;
import java.sql.SQLException;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.jdbc.ReaderInputStream;
import org.hibernate.internal.util.StringHelper;

/**
 * @author Steve Ebersole
 */
public class LobHelper {
	public static Clob createClob(char base, int times) {
		return new ClobImpl( StringHelper.repeat( base, times ) );
	}

	private static class ClobImpl implements Clob {
		private String data;

		public ClobImpl(String data) {
			this.data = data;
		}

		@Override
		public long length() {
			return data.length();
		}

		@Override
		public String getSubString(long pos, int length) {
			return data.substring( (int) pos, length );
		}

		@Override
		public Reader getCharacterStream() {
			return new StringReader( data );
		}

		@Override
		public InputStream getAsciiStream() {
			return new ReaderInputStream( new StringReader( data ) );
		}

		@Override
		public long position(String searchstr, long start) {
			return data.indexOf( searchstr, (int) start );
		}

		@Override
		public long position(Clob searchstr, long start) throws SQLException {
			return data.indexOf( searchstr.getSubString( 0, (int) searchstr.length() ), (int) start );
		}

		@Override
		public int setString(long pos, String str) {
			throw new NotYetImplementedFor6Exception();
		}

		@Override
		public int setString(long pos, String str, int offset, int len) {
			throw new NotYetImplementedFor6Exception();
		}

		@Override
		public OutputStream setAsciiStream(long pos) {
			throw new NotYetImplementedFor6Exception();
		}

		@Override
		public Writer setCharacterStream(long pos) {
			throw new NotYetImplementedFor6Exception();
		}

		@Override
		public void truncate(long len) {
			if ( len >= data.length() ) {
				return;
			}

			data = data.substring( 0, (int) len );
		}

		@Override
		public void free() {

		}

		@Override
		public Reader getCharacterStream(long pos, long length) {
			return null;
		}
	}

	private LobHelper() {
	}
}
