/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.boot;

import org.hibernate.boot.MappingException;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;

/**
 * Indicates an error happened during the Envers mapping boot process.
 *
 * @author Chris Cranford
 */
public class EnversMappingException extends MappingException {

	public EnversMappingException(String message) {
		super( message, new Origin( SourceType.OTHER, "envers" ) );
	}

	public EnversMappingException(String message, Throwable t) {
		super( message, t, new Origin( SourceType.OTHER, "envers" ) );
	}

	public EnversMappingException(Throwable t) {
		this( "", t );
	}

}
