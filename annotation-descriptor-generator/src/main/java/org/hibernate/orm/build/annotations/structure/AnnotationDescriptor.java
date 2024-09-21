/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.build.annotations.structure;

import java.util.List;
import javax.lang.model.element.TypeElement;

/**
 * @author Steve Ebersole
 */
public record AnnotationDescriptor(
		TypeElement annotationType,
		String concreteTypeName,
		String constantsClassName,
		String constantName,
		String repeatableContainerConstantName,
		List<AttributeDescriptor> attributes) {
	public String getConstantFqn() {
		return constantsClassName() + "." + constantName();
	}
}
