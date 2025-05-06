/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.models.spi.ModelsContext;

import jakarta.persistence.PrePersist;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class PrePersistJpaAnnotation implements PrePersist {

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public PrePersistJpaAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public PrePersistJpaAnnotation(PrePersist annotation, ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public PrePersistJpaAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return PrePersist.class;
	}
}
