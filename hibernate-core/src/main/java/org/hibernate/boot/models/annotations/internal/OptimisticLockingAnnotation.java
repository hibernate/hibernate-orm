/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.OptimisticLocking;
import org.hibernate.models.spi.SourceModelBuildingContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class OptimisticLockingAnnotation implements OptimisticLocking {
	private org.hibernate.annotations.OptimisticLockType type;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public OptimisticLockingAnnotation(SourceModelBuildingContext modelContext) {
		this.type = org.hibernate.annotations.OptimisticLockType.VERSION;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public OptimisticLockingAnnotation(OptimisticLocking annotation, SourceModelBuildingContext modelContext) {
		this.type = annotation.type();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public OptimisticLockingAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
		this.type = (org.hibernate.annotations.OptimisticLockType) attributeValues.get( "type" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return OptimisticLocking.class;
	}

	@Override
	public org.hibernate.annotations.OptimisticLockType type() {
		return type;
	}

	public void type(org.hibernate.annotations.OptimisticLockType value) {
		this.type = value;
	}


}
