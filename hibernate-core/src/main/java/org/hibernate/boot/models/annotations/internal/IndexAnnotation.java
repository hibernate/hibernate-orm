/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import org.hibernate.annotations.Index;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class IndexAnnotation implements Index {
	private String name;
	private String[] columnNames;

	public IndexAnnotation(SourceModelBuildingContext modelContext) {
		this.name = "";
		this.columnNames = new String[0];
	}

	public IndexAnnotation(Index annotation, SourceModelBuildingContext modelContext) {
		name( annotation.name() );
		columnNames( annotation.columnNames() );
	}

	public IndexAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		name( extractJandexValue( annotation, HibernateAnnotations.INDEX, "name", modelContext ) );
		columnNames( extractJandexValue( annotation, HibernateAnnotations.INDEX, "columnNames", modelContext ) );
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
	public String[] columnNames() {
		return columnNames;
	}

	public void columnNames(String[] value) {
		this.columnNames = value;
	}

}
