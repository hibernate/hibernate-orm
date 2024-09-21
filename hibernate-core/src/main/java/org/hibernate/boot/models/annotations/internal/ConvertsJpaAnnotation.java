/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.spi.RepeatableContainer;
import org.hibernate.models.spi.SourceModelBuildingContext;

import jakarta.persistence.Converts;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class ConvertsJpaAnnotation implements Converts, RepeatableContainer<jakarta.persistence.Convert> {
	private jakarta.persistence.Convert[] value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public ConvertsJpaAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public ConvertsJpaAnnotation(Converts annotation, SourceModelBuildingContext modelContext) {
		this.value = extractJdkValue(
				annotation,
				JpaAnnotations.CONVERTS,
				"value",
				modelContext
		);
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public ConvertsJpaAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
		this.value = (jakarta.persistence.Convert[]) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Converts.class;
	}

	@Override
	public jakarta.persistence.Convert[] value() {
		return value;
	}

	public void value(jakarta.persistence.Convert[] value) {
		this.value = value;
	}


}
