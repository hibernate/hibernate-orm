/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.GeneratorType;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class GeneratorTypeAnnotation implements GeneratorType {
	private java.lang.Class<? extends org.hibernate.tuple.ValueGenerator<?>> type;
	private org.hibernate.annotations.GenerationTime when;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public GeneratorTypeAnnotation(SourceModelBuildingContext modelContext) {
		this.when = org.hibernate.annotations.GenerationTime.ALWAYS;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public GeneratorTypeAnnotation(GeneratorType annotation, SourceModelBuildingContext modelContext) {
		this.type = extractJdkValue( annotation, HibernateAnnotations.GENERATOR_TYPE, "type", modelContext );
		this.when = extractJdkValue( annotation, HibernateAnnotations.GENERATOR_TYPE, "when", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public GeneratorTypeAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.type = extractJandexValue( annotation, HibernateAnnotations.GENERATOR_TYPE, "type", modelContext );
		this.when = extractJandexValue( annotation, HibernateAnnotations.GENERATOR_TYPE, "when", modelContext );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return GeneratorType.class;
	}

	@Override
	public java.lang.Class<? extends org.hibernate.tuple.ValueGenerator<?>> type() {
		return type;
	}

	public void type(java.lang.Class<? extends org.hibernate.tuple.ValueGenerator<?>> value) {
		this.type = value;
	}


	@Override
	public org.hibernate.annotations.GenerationTime when() {
		return when;
	}

	public void when(org.hibernate.annotations.GenerationTime value) {
		this.when = value;
	}


}
