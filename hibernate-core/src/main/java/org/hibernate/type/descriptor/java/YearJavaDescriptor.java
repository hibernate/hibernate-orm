/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.java;

import java.time.Year;

import org.hibernate.type.descriptor.WrapperOptions;

/**
 * Java type descriptor for the Year type.
 *
 * @author Steve Ebersole
 */
public class YearJavaDescriptor extends AbstractTypeDescriptor<Year> {
	/**
	 * Singleton access
	 */
	public static final YearJavaDescriptor INSTANCE = new YearJavaDescriptor();

	@SuppressWarnings("unchecked")
	public YearJavaDescriptor() {
		super( Year.class, ImmutableMutabilityPlan.INSTANCE );
	}

	@Override
	public String toString(Year value) {
		return value == null ? null : value.toString();
	}

	@Override
	public Year fromString(String string) {
		return string == null ? null : Year.parse( string );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> X unwrap(Year year, Class<X> type, WrapperOptions options) {
		if ( year == null ) {
			return null;
		}

		if ( Year.class.isAssignableFrom( type ) ) {
			return (X) year;
		}

		if ( Integer.class.isAssignableFrom( type ) ) {
			return (X) Integer.valueOf( year.getValue() );
		}

		if ( Long.class.isAssignableFrom( type ) ) {
			return (X) Long.valueOf( year.getValue() );
		}

		if ( String.class.isAssignableFrom( type ) ) {
			return (X) toString( year );
		}

		throw unknownUnwrap( type );
	}

	@Override
	public <X> Year wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if ( Year.class.isInstance( value ) ) {
			return (Year) value;
		}

		if ( Integer.class.isInstance( value ) ) {
			return Year.of( (Integer) value );
		}

		if ( Long.class.isInstance( value ) ) {
			return Year.of( ( (Long) value ).intValue() );
		}

		if ( String.class.isInstance( value ) ) {
			return Year.parse( (String) value );
		}

		throw unknownWrap( value.getClass() );
	}
}
