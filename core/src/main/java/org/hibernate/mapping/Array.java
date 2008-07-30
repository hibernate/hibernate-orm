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
package org.hibernate.mapping;

import org.hibernate.MappingException;
import org.hibernate.type.CollectionType;
import org.hibernate.type.PrimitiveType;
import org.hibernate.type.TypeFactory;
import org.hibernate.util.ReflectHelper;

/**
 * An array mapping has a primary key consisting of
 * the key columns + index column.
 * @author Gavin King
 */
public class Array extends List {

	private String elementClassName;

	/**
	 * Constructor for Array.
	 * @param owner
	 */
	public Array(PersistentClass owner) {
		super(owner);
	}

	public Class getElementClass() throws MappingException {
		if (elementClassName==null) {
			org.hibernate.type.Type elementType = getElement().getType();
			return isPrimitiveArray() ?
				( (PrimitiveType) elementType ).getPrimitiveClass() :
				elementType.getReturnedClass();
		}
		else {
			try {
				return ReflectHelper.classForName(elementClassName);
			}
			catch (ClassNotFoundException cnfe) {
				throw new MappingException(cnfe);
			}
		}
	}

	public CollectionType getDefaultCollectionType() throws MappingException {
		return TypeFactory.array( getRole(), getReferencedPropertyName(), isEmbedded(), getElementClass() );
	}

	public boolean isArray() {
		return true;
	}

	/**
	 * @return Returns the elementClassName.
	 */
	public String getElementClassName() {
		return elementClassName;
	}
	/**
	 * @param elementClassName The elementClassName to set.
	 */
	public void setElementClassName(String elementClassName) {
		this.elementClassName = elementClassName;
	}
	
	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}
}
