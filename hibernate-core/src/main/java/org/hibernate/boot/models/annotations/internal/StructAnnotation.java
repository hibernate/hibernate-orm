/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.Struct;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class StructAnnotation implements Struct {
	private String name;
	private String catalog;
	private String schema;
	private String[] attributes;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public StructAnnotation(SourceModelBuildingContext modelContext) {
		this.attributes = new String[0];
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public StructAnnotation(Struct annotation, SourceModelBuildingContext modelContext) {
		this.name = extractJdkValue( annotation, HibernateAnnotations.STRUCT, "name", modelContext );
		this.attributes = extractJdkValue( annotation, HibernateAnnotations.STRUCT, "attributes", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public StructAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.name = extractJandexValue( annotation, HibernateAnnotations.STRUCT, "name", modelContext );
		this.attributes = extractJandexValue( annotation, HibernateAnnotations.STRUCT, "attributes", modelContext );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Struct.class;
	}

	@Override
	public String name() {
		return name;
	}

	public void name(String value) {
		this.name = value;
	}

	@Override
	public String catalog() {
		return catalog;
	}

	public void catalog(String value) {
		this.catalog = value;
	}

	@Override
	public String schema() {
		return schema;
	}

	public void schema(String value) {
		this.schema = value;
	}

	@Override
	public String[] attributes() {
		return attributes;
	}

	public void attributes(String[] value) {
		this.attributes = value;
	}


}
