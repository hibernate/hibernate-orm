/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot;

import org.hibernate.boot.jaxb.Origin;

/**
 * @author Steve Ebersole
 */
public class MappingNotFoundException extends MappingException {
	public MappingNotFoundException(String message, Origin origin) {
		super( message, origin );
	}

	public MappingNotFoundException(Origin origin) {
		super( String.format( "Mapping (%s) not found : %s", origin.getType(), origin.getName() ), origin );
	}

	public MappingNotFoundException(String message, Throwable root, Origin origin) {
		super( message, root, origin );
	}

	public MappingNotFoundException(Throwable root, Origin origin) {
		super( String.format( "Mapping (%s) not found : %s", origin.getType(), origin.getName() ), root, origin );
	}
}
