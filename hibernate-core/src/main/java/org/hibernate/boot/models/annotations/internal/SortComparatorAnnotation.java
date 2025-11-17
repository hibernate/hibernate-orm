/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.SortComparator;
import org.hibernate.models.spi.ModelsContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class SortComparatorAnnotation implements SortComparator {
	private java.lang.Class<? extends java.util.Comparator<?>> value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public SortComparatorAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public SortComparatorAnnotation(SortComparator annotation, ModelsContext modelContext) {
		this.value = annotation.value();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public SortComparatorAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.value = (Class<? extends java.util.Comparator<?>>) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return SortComparator.class;
	}

	@Override
	public java.lang.Class<? extends java.util.Comparator<?>> value() {
		return value;
	}

	public void value(java.lang.Class<? extends java.util.Comparator<?>> value) {
		this.value = value;
	}


}
