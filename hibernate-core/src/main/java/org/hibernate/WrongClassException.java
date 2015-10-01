/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import java.io.Serializable;

/**
 * Thrown when loading an entity (by identifier) results in a value that cannot be treated as the subclass
 * type requested by the caller.
 *
 * @author Gavin King
 */
public class WrongClassException extends HibernateException {
	private final Serializable identifier;
	private final String entityName;

	/**
	 * Constructs a WrongClassException using the supplied information.
	 *
	 * @param message A message explaining the exception condition
	 * @param identifier The identifier of the entity
	 * @param entityName The entity-type requested
	 */
	public WrongClassException(String message, Serializable identifier, String entityName) {
		super(
				String.format(
						"Object [id=%s] was not of the specified subclass [%s] : %s",
						identifier,
						entityName,
						message
				)
		);
		this.identifier = identifier;
		this.entityName = entityName;
	}

	public String getEntityName() {
		return entityName;
	}

	public Serializable getIdentifier() {
		return identifier;
	}
}
