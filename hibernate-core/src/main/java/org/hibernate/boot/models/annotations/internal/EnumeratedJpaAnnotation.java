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

import jakarta.persistence.Enumerated;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class EnumeratedJpaAnnotation implements Enumerated {
	private jakarta.persistence.EnumType value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public EnumeratedJpaAnnotation(SourceModelBuildingContext modelContext) {
		this.value = jakarta.persistence.EnumType.ORDINAL;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public EnumeratedJpaAnnotation(Enumerated annotation, SourceModelBuildingContext modelContext) {
		this.value = annotation.value();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public EnumeratedJpaAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.value = extractJandexValue( annotation, JpaAnnotations.ENUMERATED, "value", modelContext );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Enumerated.class;
	}

	@Override
	public jakarta.persistence.EnumType value() {
		return value;
	}

	public void value(jakarta.persistence.EnumType value) {
		this.value = value;
	}


}
