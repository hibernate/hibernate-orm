/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.Where;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class WhereAnnotation implements Where {
	private String clause;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public WhereAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public WhereAnnotation(Where annotation, SourceModelBuildingContext modelContext) {
		this.clause = extractJdkValue( annotation, HibernateAnnotations.WHERE, "clause", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public WhereAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.clause = extractJandexValue( annotation, HibernateAnnotations.WHERE, "clause", modelContext );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Where.class;
	}

	@Override
	public String clause() {
		return clause;
	}

	public void clause(String value) {
		this.clause = value;
	}


}
