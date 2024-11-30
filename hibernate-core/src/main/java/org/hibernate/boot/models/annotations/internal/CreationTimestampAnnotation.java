/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.models.spi.SourceModelBuildingContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class CreationTimestampAnnotation implements CreationTimestamp {
	private org.hibernate.annotations.SourceType source;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public CreationTimestampAnnotation(SourceModelBuildingContext modelContext) {
		this.source = org.hibernate.annotations.SourceType.VM;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public CreationTimestampAnnotation(CreationTimestamp annotation, SourceModelBuildingContext modelContext) {
		this.source = annotation.source();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public CreationTimestampAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
		this.source = (org.hibernate.annotations.SourceType) attributeValues.get( "source" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return CreationTimestamp.class;
	}

	@Override
	public org.hibernate.annotations.SourceType source() {
		return source;
	}

	public void source(org.hibernate.annotations.SourceType value) {
		this.source = value;
	}


}
