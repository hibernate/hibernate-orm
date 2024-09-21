/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.build.annotations.structure;

/**
 * @author Steve Ebersole
 */
public class DeclaredType implements Type {
	private final javax.lang.model.type.DeclaredType underlyingType;

	public DeclaredType(javax.lang.model.type.DeclaredType underlyingType) {
		this.underlyingType = underlyingType;
	}

	@Override
	public String getTypeDeclarationString() {
		return underlyingType.toString();
	}
}
