/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate;

import org.hibernate.util.StringHelper;

/**
 * A problem occurred accessing a property of an instance of a
 * persistent class by reflection, or via CGLIB. There are a
 * number of possible underlying causes, including
 * <ul>
 * <li>failure of a security check
 * <li>an exception occurring inside the getter or setter method
 * <li>a nullable database column was mapped to a primitive-type property
 * <li>the Hibernate type was not castable to the property type (or vice-versa)
 * </ul>
 * @author Gavin King
 */
public class PropertyAccessException extends HibernateException {

	private final Class persistentClass;
	private final String propertyName;
	private final boolean wasSetter;

	public PropertyAccessException(Throwable root, String s, boolean wasSetter, Class persistentClass, String propertyName) {
		super(s, root);
		this.persistentClass = persistentClass;
		this.wasSetter = wasSetter;
		this.propertyName = propertyName;
	}

	public Class getPersistentClass() {
		return persistentClass;
	}

	public String getPropertyName() {
		return propertyName;
	}

	public String getMessage() {
		return super.getMessage() +
		( wasSetter ? " setter of " : " getter of ") +
		StringHelper.qualify( persistentClass.getName(), propertyName );
	}
}






