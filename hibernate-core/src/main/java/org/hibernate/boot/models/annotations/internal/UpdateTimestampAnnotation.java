/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.models.spi.ModelsContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class UpdateTimestampAnnotation implements UpdateTimestamp {
	private org.hibernate.annotations.SourceType source;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public UpdateTimestampAnnotation(ModelsContext modelContext) {
		this.source = org.hibernate.annotations.SourceType.VM;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public UpdateTimestampAnnotation(UpdateTimestamp annotation, ModelsContext modelContext) {
		this.source = annotation.source();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public UpdateTimestampAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.source = (org.hibernate.annotations.SourceType) attributeValues.get( "source" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return UpdateTimestamp.class;
	}

	@Override
	public org.hibernate.annotations.SourceType source() {
		return source;
	}

	public void source(org.hibernate.annotations.SourceType value) {
		this.source = value;
	}


}
