/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.spi.RepeatableContainer;
import org.hibernate.models.spi.SourceModelBuildingContext;


import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.SequenceGenerators;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class SequenceGeneratorsJpaAnnotation implements SequenceGenerators, RepeatableContainer<SequenceGenerator> {
	private SequenceGenerator[] value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public SequenceGeneratorsJpaAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public SequenceGeneratorsJpaAnnotation(SequenceGenerators annotation, SourceModelBuildingContext modelContext) {
		this.value = extractJdkValue( annotation, JpaAnnotations.SEQUENCE_GENERATORS, "value", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public SequenceGeneratorsJpaAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
		this.value = (SequenceGenerator[]) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return SequenceGenerators.class;
	}

	@Override
	public SequenceGenerator[] value() {
		return value;
	}

	public void value(SequenceGenerator[] value) {
		this.value = value;
	}


}
