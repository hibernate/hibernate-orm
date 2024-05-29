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

import jakarta.persistence.CheckConstraint;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class CheckConstraintJpaAnnotation implements CheckConstraint {

	private String name;
	private String constraint;
	private String options;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public CheckConstraintJpaAnnotation(SourceModelBuildingContext modelContext) {
		this.name = "";
		this.options = "";
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public CheckConstraintJpaAnnotation(CheckConstraint annotation, SourceModelBuildingContext modelContext) {
		this.name = annotation.name();
		this.constraint = annotation.constraint();
		this.options = annotation.options();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public CheckConstraintJpaAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.name = extractJandexValue( annotation, JpaAnnotations.CHECK_CONSTRAINT, "name", modelContext );
		this.constraint = extractJandexValue( annotation, JpaAnnotations.CHECK_CONSTRAINT, "constraint", modelContext );
		this.options = extractJandexValue( annotation, JpaAnnotations.CHECK_CONSTRAINT, "options", modelContext );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return CheckConstraint.class;
	}

	@Override
	public String name() {
		return name;
	}

	public void name(String value) {
		this.name = value;
	}


	@Override
	public String constraint() {
		return constraint;
	}

	public void constraint(String value) {
		this.constraint = value;
	}


	@Override
	public String options() {
		return options;
	}

	public void options(String value) {
		this.options = value;
	}


}
