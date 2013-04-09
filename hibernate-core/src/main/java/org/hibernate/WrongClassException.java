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

