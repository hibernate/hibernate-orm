/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.TypeBinderType;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class TypeBinderTypeAnnotation implements TypeBinderType {
	private java.lang.Class<? extends org.hibernate.binder.TypeBinder<?>> binder;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public TypeBinderTypeAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public TypeBinderTypeAnnotation(TypeBinderType annotation, SourceModelBuildingContext modelContext) {
		this.binder = extractJdkValue( annotation, HibernateAnnotations.TYPE_BINDER_TYPE, "binder", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public TypeBinderTypeAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.binder = extractJandexValue( annotation, HibernateAnnotations.TYPE_BINDER_TYPE, "binder", modelContext );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return TypeBinderType.class;
	}

	@Override
	public java.lang.Class<? extends org.hibernate.binder.TypeBinder<?>> binder() {
		return binder;
	}

	public void binder(java.lang.Class<? extends org.hibernate.binder.TypeBinder<?>> value) {
		this.binder = value;
	}


}
