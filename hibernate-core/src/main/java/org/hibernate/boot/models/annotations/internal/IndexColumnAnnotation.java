/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.IndexColumn;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class IndexColumnAnnotation implements IndexColumn {
	private String name;
	private int base;
	private boolean nullable;
	private String columnDefinition;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public IndexColumnAnnotation(SourceModelBuildingContext modelContext) {
		this.base = 0;
		this.nullable = true;
		this.columnDefinition = "";
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public IndexColumnAnnotation(IndexColumn annotation, SourceModelBuildingContext modelContext) {
		this.name = annotation.name();
		this.base = annotation.base();
		this.nullable = annotation.nullable();
		this.columnDefinition = annotation.columnDefinition();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public IndexColumnAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.name = extractJandexValue( annotation, HibernateAnnotations.INDEX_COLUMN, "name", modelContext );
		this.base = extractJandexValue( annotation, HibernateAnnotations.INDEX_COLUMN, "base", modelContext );
		this.nullable = extractJandexValue( annotation, HibernateAnnotations.INDEX_COLUMN, "nullable", modelContext );
		this.columnDefinition = extractJandexValue(
				annotation,
				HibernateAnnotations.INDEX_COLUMN,
				"columnDefinition",
				modelContext
		);
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return IndexColumn.class;
	}

	@Override
	public String name() {
		return name;
	}

	public void name(String value) {
		this.name = value;
	}


	@Override
	public int base() {
		return base;
	}

	public void base(int value) {
		this.base = value;
	}


	@Override
	public boolean nullable() {
		return nullable;
	}

	public void nullable(boolean value) {
		this.nullable = value;
	}


	@Override
	public String columnDefinition() {
		return columnDefinition;
	}

	public void columnDefinition(String value) {
		this.columnDefinition = value;
	}


}
