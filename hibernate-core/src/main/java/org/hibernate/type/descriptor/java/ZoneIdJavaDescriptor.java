/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.java;

import java.time.ZoneId;

import org.hibernate.type.descriptor.WrapperOptions;

/**
 * @author Steve Ebersole
 */
public class ZoneIdJavaDescriptor extends AbstractTypeDescriptor<ZoneId> {
	/**
	 * Singleton access
	 */
	public static final ZoneIdJavaDescriptor INSTANCE = new ZoneIdJavaDescriptor();

	@SuppressWarnings({"unchecked", "WeakerAccess"})
	public ZoneIdJavaDescriptor() {
		super( ZoneId.class, ImmutableMutabilityPlan.INSTANCE );
	}

	@Override
	public ZoneId fromString(String string) {
		return ZoneId.of( string );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> X unwrap(ZoneId value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if ( String.class.isAssignableFrom( type ) ) {
			return (X) toString( value );
		}

		throw unknownUnwrap( type );
	}

	@Override
	public <X> ZoneId wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if ( String.class.isInstance( value ) ) {
			return fromString( (String) value );
		}

		throw unknownWrap( value.getClass() );
	}
}
