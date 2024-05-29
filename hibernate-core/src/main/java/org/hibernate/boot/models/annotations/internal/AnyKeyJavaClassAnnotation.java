/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class AnyKeyJavaClassAnnotation implements AnyKeyJavaClass {
	private java.lang.Class<?> value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public AnyKeyJavaClassAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public AnyKeyJavaClassAnnotation(AnyKeyJavaClass annotation, SourceModelBuildingContext modelContext) {
		this.value = annotation.value();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public AnyKeyJavaClassAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.value = extractJandexValue(
				annotation,
				org.hibernate.boot.models.HibernateAnnotations.ANY_KEY_JAVA_CLASS,
				"value",
				modelContext
		);
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return AnyKeyJavaClass.class;
	}

	@Override
	public java.lang.Class<?> value() {
		return value;
	}

	public void value(java.lang.Class<?> value) {
		this.value = value;
	}


}
