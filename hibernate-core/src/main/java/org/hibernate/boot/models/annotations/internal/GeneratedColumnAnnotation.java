/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.GeneratedColumn;
import org.hibernate.models.spi.ModelsContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class GeneratedColumnAnnotation implements GeneratedColumn {
	private String value;
	private boolean stored;
	private boolean hidden;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public GeneratedColumnAnnotation(ModelsContext modelContext) {
		this.stored = true;
		this.hidden = false;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public GeneratedColumnAnnotation(GeneratedColumn annotation, ModelsContext modelContext) {
		this.value = annotation.value();
		this.stored = annotation.stored();
		this.hidden = annotation.hidden();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public GeneratedColumnAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.value = (String) attributeValues.get( "value" );
		this.stored = (boolean) attributeValues.get( "stored" );
		this.hidden = (boolean) attributeValues.get( "hidden" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return GeneratedColumn.class;
	}

	@Override
	public String value() {
		return value;
	}

	public void value(String value) {
		this.value = value;
	}

	@Override
	public boolean stored() {
		return stored;
	}

	public void stored(boolean stored) {
		this.stored = stored;
	}

	@Override
	public boolean hidden() {
		return hidden;
	}

	public void hidden(boolean hidden) {
		this.hidden = hidden;
	}

}
