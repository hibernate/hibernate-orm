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

import java.util.Comparator;
import java.util.Locale;
import java.util.StringTokenizer;

import org.hibernate.type.descriptor.WrapperOptions;

/**
 * TODO : javadoc
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
		StringTokenizer tokens = new StringTokenizer( string, "_" );
		String language = tokens.hasMoreTokens() ? tokens.nextToken() : "";
		String country = tokens.hasMoreTokens() ? tokens.nextToken() : "";
		// Need to account for allowable '_' within the variant
		String variant = "";
		String sep = "";
		while ( tokens.hasMoreTokens() ) {
			variant += sep + tokens.nextToken();
			sep = "_";
		}
		return new Locale( language, country, variant );
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
