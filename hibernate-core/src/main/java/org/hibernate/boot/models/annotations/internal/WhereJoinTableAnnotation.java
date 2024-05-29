/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.WhereJoinTable;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class WhereJoinTableAnnotation implements WhereJoinTable {
	private String clause;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public WhereJoinTableAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public WhereJoinTableAnnotation(WhereJoinTable annotation, SourceModelBuildingContext modelContext) {
		this.clause = annotation.clause();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public WhereJoinTableAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.clause = extractJandexValue( annotation, HibernateAnnotations.WHERE_JOIN_TABLE, "clause", modelContext );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return WhereJoinTable.class;
	}

	@Override
	public String clause() {
		return clause;
	}

	public void clause(String value) {
		this.clause = value;
	}


}
