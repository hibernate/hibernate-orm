/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.DialectOverride;
import org.hibernate.boot.models.annotations.spi.RepeatableContainer;
import org.hibernate.models.spi.ModelsContext;

import static org.hibernate.boot.models.DialectOverrideAnnotations.DIALECT_OVERRIDE_FILTER_DEF_OVERRIDES;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("ClassExplicitlyAnnotation")
public class OverriddenFilterDefOverridesAnnotation
		implements DialectOverride.FilterDefOverrides, RepeatableContainer<DialectOverride.FilterDefs> {
	private DialectOverride.FilterDefs[] value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public OverriddenFilterDefOverridesAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public OverriddenFilterDefOverridesAnnotation(
			DialectOverride.FilterDefOverrides annotation,
			ModelsContext modelContext) {
		this.value = extractJdkValue( annotation, DIALECT_OVERRIDE_FILTER_DEF_OVERRIDES, "value", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public OverriddenFilterDefOverridesAnnotation(
			Map<String, Object> attributeValues,
			ModelsContext modelContext) {
		this.value = (DialectOverride.FilterDefs[]) attributeValues.get( "value" );
	}

	@Override
	public DialectOverride.FilterDefs[] value() {
		return value;
	}

	@Override
	public void value(DialectOverride.FilterDefs[] value) {
		this.value = value;
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return DialectOverride.FilterDefOverrides.class;
	}
}
