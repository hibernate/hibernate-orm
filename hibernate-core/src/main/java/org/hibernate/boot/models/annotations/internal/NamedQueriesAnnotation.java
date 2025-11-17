/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;
import org.hibernate.boot.models.annotations.spi.RepeatableContainer;
import org.hibernate.models.spi.ModelsContext;

import static org.hibernate.boot.models.HibernateAnnotations.NAMED_QUERIES;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class NamedQueriesAnnotation implements NamedQueries, RepeatableContainer<NamedQuery> {
	private NamedQuery[] value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public NamedQueriesAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public NamedQueriesAnnotation(NamedQueries annotation, ModelsContext modelContext) {
		this.value = extractJdkValue( annotation, NAMED_QUERIES, "value", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public NamedQueriesAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.value = (NamedQuery[]) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return NamedQueries.class;
	}

	@Override
	public NamedQuery[] value() {
		return value;
	}

	public void value(NamedQuery[] value) {
		this.value = value;
	}


}
