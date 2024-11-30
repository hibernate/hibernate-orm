/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.MapKeyJdbcTypeCode;
import org.hibernate.models.spi.SourceModelBuildingContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class MapKeyJdbcTypeCodeAnnotation implements MapKeyJdbcTypeCode {
	private int value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public MapKeyJdbcTypeCodeAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public MapKeyJdbcTypeCodeAnnotation(MapKeyJdbcTypeCode annotation, SourceModelBuildingContext modelContext) {
		this.value = annotation.value();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public MapKeyJdbcTypeCodeAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
		this.value = (int) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return MapKeyJdbcTypeCode.class;
	}

	@Override
	public int value() {
		return value;
	}

	public void value(int value) {
		this.value = value;
	}


}
