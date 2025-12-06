/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.sql.Blob;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.SharedSessionContract;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.BinaryStream;
import org.hibernate.engine.jdbc.BlobImplementer;
import org.hibernate.engine.jdbc.proxy.BlobProxy;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.internal.StreamBackedBinaryStream;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import static org.hibernate.type.descriptor.java.DataHelper.extractBytes;

/**
 * Descriptor for {@link Blob} handling.
 * <p>
 * Note, {@link Blob}s really are mutable (their internal state can in fact be mutated).  We simply
 * treat them as immutable because we cannot properly check them for changes nor deep copy them.
 *
 * @author Steve Ebersole
 * @author Brett Meyer
 * @author Loïc Lefèvre
 */
public class BlobJavaType extends AbstractClassJavaType<Blob> {
	public static final BlobJavaType INSTANCE = new BlobJavaType();

	public static class BlobMutabilityPlan implements MutabilityPlan<Blob> {
		public static final BlobMutabilityPlan INSTANCE = new BlobMutabilityPlan();

		@Override
		public boolean isMutable() {
			return false;
		}

		@Override
		public Blob deepCopy(Blob value) {
			return value;
		}

		@Override
		public Serializable disassemble(Blob value, SharedSessionContract session) {
			throw new UnsupportedOperationException( "Blobs are not cacheable" );
		}

		@Override
		public Blob assemble(Serializable cached, SharedSessionContract session) {
			throw new UnsupportedOperationException( "Blobs are not cacheable" );
		}
	}

	public BlobJavaType() {
		super( Blob.class, BlobMutabilityPlan.INSTANCE, IncomparableComparator.INSTANCE );
	}

	@Override
	public boolean isInstance(Object value) {
		return value instanceof Blob;
	}

	@Override
	public Blob cast(Object value) {
		return (Blob) value;
	}

	@Override
	public String extractLoggableRepresentation(Blob value) {
		return value == null ? "null" : "{blob}";
	}

	@Override
	public String toString(Blob value) {
		final byte[] bytes;
		try {
			bytes = extractBytes( value.getBinaryStream() );
		}
		catch ( SQLException e ) {
			throw new HibernateException( "Unable to access blob stream", e );
		}
		return PrimitiveByteArrayJavaType.INSTANCE.toString( bytes );
	}

	@Override
	public Blob fromString(CharSequence string) {
		return BlobProxy.generateProxy( PrimitiveByteArrayJavaType.INSTANCE.fromString( string ) );
	}

	@Override
	public int extractHashCode(Blob value) {
		return System.identityHashCode( value );
	}

	@Override
	public boolean areEqual(Blob one, Blob another) {
		return one == another;
	}

	@Override
	public Blob getReplacement(Blob original, Blob target, SharedSessionContractImplementor session) {
		return session.getJdbcServices().getJdbcEnvironment().getDialect().getLobMergeStrategy()
				.mergeBlob( original, target, session );
	}

	@Override
	public <X> X unwrap(Blob value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		try {
			if ( Blob.class.isAssignableFrom( type ) ) {
				return type.cast( options.getLobCreator().toJdbcBlob( value ) );
			}
			else if ( byte[].class.isAssignableFrom( type )) {
				if (value instanceof BlobImplementer blobImplementer) {
					// if the incoming Blob is a wrapper, just grab the bytes from its BinaryStream
					return type.cast( blobImplementer.getUnderlyingStream().getBytes() );
				}
				else {
					try {
						// otherwise extract the bytes from the stream manually
						return type.cast( value.getBinaryStream().readAllBytes() );
					}
					catch ( IOException e ) {
						throw new HibernateException( "IOException occurred reading a binary value", e );
					}
				}
			}
			else if ( BinaryStream.class.isAssignableFrom( type ) ) {
				if (value instanceof BlobImplementer blobImplementer) {
					return type.cast( blobImplementer.getUnderlyingStream() );
				}
				else {
					return type.cast( new StreamBackedBinaryStream( value.getBinaryStream(), value.length() ) );
				}
			}
			else if ( InputStream.class.isAssignableFrom( type ) ) {
				if (value instanceof BlobImplementer blobImplementer) {
					// if the incoming Blob is a wrapper, just pass along its BinaryStream
					return type.cast( blobImplementer.getUnderlyingStream().getInputStream() );
				}
				else {
					// otherwise we need to build a BinaryStream...
					return type.cast( value.getBinaryStream() );
				}
			}
		}
		catch ( SQLException e ) {
			throw new HibernateException( "Unable to access blob stream", e );
		}

		throw unknownUnwrap( type );
	}

	@Override
	public <X> Blob wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		else {
			final LobCreator lobCreator = options.getLobCreator();
			if ( value instanceof Blob blob ) {
				return lobCreator.wrap( blob );
			}
			else if ( value instanceof byte[] bytes ) {
				return lobCreator.createBlob( bytes );
			}
			else if ( value instanceof BinaryStream binaryStream) {
				return binaryStream.asBlob( lobCreator );
			}
			else if ( value instanceof InputStream inputStream ) {
				// A JDBC Blob object needs to know its length, but
				// there's no way to get an accurate length from an
				// InputStream without reading the whole stream
				return lobCreator.createBlob( extractBytes( inputStream ) );
			}
			else {
				throw unknownWrap( value.getClass() );
			}
		}
	}

	@Override
	public long getDefaultSqlLength(Dialect dialect, JdbcType jdbcType) {
		return dialect.getDefaultLobLength();
	}
}
