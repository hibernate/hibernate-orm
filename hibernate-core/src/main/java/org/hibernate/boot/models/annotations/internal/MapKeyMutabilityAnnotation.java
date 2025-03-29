/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.MapKeyMutability;
import org.hibernate.models.spi.SourceModelBuildingContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class MapKeyMutabilityAnnotation implements MapKeyMutability {
	private java.lang.Class<? extends org.hibernate.type.descriptor.java.MutabilityPlan<?>> value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public MapKeyMutabilityAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public MapKeyMutabilityAnnotation(MapKeyMutability annotation, SourceModelBuildingContext modelContext) {
		this.value = annotation.value();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public MapKeyMutabilityAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
		this.value = (Class<? extends org.hibernate.type.descriptor.java.MutabilityPlan<?>>) attributeValues
				.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return MapKeyMutability.class;
	}

	@Override
	public java.lang.Class<? extends org.hibernate.type.descriptor.java.MutabilityPlan<?>> value() {
		return value;
	}

	public void value(java.lang.Class<? extends org.hibernate.type.descriptor.java.MutabilityPlan<?>> value) {
		this.value = value;
	}


}
