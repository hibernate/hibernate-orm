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

import jakarta.persistence.GeneratedValue;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class GeneratedValueJpaAnnotation implements GeneratedValue {
	private jakarta.persistence.GenerationType strategy;
	private String generator;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public GeneratedValueJpaAnnotation(SourceModelBuildingContext modelContext) {
		this.strategy = jakarta.persistence.GenerationType.AUTO;
		this.generator = "";
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public GeneratedValueJpaAnnotation(GeneratedValue annotation, SourceModelBuildingContext modelContext) {
		this.strategy = extractJdkValue( annotation, JpaAnnotations.GENERATED_VALUE, "strategy", modelContext );
		this.generator = extractJdkValue( annotation, JpaAnnotations.GENERATED_VALUE, "generator", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public GeneratedValueJpaAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.strategy = extractJandexValue( annotation, JpaAnnotations.GENERATED_VALUE, "strategy", modelContext );
		this.generator = extractJandexValue( annotation, JpaAnnotations.GENERATED_VALUE, "generator", modelContext );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return GeneratedValue.class;
	}

	@Override
	public jakarta.persistence.GenerationType strategy() {
		return strategy;
	}

	public void strategy(jakarta.persistence.GenerationType value) {
		this.strategy = value;
	}


	@Override
	public String generator() {
		return generator;
	}

	public void generator(String value) {
		this.generator = value;
	}


}
