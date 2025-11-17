/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.models.spi.ModelsContext;

import jakarta.persistence.AttributeOverrides;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class AttributeOverridesJpaAnnotation implements AttributeOverrides {
	private jakarta.persistence.AttributeOverride[] value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public AttributeOverridesJpaAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public AttributeOverridesJpaAnnotation(AttributeOverrides annotation, ModelsContext modelContext) {
		this.value = extractJdkValue( annotation, JpaAnnotations.ATTRIBUTE_OVERRIDES, "value", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public AttributeOverridesJpaAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.value = (jakarta.persistence.AttributeOverride[]) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return AttributeOverrides.class;
	}

	@Override
	public jakarta.persistence.AttributeOverride[] value() {
		return value;
	}

	public void value(jakarta.persistence.AttributeOverride[] value) {
		this.value = value;
	}


}
