/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class AnyDiscriminatorValueAnnotation implements AnyDiscriminatorValue {
	private String discriminator;
	private java.lang.Class<?> entity;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public AnyDiscriminatorValueAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public AnyDiscriminatorValueAnnotation(AnyDiscriminatorValue annotation, SourceModelBuildingContext modelContext) {
		this.discriminator = annotation.discriminator();
		this.entity = annotation.entity();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public AnyDiscriminatorValueAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.discriminator = extractJandexValue(
				annotation,
				HibernateAnnotations.ANY_DISCRIMINATOR_VALUE,
				"discriminator",
				modelContext
		);
		this.entity = extractJandexValue(
				annotation,
				HibernateAnnotations.ANY_DISCRIMINATOR_VALUE,
				"entity",
				modelContext
		);
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return AnyDiscriminatorValue.class;
	}

	@Override
	public String discriminator() {
		return discriminator;
	}

	public void discriminator(String value) {
		this.discriminator = value;
	}


	@Override
	public java.lang.Class<?> entity() {
		return entity;
	}

	public void entity(java.lang.Class<?> value) {
		this.entity = value;
	}


}
