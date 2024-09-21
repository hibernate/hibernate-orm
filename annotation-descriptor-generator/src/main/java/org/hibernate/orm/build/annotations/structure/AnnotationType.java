/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.build.annotations.structure;

import java.util.Locale;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.type.DeclaredType;

/**
 * @author Steve Ebersole
 */
public class AnnotationType implements Type {
	private final javax.lang.model.type.DeclaredType underlyingType;

	public AnnotationType(DeclaredType underlyingType) {
		this.underlyingType = underlyingType;
	}

	@Override
	public String getTypeDeclarationString() {
		return underlyingType.toString();
	}

	@Override
	public String getInitializerValue(AnnotationValue defaultValue) {
		return String.format(
				Locale.ROOT,
				"modelContext.getAnnotationDescriptorRegistry().getDescriptor(%s.class).createUsage(modelContext)",
				underlyingType.toString()
		);
	}
}
