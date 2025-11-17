/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.IdGeneratorType;
import org.hibernate.models.spi.ModelsContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class IdGeneratorTypeAnnotation implements IdGeneratorType {
	private java.lang.Class<? extends org.hibernate.generator.Generator> value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public IdGeneratorTypeAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public IdGeneratorTypeAnnotation(IdGeneratorType annotation, ModelsContext modelContext) {
		this.value = annotation.value();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public IdGeneratorTypeAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.value = (Class<? extends org.hibernate.generator.Generator>) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return IdGeneratorType.class;
	}

	@Override
	public java.lang.Class<? extends org.hibernate.generator.Generator> value() {
		return value;
	}

	public void value(java.lang.Class<? extends org.hibernate.generator.Generator> value) {
		this.value = value;
	}


}
