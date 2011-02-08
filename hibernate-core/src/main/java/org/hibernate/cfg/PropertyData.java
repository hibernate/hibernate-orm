/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.cfg;
import org.hibernate.MappingException;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;

public interface PropertyData {

	/**
	 * @return default member access (whether field or property)
	 * @throws MappingException No getter or field found or wrong JavaBean spec usage
	 */
	AccessType getDefaultAccess();

	/**
	 * @return property name
	 * @throws MappingException No getter or field found or wrong JavaBean spec usage
	 */
	String getPropertyName() throws MappingException;

	/**
	 * Returns the returned class itself or the element type if an array
	 */
	XClass getClassOrElement() throws MappingException;

	/**
	 * Return the class itself
	 */
	XClass getPropertyClass() throws MappingException;

	/**
	 * Returns the returned class name itself or the element type if an array
	 */
	String getClassOrElementName() throws MappingException;

	/**
	 * Returns the returned class name itself
	 */
	String getTypeName() throws MappingException;

	/**
	 * Return the Hibernate mapping property
	 */
	XProperty getProperty();

	/**
	 * Return the Class the property is declared on
	 * If the property is declared on a @MappedSuperclass,
	 * this class will be different than the PersistentClass's class
	 */
	XClass getDeclaringClass();
}
