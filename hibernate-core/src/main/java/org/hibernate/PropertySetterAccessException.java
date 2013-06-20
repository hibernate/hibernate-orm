/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
