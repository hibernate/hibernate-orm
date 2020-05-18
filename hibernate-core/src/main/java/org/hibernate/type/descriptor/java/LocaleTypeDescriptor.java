/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
public class LocaleTypeDescriptor extends AbstractTypeDescriptor<Locale> {
	public static final LocaleTypeDescriptor INSTANCE = new LocaleTypeDescriptor();

	public static class LocaleComparator implements Comparator<Locale> {
		public static final LocaleComparator INSTANCE = new LocaleComparator();

		public int compare(Locale o1, Locale o2) {
			return o1.toString().compareTo( o2.toString() );
		}
	}

	public LocaleTypeDescriptor() {
		super( Locale.class );
	}

	@Override
	public Comparator<Locale> getComparator() {
		return LocaleComparator.INSTANCE;
	}

	public String toString(Locale value) {
		return value.toString();
	}

	public Locale fromString(String string) {
		// TODO : Ultimately switch to Locale.Builder for this. However, Locale.Builder is Java 7

		if ( string == null ) {
			return null;
		}

		if( string.isEmpty() ) {
			return Locale.ROOT;
		}

		Locale.Builder builder = new Locale.Builder();
		String[] parts = string.split("_");

		for (int i = 0; i < parts.length; i++) {
			String s = parts[i];
			switch (i) {
				case 0:
					builder.setLanguage(s);
					break;
				case 1:
					builder.setRegion(s);
					break;
				case 2:
					if (i < parts.length - 1 || !s.startsWith("#")) {
						builder.setVariant(s);
						break;
					}
				case 3:
					if (s.startsWith("#")) {
						s = s.substring(1);
					}
					builder.setScript(s);
					break;
			}
		}

		return builder.build();
	}

	@SuppressWarnings({ "unchecked" })
	public <X> X unwrap(Locale value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
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
		if ( String.class.isInstance( value ) ) {
			return fromString( (String) value );
		}
		throw unknownWrap( value.getClass() );
	}
}
