/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot;

import org.hibernate.boot.jaxb.Origin;

/**
 * Indicates that a mapping document could not be found at a given {@link Origin}.
 *
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
