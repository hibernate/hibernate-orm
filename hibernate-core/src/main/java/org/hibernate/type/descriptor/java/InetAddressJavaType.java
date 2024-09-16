/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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

	@SuppressWarnings("unchecked")
	@Override
	public <X> X unwrap(InetAddress value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( InetAddress.class.isAssignableFrom( type ) ) {
			return (X) value;
		}
		if ( byte[].class.isAssignableFrom( type ) ) {
			return (X) value.getAddress();
		}
		if ( String.class.isAssignableFrom( type ) ) {
			return (X) value.getHostAddress();
		}
		throw unknownUnwrap( type );
	}

	@Override
	public <X> InetAddress wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if (value instanceof InetAddress) {
			return (InetAddress) value;
		}
		if (value instanceof byte[]) {
			try {
				return InetAddress.getByAddress( (byte[]) value );
			}
			catch (UnknownHostException e) {
				throw new IllegalArgumentException( e );
			}
		}
		if (value instanceof String) {
			try {
				return InetAddress.getByName( (String) value );
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
