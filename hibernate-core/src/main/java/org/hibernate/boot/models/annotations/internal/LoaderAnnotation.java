/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.Loader;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class LoaderAnnotation implements Loader {
	private String namedQuery;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public LoaderAnnotation(SourceModelBuildingContext modelContext) {
		this.namedQuery = "";
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public LoaderAnnotation(Loader annotation, SourceModelBuildingContext modelContext) {
		this.namedQuery = extractJdkValue( annotation, HibernateAnnotations.LOADER, "namedQuery", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public LoaderAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.namedQuery = extractJandexValue( annotation, HibernateAnnotations.LOADER, "namedQuery", modelContext );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Loader.class;
	}

	@Override
	public String namedQuery() {
		return namedQuery;
	}

	public void namedQuery(String value) {
		this.namedQuery = value;
	}


}
