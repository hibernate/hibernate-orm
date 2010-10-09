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
import java.util.TimeZone;

import org.hibernate.type.descriptor.WrapperOptions;

/**
 * Descriptor for {@link TimeZone} handling.
 *
 * @author Steve Ebersole
 */
public class TimeZoneTypeDescriptor extends AbstractTypeDescriptor<TimeZone> {
	public static final TimeZoneTypeDescriptor INSTANCE = new TimeZoneTypeDescriptor();

	public static class TimeZoneComparator implements Comparator<TimeZone> {
		public static final TimeZoneComparator INSTANCE = new TimeZoneComparator();

		public int compare(TimeZone o1, TimeZone o2) {
			return o1.getID().compareTo( o2.getID() );
		}
	}

	public TimeZoneTypeDescriptor() {
		super( TimeZone.class );
	}

	public String toString(TimeZone value) {
		return value.getID();
	}

	public TimeZone fromString(String string) {
		return TimeZone.getTimeZone( string );
	}

	@Override
	public Comparator<TimeZone> getComparator() {
		return TimeZoneComparator.INSTANCE;
	}

	@SuppressWarnings({ "unchecked" })
	public <X> X unwrap(TimeZone value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( String.class.isAssignableFrom( type ) ) {
			return (X) toString( value );
		}
		throw unknownUnwrap( type );
	}

	public <X> TimeZone wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( String.class.isInstance( value ) ) {
			return fromString( (String) value );
		}
		throw unknownWrap( value.getClass() );
	}
}
