/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.TypeBinderType;
import org.hibernate.models.spi.SourceModelBuildingContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class TypeBinderTypeAnnotation implements TypeBinderType {
	private java.lang.Class<? extends org.hibernate.binder.TypeBinder<?>> binder;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public TypeBinderTypeAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public TypeBinderTypeAnnotation(TypeBinderType annotation, SourceModelBuildingContext modelContext) {
		this.binder = annotation.binder();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public TypeBinderTypeAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
		this.binder = (Class<? extends org.hibernate.binder.TypeBinder<?>>) attributeValues.get( "binder" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return TypeBinderType.class;
	}

	@Override
	public java.lang.Class<? extends org.hibernate.binder.TypeBinder<?>> binder() {
		return binder;
	}

	public void binder(java.lang.Class<? extends org.hibernate.binder.TypeBinder<?>> value) {
		this.binder = value;
	}


}
