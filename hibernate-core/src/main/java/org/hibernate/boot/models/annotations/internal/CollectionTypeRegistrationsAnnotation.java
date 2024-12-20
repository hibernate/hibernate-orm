/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.CollectionTypeRegistration;
import org.hibernate.annotations.CollectionTypeRegistrations;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.annotations.spi.RepeatableContainer;
import org.hibernate.models.spi.SourceModelBuildingContext;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class CollectionTypeRegistrationsAnnotation
		implements CollectionTypeRegistrations, RepeatableContainer<CollectionTypeRegistration> {
	private org.hibernate.annotations.CollectionTypeRegistration[] value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public CollectionTypeRegistrationsAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public CollectionTypeRegistrationsAnnotation(
			CollectionTypeRegistrations annotation,
			SourceModelBuildingContext modelContext) {
		this.value = extractJdkValue(
				annotation,
				HibernateAnnotations.COLLECTION_TYPE_REGISTRATIONS,
				"value",
				modelContext
		);
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public CollectionTypeRegistrationsAnnotation(
			Map<String, Object> attributeValues,
			SourceModelBuildingContext modelContext) {
		this.value = (CollectionTypeRegistration[]) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return CollectionTypeRegistrations.class;
	}

	@Override
	public org.hibernate.annotations.CollectionTypeRegistration[] value() {
		return value;
	}

	public void value(org.hibernate.annotations.CollectionTypeRegistration[] value) {
		this.value = value;
	}


}
