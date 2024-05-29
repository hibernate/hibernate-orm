/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.Polymorphism;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class PolymorphismAnnotation implements Polymorphism {
	private org.hibernate.annotations.PolymorphismType type;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public PolymorphismAnnotation(SourceModelBuildingContext modelContext) {
		this.type = org.hibernate.annotations.PolymorphismType.IMPLICIT;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public PolymorphismAnnotation(Polymorphism annotation, SourceModelBuildingContext modelContext) {
		this.type = annotation.type();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public PolymorphismAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.type = extractJandexValue( annotation, HibernateAnnotations.POLYMORPHISM, "type", modelContext );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Polymorphism.class;
	}

	@Override
	public org.hibernate.annotations.PolymorphismType type() {
		return type;
	}

	public void type(org.hibernate.annotations.PolymorphismType value) {
		this.type = value;
	}


}
