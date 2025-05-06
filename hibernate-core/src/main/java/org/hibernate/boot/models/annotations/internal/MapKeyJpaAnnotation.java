/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.models.spi.ModelsContext;

import jakarta.persistence.MapKey;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class MapKeyJpaAnnotation implements MapKey {
	private String name;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public MapKeyJpaAnnotation(ModelsContext modelContext) {
		this.name = "";
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public MapKeyJpaAnnotation(MapKey annotation, ModelsContext modelContext) {
		this.name = annotation.name();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public MapKeyJpaAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.name = (String) attributeValues.get( "name" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return MapKey.class;
	}

	@Override
	public String name() {
		return name;
	}

	public void name(String value) {
		this.name = value;
	}


}
