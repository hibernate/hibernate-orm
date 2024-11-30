/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.Array;
import org.hibernate.models.spi.SourceModelBuildingContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class ArrayAnnotation implements Array {
	private int length;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public ArrayAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public ArrayAnnotation(Array annotation, SourceModelBuildingContext modelContext) {
		this.length = annotation.length();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public ArrayAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
		this.length = (int) attributeValues.get( "length" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Array.class;
	}

	@Override
	public int length() {
		return length;
	}

	public void length(int value) {
		this.length = value;
	}


}
