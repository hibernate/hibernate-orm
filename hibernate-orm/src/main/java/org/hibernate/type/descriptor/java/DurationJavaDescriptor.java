/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
