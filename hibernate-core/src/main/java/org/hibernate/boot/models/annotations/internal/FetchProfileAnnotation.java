/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.FetchProfile;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.models.spi.ModelsContext;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class FetchProfileAnnotation implements FetchProfile {
	private String name;
	private org.hibernate.annotations.FetchProfile.FetchOverride[] fetchOverrides;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public FetchProfileAnnotation(ModelsContext modelContext) {
		this.fetchOverrides = new org.hibernate.annotations.FetchProfile.FetchOverride[0];
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public FetchProfileAnnotation(FetchProfile annotation, ModelsContext modelContext) {
		this.name = annotation.name();
		this.fetchOverrides = extractJdkValue(
				annotation,
				HibernateAnnotations.FETCH_PROFILE,
				"fetchOverrides",
				modelContext
		);
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public FetchProfileAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.name = (String) attributeValues.get( "name" );
		this.fetchOverrides = (FetchOverride[]) attributeValues.get( "fetchOverrides" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return FetchProfile.class;
	}

	@Override
	public String name() {
		return name;
	}

	public void name(String value) {
		this.name = value;
	}


	@Override
	public org.hibernate.annotations.FetchProfile.FetchOverride[] fetchOverrides() {
		return fetchOverrides;
	}

	public void fetchOverrides(org.hibernate.annotations.FetchProfile.FetchOverride[] value) {
		this.fetchOverrides = value;
	}


}
