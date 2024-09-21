/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.QueryCacheLayout;
import org.hibernate.models.spi.SourceModelBuildingContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class QueryCacheLayoutAnnotation implements QueryCacheLayout {
	private org.hibernate.annotations.CacheLayout layout;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public QueryCacheLayoutAnnotation(SourceModelBuildingContext modelContext) {
		this.layout = org.hibernate.annotations.CacheLayout.AUTO;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public QueryCacheLayoutAnnotation(QueryCacheLayout annotation, SourceModelBuildingContext modelContext) {
		this.layout = annotation.layout();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public QueryCacheLayoutAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
		this.layout = (org.hibernate.annotations.CacheLayout) attributeValues.get( "layout" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return QueryCacheLayout.class;
	}

	@Override
	public org.hibernate.annotations.CacheLayout layout() {
		return layout;
	}

	public void layout(org.hibernate.annotations.CacheLayout value) {
		this.layout = value;
	}


}
