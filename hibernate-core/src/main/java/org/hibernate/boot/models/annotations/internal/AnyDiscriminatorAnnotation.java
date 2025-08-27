/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.models.spi.ModelsContext;

import java.lang.annotation.Annotation;
import java.util.Map;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class AnyDiscriminatorAnnotation implements AnyDiscriminator {
	private jakarta.persistence.DiscriminatorType value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public AnyDiscriminatorAnnotation(ModelsContext modelContext) {
		this.value = jakarta.persistence.DiscriminatorType.STRING;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public AnyDiscriminatorAnnotation(AnyDiscriminator annotation, ModelsContext modelContext) {
		this.value = annotation.value();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public AnyDiscriminatorAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.value = (jakarta.persistence.DiscriminatorType) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return AnyDiscriminator.class;
	}

	@Override
	public jakarta.persistence.DiscriminatorType value() {
		return value;
	}

	public void value(jakarta.persistence.DiscriminatorType value) {
		this.value = value;
	}
}
