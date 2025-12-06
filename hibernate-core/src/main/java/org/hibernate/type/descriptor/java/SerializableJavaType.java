/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.sql.Blob;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;

import org.hibernate.HibernateException;
import org.hibernate.annotations.Immutable;
import org.hibernate.engine.jdbc.BinaryStream;
import org.hibernate.engine.jdbc.internal.ArrayBackedBinaryStream;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

/**
 * Descriptor for general {@link Serializable} handling.
 *
 * @author Steve Ebersole
 * @author Brett meyer
 */
public class SerializableJavaType<T extends Serializable> extends AbstractClassJavaType<T> {

	// unfortunately, the param types cannot be the same so use something other than 'T' here to make that obvious
	public static class SerializableMutabilityPlan<S extends Serializable> extends MutableMutabilityPlan<S> {
		public static final SerializableMutabilityPlan<Serializable> INSTANCE = new SerializableMutabilityPlan<>();

		private SerializableMutabilityPlan() {
		}

		@Override
		@SuppressWarnings("unchecked")
		public S deepCopyNotNull(S value) {
			return (S) SerializationHelper.clone( value );
		}

	}

	public SerializableJavaType(Class<T> type) {
		this( type, createMutabilityPlan( type ) );
	}

	public SerializableJavaType(Class<T> type, MutabilityPlan<T> mutabilityPlan) {
		super( type, mutabilityPlan == null ? createMutabilityPlan( type ) : mutabilityPlan );
	}

	private static <T> MutabilityPlan<T> createMutabilityPlan(Class<T> type) {
		return type.isAnnotationPresent( Immutable.class )
				? ImmutableMutabilityPlan.instance()
				: (MutabilityPlan<T>) SerializableMutabilityPlan.INSTANCE;
	}

	@Override
	public boolean isInstance(Object value) {
		return value instanceof Serializable;
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators indicators) {
		final int typeCode = indicators.isLob() ? Types.BLOB : Types.VARBINARY;
		return indicators.getJdbcType( typeCode );
	}

	public String toString(T value) {
		return PrimitiveByteArrayJavaType.INSTANCE.toString( toBytes( value ) );
	}

	public T fromString(CharSequence string) {
		return fromBytes( PrimitiveByteArrayJavaType.INSTANCE.fromString( string ) );
	}

	@Override
	public boolean areEqual(T one, T another) {
		if ( one == another ) {
			return true;
		}
		else if ( one == null || another == null ) {
			return false;
		}
		else {
			return one.equals( another )
				|| Arrays.equals( toBytes( one ), toBytes( another ) );
		}
	}

	@Override
	public int extractHashCode(T value) {
		return PrimitiveByteArrayJavaType.INSTANCE.extractHashCode( toBytes( value ) );
	}

	public <X> X unwrap(T value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		else if ( type.isInstance( value ) ) {
			return type.cast( value );
		}
		else if ( byte[].class.isAssignableFrom( type ) ) {
			return type.cast( toBytes( value ) );
		}
		else if ( InputStream.class.isAssignableFrom( type ) ) {
			return type.cast( new ByteArrayInputStream( toBytes( value ) ) );
		}
		else if ( BinaryStream.class.isAssignableFrom( type ) ) {
			return type.cast( new ArrayBackedBinaryStream( toBytes( value ) ) );
		}
		else if ( Blob.class.isAssignableFrom( type ) ) {
			return type.cast( options.getLobCreator().createBlob( toBytes( value ) ) );
		}

		throw unknownUnwrap( type );
	}

	public <X> T wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		else if (value instanceof byte[] bytes) {
			return fromBytes( bytes );
		}
		else if (value instanceof InputStream inputStream) {
			return fromBytes( DataHelper.extractBytes( inputStream ) );
		}
		else if (value instanceof Blob blob) {
			try {
				return fromBytes( DataHelper.extractBytes( blob.getBinaryStream() ) );
			}
			catch ( SQLException e ) {
				throw new HibernateException( e );
			}
		}
		else if ( getJavaTypeClass().isInstance( value ) ) {
			return cast( value );
		}
		throw unknownWrap( value.getClass() );
	}

	protected byte[] toBytes(T value) {
		return SerializationHelper.serialize( value );
	}

	protected T fromBytes(byte[] bytes) {
		return cast( SerializationHelper.deserialize( bytes, getJavaTypeClass().getClassLoader() ) );
	}
}
