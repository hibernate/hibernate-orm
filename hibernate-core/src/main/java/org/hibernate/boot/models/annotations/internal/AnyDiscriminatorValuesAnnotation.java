/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyDiscriminatorValues;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.annotations.spi.RepeatableContainer;
import org.hibernate.models.spi.ModelsContext;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class AnyDiscriminatorValuesAnnotation
		implements AnyDiscriminatorValues, RepeatableContainer<AnyDiscriminatorValue> {
	private org.hibernate.annotations.AnyDiscriminatorValue[] value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public AnyDiscriminatorValuesAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public AnyDiscriminatorValuesAnnotation(
			AnyDiscriminatorValues annotation,
			ModelsContext modelContext) {
		this.value = extractJdkValue(
				annotation,
				HibernateAnnotations.ANY_DISCRIMINATOR_VALUES,
				"value",
				modelContext
		);
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public AnyDiscriminatorValuesAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.value = (AnyDiscriminatorValue[]) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return AnyDiscriminatorValues.class;
	}

	@Override
	public org.hibernate.annotations.AnyDiscriminatorValue[] value() {
		return value;
	}

	public void value(org.hibernate.annotations.AnyDiscriminatorValue[] value) {
		this.value = value;
	}


}
