/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.DerivedColumn;
import org.hibernate.models.spi.ModelsContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class DerivedColumnAnnotation implements DerivedColumn {
	private String name;
	private String table;
	private int sqlType;
	private String value;
	private boolean stored;
	private boolean hidden;
	private String comment;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public DerivedColumnAnnotation(ModelsContext modelContext) {
		this.table = "";
		this.stored = true;
		this.hidden = false;
		this.comment = "";
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public DerivedColumnAnnotation(DerivedColumn annotation, ModelsContext modelContext) {
		this.name = annotation.name();
		this.table = annotation.table();
		this.sqlType = annotation.sqlType();
		this.value = annotation.value();
		this.stored = annotation.stored();
		this.hidden = annotation.hidden();
		this.comment = annotation.comment();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public DerivedColumnAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.name = (String) attributeValues.get( "name" );
		this.table = (String) attributeValues.get( "table" );
		this.sqlType = (int) attributeValues.get( "sqlType" );
		this.value = (String) attributeValues.get( "value" );
		this.stored = (boolean) attributeValues.get( "stored" );
		this.hidden = (boolean) attributeValues.get( "hidden" );
		this.comment = (String) attributeValues.get( "comment" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return DerivedColumn.class;
	}

	@Override
	public String name() {
		return name;
	}

	public void name(String name) {
		this.name = name;
	}

	@Override
	public String table() {
		return table;
	}

	public void table(String table) {
		this.table = table;
	}

	@Override
	public int sqlType() {
		return sqlType;
	}

	public void sqlType(int sqlType) {
		this.sqlType = sqlType;
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

	@Override
	public String comment() {
		return comment;
	}

	public void comment(String comment) {
		this.comment = comment;
	}
}
