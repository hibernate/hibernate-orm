/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy;

import java.lang.reflect.Member;

/**
 * A {@link Member} wrapper that marks a property as being accessed through
 * a delegated single-value accessor (reader or writer) stored as an instance
 * field on the generated multi-value accessor class.
 *
 * <p>When {@link GetPropertyValues} or {@link SetPropertyValues} encounter this
 * type in the member array, they emit bytecode that reads from {@code this.fieldName}
 * and invokes the accessor interface method instead of direct field/method access.
 *
 * @see ForeignPackageMember
 */
public class DelegatingAccessorMember implements Member {

	private final Member member;
	private final String fieldName;

	public DelegatingAccessorMember(Member member, String fieldName) {
		this.member = member;
		this.fieldName = fieldName;
	}

	public Member getMember() {
		return member;
	}

	/**
	 * The name of the instance field on the generated accessor class
	 * that holds the single-value reader or writer for this property.
	 */
	public String getFieldName() {
		return fieldName;
	}

	@Override
	public Class<?> getDeclaringClass() {
		return member.getDeclaringClass();
	}

	@Override
	public String getName() {
		return member.getName();
	}

	@Override
	public int getModifiers() {
		return member.getModifiers();
	}

	@Override
	public boolean isSynthetic() {
		return member.isSynthetic();
	}
}
