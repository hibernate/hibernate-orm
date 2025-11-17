/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

/**
 * Indicates an attempt was made to refer to an unknown entity name or class.
 *
 * @implNote This class extends {@link MappingException} for legacy reasons.
 * Longer term I think it makes more sense to have a different hierarchy for
 * runtime-"mapping" exceptions.
 *
 * @author Steve Ebersole
 */
public class UnknownEntityTypeException extends MappingException {
	public UnknownEntityTypeException(String message, Throwable cause) {
		super( message, cause );
	}

	public UnknownEntityTypeException(String entityName) {
		super( "Unknown entity type '" + entityName + "'" );
	}

	public UnknownEntityTypeException(Class<?> entityClass) {
		this( entityClass.getName() );
	}
}
