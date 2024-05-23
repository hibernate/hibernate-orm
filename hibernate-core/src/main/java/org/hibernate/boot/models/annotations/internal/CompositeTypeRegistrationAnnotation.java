/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.CompositeTypeRegistration;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class CompositeTypeRegistrationAnnotation implements CompositeTypeRegistration {
	private java.lang.Class<?> embeddableClass;
	private java.lang.Class<? extends org.hibernate.usertype.CompositeUserType<?>> userType;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public CompositeTypeRegistrationAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public CompositeTypeRegistrationAnnotation(
			CompositeTypeRegistration annotation,
			SourceModelBuildingContext modelContext) {
		this.embeddableClass = extractJdkValue(
				annotation,
				HibernateAnnotations.COMPOSITE_TYPE_REGISTRATION,
				"embeddableClass",
				modelContext
		);
		this.userType = extractJdkValue(
				annotation,
				HibernateAnnotations.COMPOSITE_TYPE_REGISTRATION,
				"userType",
				modelContext
		);
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public CompositeTypeRegistrationAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.embeddableClass = extractJandexValue(
				annotation,
				HibernateAnnotations.COMPOSITE_TYPE_REGISTRATION,
				"embeddableClass",
				modelContext
		);
		this.userType = extractJandexValue(
				annotation,
				HibernateAnnotations.COMPOSITE_TYPE_REGISTRATION,
				"userType",
				modelContext
		);
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return CompositeTypeRegistration.class;
	}

	@Override
	public java.lang.Class<?> embeddableClass() {
		return embeddableClass;
	}

	public void embeddableClass(java.lang.Class<?> value) {
		this.embeddableClass = value;
	}


	@Override
	public java.lang.Class<? extends org.hibernate.usertype.CompositeUserType<?>> userType() {
		return userType;
	}

	public void userType(java.lang.Class<? extends org.hibernate.usertype.CompositeUserType<?>> value) {
		this.userType = value;
	}


}
