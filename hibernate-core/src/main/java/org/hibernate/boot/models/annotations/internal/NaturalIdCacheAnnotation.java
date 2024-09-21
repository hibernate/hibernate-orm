/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.models.spi.SourceModelBuildingContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class NaturalIdCacheAnnotation implements NaturalIdCache {
	private String region;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public NaturalIdCacheAnnotation(SourceModelBuildingContext modelContext) {
		this.region = "";
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public NaturalIdCacheAnnotation(NaturalIdCache annotation, SourceModelBuildingContext modelContext) {
		this.region = annotation.region();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public NaturalIdCacheAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
		this.region = (String) attributeValues.get( "region" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return NaturalIdCache.class;
	}

	@Override
	public String region() {
		return region;
	}

	public void region(String value) {
		this.region = value;
	}


}
