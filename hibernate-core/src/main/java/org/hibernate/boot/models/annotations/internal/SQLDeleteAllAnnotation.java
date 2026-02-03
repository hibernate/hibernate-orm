/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import org.hibernate.annotations.SQLDeleteAll;
import org.hibernate.boot.models.annotations.spi.CustomSqlDetails;
import org.hibernate.models.spi.ModelsContext;

import java.lang.annotation.Annotation;
import java.util.Map;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class SQLDeleteAllAnnotation implements SQLDeleteAll, CustomSqlDetails {
	private String sql;
	private boolean callable;
	private java.lang.Class<? extends org.hibernate.jdbc.Expectation> verify;
	private String table;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public SQLDeleteAllAnnotation(ModelsContext modelContext) {
		this.callable = false;
		this.verify = org.hibernate.jdbc.Expectation.class;
		this.table = "";
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public SQLDeleteAllAnnotation(SQLDeleteAll annotation, ModelsContext modelContext) {
		this.sql = annotation.sql();
		this.callable = annotation.callable();
		this.verify = annotation.verify();
		this.table = annotation.table();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public SQLDeleteAllAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.sql = (String) attributeValues.get( "sql" );
		this.callable = (boolean) attributeValues.get( "callable" );
		//noinspection unchecked
		this.verify = (Class<? extends org.hibernate.jdbc.Expectation>) attributeValues.get( "verify" );
		this.table = (String) attributeValues.get( "table" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return SQLDeleteAll.class;
	}

	@Override
	public String sql() {
		return sql;
	}

	public void sql(String value) {
		this.sql = value;
	}


	@Override
	public boolean callable() {
		return callable;
	}

	public void callable(boolean value) {
		this.callable = value;
	}


	@Override
	public java.lang.Class<? extends org.hibernate.jdbc.Expectation> verify() {
		return verify;
	}

	public void verify(java.lang.Class<? extends org.hibernate.jdbc.Expectation> value) {
		this.verify = value;
	}


	@Override
	public String table() {
		return table;
	}

	public void table(String value) {
		this.table = value;
	}


}
