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
package org.hibernate.annotations.common.reflection;

import java.util.Collection;

/**
 * @author Emmanuel Bernard
 */
public abstract interface XMember extends XAnnotatedElement {

	String getName();

	boolean isCollection();

	boolean isArray();

	/**
	 * The collection class for collections, null for others.
	 */
	Class<? extends Collection> getCollectionClass();

	// TODO We should probably try to reduce the following three methods to two.
	// the last one is particularly offensive

	/**
	 * This property's XClass.
	 */
	XClass getType();

	/**
	 * This property's type for simple properties, the type of its elements for arrays and collections.
	 */
	XClass getElementClass();

	/**
	 * The type of this property's elements for arrays, the type of the property itself for everything else.
	 */
	XClass getClassOrElementClass();

	/**
	 * The type of this map's key, or null for anything that is not a map.
	 */
	XClass getMapKey();

	/**
	 * Same modifiers as java.lang.Member#getModifiers()
	 */
	int getModifiers();

	//this breaks the Java reflect hierarchy, since accessible belongs to AccessibleObject
	void setAccessible(boolean accessible);

	public Object invoke(Object target, Object... parameters);

	boolean isTypeResolved();
}
