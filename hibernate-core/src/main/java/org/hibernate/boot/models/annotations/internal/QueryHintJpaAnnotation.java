/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.models.spi.ModelsContext;

import jakarta.persistence.QueryHint;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class QueryHintJpaAnnotation implements QueryHint {
	private String name;
	private String value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public QueryHintJpaAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public QueryHintJpaAnnotation(QueryHint annotation, ModelsContext modelContext) {
		this.name = annotation.name();
		this.value = annotation.value();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public QueryHintJpaAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.name = (String) attributeValues.get( "name" );
		this.value = (String) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return QueryHint.class;
	}

	@Override
	public String name() {
		return name;
	}

	public void name(String value) {
		this.name = value;
	}


	@Override
	public String value() {
		return value;
	}

	public void value(String value) {
		this.value = value;
	}


}
