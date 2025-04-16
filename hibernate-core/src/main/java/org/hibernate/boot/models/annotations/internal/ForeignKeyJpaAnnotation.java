/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.models.spi.ModelsContext;

import jakarta.persistence.ForeignKey;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class ForeignKeyJpaAnnotation implements ForeignKey {
	private String name;
	private jakarta.persistence.ConstraintMode value;
	private String foreignKeyDefinition;
	private String options;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public ForeignKeyJpaAnnotation(ModelsContext modelContext) {
		this.name = "";
		this.value = jakarta.persistence.ConstraintMode.CONSTRAINT;
		this.foreignKeyDefinition = "";
		this.options = "";
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public ForeignKeyJpaAnnotation(ForeignKey annotation, ModelsContext modelContext) {
		this.name = annotation.name();
		this.value = annotation.value();
		this.foreignKeyDefinition = annotation.foreignKeyDefinition();
		this.options = annotation.options();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public ForeignKeyJpaAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.name = (String) attributeValues.get( "name" );
		this.value = (jakarta.persistence.ConstraintMode) attributeValues.get( "value" );
		this.foreignKeyDefinition = (String) attributeValues.get( "foreignKeyDefinition" );
		this.options = (String) attributeValues.get( "options" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return ForeignKey.class;
	}

	@Override
	public String name() {
		return name;
	}

	public void name(String value) {
		this.name = value;
	}


	@Override
	public jakarta.persistence.ConstraintMode value() {
		return value;
	}

	public void value(jakarta.persistence.ConstraintMode value) {
		this.value = value;
	}


	@Override
	public String foreignKeyDefinition() {
		return foreignKeyDefinition;
	}

	public void foreignKeyDefinition(String value) {
		this.foreignKeyDefinition = value;
	}


	@Override
	public String options() {
		return options;
	}

	public void options(String value) {
		this.options = value;
	}


}
