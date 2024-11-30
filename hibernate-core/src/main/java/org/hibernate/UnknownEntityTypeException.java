/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

/**
 * Indicates an attempt was made to refer to an unknown entity name or class.
 * <p>
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

	public UnknownEntityTypeException(String message) {
		super( message );
	}
}
