/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;
import org.hibernate.models.spi.ModelsContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class GeneratedAnnotation implements Generated {
	private org.hibernate.generator.EventType[] event;
	private String sql;
	private boolean writable;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public GeneratedAnnotation(ModelsContext modelContext) {
		this.event = new org.hibernate.generator.EventType[] { EventType.INSERT };
		this.sql = "";
		this.writable = false;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public GeneratedAnnotation(Generated annotation, ModelsContext modelContext) {
		this.event = annotation.event();
		this.sql = annotation.sql();
		this.writable = annotation.writable();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public GeneratedAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.event = (EventType[]) attributeValues.get( "event" );
		this.sql = (String) attributeValues.get( "sql" );
		this.writable = (boolean) attributeValues.get( "writable" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Generated.class;
	}

	@Override
	public org.hibernate.generator.EventType[] event() {
		return event;
	}

	public void event(org.hibernate.generator.EventType[] value) {
		this.event = value;
	}


	@Override
	public String sql() {
		return sql;
	}

	public void sql(String value) {
		this.sql = value;
	}


	@Override
	public boolean writable() {
		return writable;
	}

	public void writable(boolean value) {
		this.writable = value;
	}


}
