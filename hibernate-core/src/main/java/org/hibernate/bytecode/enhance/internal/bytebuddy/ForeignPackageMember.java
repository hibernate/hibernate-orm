/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy;

import java.lang.reflect.Member;

public class ForeignPackageMember implements Member {

	private final Class<?> foreignPackageAccessor;
	private final Member member;

	public ForeignPackageMember(Class<?> foreignPackageAccessor, Member member) {
		this.foreignPackageAccessor = foreignPackageAccessor;
		this.member = member;
	}

	public Class<?> getForeignPackageAccessor() {
		return foreignPackageAccessor;
	}

	public Member getMember() {
		return member;
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
