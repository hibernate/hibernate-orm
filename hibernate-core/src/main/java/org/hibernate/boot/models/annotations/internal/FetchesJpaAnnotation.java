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

import jakarta.persistence.Fetch;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class FetchesJpaAnnotation implements Fetch.Fetches, RepeatableContainer<Fetch> {
	private Fetch[] value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public FetchesJpaAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public FetchesJpaAnnotation(Fetch.Fetches annotation, ModelsContext modelContext) {
		this.value = extractJdkValue( annotation, JpaAnnotations.FETCHES, "value", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public FetchesJpaAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.value = (Fetch[]) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Fetch.Fetches.class;
	}

	@Override
	public Fetch[] value() {
		return value;
	}

	public void value(Fetch[] value) {
		this.value = value;
	}
}
