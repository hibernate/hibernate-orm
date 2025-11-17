/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.DiscriminatorFormula;
import org.hibernate.models.spi.ModelsContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class DiscriminatorFormulaAnnotation implements DiscriminatorFormula {
	private String value;
	private jakarta.persistence.DiscriminatorType discriminatorType;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public DiscriminatorFormulaAnnotation(ModelsContext modelContext) {
		this.discriminatorType = jakarta.persistence.DiscriminatorType.STRING;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public DiscriminatorFormulaAnnotation(DiscriminatorFormula annotation, ModelsContext modelContext) {
		this.value = annotation.value();
		this.discriminatorType = annotation.discriminatorType();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public DiscriminatorFormulaAnnotation(
			Map<String, Object> attributeValues,
			ModelsContext modelContext) {
		this.value = (String) attributeValues.get( "value" );
		this.discriminatorType = (jakarta.persistence.DiscriminatorType) attributeValues.get( "discriminatorType" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return DiscriminatorFormula.class;
	}

	@Override
	public String value() {
		return value;
	}

	public void value(String value) {
		this.value = value;
	}


	@Override
	public jakarta.persistence.DiscriminatorType discriminatorType() {
		return discriminatorType;
	}

	public void discriminatorType(jakarta.persistence.DiscriminatorType value) {
		this.discriminatorType = value;
	}


}
