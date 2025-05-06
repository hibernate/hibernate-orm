/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.ValueGenerationType;
import org.hibernate.models.spi.ModelsContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class ValueGenerationTypeAnnotation implements ValueGenerationType {
	private java.lang.Class<? extends org.hibernate.generator.Generator> generatedBy;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public ValueGenerationTypeAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public ValueGenerationTypeAnnotation(ValueGenerationType annotation, ModelsContext modelContext) {
		this.generatedBy = annotation.generatedBy();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public ValueGenerationTypeAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.generatedBy = (Class<? extends org.hibernate.generator.Generator>) attributeValues.get( "generatedBy" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return ValueGenerationType.class;
	}

	@Override
	public java.lang.Class<? extends org.hibernate.generator.Generator> generatedBy() {
		return generatedBy;
	}

	public void generatedBy(java.lang.Class<? extends org.hibernate.generator.Generator> value) {
		this.generatedBy = value;
	}


}
