/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import jakarta.persistence.FieldResult;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class FieldResultJpaAnnotation implements FieldResult {
	private String name;
	private String column;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public FieldResultJpaAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public FieldResultJpaAnnotation(FieldResult annotation, SourceModelBuildingContext modelContext) {
		this.name = annotation.name();
		this.column = annotation.column();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public FieldResultJpaAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.name = extractJandexValue( annotation, JpaAnnotations.FIELD_RESULT, "name", modelContext );
		this.column = extractJandexValue( annotation, JpaAnnotations.FIELD_RESULT, "column", modelContext );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return FieldResult.class;
	}

	@Override
	public String name() {
		return name;
	}

	public void name(String value) {
		this.name = value;
	}


	@Override
	public String column() {
		return column;
	}

	public void column(String value) {
		this.column = value;
	}


}
