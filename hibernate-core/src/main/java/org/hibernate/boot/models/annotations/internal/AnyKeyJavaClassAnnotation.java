/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.models.spi.ModelsContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class AnyKeyJavaClassAnnotation implements AnyKeyJavaClass {
	private java.lang.Class<?> value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public AnyKeyJavaClassAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public AnyKeyJavaClassAnnotation(AnyKeyJavaClass annotation, ModelsContext modelContext) {
		this.value = annotation.value();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public AnyKeyJavaClassAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.value = (Class<?>) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return AnyKeyJavaClass.class;
	}

	@Override
	public java.lang.Class<?> value() {
		return value;
	}

	public void value(java.lang.Class<?> value) {
		this.value = value;
	}


}
