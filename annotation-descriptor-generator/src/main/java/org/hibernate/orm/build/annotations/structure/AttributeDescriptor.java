/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
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
