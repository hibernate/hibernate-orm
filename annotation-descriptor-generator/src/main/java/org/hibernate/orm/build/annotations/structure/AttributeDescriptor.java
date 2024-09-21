/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.build.annotations.structure;

import javax.lang.model.element.AnnotationValue;

/**
 * @author Steve Ebersole
 */
public class AttributeDescriptor {
	private final String name;
	private final Type type;
	private final AnnotationValue defaultValue;

	public AttributeDescriptor(String name, Type type, AnnotationValue defaultValue) {
		this.name = name;
		this.type = type;
		this.defaultValue = defaultValue;
	}

	public String getName() {
		return name;
	}

	public Type getType() {
		return type;
	}

	public AnnotationValue getDefaultValue() {
		return defaultValue;
	}
}
