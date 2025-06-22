/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.util.Comparator;
import java.util.Locale;

import org.hibernate.type.descriptor.WrapperOptions;

/**
 * Descriptor for {@link Locale} handling.
 *
 * @author Steve Ebersole
 */
public class LocaleJavaType extends AbstractClassJavaType<Locale> {
	public static final LocaleJavaType INSTANCE = new LocaleJavaType();

	public static class LocaleComparator implements Comparator<Locale> {
		public static final LocaleComparator INSTANCE = new LocaleComparator();

		public int compare(Locale o1, Locale o2) {
			return o1.toString().compareTo( o2.toString() );
		}
	}

	public LocaleJavaType() {
		super( Locale.class, ImmutableMutabilityPlan.instance(), LocaleComparator.INSTANCE );
	}

	@Override
	public boolean isInstance(Object value) {
		return value instanceof Locale;
	}

	@Override
	public boolean useObjectEqualsHashCode() {
		return true;
	}

	public String toString(Locale value) {
		return value.toString();
	}

	public Locale fromString(CharSequence sequence) {
		// TODO : Ultimately switch to Locale.Builder for this. However, Locale.Builder is Java 7

		if ( sequence == null ) {
			return null;
		}

		String string = sequence.toString();
		if( string.isEmpty() ) {
			return Locale.ROOT;
		}

		boolean separatorFound = false;
		int position = 0;
		char[] chars = string.toCharArray();

		for ( int i = 0; i < chars.length; i++ ) {
			// We just look for separators
			if ( chars[i] == '_' ) {
				if ( !separatorFound ) {
					// On the first separator we know that we have at least a language
					string = new String( chars, position, i - position );
					position = i + 1;
				}
				else {
					// On the second separator we have to check whether there are more chars available for variant
					if ( chars.length > i + 1 ) {
						// There is a variant so add it to the constructor
						return new Locale( string, new String( chars, position, i - position ), new String( chars,
								i + 1, chars.length - i - 1 ) );
					}
					else {
						// No variant given, we just have language and country
						return new Locale( string, new String( chars, position, i - position ), "" );
					}
				}

				separatorFound = true;
			}
		}

		if ( !separatorFound ) {
			// No separator found, there is only a language
			return new Locale( string );
		}
		else {
			// Only one separator found, there is a language and a country
			return new Locale( string, new String( chars, position, chars.length - position ) );
		}
	}

	@SuppressWarnings("unchecked")
	public <X> X unwrap(Locale value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( Locale.class.isAssignableFrom( type ) ) {
			return (X) value;
		}
		if ( String.class.isAssignableFrom( type ) ) {
			return (X) value.toString();
		}
		throw unknownUnwrap( type );
	}

	public <X> Locale wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( value instanceof Locale locale ) {
			return locale;
		}
		if (value instanceof CharSequence charSequence) {
			return fromString( charSequence );
		}
		throw unknownWrap( value.getClass() );
	}

}
