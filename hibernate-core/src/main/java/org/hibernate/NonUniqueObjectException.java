/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
