/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.models.spi.SourceModelBuildingContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class AnyDiscriminatorValueAnnotation implements AnyDiscriminatorValue {
	private String discriminator;
	private java.lang.Class<?> entity;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public AnyDiscriminatorValueAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public AnyDiscriminatorValueAnnotation(AnyDiscriminatorValue annotation, SourceModelBuildingContext modelContext) {
		this.discriminator = annotation.discriminator();
		this.entity = annotation.entity();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public AnyDiscriminatorValueAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
		this.discriminator = (String) attributeValues.get( "discriminator" );
		this.entity = (Class<?>) attributeValues.get( "entity" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return AnyDiscriminatorValue.class;
	}

	@Override
	public String discriminator() {
		return discriminator;
	}

	public void discriminator(String value) {
		this.discriminator = value;
	}


	@Override
	public java.lang.Class<?> entity() {
		return entity;
	}

	public void entity(java.lang.Class<?> value) {
		this.entity = value;
	}


}
