/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.models.spi.ModelsContext;

import jakarta.persistence.GeneratedValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class GeneratedValueJpaAnnotation implements GeneratedValue {
	private jakarta.persistence.GenerationType strategy;
	private String generator;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public GeneratedValueJpaAnnotation(ModelsContext modelContext) {
		this.strategy = jakarta.persistence.GenerationType.AUTO;
		this.generator = "";
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public GeneratedValueJpaAnnotation(GeneratedValue annotation, ModelsContext modelContext) {
		this.strategy = annotation.strategy();
		this.generator = annotation.generator();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public GeneratedValueJpaAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.strategy = (jakarta.persistence.GenerationType) attributeValues.get( "strategy" );
		this.generator = (String) attributeValues.get( "generator" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return GeneratedValue.class;
	}

	@Override
	public jakarta.persistence.GenerationType strategy() {
		return strategy;
	}

	public void strategy(jakarta.persistence.GenerationType value) {
		this.strategy = value;
	}


	@Override
	public String generator() {
		return generator;
	}

	public void generator(String value) {
		this.generator = value;
	}


}
