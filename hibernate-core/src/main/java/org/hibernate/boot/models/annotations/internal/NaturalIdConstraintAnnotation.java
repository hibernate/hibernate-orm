/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.NaturalIdConstraint;
import org.hibernate.models.spi.ModelsContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class NaturalIdConstraintAnnotation implements NaturalIdConstraint {

	private String name;

	/**
	 * Used when creating dynamic annotation instances (XML)
	 */
	public NaturalIdConstraintAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used when creating annotation instances from JDK reflection
	 */
	public NaturalIdConstraintAnnotation(NaturalIdConstraint annotation, ModelsContext modelContext) {
		this.name = annotation.name();
	}

	/**
	 * Used when creating annotation instances from Jandex
	 */
	public NaturalIdConstraintAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.name = (String) attributeValues.get("name");
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return NaturalIdConstraint.class;
	}

	@Override
	public String name() {
		return name;
	}

	public void name(String name) {
		this.name = name;
	}
}
