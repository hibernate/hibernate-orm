/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.boot.models.annotations.spi.CustomSqlDetails;
import org.hibernate.models.spi.ModelsContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class SQLDeleteAnnotation implements SQLDelete, CustomSqlDetails {
	private String sql;
	private boolean callable;
	private java.lang.Class<? extends org.hibernate.jdbc.Expectation> verify;
	private org.hibernate.annotations.ResultCheckStyle check;
	private String table;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public SQLDeleteAnnotation(ModelsContext modelContext) {
		this.callable = false;
		this.verify = org.hibernate.jdbc.Expectation.class;
		this.check = org.hibernate.annotations.ResultCheckStyle.NONE;
		this.table = "";
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public SQLDeleteAnnotation(SQLDelete annotation, ModelsContext modelContext) {
		this.sql = annotation.sql();
		this.callable = annotation.callable();
		this.verify = annotation.verify();
		this.check = annotation.check();
		this.table = annotation.table();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public SQLDeleteAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.sql = (String) attributeValues.get( "sql" );
		this.callable = (boolean) attributeValues.get( "callable" );
		this.verify = (Class<? extends org.hibernate.jdbc.Expectation>) attributeValues.get( "verify" );
		this.check = (org.hibernate.annotations.ResultCheckStyle) attributeValues.get( "check" );
		this.table = (String) attributeValues.get( "table" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return SQLDelete.class;
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
	public org.hibernate.annotations.ResultCheckStyle check() {
		return check;
	}

	public void check(org.hibernate.annotations.ResultCheckStyle value) {
		this.check = value;
	}


	@Override
	public String table() {
		return table;
	}

	public void table(String value) {
		this.table = value;
	}


}
