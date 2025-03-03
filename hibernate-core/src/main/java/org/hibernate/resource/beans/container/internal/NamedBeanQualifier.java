/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.beans.container.internal;

import jakarta.inject.Named;

/**
 * Used to locate named CDI beans.
 *
 * @author Yoann Rodière
 * @author Steve Ebersole
 */
public class NamedBeanQualifier extends jakarta.enterprise.util.AnnotationLiteral<Named> implements Named {
	private final String name;

	NamedBeanQualifier(String name) {
		this.name = name;
	}

	@Override
	public String value() {
		return name;
	}
}
