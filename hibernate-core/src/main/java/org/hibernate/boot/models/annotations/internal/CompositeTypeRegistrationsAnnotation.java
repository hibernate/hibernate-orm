/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.CompositeTypeRegistration;
import org.hibernate.annotations.CompositeTypeRegistrations;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.annotations.spi.RepeatableContainer;
import org.hibernate.models.spi.ModelsContext;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class CompositeTypeRegistrationsAnnotation
		implements CompositeTypeRegistrations, RepeatableContainer<CompositeTypeRegistration> {
	private org.hibernate.annotations.CompositeTypeRegistration[] value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public CompositeTypeRegistrationsAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public CompositeTypeRegistrationsAnnotation(
			CompositeTypeRegistrations annotation,
			ModelsContext modelContext) {
		this.value = extractJdkValue(
				annotation,
				HibernateAnnotations.COMPOSITE_TYPE_REGISTRATIONS,
				"value",
				modelContext
		);
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public CompositeTypeRegistrationsAnnotation(
			Map<String, Object> attributeValues,
			ModelsContext modelContext) {
		this.value = (CompositeTypeRegistration[]) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return CompositeTypeRegistrations.class;
	}

	@Override
	public org.hibernate.annotations.CompositeTypeRegistration[] value() {
		return value;
	}

	public void value(org.hibernate.annotations.CompositeTypeRegistration[] value) {
		this.value = value;
	}


}
