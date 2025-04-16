/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.SqlFragmentAlias;
import org.hibernate.models.spi.ModelsContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class SqlFragmentAliasAnnotation implements SqlFragmentAlias {
	private String alias;
	private String table;
	private java.lang.Class<?> entity;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public SqlFragmentAliasAnnotation(ModelsContext modelContext) {
		this.table = "";
		this.entity = void.class;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public SqlFragmentAliasAnnotation(SqlFragmentAlias annotation, ModelsContext modelContext) {
		this.alias = annotation.alias();
		this.table = annotation.table();
		this.entity = annotation.entity();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public SqlFragmentAliasAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.alias = (String) attributeValues.get( "alias" );
		this.table = (String) attributeValues.get( "table" );
		this.entity = (Class<?>) attributeValues.get( "entity" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return SqlFragmentAlias.class;
	}

	@Override
	public String alias() {
		return alias;
	}

	public void alias(String value) {
		this.alias = value;
	}


	@Override
	public String table() {
		return table;
	}

	public void table(String value) {
		this.table = value;
	}


	@Override
	public java.lang.Class<?> entity() {
		return entity;
	}

	public void entity(java.lang.Class<?> value) {
		this.entity = value;
	}


}
