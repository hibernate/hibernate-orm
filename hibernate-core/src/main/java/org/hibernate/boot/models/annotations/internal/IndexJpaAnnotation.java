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

import jakarta.persistence.Index;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class IndexJpaAnnotation implements Index {
	private String name;
	private String columnList;
	private boolean unique;
	private String options;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public IndexJpaAnnotation(SourceModelBuildingContext modelContext) {
		this.name = "";
		this.unique = false;
		this.options = "";
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public IndexJpaAnnotation(Index annotation, SourceModelBuildingContext modelContext) {
		this.name = annotation.name();
		this.columnList = annotation.columnList();
		this.unique = annotation.unique();
		this.options = annotation.options();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public IndexJpaAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.name = extractJandexValue( annotation, JpaAnnotations.INDEX, "name", modelContext );
		this.columnList = extractJandexValue( annotation, JpaAnnotations.INDEX, "columnList", modelContext );
		this.unique = extractJandexValue( annotation, JpaAnnotations.INDEX, "unique", modelContext );
		this.options = extractJandexValue( annotation, JpaAnnotations.INDEX, "options", modelContext );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Index.class;
	}

	@Override
	public String name() {
		return name;
	}

	public void name(String value) {
		this.name = value;
	}


	@Override
	public String columnList() {
		return columnList;
	}

	public void columnList(String value) {
		this.columnList = value;
	}


	@Override
	public boolean unique() {
		return unique;
	}

	public void unique(boolean value) {
		this.unique = value;
	}


	@Override
	public String options() {
		return options;
	}

	public void options(String value) {
		this.options = value;
	}


}
