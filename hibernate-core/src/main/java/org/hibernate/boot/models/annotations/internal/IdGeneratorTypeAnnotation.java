/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.IdGeneratorType;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class IdGeneratorTypeAnnotation implements IdGeneratorType {
	private java.lang.Class<? extends org.hibernate.generator.Generator> value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public IdGeneratorTypeAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public IdGeneratorTypeAnnotation(IdGeneratorType annotation, SourceModelBuildingContext modelContext) {
		this.value = extractJdkValue( annotation, HibernateAnnotations.ID_GENERATOR_TYPE, "value", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public IdGeneratorTypeAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.value = extractJandexValue( annotation, HibernateAnnotations.ID_GENERATOR_TYPE, "value", modelContext );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return IdGeneratorType.class;
	}

	@Override
	public java.lang.Class<? extends org.hibernate.generator.Generator> value() {
		return value;
	}

	public void value(java.lang.Class<? extends org.hibernate.generator.Generator> value) {
		this.value = value;
	}


}
