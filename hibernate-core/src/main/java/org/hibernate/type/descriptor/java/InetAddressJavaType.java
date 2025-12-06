/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

/**
 * Descriptor for {@link java.net.InetAddress} handling.
 *
 * @author Christian Beikov
 */
public class InetAddressJavaType extends AbstractClassJavaType<InetAddress> {

	public static final InetAddressJavaType INSTANCE = new InetAddressJavaType();

	public InetAddressJavaType() {
		super( InetAddress.class );
	}

	@Override
	public boolean useObjectEqualsHashCode() {
		return true;
	}

	@Override
	public String toString(InetAddress value) {
		return value == null ? null : value.toString();
	}

	@Override
	public InetAddress fromString(CharSequence string) {
		try {
			return string == null ? null : InetAddress.getByName( string.toString() );
		}
		catch (UnknownHostException e) {
			throw new IllegalArgumentException( e );
		}
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators indicators) {
		return indicators.getJdbcType( SqlTypes.INET );
	}

	@Override
	public <X> X unwrap(InetAddress value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( InetAddress.class.isAssignableFrom( type ) ) {
			return type.cast( value );
		}
		if ( byte[].class.isAssignableFrom( type ) ) {
			return type.cast( value.getAddress() );
		}
		if ( String.class.isAssignableFrom( type ) ) {
			return type.cast( value.getHostAddress() );
		}
		throw unknownUnwrap( type );
	}

	@Override
	public <X> InetAddress wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if (value instanceof InetAddress inetAddress) {
			return inetAddress;
		}
		if (value instanceof byte[] bytes) {
			try {
				return InetAddress.getByAddress( bytes );
			}
			catch (UnknownHostException e) {
				throw new IllegalArgumentException( e );
			}
		}
		if (value instanceof String string) {
			try {
				return InetAddress.getByName( string );
			}
			catch (UnknownHostException e) {
				throw new IllegalArgumentException( e );
			}
		}
		throw unknownWrap( value.getClass() );
	}

	@Override
	public long getDefaultSqlLength(Dialect dialect, JdbcType jdbcType) {
		return 19;
	}

}
