/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.Temporal;
import org.hibernate.models.spi.ModelsContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class HistoryTableAnnotation implements Temporal.HistoryTable {
	private String name;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public HistoryTableAnnotation(ModelsContext modelContext) {
		this.name = "";
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public HistoryTableAnnotation(Temporal.HistoryTable annotation, ModelsContext modelContext) {
		this.name = annotation.name();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public HistoryTableAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.name = (String) attributeValues.get( "name" );
		if ( this.name == null ) {
			this.name = "";
		}
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Temporal.HistoryTable.class;
	}

	@Override
	public String name() {
		return name;
	}

	public void name(String value) {
		this.name = value;
	}
}
