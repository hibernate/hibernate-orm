/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.TimeZoneColumn;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class TimeZoneColumnAnnotation implements TimeZoneColumn {
	private String name;
	private boolean insertable;
	private boolean updatable;
	private String columnDefinition;
	private String table;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public TimeZoneColumnAnnotation(SourceModelBuildingContext modelContext) {
		this.name = "";
		this.insertable = true;
		this.updatable = true;
		this.columnDefinition = "";
		this.table = "";
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public TimeZoneColumnAnnotation(TimeZoneColumn annotation, SourceModelBuildingContext modelContext) {
		this.name = extractJdkValue( annotation, HibernateAnnotations.TIME_ZONE_COLUMN, "name", modelContext );
		this.insertable = extractJdkValue(
				annotation,
				HibernateAnnotations.TIME_ZONE_COLUMN,
				"insertable",
				modelContext
		);
		this.updatable = extractJdkValue(
				annotation,
				HibernateAnnotations.TIME_ZONE_COLUMN,
				"updatable",
				modelContext
		);
		this.columnDefinition = extractJdkValue(
				annotation,
				HibernateAnnotations.TIME_ZONE_COLUMN,
				"columnDefinition",
				modelContext
		);
		this.table = extractJdkValue( annotation, HibernateAnnotations.TIME_ZONE_COLUMN, "table", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public TimeZoneColumnAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.name = extractJandexValue( annotation, HibernateAnnotations.TIME_ZONE_COLUMN, "name", modelContext );
		this.insertable = extractJandexValue(
				annotation,
				HibernateAnnotations.TIME_ZONE_COLUMN,
				"insertable",
				modelContext
		);
		this.updatable = extractJandexValue(
				annotation,
				HibernateAnnotations.TIME_ZONE_COLUMN,
				"updatable",
				modelContext
		);
		this.columnDefinition = extractJandexValue(
				annotation,
				HibernateAnnotations.TIME_ZONE_COLUMN,
				"columnDefinition",
				modelContext
		);
		this.table = extractJandexValue( annotation, HibernateAnnotations.TIME_ZONE_COLUMN, "table", modelContext );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return TimeZoneColumn.class;
	}

	@Override
	public String name() {
		return name;
	}

	public void name(String value) {
		this.name = value;
	}


	@Override
	public boolean insertable() {
		return insertable;
	}

	public void insertable(boolean value) {
		this.insertable = value;
	}


	@Override
	public boolean updatable() {
		return updatable;
	}

	public void updatable(boolean value) {
		this.updatable = value;
	}


	@Override
	public String columnDefinition() {
		return columnDefinition;
	}

	public void columnDefinition(String value) {
		this.columnDefinition = value;
	}


	@Override
	public String table() {
		return table;
	}

	public void table(String value) {
		this.table = value;
	}


}
