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

import jakarta.persistence.ForeignKey;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class ForeignKeyJpaAnnotation implements ForeignKey {
	private String name;
	private jakarta.persistence.ConstraintMode value;
	private String foreignKeyDefinition;
	private String options;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public ForeignKeyJpaAnnotation(SourceModelBuildingContext modelContext) {
		this.name = "";
		this.value = jakarta.persistence.ConstraintMode.CONSTRAINT;
		this.foreignKeyDefinition = "";
		this.options = "";
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public ForeignKeyJpaAnnotation(ForeignKey annotation, SourceModelBuildingContext modelContext) {
		this.name = extractJdkValue( annotation, JpaAnnotations.FOREIGN_KEY, "name", modelContext );
		this.value = extractJdkValue( annotation, JpaAnnotations.FOREIGN_KEY, "value", modelContext );
		this.foreignKeyDefinition = extractJdkValue(
				annotation,
				JpaAnnotations.FOREIGN_KEY,
				"foreignKeyDefinition",
				modelContext
		);
		this.options = extractJdkValue( annotation, JpaAnnotations.FOREIGN_KEY, "options", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public ForeignKeyJpaAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.name = extractJandexValue( annotation, JpaAnnotations.FOREIGN_KEY, "name", modelContext );
		this.value = extractJandexValue( annotation, JpaAnnotations.FOREIGN_KEY, "value", modelContext );
		this.foreignKeyDefinition = extractJandexValue(
				annotation,
				JpaAnnotations.FOREIGN_KEY,
				"foreignKeyDefinition",
				modelContext
		);
		this.options = extractJandexValue( annotation, JpaAnnotations.FOREIGN_KEY, "options", modelContext );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return ForeignKey.class;
	}

	@Override
	public String name() {
		return name;
	}

	public void name(String value) {
		this.name = value;
	}


	@Override
	public jakarta.persistence.ConstraintMode value() {
		return value;
	}

	public void value(jakarta.persistence.ConstraintMode value) {
		this.value = value;
	}


	@Override
	public String foreignKeyDefinition() {
		return foreignKeyDefinition;
	}

	public void foreignKeyDefinition(String value) {
		this.foreignKeyDefinition = value;
	}


	@Override
	public String options() {
		return options;
	}

	public void options(String value) {
		this.options = value;
	}


}
