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

	public PropertyValueException(String s, String entityName, String propertyName) {
		super(s);
		this.entityName = entityName;
		this.propertyName = propertyName;
	}

	public String getEntityName() {
		return entityName;
	}

	public String getPropertyName() {
		return propertyName;
	}

	public String getMessage() {
		return super.getMessage() + ": " +
			StringHelper.qualify(entityName, propertyName);
	}

	/**
	 * Return a well formed property path.
	 * Basicaly, it will return parent.child
	 *
	 * @param parent parent in path
	 * @param child child in path
	 * @return parent-child path
	 */
	public static String buildPropertyPath(String parent, String child) {
		return new StringBuffer(parent).append('.').append(child).toString();
	}
}






