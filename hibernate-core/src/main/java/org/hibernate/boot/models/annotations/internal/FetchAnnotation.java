/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.Fetch;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class FetchAnnotation implements Fetch {
	private org.hibernate.annotations.FetchMode value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public FetchAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public FetchAnnotation(Fetch annotation, SourceModelBuildingContext modelContext) {
		this.value = annotation.value();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public FetchAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.value = extractJandexValue( annotation, HibernateAnnotations.FETCH, "value", modelContext );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Fetch.class;
	}

	@Override
	public org.hibernate.annotations.FetchMode value() {
		return value;
	}

	public void value(org.hibernate.annotations.FetchMode value) {
		this.value = value;
	}


}
