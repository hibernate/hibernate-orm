/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.Cache;
import org.hibernate.models.spi.SourceModelBuildingContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class CacheAnnotation implements Cache {
	private org.hibernate.annotations.CacheConcurrencyStrategy usage;
	private String region;
	private boolean includeLazy;
	private String include;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public CacheAnnotation(SourceModelBuildingContext modelContext) {
		this.region = "";
		this.includeLazy = true;
		this.include = "all";
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public CacheAnnotation(Cache annotation, SourceModelBuildingContext modelContext) {
		this.usage = annotation.usage();
		this.region = annotation.region();
		this.includeLazy = annotation.includeLazy();
		this.include = annotation.include();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public CacheAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
		this.usage = (org.hibernate.annotations.CacheConcurrencyStrategy) attributeValues.get( "usage" );
		this.region = (String) attributeValues.get( "region" );
		this.includeLazy = (boolean) attributeValues.get( "includeLazy" );
		this.include = (String) attributeValues.get( "include" );
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


	@Override
	public String include() {
		return include;
	}

	public void include(String value) {
		this.include = value;
	}


}
