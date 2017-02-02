/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

/**
 * @author Steve Ebersole
 */
public class PropertySetterAccessException extends PropertyAccessException {
	/**
	 * Constructs a PropertyAccessException using the specified information.
	 *
	 * @param cause The underlying cause
	 * @param persistentClass The class which is supposed to contain the property in question
	 * @param propertyName The name of the property.
	 * @param expectedType The expected property type
	 * @param target The target, which should be of type 'persistentClass'
	 * @param value The property value we are trying to set
	 */
	public PropertySetterAccessException(
			Throwable cause,
			Class persistentClass,
			String propertyName,
			Class expectedType,
			Object target,
			Object value) {
		super(
				cause,
				String.format(
						"IllegalArgumentException occurred while calling setter for property [%s.%s (expected type = %s)]; " +
								"target = [%s], property value = [%s]",
						persistentClass.getName(),
						propertyName,
						expectedType.getName(),
						target,
						value
				),
				true,
				persistentClass,
				propertyName
		);
	}

	@Override
	public String toString() {
		return super.originalMessage();
	}
}
