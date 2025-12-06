/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
public class TimeZoneJavaType extends AbstractClassJavaType<TimeZone> {
	public static final TimeZoneJavaType INSTANCE = new TimeZoneJavaType();

	public static class TimeZoneComparator implements Comparator<TimeZone> {
		public static final TimeZoneComparator INSTANCE = new TimeZoneComparator();

		public int compare(TimeZone o1, TimeZone o2) {
			return o1.getID().compareTo( o2.getID() );
		}
	}

	public TimeZoneJavaType() {
		super( TimeZone.class, ImmutableMutabilityPlan.instance(), TimeZoneComparator.INSTANCE );
	}

	@Override
	public boolean isInstance(Object value) {
		return value instanceof TimeZone;
	}

	@Override
	public TimeZone cast(Object value) {
		return (TimeZone) value;
	}

	@Override
	public boolean useObjectEqualsHashCode() {
		return true;
	}

	public String toString(TimeZone value) {
		return value.getID();
	}

	public TimeZone fromString(CharSequence string) {
		return TimeZone.getTimeZone( string.toString() );
	}

	public <X> X unwrap(TimeZone value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( TimeZone.class.isAssignableFrom( type ) ) {
			return type.cast( value );
		}
		if ( String.class.isAssignableFrom( type ) ) {
			return type.cast( toString( value ) );
		}
		throw unknownUnwrap( type );
	}

	public <X> TimeZone wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( value instanceof TimeZone ) {
			return (TimeZone) value;
		}
		if ( value instanceof CharSequence ) {
			return fromString( (CharSequence) value );
		}
		throw unknownWrap( value.getClass() );
	}

}
