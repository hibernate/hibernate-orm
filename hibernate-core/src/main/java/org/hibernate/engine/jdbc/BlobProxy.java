/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.SQLException;

import org.hibernate.engine.jdbc.internal.BinaryStreamImpl;
import org.hibernate.type.descriptor.java.DataHelper;

/**
 * Manages aspects of representing {@link Blob} objects.
 *
 * In previous versions this used to be implemented by using a java.lang.reflect.Proxy to deal with
 * incompatibilities across various JDBC versions, hence the class name, but using a real Proxy is no longer necessary.
 *
 * The class name could be updated to reflect this but that would break APIs, so this operation is deferred.
 *
 * @author Gavin King
 * @author Steve Ebersole
 * @author Gail Badner
 * @author Sanne Grinovero
 */
public final class BlobProxy implements Blob, BlobImplementer {

	private final BinaryStream binaryStream;
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


	private InputStream getStream() throws SQLException {
		return getUnderlyingStream().getInputStream();
	}

	public BinaryStream getUnderlyingStream() throws SQLException {
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
	 * Generates a BlobImpl using byte data.
	 *
	 * @param bytes The data to be created as a Blob.
	 *
	 * @return The BlobProxy instance to represent this data.
	 */
	public static Blob generateProxy(byte[] bytes) {
		return new BlobProxy( bytes );
	}

	/**
	 * Generates a BlobImpl proxy using a given number of bytes from an InputStream.
	 *
	 * @param stream The input stream of bytes to be created as a Blob.
	 * @param length The number of bytes from stream to be written to the Blob.
	 *
	 * @return The BlobProxy instance to represent this data.
	 */
	public static Blob generateProxy(InputStream stream, long length) {
		return new BlobProxy( stream, length );
	}

	@Override
	public long length() throws SQLException {
		return binaryStream.getLength();
	}

	@Override
	public byte[] getBytes(final long start, final int length) throws SQLException {
		if ( start < 1 ) {
			throw new SQLException( "Start position 1-based; must be 1 or more." );
		}
		if ( length < 0 ) {
			throw new SQLException( "Length must be great-than-or-equal to zero." );
		}
		return DataHelper.extractBytes( getStream(), start-1, length );
	}

	@Override
	public InputStream getBinaryStream() throws SQLException {
		return getStream();
	}

	@Override
	public long position(byte[] pattern, long start) {
		throw notSupported();
	}

	@Override
	public long position(Blob pattern, long start) {
		throw notSupported();
	}

	@Override
	public int setBytes(long pos, byte[] bytes) {
		throw notSupported();
	}

	@Override
	public int setBytes(long pos, byte[] bytes, int offset, int len) {
		throw notSupported();
	}

	@Override
	public OutputStream setBinaryStream(long pos) {
		throw notSupported();
	}

	@Override
	public void truncate(long len) {
		throw notSupported();
	}

	@Override
	public void free() {
		binaryStream.release();
	}

	@Override
	public InputStream getBinaryStream(final long start, final long length) throws SQLException {
		if ( start < 1 ) {
			throw new SQLException( "Start position 1-based; must be 1 or more." );
		}
		if ( start > length() ) {
			throw new SQLException( "Start position [" + start + "] cannot exceed overall CLOB length [" + length() + "]" );
		}
		if ( length > Integer.MAX_VALUE ) {
			throw new SQLException( "Can't deal with Blobs larger than Integer.MAX_VALUE" );
		}
		final int intLength = (int)length;
		if ( intLength < 0 ) {
			// java docs specifically say for getBinaryStream(long,int) that the start+length must not exceed the
			// total length, however that is at odds with the getBytes(long,int) behavior.
			throw new SQLException( "Length must be great-than-or-equal to zero." );
		}
		return DataHelper.subStream( getStream(), start-1, intLength );
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

	private static UnsupportedOperationException notSupported() {
		return new UnsupportedOperationException( "Blob may not be manipulated from creating session" );
	}

}
