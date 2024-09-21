/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.build.annotations.structure;

import java.util.Locale;
import javax.lang.model.element.AnnotationValue;

/**
 * @author Steve Ebersole
 */
public class ArrayType implements Type {
	private final Type componentType;

	public ArrayType(Type componentType) {
		this.componentType = componentType;
	}

	@Override
	public String getTypeDeclarationString() {
		return componentType.getTypeDeclarationString() + "[]";
	}

	@Override
	public String getInitializerValue(AnnotationValue defaultValue) {
		return String.format( Locale.ROOT, "new %s[0]", componentType.getTypeDeclarationString() );
	}
}
