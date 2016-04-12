/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.sql.Blob;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.annotations.Immutable;
import org.hibernate.engine.jdbc.BinaryStream;
import org.hibernate.engine.jdbc.internal.BinaryStreamImpl;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.type.descriptor.WrapperOptions;

/**
 * Descriptor for general {@link Serializable} handling.
 *
 * @author Steve Ebersole
 * @author Brett meyer
 */
public class SerializableTypeDescriptor<T extends Serializable> extends AbstractTypeDescriptor<T> {

	// unfortunately the param types cannot be the same so use something other than 'T' here to make that obvious
	public static class SerializableMutabilityPlan<S extends Serializable> extends MutableMutabilityPlan<S> {

		public static final SerializableMutabilityPlan<Serializable> INSTANCE
				= new SerializableMutabilityPlan<Serializable>( );

		public SerializableMutabilityPlan() {
		}

		@Override
		@SuppressWarnings({ "unchecked" })
		public S deepCopyNotNull(S value) {
			return (S) SerializationHelper.clone( value );
		}

	}

	public SerializableTypeDescriptor(Class<T> type) {
		super( type, createMutabilityPlan( type ) );
	}

	@SuppressWarnings({ "unchecked" })
	private static <T> MutabilityPlan<T> createMutabilityPlan(Class<T> type) {
		if ( type.isAnnotationPresent( Immutable.class ) ) {
			return ImmutableMutabilityPlan.INSTANCE;
		}
		return (MutabilityPlan<T>) SerializableMutabilityPlan.INSTANCE;
	}

	public String toString(T value) {
		return PrimitiveByteArrayTypeDescriptor.INSTANCE.toString( toBytes( value ) );
	}

	public T fromString(String string) {
		return fromBytes( PrimitiveByteArrayTypeDescriptor.INSTANCE.fromString( string ) );
	}

	@Override
	public boolean areEqual(T one, T another) {
		if ( one == another ) {
			return true;
		}
		if ( one == null || another == null ) {
			return false;
		}
		return one.equals( another )
				|| PrimitiveByteArrayTypeDescriptor.INSTANCE.areEqual( toBytes( one ), toBytes( another ) );
	}

	@Override
	public int extractHashCode(T value) {
		return PrimitiveByteArrayTypeDescriptor.INSTANCE.extractHashCode( toBytes( value ) );
	}

	@SuppressWarnings({ "unchecked" })
	public <X> X unwrap(T value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		else if ( type.isInstance( value ) ) {
			return (X) value;
		}
		else if ( byte[].class.isAssignableFrom( type ) ) {
			return (X) toBytes( value );
		}
		else if ( InputStream.class.isAssignableFrom( type ) ) {
			return (X) new ByteArrayInputStream( toBytes( value ) );
		}
		else if ( BinaryStream.class.isAssignableFrom( type ) ) {
			return (X) new BinaryStreamImpl( toBytes( value ) );
		}
		else if ( Blob.class.isAssignableFrom( type ) ) {
			return (X) options.getLobCreator().createBlob( toBytes( value ) );
		}

		throw unknownUnwrap( type );
	}

	@SuppressWarnings("unchecked")
	public <X> T wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		else if ( byte[].class.isInstance( value ) ) {
			return fromBytes( (byte[]) value );
		}
		else if ( InputStream.class.isInstance( value ) ) {
			return fromBytes( DataHelper.extractBytes( (InputStream) value ) );
		}
		else if ( Blob.class.isInstance( value ) ) {
			try {
				return fromBytes( DataHelper.extractBytes( ((Blob) value).getBinaryStream() ) );
			}
			catch ( SQLException e ) {
				throw new HibernateException( e );
			}
		}
		else if ( getJavaTypeClass().isInstance( value ) ) {
			return (T) value;
		}
		throw unknownWrap( value.getClass() );
	}

	protected byte[] toBytes(T value) {
		return SerializationHelper.serialize( value );
	}

	@SuppressWarnings({ "unchecked" })
	protected T fromBytes(byte[] bytes) {
		return (T) SerializationHelper.deserialize( bytes, getJavaTypeClass().getClassLoader() );
	}
}
