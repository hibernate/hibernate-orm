/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.GenericGenerators;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.annotations.spi.RepeatableContainer;
import org.hibernate.models.spi.ModelsContext;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class GenericGeneratorsAnnotation implements GenericGenerators, RepeatableContainer<GenericGenerator> {
	private org.hibernate.annotations.GenericGenerator[] value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public GenericGeneratorsAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public GenericGeneratorsAnnotation(GenericGenerators annotation, ModelsContext modelContext) {
		this.value = extractJdkValue( annotation, HibernateAnnotations.GENERIC_GENERATORS, "value", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public GenericGeneratorsAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.value = (GenericGenerator[]) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return GenericGenerators.class;
	}

	@Override
	public org.hibernate.annotations.GenericGenerator[] value() {
		return value;
	}

	public void value(org.hibernate.annotations.GenericGenerator[] value) {
		this.value = value;
	}


}
