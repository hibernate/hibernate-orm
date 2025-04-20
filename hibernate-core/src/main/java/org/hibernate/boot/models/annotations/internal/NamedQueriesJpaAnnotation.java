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

import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class NamedQueriesJpaAnnotation implements NamedQueries, RepeatableContainer<NamedQuery> {
	private jakarta.persistence.NamedQuery[] value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public NamedQueriesJpaAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public NamedQueriesJpaAnnotation(NamedQueries annotation, ModelsContext modelContext) {
		this.value = extractJdkValue( annotation, JpaAnnotations.NAMED_QUERIES, "value", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public NamedQueriesJpaAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.value = (NamedQuery[]) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return NamedQueries.class;
	}

	@Override
	public jakarta.persistence.NamedQuery[] value() {
		return value;
	}

	public void value(jakarta.persistence.NamedQuery[] value) {
		this.value = value;
	}


}
