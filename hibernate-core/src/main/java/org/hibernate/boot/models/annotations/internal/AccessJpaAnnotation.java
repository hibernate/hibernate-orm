/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.models.spi.ModelsContext;

import jakarta.persistence.Access;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class AccessJpaAnnotation implements Access {

	private jakarta.persistence.AccessType value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public AccessJpaAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public AccessJpaAnnotation(Access annotation, ModelsContext modelContext) {
		this.value = annotation.value();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public AccessJpaAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.value = (jakarta.persistence.AccessType) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Access.class;
	}

	@Override
	public jakarta.persistence.AccessType value() {
		return value;
	}

	public void value(jakarta.persistence.AccessType value) {
		this.value = value;
	}
}
