/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.CompositeTypeRegistration;
import org.hibernate.models.spi.SourceModelBuildingContext;

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
		this.embeddableClass = annotation.embeddableClass();
		this.userType = annotation.userType();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public CompositeTypeRegistrationAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
		this.embeddableClass = (Class<?>) attributeValues.get( "embeddableClass" );
		this.userType = (Class<? extends org.hibernate.usertype.CompositeUserType<?>>) attributeValues.get( "userType" );
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
