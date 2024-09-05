/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.ValueGenerationType;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class ValueGenerationTypeAnnotation implements ValueGenerationType {
	private java.lang.Class<? extends org.hibernate.generator.Generator> generatedBy;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public ValueGenerationTypeAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public ValueGenerationTypeAnnotation(ValueGenerationType annotation, SourceModelBuildingContext modelContext) {
		this.generatedBy = annotation.generatedBy();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public ValueGenerationTypeAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.generatedBy = extractJandexValue(
				annotation,
				HibernateAnnotations.VALUE_GENERATION_TYPE,
				"generatedBy",
				modelContext
		);
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return ValueGenerationType.class;
	}

	@Override
	public java.lang.Class<? extends org.hibernate.generator.Generator> generatedBy() {
		return generatedBy;
	}

	public void generatedBy(java.lang.Class<? extends org.hibernate.generator.Generator> value) {
		this.generatedBy = value;
	}


}
