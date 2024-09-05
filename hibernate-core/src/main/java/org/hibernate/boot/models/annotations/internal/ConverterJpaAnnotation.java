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

import jakarta.persistence.Converter;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class ConverterJpaAnnotation implements Converter {

	private boolean autoApply;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public ConverterJpaAnnotation(SourceModelBuildingContext modelContext) {
		this.autoApply = false;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public ConverterJpaAnnotation(Converter annotation, SourceModelBuildingContext modelContext) {
		this.autoApply = annotation.autoApply();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public ConverterJpaAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.autoApply = extractJandexValue( annotation, JpaAnnotations.CONVERTER, "autoApply", modelContext );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Converter.class;
	}

	@Override
	public boolean autoApply() {
		return autoApply;
	}

	public void autoApply(boolean value) {
		this.autoApply = value;
	}


}
