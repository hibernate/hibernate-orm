/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.TypeRegistration;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class TypeRegistrationAnnotation implements TypeRegistration {
	private java.lang.Class<?> basicClass;
	private java.lang.Class<? extends org.hibernate.usertype.UserType<?>> userType;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public TypeRegistrationAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public TypeRegistrationAnnotation(TypeRegistration annotation, SourceModelBuildingContext modelContext) {
		this.basicClass = extractJdkValue(
				annotation,
				HibernateAnnotations.TYPE_REGISTRATION,
				"basicClass",
				modelContext
		);
		this.userType = extractJdkValue( annotation, HibernateAnnotations.TYPE_REGISTRATION, "userType", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public TypeRegistrationAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.basicClass = extractJandexValue(
				annotation,
				HibernateAnnotations.TYPE_REGISTRATION,
				"basicClass",
				modelContext
		);
		this.userType = extractJandexValue(
				annotation,
				HibernateAnnotations.TYPE_REGISTRATION,
				"userType",
				modelContext
		);
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return TypeRegistration.class;
	}

	@Override
	public java.lang.Class<?> basicClass() {
		return basicClass;
	}

	public void basicClass(java.lang.Class<?> value) {
		this.basicClass = value;
	}


	@Override
	public java.lang.Class<? extends org.hibernate.usertype.UserType<?>> userType() {
		return userType;
	}

	public void userType(java.lang.Class<? extends org.hibernate.usertype.UserType<?>> value) {
		this.userType = value;
	}


}
