/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.Check;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class CheckAnnotation implements Check {
	private String name;
	private String constraints;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public CheckAnnotation(SourceModelBuildingContext modelContext) {
		this.name = "";
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public CheckAnnotation(Check annotation, SourceModelBuildingContext modelContext) {
		this.name = annotation.name();
		this.constraints = annotation.constraints();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public CheckAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.name = extractJandexValue(
				annotation,
				org.hibernate.boot.models.HibernateAnnotations.CHECK,
				"name",
				modelContext
		);
		this.constraints = extractJandexValue(
				annotation,
				org.hibernate.boot.models.HibernateAnnotations.CHECK,
				"constraints",
				modelContext
		);
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Check.class;
	}

	@Override
	public String name() {
		return name;
	}

	public void name(String value) {
		this.name = value;
	}


	@Override
	public String constraints() {
		return constraints;
	}

	public void constraints(String value) {
		this.constraints = value;
	}


}
