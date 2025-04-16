/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.AttributeBinderType;
import org.hibernate.models.spi.ModelsContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class AttributeBinderTypeAnnotation implements AttributeBinderType {
	private java.lang.Class<? extends org.hibernate.binder.AttributeBinder<?>> binder;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public AttributeBinderTypeAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public AttributeBinderTypeAnnotation(AttributeBinderType annotation, ModelsContext modelContext) {
		this.binder = annotation.binder();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public AttributeBinderTypeAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.binder = (Class<? extends org.hibernate.binder.AttributeBinder<?>>) attributeValues.get( "binder" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return AttributeBinderType.class;
	}

	@Override
	public java.lang.Class<? extends org.hibernate.binder.AttributeBinder<?>> binder() {
		return binder;
	}

	public void binder(java.lang.Class<? extends org.hibernate.binder.AttributeBinder<?>> value) {
		this.binder = value;
	}


}
