/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java.internal;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.sql.Blob;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.BinaryStream;
import org.hibernate.engine.jdbc.internal.BinaryStreamImpl;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.type.descriptor.java.spi.AbstractBasicJavaDescriptor;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.spi.MutableMutabilityPlan;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * Descriptor for general {@link Serializable} handling.
 *
 * @author Steve Ebersole
 * @author Brett meyer
 */
public class SerializableJavaDescriptor<T extends Serializable> extends AbstractBasicJavaDescriptor<T> {
	public static final SerializableJavaDescriptor<Serializable> INSTANCE = new SerializableJavaDescriptor<>( Serializable.class );

	// unfortunately the param types cannot be the same so use something other than 'T' here to make that obvious
	public static class SerializableMutabilityPlan<S extends Serializable> extends MutableMutabilityPlan<S> {
		private final Class<S> type;

		public static final SerializableMutabilityPlan<Serializable> INSTANCE = new SerializableMutabilityPlan<>( Serializable.class );

		public SerializableMutabilityPlan(Class<S> type) {
			this.type = type;
		}

		@Override
		@SuppressWarnings({ "unchecked" })
		public S deepCopyNotNull(S value) {
			return (S) SerializationHelper.clone( value );
		}

	}

	@SuppressWarnings({ "unchecked" })
	public SerializableJavaDescriptor(Class<T> type) {
		super(
				type,
				Serializable.class.equals( type )
						? (MutabilityPlan<T>) SerializableMutabilityPlan.INSTANCE
						: new SerializableMutabilityPlan<T>( type )
		);
	}

	public String toString(T value) {
		return PrimitiveByteArrayJavaDescriptor.INSTANCE.toString( toBytes( value ) );
	}

	public T fromString(String string) {
		return fromBytes( PrimitiveByteArrayJavaDescriptor.INSTANCE.fromString( string ) );
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
				|| PrimitiveByteArrayJavaDescriptor.INSTANCE.areEqual( toBytes( one ), toBytes( another ) );
	}

	@Override
	public SqlTypeDescriptor getJdbcRecommendedSqlType(JdbcRecommendedSqlTypeMappingContext context) {
		return context.getTypeConfiguration().getSqlTypeDescriptorRegistry().getDescriptor( Types.VARBINARY );
	}

	@Override
	public int extractHashCode(T value) {
		return PrimitiveByteArrayJavaDescriptor.INSTANCE.extractHashCode( toBytes( value ) );
	}

	@SuppressWarnings({ "unchecked" })
	public <X> X unwrap(T value, Class<X> type, SharedSessionContractImplementor session) {
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
		else if ( Blob.class.isAssignableFrom( type )) {
			return (X) session.getLobCreator().createBlob( toBytes( value) );
		}
		
		throw unknownUnwrap( type );
	}

	@SuppressWarnings("unchecked")
	public <X> T wrap(X value, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}
		else if ( byte[].class.isInstance( value ) ) {
			return fromBytes( (byte[]) value );
		}
		else if ( InputStream.class.isInstance( value ) ) {
			return fromBytes( LobStreamDataHelper.extractBytes( (InputStream) value ) );
		}
		else if ( Blob.class.isInstance( value )) {
			try {
				return fromBytes( LobStreamDataHelper.extractBytes( ( (Blob) value ).getBinaryStream() ) );
			}
			catch ( SQLException e ) {
				throw new HibernateException( e);
			}
		}
		else if ( getJavaType() == null && getJavaType().isInstance( value ) ) {
			return (T) value;
		}
		throw unknownWrap( value.getClass() );
	}

	protected byte[] toBytes(T value) {
		return SerializationHelper.serialize( value );
	}

	@SuppressWarnings({ "unchecked" })
	protected T fromBytes(byte[] bytes) {
		if ( getJavaType() == null ) {
			throw new IllegalStateException( "Cannot read bytes for Serializable type" );
		}

		return (T) SerializationHelper.deserialize( bytes, getJavaType().getClassLoader() );
	}
}
