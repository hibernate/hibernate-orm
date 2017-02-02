/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import java.io.Serializable;

import org.hibernate.pretty.MessageHelper;

/**
 * This exception is thrown when an operation would break session-scoped identity. This occurs if the
 * user tries to associate two different instances of the same Java class with a particular identifier,
 * in the scope of a single Session.
 *
 * @author Gavin King
 */
public class NonUniqueObjectException extends HibernateException {
	private final Serializable identifier;
	private final String entityName;

	/**
	 * Constructs a NonUniqueObjectException using the given information.
	 *
	 * @param message A message explaining the exception condition
	 * @param entityId The identifier of the entity
	 * @param entityName The name of the entity
	 */
	public NonUniqueObjectException(String message, Serializable entityId, String entityName) {
		super( message );
		this.entityName = entityName;
		this.identifier = entityId;
	}

	/**
	 * Constructs a NonUniqueObjectException using the given information, using a standard message.
	 *
	 * @param entityId The identifier of the entity
	 * @param entityName The name of the entity
	 */
	public NonUniqueObjectException(Serializable entityId, String entityName) {
		this(
				"A different object with the same identifier value was already associated with the session",
				entityId,
				entityName
		);
	}

	public String getEntityName() {
		return entityName;
	}

	public Serializable getIdentifier() {
		return identifier;
	}

	@Override
	public String getMessage() {
		return super.getMessage() + " : " + MessageHelper.infoString( entityName, identifier );
	}
}
