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

import jakarta.persistence.UniqueConstraint;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class UniqueConstraintJpaAnnotation implements UniqueConstraint {
	private String name;
	private String[] columnNames;
	private String options;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public UniqueConstraintJpaAnnotation(SourceModelBuildingContext modelContext) {
		this.name = "";
		this.options = "";
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public UniqueConstraintJpaAnnotation(UniqueConstraint annotation, SourceModelBuildingContext modelContext) {
		this.name = extractJdkValue( annotation, JpaAnnotations.UNIQUE_CONSTRAINT, "name", modelContext );
		this.columnNames = extractJdkValue( annotation, JpaAnnotations.UNIQUE_CONSTRAINT, "columnNames", modelContext );
		this.options = extractJdkValue( annotation, JpaAnnotations.UNIQUE_CONSTRAINT, "options", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public UniqueConstraintJpaAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.name = extractJandexValue( annotation, JpaAnnotations.UNIQUE_CONSTRAINT, "name", modelContext );
		this.columnNames = extractJandexValue(
				annotation,
				JpaAnnotations.UNIQUE_CONSTRAINT,
				"columnNames",
				modelContext
		);
		this.options = extractJandexValue( annotation, JpaAnnotations.UNIQUE_CONSTRAINT, "options", modelContext );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return UniqueConstraint.class;
	}

	@Override
	public String name() {
		return name;
	}

	public void name(String value) {
		this.name = value;
	}


	@Override
	public String[] columnNames() {
		return columnNames;
	}

	public void columnNames(String[] value) {
		this.columnNames = value;
	}


	@Override
	public String options() {
		return options;
	}

	public void options(String value) {
		this.options = value;
	}


}
