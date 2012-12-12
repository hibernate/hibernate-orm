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
package org.hibernate.mapping;

import org.hibernate.MappingException;
import org.hibernate.cfg.Mappings;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.type.CollectionType;
import org.hibernate.type.PrimitiveType;

/**
 * An array mapping has a primary key consisting of the key columns + index column.
 *
 * @author Gavin King
 */
public class Array extends List {

	private String elementClassName;

	public Array(Mappings mappings, PersistentClass owner) {
		super( mappings, owner );
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

	@Override
    public CollectionType getDefaultCollectionType() throws MappingException {
		return getMappings().getTypeResolver()
				.getTypeFactory()
				.array( getRole(), getReferencedPropertyName(), getElementClass() );
	}

	@Override
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

	@Override
    public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}
}
