/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.CompositeType;
import org.hibernate.models.spi.SourceModelBuildingContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class CompositeTypeAnnotation implements CompositeType {
	private java.lang.Class<? extends org.hibernate.usertype.CompositeUserType<?>> value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public CompositeTypeAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public CompositeTypeAnnotation(CompositeType annotation, SourceModelBuildingContext modelContext) {
		this.value = annotation.value();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public CompositeTypeAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
		this.value = (Class<? extends org.hibernate.usertype.CompositeUserType<?>>) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return CompositeType.class;
	}

	@Override
	public java.lang.Class<? extends org.hibernate.usertype.CompositeUserType<?>> value() {
		return value;
	}

	public void value(java.lang.Class<? extends org.hibernate.usertype.CompositeUserType<?>> value) {
		this.value = value;
	}


}
