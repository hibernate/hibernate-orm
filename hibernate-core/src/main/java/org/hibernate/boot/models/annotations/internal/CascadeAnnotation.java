/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.Cascade;
import org.hibernate.models.spi.ModelsContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class CascadeAnnotation implements Cascade {
	private org.hibernate.annotations.CascadeType[] value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public CascadeAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public CascadeAnnotation(Cascade annotation, ModelsContext modelContext) {
		this.value = annotation.value();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public CascadeAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.value = (org.hibernate.annotations.CascadeType[]) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Cascade.class;
	}

	@Override
	public org.hibernate.annotations.CascadeType[] value() {
		return value;
	}

	public void value(org.hibernate.annotations.CascadeType[] value) {
		this.value = value;
	}


}
