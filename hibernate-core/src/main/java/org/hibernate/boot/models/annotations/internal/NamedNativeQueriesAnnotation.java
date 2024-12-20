/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.NamedNativeQueries;
import org.hibernate.annotations.NamedNativeQuery;
import org.hibernate.boot.models.annotations.spi.RepeatableContainer;
import org.hibernate.models.spi.SourceModelBuildingContext;

import static org.hibernate.boot.models.HibernateAnnotations.NAMED_NATIVE_QUERIES;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class NamedNativeQueriesAnnotation implements NamedNativeQueries, RepeatableContainer<NamedNativeQuery> {
	private NamedNativeQuery[] value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public NamedNativeQueriesAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public NamedNativeQueriesAnnotation(NamedNativeQueries annotation, SourceModelBuildingContext modelContext) {
		this.value = extractJdkValue( annotation, NAMED_NATIVE_QUERIES, "value", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public NamedNativeQueriesAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
		this.value = (NamedNativeQuery[]) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return NamedNativeQueries.class;
	}

	@Override
	public NamedNativeQuery[] value() {
		return value;
	}

	public void value(NamedNativeQuery[] value) {
		this.value = value;
	}


}
