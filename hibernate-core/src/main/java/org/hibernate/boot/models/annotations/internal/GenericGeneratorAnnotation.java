/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class GenericGeneratorAnnotation implements GenericGenerator {
	private String name;
	private java.lang.Class<? extends org.hibernate.generator.Generator> type;
	private String strategy;
	private org.hibernate.annotations.Parameter[] parameters;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public GenericGeneratorAnnotation(SourceModelBuildingContext modelContext) {
		this.type = org.hibernate.generator.Generator.class;
		this.strategy = "native";
		this.parameters = new org.hibernate.annotations.Parameter[0];
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public GenericGeneratorAnnotation(GenericGenerator annotation, SourceModelBuildingContext modelContext) {
		this.name = annotation.name();
		this.type = annotation.type();
		this.strategy = annotation.strategy();
		this.parameters = extractJdkValue(
				annotation,
				HibernateAnnotations.GENERIC_GENERATOR,
				"parameters",
				modelContext
		);
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public GenericGeneratorAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.name = extractJandexValue( annotation, HibernateAnnotations.GENERIC_GENERATOR, "name", modelContext );
		this.type = extractJandexValue( annotation, HibernateAnnotations.GENERIC_GENERATOR, "type", modelContext );
		this.strategy = extractJandexValue(
				annotation,
				HibernateAnnotations.GENERIC_GENERATOR,
				"strategy",
				modelContext
		);
		this.parameters = extractJandexValue(
				annotation,
				HibernateAnnotations.GENERIC_GENERATOR,
				"parameters",
				modelContext
		);
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return GenericGenerator.class;
	}

	@Override
	public String name() {
		return name;
	}

	public void name(String value) {
		this.name = value;
	}


	@Override
	public java.lang.Class<? extends org.hibernate.generator.Generator> type() {
		return type;
	}

	public void type(java.lang.Class<? extends org.hibernate.generator.Generator> value) {
		this.type = value;
	}


	@Override
	public String strategy() {
		return strategy;
	}

	public void strategy(String value) {
		this.strategy = value;
	}


	@Override
	public org.hibernate.annotations.Parameter[] parameters() {
		return parameters;
	}

	public void parameters(org.hibernate.annotations.Parameter[] value) {
		this.parameters = value;
	}


}
