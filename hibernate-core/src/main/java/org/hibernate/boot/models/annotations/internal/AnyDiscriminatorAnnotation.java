/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.AnyDiscriminator;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class AnyDiscriminatorAnnotation implements AnyDiscriminator {
	private jakarta.persistence.DiscriminatorType value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public AnyDiscriminatorAnnotation(SourceModelBuildingContext modelContext) {
		this.value = jakarta.persistence.DiscriminatorType.STRING;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public AnyDiscriminatorAnnotation(AnyDiscriminator annotation, SourceModelBuildingContext modelContext) {
		this.value = extractJdkValue( annotation, HibernateAnnotations.ANY_DISCRIMINATOR, "value", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public AnyDiscriminatorAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.value = extractJandexValue( annotation, HibernateAnnotations.ANY_DISCRIMINATOR, "value", modelContext );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return AnyDiscriminator.class;
	}

	@Override
	public jakarta.persistence.DiscriminatorType value() {
		return value;
	}

	public void value(jakarta.persistence.DiscriminatorType value) {
		this.value = value;
	}


}
