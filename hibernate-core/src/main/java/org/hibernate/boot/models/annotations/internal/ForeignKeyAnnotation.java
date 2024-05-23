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

import org.hibernate.annotations.ForeignKey;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class ForeignKeyAnnotation implements ForeignKey {
	private String name;
	private String inverseName;

	public ForeignKeyAnnotation(SourceModelBuildingContext modelContext) {
		this.name = "";
		this.inverseName = "";
	}

	public ForeignKeyAnnotation(ForeignKey annotation, SourceModelBuildingContext modelContext) {
		name( annotation.name() );
		inverseName( annotation.inverseName() );
	}

	public ForeignKeyAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		name( extractJandexValue( annotation, HibernateAnnotations.FOREIGN_KEY, "name", modelContext ) );
		inverseName( extractJandexValue( annotation, HibernateAnnotations.FOREIGN_KEY, "inverseName", modelContext ) );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return ForeignKey.class;
	}

	@Override
	public String name() {
		return name;
	}

	public void name(String value) {
		this.name = value;
	}

	@Override
	public String inverseName() {
		return inverseName;
	}

	public void inverseName(String value) {
		this.inverseName = value;
	}
}
