/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.FilterDefs;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.annotations.spi.RepeatableContainer;
import org.hibernate.models.spi.ModelsContext;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class FilterDefsAnnotation implements FilterDefs, RepeatableContainer<FilterDef> {
	private org.hibernate.annotations.FilterDef[] value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public FilterDefsAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public FilterDefsAnnotation(FilterDefs annotation, ModelsContext modelContext) {
		this.value = extractJdkValue( annotation, HibernateAnnotations.FILTER_DEFS, "value", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public FilterDefsAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.value = (FilterDef[]) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return FilterDefs.class;
	}

	@Override
	public org.hibernate.annotations.FilterDef[] value() {
		return value;
	}

	public void value(org.hibernate.annotations.FilterDef[] value) {
		this.value = value;
	}


}
