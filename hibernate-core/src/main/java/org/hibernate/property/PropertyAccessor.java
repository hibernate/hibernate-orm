/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.property;
import org.hibernate.PropertyNotFoundException;

/**
 * Abstracts the notion of a "property". Defines a strategy for accessing the
 * value of an attribute.
 *
 * @author Gavin King
 */
public interface PropertyAccessor {
	/**
	 * Create a "getter" for the named attribute
	 *
	 * @param theClass The class on which the property is defined.
	 * @param propertyName The name of the property.
	 *
	 * @return An appropriate getter.
	 *
	 * @throws PropertyNotFoundException Indicates a problem interpretting the propertyName
	 */
	public Getter getGetter(Class theClass, String propertyName) throws PropertyNotFoundException;

	/**
	 * Create a "setter" for the named attribute
	 *
	 * @param theClass The class on which the property is defined.
	 * @param propertyName The name of the property.
	 *
	 * @return An appropriate setter
	 *
	 * @throws PropertyNotFoundException Indicates a problem interpretting the propertyName
	 */
	public Setter getSetter(Class theClass, String propertyName) throws PropertyNotFoundException;
}
