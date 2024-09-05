/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.Source;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class SourceAnnotation implements Source {
	private org.hibernate.annotations.SourceType value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public SourceAnnotation(SourceModelBuildingContext modelContext) {
		this.value = org.hibernate.annotations.SourceType.VM;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public SourceAnnotation(Source annotation, SourceModelBuildingContext modelContext) {
		this.value = annotation.value();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public SourceAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.value = extractJandexValue( annotation, HibernateAnnotations.SOURCE, "value", modelContext );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Source.class;
	}

	@Override
	public org.hibernate.annotations.SourceType value() {
		return value;
	}

	public void value(org.hibernate.annotations.SourceType value) {
		this.value = value;
	}


}
