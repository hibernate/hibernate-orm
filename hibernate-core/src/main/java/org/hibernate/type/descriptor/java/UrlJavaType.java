/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.net.MalformedURLException;
import java.net.URL;

import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

/**
 * Descriptor for {@link URL} handling.
 *
 * @author Steve Ebersole
 */
public class UrlJavaType extends AbstractClassJavaType<URL> {
	public static final UrlJavaType INSTANCE = new UrlJavaType();

	public UrlJavaType() {
		super( URL.class );
	}

	@Override
	public boolean isInstance(Object value) {
		return value instanceof URL;
	}

	@Override
	public URL cast(Object value) {
		return (URL) value;
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators context) {
		return context.getJdbcType( SqlTypes.VARCHAR );
	}

	@Override
	public boolean useObjectEqualsHashCode() {
		return true;
	}

	public String toString(URL value) {
		return value.toExternalForm();
	}

	public URL fromString(CharSequence string) {
		try {
			return new URL( string.toString() );
		}
		catch ( MalformedURLException e ) {
			throw new CoercionException( "Unable to convert string [" + string + "] to URL : " + e );
		}
	}

	public <X> X unwrap(URL value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( URL.class.isAssignableFrom( type ) ) {
			return type.cast( value );
		}
		if ( String.class.isAssignableFrom( type ) ) {
			return type.cast( toString( value ) );
		}
		throw unknownUnwrap( type );
	}

	public <X> URL wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if (value instanceof URL url) {
			return url;
		}
		if (value instanceof CharSequence charSequence) {
			return fromString( charSequence );
		}
		throw unknownWrap( value.getClass() );
	}

}
