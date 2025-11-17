/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.boot.internal.Abstract;
import org.hibernate.models.spi.ModelsContext;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
public class AbstractXmlAnnotation implements Abstract {
	public AbstractXmlAnnotation(ModelsContext modelContext) {
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Abstract.class;
	}
}
