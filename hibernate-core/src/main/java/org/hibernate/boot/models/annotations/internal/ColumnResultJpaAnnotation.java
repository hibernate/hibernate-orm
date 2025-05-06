/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.models.spi.ModelsContext;

import jakarta.persistence.ColumnResult;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class ColumnResultJpaAnnotation implements ColumnResult {
	private String name;
	private java.lang.Class<?> type;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public ColumnResultJpaAnnotation(ModelsContext modelContext) {
		this.type = void.class;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public ColumnResultJpaAnnotation(ColumnResult annotation, ModelsContext modelContext) {
		this.name = annotation.name();
		this.type = annotation.type();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public ColumnResultJpaAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.name = (String) attributeValues.get( "name" );
		this.type = (Class<?>) attributeValues.get( "type" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return ColumnResult.class;
	}

	@Override
	public String name() {
		return name;
	}

	public void name(String value) {
		this.name = value;
	}


	@Override
	public java.lang.Class<?> type() {
		return type;
	}

	public void type(java.lang.Class<?> value) {
		this.type = value;
	}


}
