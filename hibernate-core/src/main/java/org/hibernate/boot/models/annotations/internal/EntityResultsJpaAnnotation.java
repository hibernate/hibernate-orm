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

import jakarta.persistence.EntityResult;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class EntityResultsJpaAnnotation
		implements EntityResult.EntityResults, RepeatableContainer<EntityResult> {
	private jakarta.persistence.EntityResult[] value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public EntityResultsJpaAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public EntityResultsJpaAnnotation(
			EntityResult.EntityResults annotation,
			ModelsContext modelContext) {
		this.value = extractJdkValue( annotation, JpaAnnotations.ENTITY_RESULTS, "value", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public EntityResultsJpaAnnotation(
			Map<String, Object> attributeValues,
			ModelsContext modelContext) {
		this.value = (EntityResult[]) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return EntityResult.EntityResults.class;
	}

	@Override
	public jakarta.persistence.EntityResult[] value() {
		return value;
	}

	public void value(jakarta.persistence.EntityResult[] value) {
		this.value = value;
	}
}
