/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.models.spi.ModelsContext;

import jakarta.persistence.IdClass;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class IdClassJpaAnnotation implements IdClass {
	private java.lang.Class<?> value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public IdClassJpaAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public IdClassJpaAnnotation(IdClass annotation, ModelsContext modelContext) {
		this.value = annotation.value();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public IdClassJpaAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.value = (Class<?>) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return IdClass.class;
	}

	@Override
	public java.lang.Class<?> value() {
		return value;
	}

	public void value(java.lang.Class<?> value) {
		this.value = value;
	}


}
