/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.Filters;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.annotations.spi.RepeatableContainer;
import org.hibernate.models.spi.ModelsContext;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class FiltersAnnotation implements Filters, RepeatableContainer<Filter> {
	private org.hibernate.annotations.Filter[] value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public FiltersAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public FiltersAnnotation(Filters annotation, ModelsContext modelContext) {
		this.value = extractJdkValue( annotation, HibernateAnnotations.FILTERS, "value", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public FiltersAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.value = (Filter[]) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Filters.class;
	}

	@Override
	public org.hibernate.annotations.Filter[] value() {
		return value;
	}

	public void value(org.hibernate.annotations.Filter[] value) {
		this.value = value;
	}


}
