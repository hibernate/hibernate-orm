/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.SoftDelete;
import org.hibernate.models.spi.ModelsContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class SoftDeleteAnnotation implements SoftDelete {
	private String columnName;
	private String options;
	private String comment;
	private org.hibernate.annotations.SoftDeleteType strategy;
	private java.lang.Class<? extends jakarta.persistence.AttributeConverter<java.lang.Boolean, ?>> converter;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public SoftDeleteAnnotation(ModelsContext modelContext) {
		this.columnName = "";
		this.strategy = org.hibernate.annotations.SoftDeleteType.DELETED;
		this.converter = org.hibernate.annotations.SoftDelete.UnspecifiedConversion.class;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public SoftDeleteAnnotation(SoftDelete annotation, ModelsContext modelContext) {
		this.columnName = annotation.columnName();
		this.strategy = annotation.strategy();
		this.options = annotation.options();
		this.comment = annotation.comment();
		this.converter = annotation.converter();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public SoftDeleteAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.columnName = (String) attributeValues.get( "columnName" );
		this.strategy = (org.hibernate.annotations.SoftDeleteType) attributeValues.get( "strategy" );
		this.options = (String) attributeValues.get( "options" );
		this.comment = (String) attributeValues.get( "comment" );
		this.converter = (Class<? extends jakarta.persistence.AttributeConverter<Boolean, ?>>) attributeValues.
				get( "converter" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return SoftDelete.class;
	}

	@Override
	public String columnName() {
		return columnName;
	}

	public void columnName(String value) {
		this.columnName = value;
	}

	@Override
	public String options() {
		return options;
	}

	public void options(String options) {
		this.options = options;
	}

	@Override
	public String comment() {
		return comment;
	}

	public void comment(String comment) {
		this.comment = comment;
	}

	@Override
	public org.hibernate.annotations.SoftDeleteType strategy() {
		return strategy;
	}

	public void strategy(org.hibernate.annotations.SoftDeleteType value) {
		this.strategy = value;
	}


	@Override
	public java.lang.Class<? extends jakarta.persistence.AttributeConverter<java.lang.Boolean, ?>> converter() {
		return converter;
	}

	public void converter(java.lang.Class<? extends jakarta.persistence.AttributeConverter<java.lang.Boolean, ?>> value) {
		this.converter = value;
	}


}
