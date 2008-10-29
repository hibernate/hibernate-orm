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
import java.sql.Blob;
import java.sql.SQLException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * A proxy for a dummy implementation of <tt>java.sql.Blob</tt> that
 * is used to insert new data into a Blob when the Connection is not used
 * for creating new Blobs.
 *
 * This implementation provides minimal functionality for creating a
 * new Blob. The only operations that are supported are {@link #length()}
 * and {@link #getBinaryStream()}. All other operations will throw
 * UnsupportedOperationException.
 *
 * The proxy extends BlobImpl so that it can be distinguished from a Blob.
 *
 * @author Gail Badner
 */
public class BlobImplProxy implements InvocationHandler {

	private static final Class[] PROXY_INTERFACES = new Class[] { BlobImpl.class };

	private InputStream stream;
	private int length;
	private boolean needsReset = false;

	/**
	 * Generates a BlobImpl proxy using byte data.
	 *
	 * @param bytes The data to be created as a Blob.
	 * @return The generated proxy.
	 */
	public static Blob generateProxy(byte[] bytes) {
		return ( Blob ) Proxy.newProxyInstance(
				getProxyClassLoader(),
				PROXY_INTERFACES,
				new BlobImplProxy( bytes )
		);
	}

	/**
	 * Generates a BlobImpl proxy using a given number of bytes from an InputStream.
	 *
	 * @param stream The input stream of bytes to be created as a Blob.
	 * @param length The number of bytes from stream to be written to the Blob.
	 * @return The generated proxy.
	 */
	public static Blob generateProxy(InputStream stream, int length) {
		return ( Blob ) Proxy.newProxyInstance(
				getProxyClassLoader(),
				PROXY_INTERFACES,
				new BlobImplProxy( stream, length )
		);
	}

	private BlobImplProxy(byte[] bytes) {
		this.stream = new ByteArrayInputStream(bytes);
		this.length = bytes.length;
	}

	private BlobImplProxy(InputStream stream, int length) {
		this.stream = stream;
		this.length = length;
	}

	/**
	 * @see java.sql.Blob#length()
	 */
	public long length() throws SQLException {
		return length;
	}

	/**
	 * @see java.sql.Blob#getBinaryStream()
	 */
	public InputStream getBinaryStream() throws SQLException {
		try {
			if (needsReset) stream.reset();
		}
		catch ( IOException ioe) {
			throw new SQLException("could not reset reader");
		}
		needsReset = true;
		return stream;
	}
	
	/**
	 * {@inheritDoc}
	 *
	 * @throws UnsupportedOperationException if any methods other than {@link #length()}
	 * or {@link #getBinaryStream} are invoked.
	 */
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if ( "length".equals( method.getName() ) ) {
			return new Long( length() );
		}
		if ( "getBinaryStream".equals( method.getName() ) && method.getParameterTypes().length == 0) {
			return getBinaryStream();
		}
		throw new UnsupportedOperationException("Blob may not be manipulated from creating session");
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
			cl = BlobImpl.class.getClassLoader();
		}
		return cl;
	}
}