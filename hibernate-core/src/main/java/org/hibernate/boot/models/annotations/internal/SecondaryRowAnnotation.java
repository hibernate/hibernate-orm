/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.SecondaryRow;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class SecondaryRowAnnotation implements SecondaryRow {
	private String table;
	private boolean owned;
	private boolean optional;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public SecondaryRowAnnotation(SourceModelBuildingContext modelContext) {
		this.table = "";
		this.owned = true;
		this.optional = true;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public SecondaryRowAnnotation(SecondaryRow annotation, SourceModelBuildingContext modelContext) {
		this.table = extractJdkValue( annotation, HibernateAnnotations.SECONDARY_ROW, "table", modelContext );
		this.owned = extractJdkValue( annotation, HibernateAnnotations.SECONDARY_ROW, "owned", modelContext );
		this.optional = extractJdkValue( annotation, HibernateAnnotations.SECONDARY_ROW, "optional", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public SecondaryRowAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.table = extractJandexValue( annotation, HibernateAnnotations.SECONDARY_ROW, "table", modelContext );
		this.owned = extractJandexValue( annotation, HibernateAnnotations.SECONDARY_ROW, "owned", modelContext );
		this.optional = extractJandexValue( annotation, HibernateAnnotations.SECONDARY_ROW, "optional", modelContext );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return SecondaryRow.class;
	}

	@Override
	public String table() {
		return table;
	}

	public void table(String value) {
		this.table = value;
	}


	@Override
	public boolean owned() {
		return owned;
	}

	public void owned(boolean value) {
		this.owned = value;
	}


	@Override
	public boolean optional() {
		return optional;
	}

	public void optional(boolean value) {
		this.optional = value;
	}


}
