/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.models.spi.ModelsContext;

import jakarta.persistence.Temporal;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class TemporalJpaAnnotation implements Temporal {
	private jakarta.persistence.TemporalType value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public TemporalJpaAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public TemporalJpaAnnotation(Temporal annotation, ModelsContext modelContext) {
		this.value = annotation.value();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public TemporalJpaAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.value = (jakarta.persistence.TemporalType) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Temporal.class;
	}

	@Override
	public jakarta.persistence.TemporalType value() {
		return value;
	}

	public void value(jakarta.persistence.TemporalType value) {
		this.value = value;
	}


}
