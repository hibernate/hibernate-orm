/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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

	public String toString(UUID value) {
		return ToStringTransformer.INSTANCE.transform( value );
	}

	public UUID fromString(String string) {
		return ToStringTransformer.INSTANCE.parse( string );
	}

	@SuppressWarnings({ "unchecked" })
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

		public UUID transform(UUID uuid) {
			return uuid;
		}

		public UUID parse(Object value) {
			return (UUID)value;
		}
	}

	public static class ToStringTransformer implements ValueTransformer {
		public static final ToStringTransformer INSTANCE = new ToStringTransformer();

		public String transform(UUID uuid) {
			return uuid.toString();
		}

		public UUID parse(Object value) {
			return UUID.fromString( (String) value );
		}
	}

	public static class ToBytesTransformer implements ValueTransformer {
		public static final ToBytesTransformer INSTANCE = new ToBytesTransformer();

		public byte[] transform(UUID uuid) {
			byte[] bytes = new byte[16];
			System.arraycopy( BytesHelper.fromLong( uuid.getMostSignificantBits() ), 0, bytes, 0, 8 );
			System.arraycopy( BytesHelper.fromLong( uuid.getLeastSignificantBits() ), 0, bytes, 8, 8 );
			return bytes;
		}

		public UUID parse(Object value) {
			byte[] msb = new byte[8];
			byte[] lsb = new byte[8];
			System.arraycopy( value, 0, msb, 0, 8 );
			System.arraycopy( value, 8, lsb, 0, 8 );
			return new UUID( BytesHelper.asLong( msb ), BytesHelper.asLong( lsb ) );
		}
	}
}
