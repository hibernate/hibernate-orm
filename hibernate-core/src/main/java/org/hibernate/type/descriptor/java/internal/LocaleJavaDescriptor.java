/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java.internal;

import java.util.Comparator;
import java.util.Locale;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.java.spi.AbstractBasicJavaDescriptor;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * Descriptor for {@link Locale} handling.
 * 
 * @author Steve Ebersole
 */
public class LocaleJavaDescriptor extends AbstractBasicJavaDescriptor<Locale> {
	public static final LocaleJavaDescriptor INSTANCE = new LocaleJavaDescriptor();

	public static class LocaleComparator implements Comparator<Locale> {
		public static final LocaleComparator INSTANCE = new LocaleComparator();

		public int compare(Locale o1, Locale o2) {
			return o1.toString().compareTo( o2.toString() );
		}
	}

	public LocaleJavaDescriptor() {
		super( Locale.class );
	}

	@Override
	public SqlTypeDescriptor getJdbcRecommendedSqlType(JdbcRecommendedSqlTypeMappingContext context) {
		return StringJavaDescriptor.INSTANCE.getJdbcRecommendedSqlType( context );
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

		if ( string == null || string.isEmpty() ) {
			return null;
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

	@SuppressWarnings({ "unchecked" })
	public <X> X unwrap(Locale value, Class<X> type, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}
		if ( String.class.isAssignableFrom( type ) ) {
			return (X) value.toString();
		}
		throw unknownUnwrap( type );
	}

	public <X> Locale wrap(X value, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}
		if ( String.class.isInstance( value ) ) {
			return fromString( (String) value );
		}
		throw unknownWrap( value.getClass() );
	}
}
