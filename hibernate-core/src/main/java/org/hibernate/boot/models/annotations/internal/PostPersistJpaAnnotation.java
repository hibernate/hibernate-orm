/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.models.spi.SourceModelBuildingContext;

import jakarta.persistence.PostPersist;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class PostPersistJpaAnnotation implements PostPersist {

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public PostPersistJpaAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public PostPersistJpaAnnotation(PostPersist annotation, SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public PostPersistJpaAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return PostPersist.class;
	}
}
