/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.DefaultSchema;
import org.hibernate.models.spi.ModelsContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class DefaultSchemaAnnotation implements DefaultSchema {
	private String schema;
	private String catalog;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public DefaultSchemaAnnotation(ModelsContext modelContext) {
		this.schema = "";
		this.catalog = "";
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public DefaultSchemaAnnotation(DefaultSchema annotation, ModelsContext modelContext) {
		this.schema = annotation.schema();
		this.catalog = annotation.catalog();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public DefaultSchemaAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.schema = (String) attributeValues.get( "schema" );
		this.catalog = (String) attributeValues.get( "catalog" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return DefaultSchema.class;
	}

	@Override
	public String schema() {
		return schema;
	}

	public void schema(String value) {
		this.schema = value;
	}

	@Override
	public String catalog() {
		return catalog;
	}

	public void catalog(String value) {
		this.catalog = value;
	}
}
