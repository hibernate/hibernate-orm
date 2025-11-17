/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.Cache;
import org.hibernate.models.spi.ModelsContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class CacheAnnotation implements Cache {
	private org.hibernate.annotations.CacheConcurrencyStrategy usage;
	private String region;
	private boolean includeLazy;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public CacheAnnotation(ModelsContext modelContext) {
		this.region = "";
		this.includeLazy = true;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public CacheAnnotation(Cache annotation, ModelsContext modelContext) {
		this.usage = annotation.usage();
		this.region = annotation.region();
		this.includeLazy = annotation.includeLazy();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public CacheAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.usage = (org.hibernate.annotations.CacheConcurrencyStrategy) attributeValues.get( "usage" );
		this.region = (String) attributeValues.get( "region" );
		this.includeLazy = (boolean) attributeValues.get( "includeLazy" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Cache.class;
	}

	@Override
	public org.hibernate.annotations.CacheConcurrencyStrategy usage() {
		return usage;
	}

	public void usage(org.hibernate.annotations.CacheConcurrencyStrategy value) {
		this.usage = value;
	}


	@Override
	public String region() {
		return region;
	}

	public void region(String value) {
		this.region = value;
	}


	@Override
	public boolean includeLazy() {
		return includeLazy;
	}

	public void includeLazy(boolean value) {
		this.includeLazy = value;
	}

}
