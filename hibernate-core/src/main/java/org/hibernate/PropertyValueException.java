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

import org.hibernate.internal.util.StringHelper;

/**
 * Thrown when the (illegal) value of a property can not be persisted.
 * There are two main causes:
 * <ul>
 * <li>a property declared <tt>not-null="true"</tt> is null
 * <li>an association references an unsaved transient instance
 * </ul>
 * @author Gavin King
 */
public class PropertyValueException extends HibernateException {
	private final String entityName;
	private final String propertyName;

	/**
	 * Constructs a PropertyValueException using the specified information.
	 *
	 * @param message A message explaining the exception condition
	 * @param entityName The name of the entity, containing the property
	 * @param propertyName The name of the property being accessed.
	 */
	public PropertyValueException(String message, String entityName, String propertyName) {
		super( message );
		this.entityName = entityName;
		this.propertyName = propertyName;
	}

	public String getEntityName() {
		return entityName;
	}

	public String getPropertyName() {
		return propertyName;
	}

	@Override
	public String getMessage() {
		return super.getMessage() + " : " + StringHelper.qualify( entityName, propertyName );
	}
}
