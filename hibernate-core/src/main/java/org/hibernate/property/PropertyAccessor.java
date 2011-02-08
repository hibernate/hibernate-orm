/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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
