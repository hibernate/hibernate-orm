/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.JavaTypeRegistration;
import org.hibernate.annotations.JavaTypeRegistrations;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.annotations.spi.RepeatableContainer;
import org.hibernate.models.spi.ModelsContext;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class JavaTypeRegistrationsAnnotation
		implements JavaTypeRegistrations, RepeatableContainer<JavaTypeRegistration> {
	private org.hibernate.annotations.JavaTypeRegistration[] value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public JavaTypeRegistrationsAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public JavaTypeRegistrationsAnnotation(JavaTypeRegistrations annotation, ModelsContext modelContext) {
		this.value = extractJdkValue( annotation, HibernateAnnotations.JAVA_TYPE_REGISTRATIONS, "value", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public JavaTypeRegistrationsAnnotation(
			Map<String, Object> attributeValues,
			ModelsContext modelContext) {
		this.value = (JavaTypeRegistration[]) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return JavaTypeRegistrations.class;
	}

	@Override
	public org.hibernate.annotations.JavaTypeRegistration[] value() {
		return value;
	}

	public void value(org.hibernate.annotations.JavaTypeRegistration[] value) {
		this.value = value;
	}


}
