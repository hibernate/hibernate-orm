/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.models.spi.ModelsContext;

import jakarta.persistence.Enumerated;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class EnumeratedJpaAnnotation implements Enumerated {
	private jakarta.persistence.EnumType value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public EnumeratedJpaAnnotation(ModelsContext modelContext) {
		this.value = jakarta.persistence.EnumType.ORDINAL;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public EnumeratedJpaAnnotation(Enumerated annotation, ModelsContext modelContext) {
		this.value = annotation.value();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public EnumeratedJpaAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.value = (jakarta.persistence.EnumType) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Enumerated.class;
	}

	@Override
	public jakarta.persistence.EnumType value() {
		return value;
	}

	public void value(jakarta.persistence.EnumType value) {
		this.value = value;
	}


}
