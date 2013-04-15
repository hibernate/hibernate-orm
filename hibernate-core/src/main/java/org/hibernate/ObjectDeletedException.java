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







