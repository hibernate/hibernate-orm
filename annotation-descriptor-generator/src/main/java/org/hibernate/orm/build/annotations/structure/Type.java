/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.build.annotations.structure;

import javax.lang.model.element.AnnotationValue;

/**
 * @author Steve Ebersole
 */
public interface Type {
	String getTypeDeclarationString();

	default String getInitializerValue(AnnotationValue defaultValue) {
		return defaultValue.toString();
	}
}
