/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import java.io.Serializable;

/**
 * Thrown when the user tries to do something illegal with a deleted object.
 *
 * @author Gavin King
 */
public class ObjectDeletedException extends UnresolvableObjectException {
	/**
	 * Constructs an ObjectDeletedException using the given information.
	 *
	 * @param message A message explaining the exception condition
	 * @param identifier The identifier of the entity
	 * @param entityName The name of the entity
	 */
	public ObjectDeletedException(String message, Serializable identifier, String entityName) {
		super( message, identifier, entityName );
	}

}
