/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.FetchProfileOverride;
import org.hibernate.boot.models.DialectOverrideAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class FetchProfileOverrideAnnotation implements FetchProfileOverride {

	private org.hibernate.annotations.FetchMode mode;
	private jakarta.persistence.FetchType fetch;
	private String profile;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public FetchProfileOverrideAnnotation(SourceModelBuildingContext modelContext) {
		this.mode = org.hibernate.annotations.FetchMode.JOIN;
		this.fetch = jakarta.persistence.FetchType.EAGER;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public FetchProfileOverrideAnnotation(FetchProfileOverride annotation, SourceModelBuildingContext modelContext) {
		this.mode = annotation.mode();
		this.fetch = annotation.fetch();
		this.profile = annotation.profile();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public FetchProfileOverrideAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.mode = extractJandexValue(
				annotation,
				DialectOverrideAnnotations.FETCH_PROFILE_OVERRIDE,
				"mode",
				modelContext
		);
		this.fetch = extractJandexValue(
				annotation,
				DialectOverrideAnnotations.FETCH_PROFILE_OVERRIDE,
				"fetch",
				modelContext
		);
		this.profile = extractJandexValue(
				annotation,
				DialectOverrideAnnotations.FETCH_PROFILE_OVERRIDE,
				"profile",
				modelContext
		);
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return FetchProfileOverride.class;
	}

	@Override
	public org.hibernate.annotations.FetchMode mode() {
		return mode;
	}

	public void mode(org.hibernate.annotations.FetchMode value) {
		this.mode = value;
	}


	@Override
	public jakarta.persistence.FetchType fetch() {
		return fetch;
	}

	public void fetch(jakarta.persistence.FetchType value) {
		this.fetch = value;
	}


	@Override
	public String profile() {
		return profile;
	}

	public void profile(String value) {
		this.profile = value;
	}


}
