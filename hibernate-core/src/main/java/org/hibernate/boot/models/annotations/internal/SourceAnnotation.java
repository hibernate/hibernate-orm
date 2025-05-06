/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.Source;
import org.hibernate.models.spi.ModelsContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class SourceAnnotation implements Source {
	private org.hibernate.annotations.SourceType value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public SourceAnnotation(ModelsContext modelContext) {
		this.value = org.hibernate.annotations.SourceType.VM;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public SourceAnnotation(Source annotation, ModelsContext modelContext) {
		this.value = annotation.value();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public SourceAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.value = (org.hibernate.annotations.SourceType) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Source.class;
	}

	@Override
	public org.hibernate.annotations.SourceType value() {
		return value;
	}

	public void value(org.hibernate.annotations.SourceType value) {
		this.value = value;
	}


}
