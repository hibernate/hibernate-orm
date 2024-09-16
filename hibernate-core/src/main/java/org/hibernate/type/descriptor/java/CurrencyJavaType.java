/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.util.Currency;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * Descriptor for {@link Currency} handling.
 *
 * @author Steve Ebersole
 */
public class CurrencyJavaType extends AbstractClassJavaType<Currency> {
	public static final CurrencyJavaType INSTANCE = new CurrencyJavaType();

	public CurrencyJavaType() {
		super( Currency.class );
	}

	@Override
	public boolean useObjectEqualsHashCode() {
		return true;
	}

	@Override
	public String toString(Currency value) {
		return value.getCurrencyCode();
	}

	@Override
	public Currency fromString(CharSequence string) {
		return Currency.getInstance( string.toString() );
	}

	@SuppressWarnings("unchecked")
	public <X> X unwrap(Currency value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( Currency.class.isAssignableFrom( type ) ) {
			return (X) value;
		}
		if ( String.class.isAssignableFrom( type ) ) {
			return (X) value.getCurrencyCode();
		}
		throw unknownUnwrap( type );
	}

	@Override
	public <X> Currency wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( value instanceof Currency ) {
			return (Currency) value;
		}
		if (value instanceof String) {
			return Currency.getInstance( (String) value );
		}
		throw unknownWrap( value.getClass() );
	}

	@Override
	public long getDefaultSqlLength(Dialect dialect, JdbcType jdbcType) {
		return 3;
	}
}
