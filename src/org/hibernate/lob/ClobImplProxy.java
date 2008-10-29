//$Id: $
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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.sql.Clob;
import java.io.InputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * A proxy for a dummy implementation of <tt>java.sql.Clob</tt> that
 * is used to insert new data into a Clob when the Connection is not used
 * for creating new Clobs.
 *
 * This implementation provides minimal functionality for creating a
 * new Clob. The only operations that are supported are {@link #length()},
 * {@link #getAsciiStream()}, and {@link #getCharacterStream()}. All other
 * operations will throw UnsupportedOperationException.
 *
 * The proxy extends ClobImpl so that it can be distinguished from a Clob.
 *
 *  @author Gail Badner
 */
public class ClobImplProxy implements InvocationHandler {

	private static final Class[] PROXY_INTERFACES = new Class[] { ClobImpl.class };

	private Reader reader;
	private int length;
	private boolean needsReset = false;

	/**
	 * Generates a BlobImpl proxy using a String.
	 *
	 * @param string The data to be created as a Clob.
	 * @return The generated proxy.
	 */
	public static Clob generateProxy(String string) {
		return ( Clob ) Proxy.newProxyInstance(
				getProxyClassLoader(),
				PROXY_INTERFACES,
				new ClobImplProxy( string )
		);
	}

	/**
	 * Generates a ClobImpl proxy using a given number of characters from a Reader.
	 *
	 * @param reader The Reader for character data to be created as a Clob.
	 * @param length The number of characters from Reader to be written to the Clob.
	 * @return The generated proxy.
	 */
	public static Clob generateProxy(Reader reader, int length) {
		return ( Clob ) Proxy.newProxyInstance(
				getProxyClassLoader(),
				PROXY_INTERFACES,
				new ClobImplProxy( reader, length )
		);
	}

	private ClobImplProxy(String string) {
		reader = new StringReader(string);
		length = string.length();
	}

	private ClobImplProxy(Reader reader, int length) {
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
	 * @see java.sql.Clob#getAsciiStream()
	 */
	public InputStream getAsciiStream() throws SQLException {
		reset();
		return new ReaderInputStream(reader);
	}

	/**
	 * @see java.sql.Clob#getCharacterStream()
	 */
	public Reader getCharacterStream() throws SQLException {
		reset();
		return reader;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @throws UnsupportedOperationException if any methods other than {@link #length()},
	 * {@link #getAsciiStream()}, or {@link #getCharacterStream()} are invoked.
	 */
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if ( "length".equals( method.getName() ) ) {
			return new Long( length() );
		}
		if ( "getAsciiStream".equals( method.getName() ) ) {
			return getAsciiStream();
		}
		if ( "getCharacterStream".equals( method.getName() ) ) {
			return getCharacterStream();
		}
		throw new UnsupportedOperationException("Clob may not be manipulated from creating session");
	}

	/**
	 * Determines the appropriate class loader to which the generated proxy
	 * should be scoped.
	 *
	 * @return The class loader appropriate for proxy construction.
	 */
	public static ClassLoader getProxyClassLoader() {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		if ( cl == null ) {
			cl = ClobImpl.class.getClassLoader();
		}
		return cl;
	}


	private void reset() throws SQLException {
		try {
			if (needsReset) reader.reset();
		}
		catch (IOException ioe) {
			throw new SQLException("could not reset reader");
		}
		needsReset = true;
	}
}