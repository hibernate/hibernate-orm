/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import org.hibernate.annotations.CollectionIdJavaClass;
import org.hibernate.models.spi.ModelsContext;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
public class CollectionIdJavaClassAnnotation implements CollectionIdJavaClass {
	private Class<?> idType;

	@Override
	public Class<?> idType() {
		return idType;
	}

	public void idType(Class<?> idType) {
		this.idType = idType;
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return CollectionIdJavaClass.class;
	}

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public CollectionIdJavaClassAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public CollectionIdJavaClassAnnotation(CollectionIdJavaClass annotation, ModelsContext modelContext) {
		this.idType = annotation.idType();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public CollectionIdJavaClassAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.idType = (Class<?>) attributeValues.get( "idType" );
	}
}
