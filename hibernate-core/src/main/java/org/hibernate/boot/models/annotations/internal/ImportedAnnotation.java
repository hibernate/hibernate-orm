/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.Imported;
import org.hibernate.models.spi.ModelsContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class ImportedAnnotation implements Imported {
	private String rename;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public ImportedAnnotation(ModelsContext modelContext) {
		this.rename = "";
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public ImportedAnnotation(Imported annotation, ModelsContext modelContext) {
		this.rename = annotation.rename();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public ImportedAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.rename = (String) attributeValues.get( "rename" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Imported.class;
	}

	@Override
	public String rename() {
		return rename;
	}

	public void rename(String value) {
		this.rename = value;
	}


}
