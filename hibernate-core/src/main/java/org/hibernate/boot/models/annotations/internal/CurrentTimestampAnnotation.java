/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.CurrentTimestamp;
import org.hibernate.models.spi.ModelsContext;

import static org.hibernate.generator.EventType.INSERT;
import static org.hibernate.generator.EventType.UPDATE;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class CurrentTimestampAnnotation implements CurrentTimestamp {
	private org.hibernate.generator.EventType[] event;
	private org.hibernate.annotations.SourceType source;
	private boolean allowMutation;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public CurrentTimestampAnnotation(ModelsContext modelContext) {
		this.event = new org.hibernate.generator.EventType[] {INSERT, UPDATE};
		this.source = org.hibernate.annotations.SourceType.DB;
		this.allowMutation = false;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public CurrentTimestampAnnotation(CurrentTimestamp annotation, ModelsContext modelContext) {
		this.event = annotation.event();
		this.source = annotation.source();
		this.allowMutation = annotation.allowMutation();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public CurrentTimestampAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.event = (org.hibernate.generator.EventType[]) attributeValues.get( "event" );
		this.source = (org.hibernate.annotations.SourceType) attributeValues.get( "source" );
		this.allowMutation = (Boolean) attributeValues.get( "allowMutation" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return CurrentTimestamp.class;
	}

	@Override
	public org.hibernate.generator.EventType[] event() {
		return event;
	}

	public void event(org.hibernate.generator.EventType[] value) {
		this.event = value;
	}

	@Override
	public org.hibernate.annotations.SourceType source() {
		return source;
	}

	public void source(org.hibernate.annotations.SourceType value) {
		this.source = value;
	}

	@Override
	public boolean allowMutation() {
		return allowMutation;
	}

	public void allowMutation(boolean value) {
		this.allowMutation = value;
	}
}
