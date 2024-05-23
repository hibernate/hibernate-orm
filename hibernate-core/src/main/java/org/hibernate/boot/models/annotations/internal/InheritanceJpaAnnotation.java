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

import jakarta.persistence.Inheritance;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class InheritanceJpaAnnotation implements Inheritance {
	private jakarta.persistence.InheritanceType strategy;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public InheritanceJpaAnnotation(SourceModelBuildingContext modelContext) {
		this.strategy = jakarta.persistence.InheritanceType.SINGLE_TABLE;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public InheritanceJpaAnnotation(Inheritance annotation, SourceModelBuildingContext modelContext) {
		this.strategy = extractJdkValue( annotation, JpaAnnotations.INHERITANCE, "strategy", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public InheritanceJpaAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.strategy = extractJandexValue( annotation, JpaAnnotations.INHERITANCE, "strategy", modelContext );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Inheritance.class;
	}

	@Override
	public jakarta.persistence.InheritanceType strategy() {
		return strategy;
	}

	public void strategy(jakarta.persistence.InheritanceType value) {
		this.strategy = value;
	}


}
