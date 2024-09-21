/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.models.spi.SourceModelBuildingContext;

import jakarta.persistence.Entity;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class EntityJpaAnnotation implements Entity {
	private String name;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public EntityJpaAnnotation(SourceModelBuildingContext modelContext) {
		this.name = "";
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public EntityJpaAnnotation(Entity annotation, SourceModelBuildingContext modelContext) {
		this.name = annotation.name();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public EntityJpaAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
		this.name = (String) attributeValues.get( "name" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Entity.class;
	}

	@Override
	public String name() {
		return name;
	}

	public void name(String value) {
		this.name = value;
	}


}
