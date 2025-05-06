/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.spi.RepeatableContainer;
import org.hibernate.models.spi.ModelsContext;

import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.SqlResultSetMappings;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class SqlResultSetMappingsJpaAnnotation
		implements SqlResultSetMappings, RepeatableContainer<SqlResultSetMapping> {
	private SqlResultSetMapping[] value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public SqlResultSetMappingsJpaAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public SqlResultSetMappingsJpaAnnotation(SqlResultSetMappings annotation, ModelsContext modelContext) {
		this.value = extractJdkValue( annotation, JpaAnnotations.SQL_RESULT_SET_MAPPINGS, "value", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public SqlResultSetMappingsJpaAnnotation(
			Map<String, Object> attributeValues,
			ModelsContext modelContext) {
		this.value = (SqlResultSetMapping[]) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return SqlResultSetMappings.class;
	}

	@Override
	public SqlResultSetMapping[] value() {
		return value;
	}

	public void value(SqlResultSetMapping[] value) {
		this.value = value;
	}


}
