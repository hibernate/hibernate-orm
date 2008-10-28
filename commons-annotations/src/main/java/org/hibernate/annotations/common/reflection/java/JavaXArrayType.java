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
package org.hibernate.annotations.common.reflection.java;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.util.Collection;

import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.java.generics.TypeEnvironment;
import org.hibernate.annotations.common.reflection.java.generics.TypeSwitch;

/**
 * @author Emmanuel Bernard
 * @author Paolo Perrotta
 */
class JavaXArrayType extends JavaXType {

	public JavaXArrayType(Type type, TypeEnvironment context, JavaReflectionManager factory) {
		super( type, context, factory );
	}

	public boolean isArray() {
		return true;
	}

	public boolean isCollection() {
		return false;
	}

	public XClass getElementClass() {
		return toXClass( getElementType() );
	}

	private Type getElementType() {
		//TODO make it a static class for faster performance?
		return new TypeSwitch<Type>() {
			@Override
			public Type caseClass(Class classType) {
				return classType.getComponentType();
			}

			@Override
			public Type caseGenericArrayType(GenericArrayType genericArrayType) {
				return genericArrayType.getGenericComponentType();
			}

			@Override
			public Type defaultCase(Type t) {
				throw new IllegalArgumentException( t + " is not an array type" );
			}
		}.doSwitch( approximate() );
	}

	public XClass getClassOrElementClass() {
		return getElementClass();
	}

	public Class<? extends Collection> getCollectionClass() {
		return null;
	}

	public XClass getMapKey() {
		return null;
	}

	public XClass getType() {
		Type boundType = getElementType();
		if ( boundType instanceof Class ) {
			boundType = arrayTypeOf( (Class) boundType );
		}
		return toXClass( boundType );
	}

	private Class<? extends Object> arrayTypeOf(Class componentType) {
		return Array.newInstance( componentType, 0 ).getClass();
	}
}