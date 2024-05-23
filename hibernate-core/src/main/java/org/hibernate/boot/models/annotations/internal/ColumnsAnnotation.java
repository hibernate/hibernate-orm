/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.Columns;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.models.spi.AnnotationDescriptor;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class ColumnsAnnotation implements Columns {
	public static final AnnotationDescriptor<Columns> COLUMNS = null;

	private jakarta.persistence.Column[] columns;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public ColumnsAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public ColumnsAnnotation(Columns annotation, SourceModelBuildingContext modelContext) {
		this.columns = extractJdkValue( annotation, HibernateAnnotations.COLUMNS, "columns", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public ColumnsAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.columns = extractJandexValue( annotation, HibernateAnnotations.COLUMNS, "columns", modelContext );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Columns.class;
	}

	@Override
	public jakarta.persistence.Column[] columns() {
		return columns;
	}

	public void columns(jakarta.persistence.Column[] value) {
		this.columns = value;
	}


}
