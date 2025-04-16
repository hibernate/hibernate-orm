/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.models.spi.ModelsContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class NaturalIdCacheAnnotation implements NaturalIdCache {
	private String region;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public NaturalIdCacheAnnotation(ModelsContext modelContext) {
		this.region = "";
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public NaturalIdCacheAnnotation(NaturalIdCache annotation, ModelsContext modelContext) {
		this.region = annotation.region();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public NaturalIdCacheAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
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
