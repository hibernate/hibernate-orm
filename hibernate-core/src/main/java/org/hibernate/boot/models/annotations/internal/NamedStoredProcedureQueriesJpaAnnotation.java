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

import jakarta.persistence.NamedStoredProcedureQueries;
import jakarta.persistence.NamedStoredProcedureQuery;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class NamedStoredProcedureQueriesJpaAnnotation
		implements NamedStoredProcedureQueries, RepeatableContainer<NamedStoredProcedureQuery> {
	private jakarta.persistence.NamedStoredProcedureQuery[] value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public NamedStoredProcedureQueriesJpaAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public NamedStoredProcedureQueriesJpaAnnotation(
			NamedStoredProcedureQueries annotation,
			ModelsContext modelContext) {
		this.value = extractJdkValue(
				annotation,
				JpaAnnotations.NAMED_STORED_PROCEDURE_QUERIES,
				"value",
				modelContext
		);
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public NamedStoredProcedureQueriesJpaAnnotation(
			Map<String, Object> attributeValues,
			ModelsContext modelContext) {
		this.value = (NamedStoredProcedureQuery[]) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return NamedStoredProcedureQueries.class;
	}

	@Override
	public jakarta.persistence.NamedStoredProcedureQuery[] value() {
		return value;
	}

	public void value(jakarta.persistence.NamedStoredProcedureQuery[] value) {
		this.value = value;
	}


}
