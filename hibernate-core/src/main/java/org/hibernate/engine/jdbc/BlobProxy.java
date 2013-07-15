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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Blob;
import java.sql.SQLException;

import org.hibernate.engine.jdbc.internal.BinaryStreamImpl;
import org.hibernate.internal.util.ClassLoaderHelper;
import org.hibernate.type.descriptor.java.DataHelper;

/**
 * Manages aspects of proxying {@link Blob} references for non-contextual creation, including proxy creation and
 * handling proxy invocations.  We use proxies here solely to avoid JDBC version incompatibilities.
 *
 * @author Gavin King
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class BlobProxy implements InvocationHandler {
	private static final Class[] PROXY_INTERFACES = new Class[] { Blob.class, BlobImplementer.class };

	private BinaryStream binaryStream;
	private boolean needsReset;

	/**
	 * Constructor used to build {@link Blob} from byte array.
	 *
	 * @param bytes The byte array
	 * @see #generateProxy(byte[])
	 */
	private BlobProxy(byte[] bytes) {
		binaryStream = new BinaryStreamImpl( bytes );
	}

	/**
	 * Constructor used to build {@link Blob} from a stream.
	 *
	 * @param stream The binary stream
	 * @param length The length of the stream
	 * @see #generateProxy(java.io.InputStream, long)
	 */
	private BlobProxy(InputStream stream, long length) {
		this.binaryStream = new StreamBackedBinaryStream( stream, length );
	}

	private long getLength() {
		return binaryStream.getLength();
	}

	private InputStream getStream() throws SQLException {
		return getUnderlyingStream().getInputStream();
	}

	private BinaryStream getUnderlyingStream() throws SQLException {
		resetIfNeeded();
		return binaryStream;
	}

	private void resetIfNeeded() throws SQLException {
		try {
			if ( needsReset ) {
				binaryStream.getInputStream().reset();
			}
		}
		catch ( IOException ioe) {
			throw new SQLException("could not reset reader");
		}
		needsReset = true;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @throws UnsupportedOperationException if any methods other than
	 * {@link Blob#length}, {@link BlobImplementer#getUnderlyingStream},
	 * {@link Blob#getBinaryStream}, {@link Blob#getBytes}, {@link Blob#free},
	 * or toString/equals/hashCode are invoked.
	 */
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		final String methodName = method.getName();
		final int argCount = method.getParameterTypes().length;

		if ( "length".equals( methodName ) && argCount == 0 ) {
			return getLength();
		}
		if ( "getUnderlyingStream".equals( methodName ) ) {
			return getUnderlyingStream(); // Reset stream if needed.
		}
		if ( "getBinaryStream".equals( methodName ) ) {
			if ( argCount == 0 ) {
				return getStream();
			}
			else if ( argCount == 2 ) {
				final long start = (Long) args[0];
				if ( start < 1 ) {
					throw new SQLException( "Start position 1-based; must be 1 or more." );
				}
				if ( start > getLength() ) {
					throw new SQLException( "Start position [" + start + "] cannot exceed overall CLOB length [" + getLength() + "]" );
				}
				final int length = (Integer) args[1];
				if ( length < 0 ) {
					// java docs specifically say for getBinaryStream(long,int) that the start+length must not exceed the
					// total length, however that is at odds with the getBytes(long,int) behavior.
					throw new SQLException( "Length must be great-than-or-equal to zero." );
				}
				return DataHelper.subStream( getStream(), start-1, length );
			}
		}
		if ( "getBytes".equals( methodName ) ) {
			if ( argCount == 2 ) {
				final long start = (Long) args[0];
				if ( start < 1 ) {
					throw new SQLException( "Start position 1-based; must be 1 or more." );
				}
				final int length = (Integer) args[1];
				if ( length < 0 ) {
					throw new SQLException( "Length must be great-than-or-equal to zero." );
				}
				return DataHelper.extractBytes( getStream(), start-1, length );
			}
		}
		if ( "free".equals( methodName ) && argCount == 0 ) {
			binaryStream.release();
			return null;
		}
		if ( "toString".equals( methodName ) && argCount == 0 ) {
			return this.toString();
		}
		if ( "equals".equals( methodName ) && argCount == 1 ) {
			return proxy == args[0];
		}
		if ( "hashCode".equals( methodName ) && argCount == 0 ) {
			return this.hashCode();
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
		return (Blob) Proxy.newProxyInstance( getProxyClassLoader(), PROXY_INTERFACES, new BlobProxy( bytes ) );
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
		return (Blob) Proxy.newProxyInstance( getProxyClassLoader(), PROXY_INTERFACES, new BlobProxy( stream, length ) );
	}

	/**
	 * Determines the appropriate class loader to which the generated proxy
	 * should be scoped.
	 *
	 * @return The class loader appropriate for proxy construction.
	 */
	private static ClassLoader getProxyClassLoader() {
		ClassLoader cl = ClassLoaderHelper.getContextClassLoader();
		if ( cl == null ) {
			cl = BlobImplementer.class.getClassLoader();
		}
		return cl;
	}

	private static class StreamBackedBinaryStream implements BinaryStream {
		private final InputStream stream;
		private final long length;

		private byte[] bytes;

		private StreamBackedBinaryStream(InputStream stream, long length) {
			this.stream = stream;
			this.length = length;
		}

		@Override
		public InputStream getInputStream() {
			return stream;
		}

		@Override
		public byte[] getBytes() {
			if ( bytes == null ) {
				bytes = DataHelper.extractBytes( stream );
			}
			return bytes;
		}

		@Override
		public long getLength() {
			return (int) length;
		}

		@Override
		public void release() {
			try {
				stream.close();
			}
			catch (IOException ignore) {
			}
		}
	}
}
