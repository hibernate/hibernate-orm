/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.CollectionTypeRegistration;
import org.hibernate.models.spi.ModelsContext;

import static org.hibernate.boot.models.HibernateAnnotations.COLLECTION_TYPE_REGISTRATION;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class CollectionTypeRegistrationAnnotation implements CollectionTypeRegistration {
	private org.hibernate.metamodel.CollectionClassification classification;
	private java.lang.Class<? extends org.hibernate.usertype.UserCollectionType> type;
	private org.hibernate.annotations.Parameter[] parameters;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public CollectionTypeRegistrationAnnotation(ModelsContext modelContext) {
		this.parameters = new org.hibernate.annotations.Parameter[0];
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public CollectionTypeRegistrationAnnotation(
			CollectionTypeRegistration annotation,
			ModelsContext modelContext) {
		this.classification = annotation.classification();
		this.type = annotation.type();
		this.parameters = extractJdkValue( annotation, COLLECTION_TYPE_REGISTRATION, "parameters", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public CollectionTypeRegistrationAnnotation(
			Map<String, Object> attributeValues,
			ModelsContext modelContext) {
		this.classification = (org.hibernate.metamodel.CollectionClassification) attributeValues.get( "classification" );
		this.type = (Class<? extends org.hibernate.usertype.UserCollectionType>) attributeValues.get( "type" );
		this.parameters = (org.hibernate.annotations.Parameter[]) attributeValues.get( "parameters" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return CollectionTypeRegistration.class;
	}

	@Override
	public org.hibernate.metamodel.CollectionClassification classification() {
		return classification;
	}

	public void classification(org.hibernate.metamodel.CollectionClassification value) {
		this.classification = value;
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
