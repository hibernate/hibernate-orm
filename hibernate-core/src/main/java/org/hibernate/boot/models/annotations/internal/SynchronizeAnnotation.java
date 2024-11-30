/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.Synchronize;
import org.hibernate.models.spi.SourceModelBuildingContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class SynchronizeAnnotation implements Synchronize {
	private String[] value;
	private boolean logical;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public SynchronizeAnnotation(SourceModelBuildingContext modelContext) {
		this.logical = true;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public SynchronizeAnnotation(Synchronize annotation, SourceModelBuildingContext modelContext) {
		this.value = annotation.value();
		this.logical = annotation.logical();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public SynchronizeAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
		this.value = (String[]) attributeValues.get( "value" );
		this.logical = (boolean) attributeValues.get( "logical" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Synchronize.class;
	}

	@Override
	public String[] value() {
		return value;
	}

	public void value(String[] value) {
		this.value = value;
	}


	@Override
	public boolean logical() {
		return logical;
	}

	public void logical(boolean value) {
		this.logical = value;
	}


}
