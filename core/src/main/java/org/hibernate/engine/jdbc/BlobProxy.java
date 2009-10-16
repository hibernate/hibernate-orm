/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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
package org.hibernate.engine.jdbc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Blob;
import java.sql.SQLException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Manages aspects of proxying {@link Blob Blobs} for non-contextual creation, including proxy creation and
 * handling proxy invocations.
 *
 * @author Gavin King
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class BlobProxy implements InvocationHandler {
	private static final Class[] PROXY_INTERFACES = new Class[] { Blob.class, BlobImplementer.class };

	private InputStream stream;
	private long length;
	private boolean needsReset = false;

	/**
	 * Ctor used to build {@link Blob} from byte array.
	 *
	 * @param bytes The byte array
	 * @see #generateProxy(byte[])
	 */
	private BlobProxy(byte[] bytes) {
		this.stream = new ByteArrayInputStream( bytes );
		this.length = bytes.length;
	}

	/**
	 * Ctor used to build {@link Blob} from a stream.
	 *
	 * @param stream The binary stream
	 * @param length The length of the stream
	 * @see #generateProxy(java.io.InputStream, long)
	 */
	private BlobProxy(InputStream stream, long length) {
		this.stream = stream;
		this.length = length;
	}

	private long getLength() {
		return length;
	}

	private InputStream getStream() throws SQLException {
		try {
			if (needsReset) {
				stream.reset();
			}
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
	 * @throws UnsupportedOperationException if any methods other than {@link Blob#length()}
	 * or {@link Blob#getBinaryStream} are invoked.
	 */
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if ( "length".equals( method.getName() ) ) {
			return new Long( getLength() );
		}
		if ( "getBinaryStream".equals( method.getName() ) && method.getParameterTypes().length == 0 ) {
			return getStream();
		}
		if ( "free".equals( method.getName() ) ) {
			stream.close();
			return null;
		}
		if ( "toString".equals( method.getName() ) ) {
			return this.toString();
		}
		if ( "equals".equals( method.getName() ) ) {
			return Boolean.valueOf( proxy == args[0] );
		}
		if ( "hashCode".equals( method.getName() ) ) {
			return new Integer( this.hashCode() );
		}
		throw new UnsupportedOperationException( "Blob may not be manipulated from creating session" );
	}

	/**
	 * Generates a BlobImpl proxy using byte data.
	 *
	 * @param bytes The data to be created as a Blob.
	 *
	 * @return The generated proxy.
	 */
	public static Blob generateProxy(byte[] bytes) {
		return ( Blob ) Proxy.newProxyInstance(
				getProxyClassLoader(),
				PROXY_INTERFACES,
				new BlobProxy( bytes )
		);
	}

	/**
	 * Generates a BlobImpl proxy using a given number of bytes from an InputStream.
	 *
	 * @param stream The input stream of bytes to be created as a Blob.
	 * @param length The number of bytes from stream to be written to the Blob.
	 *
	 * @return The generated proxy.
	 */
	public static Blob generateProxy(InputStream stream, long length) {
		return ( Blob ) Proxy.newProxyInstance(
				getProxyClassLoader(),
				PROXY_INTERFACES,
				new BlobProxy( stream, length )
		);
	}

	/**
	 * Determines the appropriate class loader to which the generated proxy
	 * should be scoped.
	 *
	 * @return The class loader appropriate for proxy construction.
	 */
	private static ClassLoader getProxyClassLoader() {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		if ( cl == null ) {
			cl = BlobImplementer.class.getClassLoader();
		}
		return cl;
	}
}
