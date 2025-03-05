/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

/**
 * Thrown when the user tries to do something illegal with a deleted object.
 *
 * @author Gavin King
 */
public class ObjectDeletedException extends UnresolvableObjectException {
	/**
	 * Constructs an {@code ObjectDeletedException} using the given information.
	 *
	 *  @param message A message explaining the exception condition
	 * @param identifier The identifier of the entity
	 * @param entityName The name of the entity
	 */
	public ObjectDeletedException(String message, Object identifier, String entityName) {
		super( message, identifier, entityName );
	}

}
