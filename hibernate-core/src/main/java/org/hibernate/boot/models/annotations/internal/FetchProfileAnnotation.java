/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.FetchProfile;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class FetchProfileAnnotation implements FetchProfile {
	private String name;
	private org.hibernate.annotations.FetchProfile.FetchOverride[] fetchOverrides;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public FetchProfileAnnotation(SourceModelBuildingContext modelContext) {
		this.fetchOverrides = new org.hibernate.annotations.FetchProfile.FetchOverride[0];
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public FetchProfileAnnotation(FetchProfile annotation, SourceModelBuildingContext modelContext) {
		this.name = extractJdkValue( annotation, HibernateAnnotations.FETCH_PROFILE, "name", modelContext );
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
	public FetchProfileAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.name = extractJandexValue( annotation, HibernateAnnotations.FETCH_PROFILE, "name", modelContext );
		this.fetchOverrides = extractJandexValue(
				annotation,
				HibernateAnnotations.FETCH_PROFILE,
				"fetchOverrides",
				modelContext
		);
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
