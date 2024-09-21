/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.View;
import org.hibernate.models.spi.SourceModelBuildingContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class ViewAnnotation implements View {

	private String query;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public ViewAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public ViewAnnotation(View annotation, SourceModelBuildingContext modelContext) {
		this.query = annotation.query();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public ViewAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
		this.query = (String) attributeValues.get( "query" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return View.class;
	}

	@Override
	public String query() {
		return query;
	}

	public void query(String value) {
		this.query = value;
	}


}
