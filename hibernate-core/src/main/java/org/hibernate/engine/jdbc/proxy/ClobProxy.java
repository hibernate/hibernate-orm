/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.sql.Clob;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.Internal;
import org.hibernate.engine.jdbc.CharacterStream;
import org.hibernate.engine.jdbc.ClobImplementer;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.internal.util.ReaderInputStream;
import org.hibernate.engine.jdbc.internal.CharacterStreamImpl;
import org.hibernate.type.descriptor.java.DataHelper;

/**
 * Manages aspects of proxying {@link Clob}s for non-contextual creation, including proxy creation and
 * handling proxy invocations.  We use proxies here solely to avoid JDBC version incompatibilities.
 *
 * @apiNote This class is not intended to be called directly by the application program.
 *          Instead, use {@link org.hibernate.Hibernate#getLobHelper()}.
 *
 * @see NClobProxy
 * @see BlobProxy
 * @see LobCreator
 * @see org.hibernate.LobHelper
 *
 * @author Gavin King
 * @author Steve Ebersole
 * @author Gail Badner
 */
@Internal
public class ClobProxy implements Clob, ClobImplementer {

	private final CharacterStream characterStream;
	private boolean needsReset;

	/**
	 * Constructor used to build {@link Clob} from string data.
	 *
	 * @param string The byte array
	 * @see #generateProxy(String)
	 */
	protected ClobProxy(String string) {
		this.characterStream = new CharacterStreamImpl( string );
	}

	/**
	 * Constructor used to build {@link Clob} from a reader.
	 *
	 * @param reader The character reader.
	 * @param length The length of the reader stream.
	 * @see #generateProxy(Reader, long)
	 */
	protected ClobProxy(Reader reader, long length) {
		this.characterStream = new CharacterStreamImpl( reader, length );
	}

	@Override
	public long length() {
		return characterStream.getLength();
	}

	@Override
	public InputStream getAsciiStream() throws SQLException {
		return new ReaderInputStream( getCharacterStream() );
	}

	@Override
	public Reader getCharacterStream() throws SQLException {
		return getUnderlyingStream().asReader();
	}

	@Override
	public CharacterStream getUnderlyingStream() {
		resetIfNeeded();
		return characterStream;
	}

	@Override
	public long position(String searchstr, long start) throws SQLException {
		return characterStream.asString().indexOf( searchstr, (int) start + 1 );
	}

	@Override
	public long position(Clob searchstr, long start) throws SQLException {
		throw notSupported();
	}

	@Override
	public int setString(long pos, String str) throws SQLException {
		throw notSupported();
	}

	@Override
	public int setString(long pos, String str, int offset, int len) throws SQLException {
		throw notSupported();
	}

	@Override
	public OutputStream setAsciiStream(long pos) {
		throw notSupported();
	}

	@Override
	public Writer setCharacterStream(long pos) {
		throw notSupported();
	}

	@Override
	public void truncate(long len) throws SQLException {
		throw notSupported();
	}

	@Override
	public String getSubString(long start, int length) throws SQLException {
		if ( start < 1 ) {
			throw new SQLException( "Start position 1-based; must be 1 or more." );
		}
		if ( start > length() + 1 ) {
			throw new SQLException( "Start position [" + start + "] cannot exceed overall CLOB length [" + length() + "]" );
		}
		if ( length < 0 ) {
			throw new SQLException( "Length must be great-than-or-equal to zero." );
		}
		final String string = characterStream.asString();
		final long endIndex = Math.min( start + length - 1, string.length() );
		return string.substring( (int) start - 1, (int) endIndex );
	}

	@Override
	public Reader getCharacterStream(long start, long length) throws SQLException {
		if ( start < 1 ) {
			throw new SQLException( "Start position 1-based; must be 1 or more." );
		}
		if ( start > length() + 1 ) {
			throw new SQLException( "Start position [" + start + "] cannot exceed overall CLOB length [" + length() + "]" );
		}
		if ( length > Integer.MAX_VALUE ) {
			throw new SQLException( "Can't deal with Clobs larger than 'Integer.MAX_VALUE'" );
		}
		if ( length < 0 ) {
			// javadoc for getCharacterStream(long,int) specifies that the start+length must not exceed the
			// total length (this is at odds with the behavior of getSubString(long,int))
			throw new SQLException( "Length must be greater than or equal to zero" );
		}
		return DataHelper.subStream( getCharacterStream(), start-1, (int) length );
	}

	@Override
	public void free() throws SQLException {
		characterStream.release();
	}

	protected void resetIfNeeded() {
		try {
			if ( needsReset ) {
				characterStream.asReader().reset();
			}
		}
		catch ( IOException ioe ) {
			throw new HibernateException( "could not reset reader", ioe );
		}
		needsReset = true;
	}

	/**
	 * Generates a {@link Clob} proxy using the string data.
	 *
	 * @param string The data to be wrapped as a {@link Clob}.
	 *
	 * @return The generated proxy.
	 */
	public static Clob generateProxy(String string) {
		return new ClobProxy( string );
	}

	/**
	 * Generates a {@link Clob} proxy using a character reader of given length.
	 *
	 * @param reader The character reader
	 * @param length The length of the character reader
	 *
	 * @return The generated proxy.
	 */
	public static Clob generateProxy(Reader reader, long length) {
		return new ClobProxy( reader, length );
	}

	private static UnsupportedOperationException notSupported() {
		return new UnsupportedOperationException("Clob may not be manipulated from creating session");
	}
}
