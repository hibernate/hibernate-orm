/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import org.hibernate.internal.util.StringHelper;

/**
 * A problem occurred accessing a property of an instance of a
 * persistent class by reflection, or via enhanced entities.
 * There are a number of possible underlying causes, including
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

	/**
	 * Constructs a PropertyAccessException using the specified information.
	 *
	 * @param cause The underlying cause
	 * @param message A message explaining the exception condition
	 * @param wasSetter Was the attempting to access the setter the cause of the exception?
	 * @param persistentClass The class which is supposed to contain the property in question
	 * @param propertyName The name of the property.
	 */
	public PropertyAccessException(
			Throwable cause,
			String message,
			boolean wasSetter,
			Class persistentClass,
			String propertyName) {
		super( message, cause );
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

	protected String originalMessage() {
		return super.getMessage();
	}

	@Override
	public String getMessage() {
		return originalMessage()
				+ ( wasSetter ? " setter of " : " getter of " )
				+ StringHelper.qualify( persistentClass.getName(), propertyName );
	}
}
