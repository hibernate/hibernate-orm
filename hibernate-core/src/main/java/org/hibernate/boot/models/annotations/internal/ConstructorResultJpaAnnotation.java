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

import jakarta.persistence.ConstructorResult;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class ConstructorResultJpaAnnotation implements ConstructorResult {

	private java.lang.Class<?> targetClass;
	private jakarta.persistence.ColumnResult[] columns;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public ConstructorResultJpaAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public ConstructorResultJpaAnnotation(ConstructorResult annotation, SourceModelBuildingContext modelContext) {
		this.targetClass = extractJdkValue(
				annotation,
				JpaAnnotations.CONSTRUCTOR_RESULT,
				"targetClass",
				modelContext
		);
		this.columns = extractJdkValue( annotation, JpaAnnotations.CONSTRUCTOR_RESULT, "columns", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public ConstructorResultJpaAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.targetClass = extractJandexValue(
				annotation,
				JpaAnnotations.CONSTRUCTOR_RESULT,
				"targetClass",
				modelContext
		);
		this.columns = extractJandexValue( annotation, JpaAnnotations.CONSTRUCTOR_RESULT, "columns", modelContext );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return ConstructorResult.class;
	}

	@Override
	public java.lang.Class<?> targetClass() {
		return targetClass;
	}

	public void targetClass(java.lang.Class<?> value) {
		this.targetClass = value;
	}


	@Override
	public jakarta.persistence.ColumnResult[] columns() {
		return columns;
	}

	public void columns(jakarta.persistence.ColumnResult[] value) {
		this.columns = value;
	}


}
