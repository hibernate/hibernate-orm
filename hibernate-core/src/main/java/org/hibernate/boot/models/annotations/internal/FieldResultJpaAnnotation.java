/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.models.spi.SourceModelBuildingContext;

import jakarta.persistence.FieldResult;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class FieldResultJpaAnnotation implements FieldResult {
	private String name;
	private String column;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public FieldResultJpaAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public FieldResultJpaAnnotation(FieldResult annotation, SourceModelBuildingContext modelContext) {
		this.name = annotation.name();
		this.column = annotation.column();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public FieldResultJpaAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
		this.name = (String) attributeValues.get( "name" );
		this.column = (String) attributeValues.get( "column" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return FieldResult.class;
	}

	@Override
	public String name() {
		return name;
	}

	public void name(String value) {
		this.name = value;
	}


	@Override
	public String column() {
		return column;
	}

	public void column(String value) {
		this.column = value;
	}


}
