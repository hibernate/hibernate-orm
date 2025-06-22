/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy;

import java.lang.annotation.Annotation;

import org.hibernate.bytecode.enhance.spi.UnloadedClass;

import net.bytebuddy.description.type.TypeDescription;

class UnloadedTypeDescription implements UnloadedClass {

	private final TypeDescription typeDescription;

	UnloadedTypeDescription(TypeDescription typeDescription) {
		this.typeDescription = typeDescription;
	}

	@Override
	public boolean hasAnnotation(Class<? extends Annotation> annotationType) {
		return typeDescription.getDeclaredAnnotations().isAnnotationPresent( annotationType );
	}

	@Override
	public String getName() {
		return typeDescription.getName();
	}
}
