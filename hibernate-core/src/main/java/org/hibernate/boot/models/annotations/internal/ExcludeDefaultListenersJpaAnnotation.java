/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.models.spi.ModelsContext;

import jakarta.persistence.ExcludeDefaultListeners;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class ExcludeDefaultListenersJpaAnnotation implements ExcludeDefaultListeners {

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public ExcludeDefaultListenersJpaAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public ExcludeDefaultListenersJpaAnnotation(
			ExcludeDefaultListeners annotation,
			ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public ExcludeDefaultListenersJpaAnnotation(
			Map<String, Object> attributeValues,
			ModelsContext modelContext) {
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return ExcludeDefaultListeners.class;
	}
}
