/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.build.annotations.structure;

import javax.lang.model.element.AnnotationValue;
import javax.lang.model.type.DeclaredType;

/**
 * @author Steve Ebersole
 */
public class EnumType implements Type {
	private final DeclaredType underlyingType;

	public EnumType(DeclaredType underlyingType) {
		this.underlyingType = underlyingType;
	}

	@Override
	public String getTypeDeclarationString() {
		return underlyingType.toString();
	}

	@Override
	public String getInitializerValue(AnnotationValue defaultValue) {
		return underlyingType.toString() + "." + defaultValue.toString();
	}
}
