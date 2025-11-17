/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.models.spi.ModelsContext;

import jakarta.persistence.ConstructorResult;

import static org.hibernate.boot.models.JpaAnnotations.CONSTRUCTOR_RESULT;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class ConstructorResultJpaAnnotation implements ConstructorResult {

	private java.lang.Class<?> targetClass;
	private jakarta.persistence.ColumnResult[] columns;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public ConstructorResultJpaAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public ConstructorResultJpaAnnotation(ConstructorResult annotation, ModelsContext modelContext) {
		this.targetClass = annotation.targetClass();
		this.columns = extractJdkValue( annotation, CONSTRUCTOR_RESULT, "columns", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public ConstructorResultJpaAnnotation(
			Map<String, Object> attributeValues,
			ModelsContext modelContext) {
		this.targetClass = (Class<?>) attributeValues.get( "targetClass" );
		this.columns = (jakarta.persistence.ColumnResult[]) attributeValues.get( "columns" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return ConstructorResult.class;
	}

	@Override
	public java.lang.Class<?> targetClass() {
		return targetClass;
	}

	public void targetClass(java.lang.Class<?> value) {
		this.targetClass = value;
	}


	@Override
	public jakarta.persistence.ColumnResult[] columns() {
		return columns;
	}

	public void columns(jakarta.persistence.ColumnResult[] value) {
		this.columns = value;
	}


}
