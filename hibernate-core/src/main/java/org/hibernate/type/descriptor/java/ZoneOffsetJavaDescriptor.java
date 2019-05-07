/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.java;

import java.time.ZoneId;
import java.time.ZoneOffset;

import org.hibernate.type.descriptor.WrapperOptions;

/**
 * @author Steve Ebersole
 */
public class ZoneOffsetJavaDescriptor extends AbstractTypeDescriptor<ZoneOffset> {
	/**
	 * Singleton access
	 */
	public static final ZoneOffsetJavaDescriptor INSTANCE = new ZoneOffsetJavaDescriptor();

	@SuppressWarnings({"unchecked", "WeakerAccess"})
	public ZoneOffsetJavaDescriptor() {
		super( ZoneOffset.class, ImmutableMutabilityPlan.INSTANCE );
	}

	@Override
	public ZoneOffset fromString(String string) {
		return ZoneOffset.of( string );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> X unwrap(ZoneOffset value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if ( Integer.class.isAssignableFrom( type ) ) {
			return (X) Integer.valueOf( value.getTotalSeconds() );
		}

		if ( String.class.isAssignableFrom( type ) ) {
			return (X) toString( value );
		}

		throw unknownUnwrap( type );
	}

	@Override
	public <X> ZoneOffset wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if ( Integer.class.isInstance( value ) ) {
			return ZoneOffset.ofTotalSeconds( (Integer) value );
		}

		if ( String.class.isInstance( value ) ) {
			return fromString( (String) value );
		}

		throw unknownWrap( value.getClass() );
	}
}
