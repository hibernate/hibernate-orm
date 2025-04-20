/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.models.spi.ModelsContext;

import jakarta.persistence.CheckConstraint;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class CheckConstraintJpaAnnotation implements CheckConstraint {

	private String name;
	private String constraint;
	private String options;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public CheckConstraintJpaAnnotation(ModelsContext modelContext) {
		this.name = "";
		this.options = "";
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public CheckConstraintJpaAnnotation(CheckConstraint annotation, ModelsContext modelContext) {
		this.name = annotation.name();
		this.constraint = annotation.constraint();
		this.options = annotation.options();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public CheckConstraintJpaAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.name = (String) attributeValues.get( "name" );
		this.constraint = (String) attributeValues.get( "constraint" );
		this.options = (String) attributeValues.get( "options" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return CheckConstraint.class;
	}

	@Override
	public String name() {
		return name;
	}

	public void name(String value) {
		this.name = value;
	}


	@Override
	public String constraint() {
		return constraint;
	}

	public void constraint(String value) {
		this.constraint = value;
	}


	@Override
	public String options() {
		return options;
	}

	public void options(String value) {
		this.options = value;
	}


}
