/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy.model.impl;

import org.hibernate.bytecode.enhance.internal.bytebuddy.model.ClassDetails;
import org.hibernate.bytecode.enhance.internal.bytebuddy.model.FieldDetails;
import org.hibernate.bytecode.enhance.internal.bytebuddy.model.MethodDetails;
import org.hibernate.bytecode.enhance.internal.bytebuddy.model.PersistentAttribute;

import jakarta.persistence.AccessType;

/**
 * Builder for {@link PersistentAttribute} references
 *
 * @author Steve Ebersole
 */
public class PersistentAttributeFactory {
	private final ClassDetails declaringType;
	private final String name;
	private final AccessType accessType;

	private FieldDetails backingField;
	private MethodDetails getterMethod;
	private MethodDetails setterMethod;

	// todo (enhancement naming) : track other method references like inline dirty checking methods, etc

	public PersistentAttributeFactory(ClassDetails declaringType, String name, AccessType accessType) {
		this.declaringType = declaringType;
		this.name = name;
		this.accessType = accessType;
	}

	public PersistentAttributeFactory(
			ClassDetails declaringType,
			String name,
			AccessType accessType,
			FieldDetails backingField,
			MethodDetails getterMethod,
			MethodDetails setterMethod) {
		this( declaringType, name, accessType );

		assert getterMethod == null || getterMethod.getMethodKind() == MethodDetails.MethodKind.GETTER;
		assert setterMethod == null || setterMethod.getMethodKind() == MethodDetails.MethodKind.SETTER;

		this.backingField = backingField;
		this.getterMethod = getterMethod;
		this.setterMethod = setterMethod;
	}

	public ClassDetails getDeclaringType() {
		return declaringType;
	}

	public String getName() {
		return name;
	}

	public AccessType getAccessType() {
		return accessType;
	}

	public FieldDetails getBackingField() {
		return backingField;
	}

	public void setBackingField(FieldDetails backingField) {
		this.backingField = backingField;
	}

	public MethodDetails getGetterMethod() {
		return getterMethod;
	}

	public void setGetterMethod(MethodDetails getterMethod) {
		this.getterMethod = getterMethod;
	}

	public MethodDetails getSetterMethod() {
		return setterMethod;
	}

	public void setSetterMethod(MethodDetails setterMethod) {
		this.setterMethod = setterMethod;
	}

	public PersistentAttribute buildPersistentAttribute() {
		return new PersistentAttribute( name, accessType, backingField, getterMethod, setterMethod );
	}
}
