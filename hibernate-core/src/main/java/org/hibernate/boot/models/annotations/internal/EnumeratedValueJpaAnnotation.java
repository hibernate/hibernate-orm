/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.models.spi.ModelsContext;

import jakarta.persistence.EnumeratedValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class EnumeratedValueJpaAnnotation implements EnumeratedValue {
	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public EnumeratedValueJpaAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public EnumeratedValueJpaAnnotation(EnumeratedValue annotation, ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public EnumeratedValueJpaAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return EnumeratedValue.class;
	}
}
