/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.build.annotations.structure;

import javax.lang.model.element.AnnotationValue;

/**
 * @author Steve Ebersole
 */
public class IntType implements Type {
	public static final IntType INT_TYPE = new IntType();

	@Override
	public String getTypeDeclarationString() {
		return "int";
	}

	@Override
	public String getInitializerValue(AnnotationValue defaultValue) {
		return defaultValue.toString();
	}
}
