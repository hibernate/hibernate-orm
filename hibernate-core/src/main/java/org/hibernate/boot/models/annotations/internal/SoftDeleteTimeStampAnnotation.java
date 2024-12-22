/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import org.hibernate.annotations.SoftDeleteTimestamp;
import org.hibernate.annotations.SourceType;
import org.hibernate.models.spi.SourceModelBuildingContext;

import java.lang.annotation.Annotation;
import java.util.Map;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class SoftDeleteTimeStampAnnotation implements SoftDeleteTimestamp {
	private org.hibernate.annotations.SourceType source;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public SoftDeleteTimeStampAnnotation(SourceModelBuildingContext modelContext) {
		this.source = org.hibernate.annotations.SourceType.VM;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public SoftDeleteTimeStampAnnotation(SoftDeleteTimestamp annotation, SourceModelBuildingContext modelContext) {
		this.source = annotation.source();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public SoftDeleteTimeStampAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
		this.source = (org.hibernate.annotations.SourceType) attributeValues.get( "source" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return SoftDeleteTimestamp.class;
	}

	@Override
	public SourceType source() {
		return source;
	}

	public void source(org.hibernate.annotations.SourceType value) {
		this.source = value;
	}
}
