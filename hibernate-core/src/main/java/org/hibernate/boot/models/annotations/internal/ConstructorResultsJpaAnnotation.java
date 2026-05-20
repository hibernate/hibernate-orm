/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.spi.RepeatableContainer;
import org.hibernate.models.spi.ModelsContext;

import jakarta.persistence.ConstructorResult;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class ConstructorResultsJpaAnnotation
		implements ConstructorResult.ConstructorResults, RepeatableContainer<ConstructorResult> {
	private jakarta.persistence.ConstructorResult[] value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public ConstructorResultsJpaAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public ConstructorResultsJpaAnnotation(
			ConstructorResult.ConstructorResults annotation,
			ModelsContext modelContext) {
		this.value = extractJdkValue( annotation, JpaAnnotations.CONSTRUCTOR_RESULTS, "value", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public ConstructorResultsJpaAnnotation(
			Map<String, Object> attributeValues,
			ModelsContext modelContext) {
		this.value = (ConstructorResult[]) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return ConstructorResult.ConstructorResults.class;
	}

	@Override
	public jakarta.persistence.ConstructorResult[] value() {
		return value;
	}

	public void value(jakarta.persistence.ConstructorResult[] value) {
		this.value = value;
	}
}
