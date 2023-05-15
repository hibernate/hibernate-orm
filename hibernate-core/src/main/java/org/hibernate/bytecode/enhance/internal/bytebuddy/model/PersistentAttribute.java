/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy.model;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;

/**
 * @author Steve Ebersole
 */
public class PersistentAttribute {
	private final String name;
	private final AccessType accessType;

	private final FieldDetails underlyingField;
	private final MethodDetails underlyingGetter;
	private final MethodDetails underlyingSetter;

	// todo (enhancement-naming) : others like writerMethod, readerMethod, etc?

	public PersistentAttribute(
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

	public String getName() {
		return name;
	}

	/**
	 * The implicit or {@link #isAccessTypeExplicit() explicit} access-type for this attribute
	 */
	public AccessType getAccessType() {
		return accessType;
	}

	/**
	 * Whether {@linkplain  jakarta.persistence.Access @Access} was explicitly
	 * defined on the attribute member.
	 */
	public boolean isAccessTypeExplicit() {
		return getUnderlyingMember().hasAnnotation( Access.class );
	}

	public MemberDetails getUnderlyingMember() {
		return accessType == AccessType.FIELD ? getUnderlyingField() : getUnderlyingGetter();
	}

	public FieldDetails getUnderlyingField() {
		return underlyingField;
	}

	public MethodDetails getUnderlyingGetter() {
		return underlyingGetter;
	}

	public MethodDetails getUnderlyingSetter() {
		return underlyingSetter;
	}
}
