/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
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

import java.time.Duration;

import org.hibernate.type.descriptor.WrapperOptions;

/**
 * @author Steve Ebersole
 */
public class DurationJavaDescriptor extends AbstractTypeDescriptor<Duration> {
	/**
	 * Singleton access
	 */
	public static final DurationJavaDescriptor INSTANCE = new DurationJavaDescriptor();

	@SuppressWarnings("unchecked")
	public DurationJavaDescriptor() {
		super( Duration.class, ImmutableMutabilityPlan.INSTANCE );
	}

	@Override
	public String toString(Duration value) {
		if ( value == null ) {
			return null;
		}
		return String.valueOf( value.toNanos() );
	}

	@Override
	public Duration fromString(String string) {
		if ( string == null ) {
			return null;
		}
		return Duration.ofNanos( Long.valueOf( string ) );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> X unwrap(Duration duration, Class<X> type, WrapperOptions options) {
		if ( duration == null ) {
			return null;
		}

		if ( Duration.class.isAssignableFrom( type ) ) {
			return (X) duration;
		}

		if ( String.class.isAssignableFrom( type ) ) {
			return (X) duration.toString();
		}

		if ( Long.class.isAssignableFrom( type ) ) {
			return (X) Long.valueOf( duration.toNanos() );
		}

		throw unknownUnwrap( type );
	}

	@Override
	public <X> Duration wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if ( Duration.class.isInstance( value ) ) {
			return (Duration) value;
		}

		if ( Long.class.isInstance( value ) ) {
			return Duration.ofNanos( (Long) value );
		}

		if ( String.class.isInstance( value ) ) {
			return Duration.parse( (String) value );
		}

		throw unknownWrap( value.getClass() );
	}
}
