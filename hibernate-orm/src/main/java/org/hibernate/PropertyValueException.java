/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
