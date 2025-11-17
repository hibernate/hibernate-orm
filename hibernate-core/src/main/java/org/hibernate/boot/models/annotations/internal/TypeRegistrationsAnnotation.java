/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.TypeRegistration;
import org.hibernate.annotations.TypeRegistrations;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.annotations.spi.RepeatableContainer;
import org.hibernate.models.spi.ModelsContext;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class TypeRegistrationsAnnotation implements TypeRegistrations, RepeatableContainer<TypeRegistration> {
	private org.hibernate.annotations.TypeRegistration[] value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public TypeRegistrationsAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public TypeRegistrationsAnnotation(TypeRegistrations annotation, ModelsContext modelContext) {
		this.value = extractJdkValue( annotation, HibernateAnnotations.TYPE_REGISTRATIONS, "value", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public TypeRegistrationsAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.value = (TypeRegistration[]) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return TypeRegistrations.class;
	}

	@Override
	public org.hibernate.annotations.TypeRegistration[] value() {
		return value;
	}

	public void value(org.hibernate.annotations.TypeRegistration[] value) {
		this.value = value;
	}


}
