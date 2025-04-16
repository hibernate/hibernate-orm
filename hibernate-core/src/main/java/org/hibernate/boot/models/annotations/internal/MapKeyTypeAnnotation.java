/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.MapKeyType;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.models.spi.ModelsContext;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class MapKeyTypeAnnotation implements MapKeyType {
	private java.lang.Class<? extends org.hibernate.usertype.UserType<?>> value;
	private org.hibernate.annotations.Parameter[] parameters;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public MapKeyTypeAnnotation(ModelsContext modelContext) {
		this.parameters = new org.hibernate.annotations.Parameter[0];
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public MapKeyTypeAnnotation(MapKeyType annotation, ModelsContext modelContext) {
		this.value = annotation.value();
		this.parameters = extractJdkValue( annotation, HibernateAnnotations.MAP_KEY_TYPE, "parameters", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public MapKeyTypeAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.value = (Class<? extends org.hibernate.usertype.UserType<?>>) attributeValues.get( "value" );
		this.parameters = (org.hibernate.annotations.Parameter[]) attributeValues.get( "parameters" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return MapKeyType.class;
	}

	@Override
	public java.lang.Class<? extends org.hibernate.usertype.UserType<?>> value() {
		return value;
	}

	public void value(java.lang.Class<? extends org.hibernate.usertype.UserType<?>> value) {
		this.value = value;
	}


	@Override
	public org.hibernate.annotations.Parameter[] parameters() {
		return parameters;
	}

	public void parameters(org.hibernate.annotations.Parameter[] value) {
		this.parameters = value;
	}


}
