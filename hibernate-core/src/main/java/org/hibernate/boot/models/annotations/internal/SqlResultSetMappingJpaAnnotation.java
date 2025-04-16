/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.models.spi.ModelsContext;

import jakarta.persistence.SqlResultSetMapping;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class SqlResultSetMappingJpaAnnotation implements SqlResultSetMapping {
	private String name;
	private jakarta.persistence.EntityResult[] entities;
	private jakarta.persistence.ConstructorResult[] classes;
	private jakarta.persistence.ColumnResult[] columns;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public SqlResultSetMappingJpaAnnotation(ModelsContext modelContext) {
		this.entities = new jakarta.persistence.EntityResult[0];
		this.classes = new jakarta.persistence.ConstructorResult[0];
		this.columns = new jakarta.persistence.ColumnResult[0];
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public SqlResultSetMappingJpaAnnotation(SqlResultSetMapping annotation, ModelsContext modelContext) {
		this.name = annotation.name();
		this.entities = extractJdkValue( annotation, JpaAnnotations.SQL_RESULT_SET_MAPPING, "entities", modelContext );
		this.classes = extractJdkValue( annotation, JpaAnnotations.SQL_RESULT_SET_MAPPING, "classes", modelContext );
		this.columns = extractJdkValue( annotation, JpaAnnotations.SQL_RESULT_SET_MAPPING, "columns", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public SqlResultSetMappingJpaAnnotation(
			Map<String, Object> attributeValues,
			ModelsContext modelContext) {
		this.name = (String) attributeValues.get( "name" );
		this.entities = (jakarta.persistence.EntityResult[]) attributeValues.get( "entities" );
		this.classes = (jakarta.persistence.ConstructorResult[]) attributeValues.get( "classes" );
		this.columns = (jakarta.persistence.ColumnResult[]) attributeValues.get( "columns" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return SqlResultSetMapping.class;
	}

	@Override
	public String name() {
		return name;
	}

	public void name(String value) {
		this.name = value;
	}


	@Override
	public jakarta.persistence.EntityResult[] entities() {
		return entities;
	}

	public void entities(jakarta.persistence.EntityResult[] value) {
		this.entities = value;
	}


	@Override
	public jakarta.persistence.ConstructorResult[] classes() {
		return classes;
	}

	public void classes(jakarta.persistence.ConstructorResult[] value) {
		this.classes = value;
	}


	@Override
	public jakarta.persistence.ColumnResult[] columns() {
		return columns;
	}

	public void columns(jakarta.persistence.ColumnResult[] value) {
		this.columns = value;
	}


}
