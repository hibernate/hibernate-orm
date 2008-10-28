//$Id$
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
