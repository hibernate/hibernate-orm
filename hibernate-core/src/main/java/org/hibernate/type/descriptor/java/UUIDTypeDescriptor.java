/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java;

import java.io.Serializable;
import java.util.UUID;

import org.hibernate.internal.util.BytesHelper;
import org.hibernate.type.descriptor.WrapperOptions;

/**
 * Descriptor for {@link UUID} handling.
 *
 * @author Steve Ebersole
 */
public class UUIDTypeDescriptor extends AbstractTypeDescriptor<UUID> {
	public static final UUIDTypeDescriptor INSTANCE = new UUIDTypeDescriptor();

	public UUIDTypeDescriptor() {
		super( UUID.class );
	}

	@Override
	public String toString(UUID value) {
		return ToStringTransformer.INSTANCE.transform( value );
	}

	@Override
	public UUID fromString(String string) {
		return ToStringTransformer.INSTANCE.parse( string );
	}

	@SuppressWarnings({ "unchecked" })
	@Override
	public <X> X unwrap(UUID value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( UUID.class.isAssignableFrom( type ) ) {
			return (X) PassThroughTransformer.INSTANCE.transform( value );
		}
		if ( String.class.isAssignableFrom( type ) ) {
			return (X) ToStringTransformer.INSTANCE.transform( value );
		}
		if ( byte[].class.isAssignableFrom( type ) ) {
			return (X) ToBytesTransformer.INSTANCE.transform( value );
		}
		throw unknownUnwrap( type );
	}

	@Override
	public <X> UUID wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( UUID.class.isInstance( value ) ) {
			return PassThroughTransformer.INSTANCE.parse( value );
		}
		if ( String.class.isInstance( value ) ) {
			return ToStringTransformer.INSTANCE.parse( value );
		}
		if ( byte[].class.isInstance( value ) ) {
			return ToBytesTransformer.INSTANCE.parse( value );
		}
		throw unknownWrap( value.getClass() );
	}

	public static interface ValueTransformer {
		public Serializable transform(UUID uuid);
		public UUID parse(Object value);
	}

	public static class PassThroughTransformer implements ValueTransformer {
		public static final PassThroughTransformer INSTANCE = new PassThroughTransformer();

		@Override
		public UUID transform(UUID uuid) {
			return uuid;
		}

		@Override
		public UUID parse(Object value) {
			return (UUID)value;
		}
	}

	public static class ToStringTransformer implements ValueTransformer {
		public static final ToStringTransformer INSTANCE = new ToStringTransformer();

		@Override
		public String transform(UUID uuid) {
			return uuid.toString();
		}

		@Override
		public UUID parse(Object value) {
			return UUID.fromString( (String) value );
		}
	}

	public static class ToBytesTransformer implements ValueTransformer {
		public static final ToBytesTransformer INSTANCE = new ToBytesTransformer();

		@Override
		public byte[] transform(UUID uuid) {
			byte[] bytes = new byte[16];
			BytesHelper.fromLong( uuid.getMostSignificantBits(), bytes, 0);
			BytesHelper.fromLong( uuid.getLeastSignificantBits(), bytes, 8 );
			return bytes;
		}

		@Override
		public UUID parse(Object value) {
			byte[] bytea = (byte[]) value;
			return new UUID( BytesHelper.asLong( bytea, 0 ), BytesHelper.asLong( bytea, 8 ) );
		}
	}
}
