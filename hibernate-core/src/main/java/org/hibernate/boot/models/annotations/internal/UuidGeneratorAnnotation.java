/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.UuidGenerator;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class UuidGeneratorAnnotation implements UuidGenerator {
	private org.hibernate.annotations.UuidGenerator.Style style;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public UuidGeneratorAnnotation(SourceModelBuildingContext modelContext) {
		this.style = org.hibernate.annotations.UuidGenerator.Style.AUTO;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public UuidGeneratorAnnotation(UuidGenerator annotation, SourceModelBuildingContext modelContext) {
		this.style = extractJdkValue( annotation, HibernateAnnotations.UUID_GENERATOR, "style", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public UuidGeneratorAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.style = extractJandexValue( annotation, HibernateAnnotations.UUID_GENERATOR, "style", modelContext );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return UuidGenerator.class;
	}

	@Override
	public org.hibernate.annotations.UuidGenerator.Style style() {
		return style;
	}

	public void style(org.hibernate.annotations.UuidGenerator.Style value) {
		this.style = value;
	}


}
