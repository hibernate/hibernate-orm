/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.OptimisticLock;
import org.hibernate.models.spi.ModelsContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class OptimisticLockAnnotation implements OptimisticLock {
	private boolean excluded;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public OptimisticLockAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public OptimisticLockAnnotation(OptimisticLock annotation, ModelsContext modelContext) {
		this.excluded = annotation.excluded();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public OptimisticLockAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.excluded = (boolean) attributeValues.get( "excluded" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return OptimisticLock.class;
	}

	@Override
	public boolean excluded() {
		return excluded;
	}

	public void excluded(boolean value) {
		this.excluded = value;
	}


}
