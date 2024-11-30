/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.Fetch;
import org.hibernate.models.spi.SourceModelBuildingContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class FetchAnnotation implements Fetch {
	private org.hibernate.annotations.FetchMode value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public FetchAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public FetchAnnotation(Fetch annotation, SourceModelBuildingContext modelContext) {
		this.value = annotation.value();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public FetchAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
		this.value = (org.hibernate.annotations.FetchMode) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Fetch.class;
	}

	@Override
	public org.hibernate.annotations.FetchMode value() {
		return value;
	}

	public void value(org.hibernate.annotations.FetchMode value) {
		this.value = value;
	}


}
