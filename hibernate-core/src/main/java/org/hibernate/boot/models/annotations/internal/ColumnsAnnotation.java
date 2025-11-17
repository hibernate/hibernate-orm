/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.Columns;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.models.spi.AnnotationDescriptor;
import org.hibernate.models.spi.ModelsContext;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class ColumnsAnnotation implements Columns {
	public static final AnnotationDescriptor<Columns> COLUMNS = null;

	private jakarta.persistence.Column[] columns;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public ColumnsAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public ColumnsAnnotation(Columns annotation, ModelsContext modelContext) {
		this.columns = extractJdkValue( annotation, HibernateAnnotations.COLUMNS, "columns", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public ColumnsAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.columns = (jakarta.persistence.Column[]) attributeValues.get( "columns" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Columns.class;
	}

	@Override
	public jakarta.persistence.Column[] columns() {
		return columns;
	}

	public void columns(jakarta.persistence.Column[] value) {
		this.columns = value;
	}


}
