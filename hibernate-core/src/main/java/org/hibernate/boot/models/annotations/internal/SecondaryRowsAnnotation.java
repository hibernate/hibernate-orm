/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.SecondaryRow;
import org.hibernate.annotations.SecondaryRows;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.annotations.spi.RepeatableContainer;
import org.hibernate.models.spi.SourceModelBuildingContext;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class SecondaryRowsAnnotation implements SecondaryRows, RepeatableContainer<SecondaryRow> {
	private org.hibernate.annotations.SecondaryRow[] value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public SecondaryRowsAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public SecondaryRowsAnnotation(SecondaryRows annotation, SourceModelBuildingContext modelContext) {
		this.value = extractJdkValue( annotation, HibernateAnnotations.SECONDARY_ROWS, "value", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public SecondaryRowsAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
		this.value = (SecondaryRow[]) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return SecondaryRows.class;
	}

	@Override
	public org.hibernate.annotations.SecondaryRow[] value() {
		return value;
	}

	public void value(org.hibernate.annotations.SecondaryRow[] value) {
		this.value = value;
	}


}
