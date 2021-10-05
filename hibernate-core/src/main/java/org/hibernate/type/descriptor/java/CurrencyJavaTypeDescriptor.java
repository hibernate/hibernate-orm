/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java;

import java.util.Currency;

import org.hibernate.type.descriptor.WrapperOptions;

/**
 * Descriptor for {@link Currency} handling.
 *
 * @author Steve Ebersole
 */
public class CurrencyJavaTypeDescriptor extends AbstractClassJavaTypeDescriptor<Currency> {
	public static final CurrencyJavaTypeDescriptor INSTANCE = new CurrencyJavaTypeDescriptor();

	public CurrencyJavaTypeDescriptor() {
		super( Currency.class );
	}

	@Override
	public String toString(Currency value) {
		return value.getCurrencyCode();
	}

	@Override
	public Currency fromString(CharSequence string) {
		return Currency.getInstance( string.toString() );
	}

	@SuppressWarnings({ "unchecked" })
	public <X> X unwrap(Currency value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
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
		if ( String.class.isInstance( value ) ) {
			return Currency.getInstance( (String) value );
		}
		throw unknownWrap( value.getClass() );
	}
}
