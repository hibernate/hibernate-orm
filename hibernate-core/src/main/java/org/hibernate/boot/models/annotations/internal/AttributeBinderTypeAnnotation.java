/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.AttributeBinderType;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class AttributeBinderTypeAnnotation implements AttributeBinderType {
	private java.lang.Class<? extends org.hibernate.binder.AttributeBinder<?>> binder;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public AttributeBinderTypeAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public AttributeBinderTypeAnnotation(AttributeBinderType annotation, SourceModelBuildingContext modelContext) {
		this.binder = extractJdkValue(
				annotation,
				org.hibernate.boot.models.HibernateAnnotations.ATTRIBUTE_BINDER_TYPE,
				"binder",
				modelContext
		);
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public AttributeBinderTypeAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.binder = extractJandexValue(
				annotation,
				org.hibernate.boot.models.HibernateAnnotations.ATTRIBUTE_BINDER_TYPE,
				"binder",
				modelContext
		);
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return AttributeBinderType.class;
	}

	@Override
	public java.lang.Class<? extends org.hibernate.binder.AttributeBinder<?>> binder() {
		return binder;
	}

	public void binder(java.lang.Class<? extends org.hibernate.binder.AttributeBinder<?>> value) {
		this.binder = value;
	}


}
