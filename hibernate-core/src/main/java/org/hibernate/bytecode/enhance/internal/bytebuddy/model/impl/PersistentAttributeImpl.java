/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy.model.impl;

import org.hibernate.bytecode.enhance.internal.bytebuddy.model.FieldDetails;
import org.hibernate.bytecode.enhance.internal.bytebuddy.model.MemberDetails;
import org.hibernate.bytecode.enhance.internal.bytebuddy.model.MethodDetails;
import org.hibernate.bytecode.enhance.internal.bytebuddy.model.PersistentAttribute;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;

/**
 * @author Steve Ebersole
 */
public class PersistentAttributeImpl implements PersistentAttribute {
	private final String name;
	private final AccessType accessType;

	private final MethodDetails underlyingGetter;
	private final MethodDetails underlyingSetter;

	private FieldDetails underlyingField;

	public PersistentAttributeImpl(
			String name,
			AccessType accessType,
			FieldDetails underlyingField,
			MethodDetails underlyingGetter,
			MethodDetails underlyingSetter) {
		this.name = name;
		this.accessType = accessType;
		this.underlyingField = underlyingField;
		this.underlyingGetter = underlyingGetter;
		this.underlyingSetter = underlyingSetter;
	}

	@Override
	public String getName() {
		return name;
	}

	/**
	 * The implicit or {@link #isAccessTypeExplicit() explicit} access-type for this attribute
	 */
	@Override
	public AccessType getAccessType() {
		return accessType;
	}

	/**
	 * Whether {@linkplain  Access @Access} was explicitly
	 * defined on the attribute member.
	 */
	@Override
	public boolean isAccessTypeExplicit() {
		return getUnderlyingMember().hasAnnotation( Access.class );
	}

	@Override
	public MemberDetails getUnderlyingMember() {
		return accessType == AccessType.FIELD ? getUnderlyingField() : getUnderlyingGetter();
	}

	@Override
	public FieldDetails getUnderlyingField() {
		return underlyingField;
	}

	@Override
	public void setUnderlyingField(FieldDetails underlyingField) {
		this.underlyingField = underlyingField;
	}

	@Override
	public MethodDetails getUnderlyingGetter() {
		return underlyingGetter;
	}

	@Override
	public MethodDetails getUnderlyingSetter() {
		return underlyingSetter;
	}
}
