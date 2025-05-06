/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import org.hibernate.annotations.EmbeddedColumnNaming;
import org.hibernate.models.spi.ModelsContext;

import java.lang.annotation.Annotation;
import java.util.Map;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
public class EmbeddedColumnNamingAnnotation implements EmbeddedColumnNaming {
	private String value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public EmbeddedColumnNamingAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public EmbeddedColumnNamingAnnotation(
			EmbeddedColumnNaming annotation,
			ModelsContext modelContext) {
		this.value = annotation.value();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public EmbeddedColumnNamingAnnotation(
			Map<String, Object> attributeValues,
			ModelsContext modelContext) {
		this.value = (String) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return EmbeddedColumnNaming.class;
	}

	@Override
	public String value() {
		return value;
	}

	public void value(String value) {
		this.value = value;
	}
}
