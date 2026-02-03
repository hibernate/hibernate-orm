/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import jakarta.persistence.ExcludedFromVersioning;
import org.hibernate.models.spi.ModelsContext;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
public class ExcludedFromVersioningJpaAnnotation implements ExcludedFromVersioning {

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public ExcludedFromVersioningJpaAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public ExcludedFromVersioningJpaAnnotation(ExcludedFromVersioning annotation, ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public ExcludedFromVersioningJpaAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return ExcludedFromVersioning.class;
	}
}
