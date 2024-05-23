/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.LazyToOne;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class LazyToOneAnnotation implements LazyToOne {
	private org.hibernate.annotations.LazyToOneOption value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public LazyToOneAnnotation(SourceModelBuildingContext modelContext) {
		this.value = org.hibernate.annotations.LazyToOneOption.PROXY;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public LazyToOneAnnotation(LazyToOne annotation, SourceModelBuildingContext modelContext) {
		this.value = extractJdkValue( annotation, HibernateAnnotations.LAZY_TO_ONE, "value", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public LazyToOneAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.value = extractJandexValue( annotation, HibernateAnnotations.LAZY_TO_ONE, "value", modelContext );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return LazyToOne.class;
	}

	@Override
	public org.hibernate.annotations.LazyToOneOption value() {
		return value;
	}

	public void value(org.hibernate.annotations.LazyToOneOption value) {
		this.value = value;
	}


}
