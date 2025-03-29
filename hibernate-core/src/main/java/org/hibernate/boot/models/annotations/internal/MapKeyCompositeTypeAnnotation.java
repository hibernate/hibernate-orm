/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.MapKeyCompositeType;
import org.hibernate.models.spi.SourceModelBuildingContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class MapKeyCompositeTypeAnnotation implements MapKeyCompositeType {
	private java.lang.Class<? extends org.hibernate.usertype.CompositeUserType<?>> value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public MapKeyCompositeTypeAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public MapKeyCompositeTypeAnnotation(MapKeyCompositeType annotation, SourceModelBuildingContext modelContext) {
		this.value = annotation.value();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public MapKeyCompositeTypeAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
		this.value = (Class<? extends org.hibernate.usertype.CompositeUserType<?>>) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return MapKeyCompositeType.class;
	}

	@Override
	public java.lang.Class<? extends org.hibernate.usertype.CompositeUserType<?>> value() {
		return value;
	}

	public void value(java.lang.Class<? extends org.hibernate.usertype.CompositeUserType<?>> value) {
		this.value = value;
	}


}
