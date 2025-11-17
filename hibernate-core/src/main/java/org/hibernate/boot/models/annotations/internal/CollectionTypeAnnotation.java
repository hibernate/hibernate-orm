/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.CollectionType;
import org.hibernate.models.spi.ModelsContext;

import static org.hibernate.boot.models.HibernateAnnotations.COLLECTION_TYPE;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class CollectionTypeAnnotation implements CollectionType {
	private java.lang.Class<? extends org.hibernate.usertype.UserCollectionType> type;
	private org.hibernate.annotations.Parameter[] parameters;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public CollectionTypeAnnotation(ModelsContext modelContext) {
		this.parameters = new org.hibernate.annotations.Parameter[0];
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public CollectionTypeAnnotation(CollectionType annotation, ModelsContext modelContext) {
		this.type = annotation.type();
		this.parameters = extractJdkValue( annotation, COLLECTION_TYPE, "parameters", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public CollectionTypeAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.type = (Class<? extends org.hibernate.usertype.UserCollectionType>) attributeValues.get( "type" );
		this.parameters = (org.hibernate.annotations.Parameter[]) attributeValues.get( "parameters" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return CollectionType.class;
	}

	@Override
	public java.lang.Class<? extends org.hibernate.usertype.UserCollectionType> type() {
		return type;
	}

	public void type(java.lang.Class<? extends org.hibernate.usertype.UserCollectionType> value) {
		this.type = value;
	}


	@Override
	public org.hibernate.annotations.Parameter[] parameters() {
		return parameters;
	}

	public void parameters(org.hibernate.annotations.Parameter[] value) {
		this.parameters = value;
	}


}
