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

public class PropertyPreloadedData implements PropertyData {
	private final AccessType defaultAccess;

	private final String propertyName;

	private final XClass returnedClass;

	public PropertyPreloadedData(AccessType defaultAccess, String propertyName, XClass returnedClass) {
		this.defaultAccess = defaultAccess;
		this.propertyName = propertyName;
		this.returnedClass = returnedClass;
	}

	public AccessType getDefaultAccess() throws MappingException {
		return defaultAccess;
	}

	public String getPropertyName() throws MappingException {
		return propertyName;
	}

	public XClass getClassOrElement() throws MappingException {
		return getPropertyClass();
	}

	public XClass getPropertyClass() throws MappingException {
		return returnedClass;
	}

	public String getClassOrElementName() throws MappingException {
		return getTypeName();
	}

	public String getTypeName() throws MappingException {
		return returnedClass == null ? null : returnedClass.getName();
	}

	public XProperty getProperty() {
		return null; //instead of UnsupportedOperationException
	}

	public XClass getDeclaringClass() {
		//Preloaded properties are artificial wrapper for colleciton element accesses
		//and idClass creation, ignore.
		return null;
	}
}
