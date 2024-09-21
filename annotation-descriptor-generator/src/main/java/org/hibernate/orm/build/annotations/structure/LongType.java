/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.build.annotations.structure;

import javax.lang.model.element.AnnotationValue;

/**
 * @author Steve Ebersole
 */
public class LongType implements Type {
	public static final LongType LONG_TYPE = new LongType();

	@Override
	public String getTypeDeclarationString() {
		return "long";
	}

	@Override
	public String getInitializerValue(AnnotationValue defaultValue) {
		return Type.super.getInitializerValue( defaultValue ) + "L";
	}
}
