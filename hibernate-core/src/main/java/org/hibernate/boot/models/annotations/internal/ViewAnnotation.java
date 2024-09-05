/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.View;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class ViewAnnotation implements View {

	private String query;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public ViewAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public ViewAnnotation(View annotation, SourceModelBuildingContext modelContext) {
		this.query = annotation.query();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public ViewAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.query = extractJandexValue( annotation, HibernateAnnotations.VIEW, "query", modelContext );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return View.class;
	}

	@Override
	public String query() {
		return query;
	}

	public void query(String value) {
		this.query = value;
	}


}
