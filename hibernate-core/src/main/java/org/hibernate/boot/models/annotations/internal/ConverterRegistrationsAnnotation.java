/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.ConverterRegistration;
import org.hibernate.annotations.ConverterRegistrations;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.annotations.spi.RepeatableContainer;
import org.hibernate.models.spi.ModelsContext;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class ConverterRegistrationsAnnotation
		implements ConverterRegistrations, RepeatableContainer<ConverterRegistration> {
	private org.hibernate.annotations.ConverterRegistration[] value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public ConverterRegistrationsAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public ConverterRegistrationsAnnotation(
			ConverterRegistrations annotation,
			ModelsContext modelContext) {
		this.value = extractJdkValue( annotation, HibernateAnnotations.CONVERTER_REGISTRATIONS, "value", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public ConverterRegistrationsAnnotation(
			Map<String, Object> attributeValues,
			ModelsContext modelContext) {
		this.value = (ConverterRegistration[]) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return ConverterRegistrations.class;
	}

	@Override
	public org.hibernate.annotations.ConverterRegistration[] value() {
		return value;
	}

	public void value(org.hibernate.annotations.ConverterRegistration[] value) {
		this.value = value;
	}


}
