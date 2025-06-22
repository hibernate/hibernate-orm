/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.TimeZoneColumn;
import org.hibernate.models.spi.ModelsContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class TimeZoneColumnAnnotation implements TimeZoneColumn {
	private String name;
	private boolean insertable;
	private boolean updatable;
	private String columnDefinition;
	private String table;
	private String options;
	private String comment;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public TimeZoneColumnAnnotation(ModelsContext modelContext) {
		this.name = "";
		this.insertable = true;
		this.updatable = true;
		this.columnDefinition = "";
		this.table = "";
		this.options = "";
		this.comment = "";
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public TimeZoneColumnAnnotation(TimeZoneColumn annotation, ModelsContext modelContext) {
		this.name = annotation.name();
		this.insertable = annotation.insertable();
		this.updatable = annotation.updatable();
		this.columnDefinition = annotation.columnDefinition();
		this.table = annotation.table();
		this.options = annotation.options();
		this.comment = annotation.comment();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public TimeZoneColumnAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.name = (String) attributeValues.get( "name" );
		this.insertable = (boolean) attributeValues.get( "insertable" );
		this.updatable = (boolean) attributeValues.get( "updatable" );
		this.columnDefinition = (String) attributeValues.get( "columnDefinition" );
		this.table = (String) attributeValues.get( "table" );
		this.options = (String) attributeValues.get( "options" );
		this.comment = (String) attributeValues.get( "comment" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return TimeZoneColumn.class;
	}

	@Override
	public String name() {
		return name;
	}

	public void name(String value) {
		this.name = value;
	}


	@Override
	public boolean insertable() {
		return insertable;
	}

	public void insertable(boolean value) {
		this.insertable = value;
	}


	@Override
	public boolean updatable() {
		return updatable;
	}

	public void updatable(boolean value) {
		this.updatable = value;
	}


	@Override
	public String columnDefinition() {
		return columnDefinition;
	}

	public void columnDefinition(String value) {
		this.columnDefinition = value;
	}


	@Override
	public String table() {
		return table;
	}

	public void table(String value) {
		this.table = value;
	}

	@Override
	public String options() {
		return options;
	}

	public void options(String value) {
		this.options = value;
	}

	@Override
	public String comment() {
		return comment;
	}

	public void comment(String value) {
		this.comment = value;
	}
}
