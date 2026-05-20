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

import jakarta.persistence.ColumnResult;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class ColumnResultsJpaAnnotation
		implements ColumnResult.ColumnResults, RepeatableContainer<ColumnResult> {
	private jakarta.persistence.ColumnResult[] value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public ColumnResultsJpaAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public ColumnResultsJpaAnnotation(
			ColumnResult.ColumnResults annotation,
			ModelsContext modelContext) {
		this.value = extractJdkValue( annotation, JpaAnnotations.COLUMN_RESULTS, "value", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public ColumnResultsJpaAnnotation(
			Map<String, Object> attributeValues,
			ModelsContext modelContext) {
		this.value = (ColumnResult[]) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return ColumnResult.ColumnResults.class;
	}

	@Override
	public jakarta.persistence.ColumnResult[] value() {
		return value;
	}

	public void value(jakarta.persistence.ColumnResult[] value) {
		this.value = value;
	}
}
