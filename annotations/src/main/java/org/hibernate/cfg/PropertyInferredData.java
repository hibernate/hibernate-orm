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
 */
package org.hibernate.cfg;

import org.hibernate.MappingException;
import org.hibernate.annotations.AccessType;
import org.hibernate.annotations.Target;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;

/**
 * Retrieve all inferred data from an annnoted element
 *
 * @author Emmanuel Bernard
 * @author Paolo Perrotta
 */
public class PropertyInferredData implements PropertyData {
	private final String defaultAccess;

	private final XProperty property;
	private final ReflectionManager reflectionManager;
	private final XClass declaringClass;


	/**
	 * Take the annoted element for lazy process
	 */
	public PropertyInferredData(XClass declaringClass, XProperty property, String propertyAccessor, ReflectionManager reflectionManager) {
		this.declaringClass = declaringClass;
		this.property = property;
		this.defaultAccess = propertyAccessor;
		this.reflectionManager = reflectionManager;
	}

	public String getDefaultAccess() throws MappingException {
		// if(skip())
		// return defaultAccess;
		AccessType access = property.getAnnotation( AccessType.class );
		return access != null ? access.value() : defaultAccess;
	}

	public String getPropertyName() throws MappingException {
		return property.getName();
	}

	public XClass getPropertyClass() throws MappingException {
		if ( property.isAnnotationPresent( Target.class ) ) {
			return reflectionManager.toXClass( property.getAnnotation( Target.class ).value() );
		}
		else {
			return property.getType();
		}
	}

	public XClass getClassOrElement() throws MappingException {
		if ( property.isAnnotationPresent( Target.class ) ) {
			return reflectionManager.toXClass( property.getAnnotation( Target.class ).value() );
		}
		else {
			return property.getClassOrElementClass();
		}
	}

	public String getClassOrElementName() throws MappingException {
		return getClassOrElement().getName();
	}

	public String getTypeName() throws MappingException {
		return getPropertyClass().getName();
	}

	public XProperty getProperty() {
		return property;
	}

	public XClass getDeclaringClass() {
		return declaringClass;
	}
}
