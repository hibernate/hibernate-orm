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

import jakarta.persistence.ColumnResult;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class ColumnResultJpaAnnotation implements ColumnResult {
	private String name;
	private java.lang.Class<?> type;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public ColumnResultJpaAnnotation(SourceModelBuildingContext modelContext) {
		this.type = void.class;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public ColumnResultJpaAnnotation(ColumnResult annotation, SourceModelBuildingContext modelContext) {
		this.name = annotation.name();
		this.type = annotation.type();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public ColumnResultJpaAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.name = extractJandexValue( annotation, JpaAnnotations.COLUMN_RESULT, "name", modelContext );
		this.type = extractJandexValue( annotation, JpaAnnotations.COLUMN_RESULT, "type", modelContext );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return ColumnResult.class;
	}

	@Override
	public String name() {
		return name;
	}

	public void name(String value) {
		this.name = value;
	}


	@Override
	public java.lang.Class<?> type() {
		return type;
	}

	public void type(java.lang.Class<?> value) {
		this.type = value;
	}


}
