/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.models.spi.ModelsContext;

import jakarta.persistence.Converter;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class ConverterJpaAnnotation implements Converter {

	private boolean autoApply;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public ConverterJpaAnnotation(ModelsContext modelContext) {
		this.autoApply = false;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public ConverterJpaAnnotation(Converter annotation, ModelsContext modelContext) {
		this.autoApply = annotation.autoApply();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public ConverterJpaAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.autoApply = (boolean) attributeValues.get( "autoApply" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Converter.class;
	}

	@Override
	public boolean autoApply() {
		return autoApply;
	}

	public void autoApply(boolean value) {
		this.autoApply = value;
	}


}
