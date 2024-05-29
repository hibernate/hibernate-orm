/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.Generated;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class GeneratedAnnotation implements Generated {
	private org.hibernate.generator.EventType[] event;
	private org.hibernate.annotations.GenerationTime value;
	private String sql;
	private boolean writable;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public GeneratedAnnotation(SourceModelBuildingContext modelContext) {
		this.event = new org.hibernate.generator.EventType[0];
		this.value = org.hibernate.annotations.GenerationTime.INSERT;
		this.sql = "";
		this.writable = false;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public GeneratedAnnotation(Generated annotation, SourceModelBuildingContext modelContext) {
		this.event = annotation.event();
		this.value = annotation.value();
		this.sql = annotation.sql();
		this.writable = annotation.writable();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public GeneratedAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.event = extractJandexValue( annotation, HibernateAnnotations.GENERATED, "event", modelContext );
		this.value = extractJandexValue( annotation, HibernateAnnotations.GENERATED, "value", modelContext );
		this.sql = extractJandexValue( annotation, HibernateAnnotations.GENERATED, "sql", modelContext );
		this.writable = extractJandexValue( annotation, HibernateAnnotations.GENERATED, "writable", modelContext );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Generated.class;
	}

	@Override
	public org.hibernate.generator.EventType[] event() {
		return event;
	}

	public void event(org.hibernate.generator.EventType[] value) {
		this.event = value;
	}


	@Override
	public org.hibernate.annotations.GenerationTime value() {
		return value;
	}

	public void value(org.hibernate.annotations.GenerationTime value) {
		this.value = value;
	}


	@Override
	public String sql() {
		return sql;
	}

	public void sql(String value) {
		this.sql = value;
	}


	@Override
	public boolean writable() {
		return writable;
	}

	public void writable(boolean value) {
		this.writable = value;
	}


}
