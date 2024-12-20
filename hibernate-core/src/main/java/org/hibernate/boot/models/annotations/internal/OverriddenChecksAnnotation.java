/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.DialectOverride;
import org.hibernate.boot.models.DialectOverrideAnnotations;
import org.hibernate.boot.models.annotations.spi.RepeatableContainer;
import org.hibernate.models.spi.SourceModelBuildingContext;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("ClassExplicitlyAnnotation")
public class OverriddenChecksAnnotation
		implements DialectOverride.Checks, RepeatableContainer<DialectOverride.Check> {
	private DialectOverride.Check[] value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public OverriddenChecksAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public OverriddenChecksAnnotation(DialectOverride.Checks annotation, SourceModelBuildingContext modelContext) {
		value( extractJdkValue( annotation, DialectOverrideAnnotations.DIALECT_OVERRIDE_CHECKS, "value", modelContext ) );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public OverriddenChecksAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
		value( (DialectOverride.Check[]) attributeValues.get( "value" ) );
	}

	@Override
	public DialectOverride.Check[] value() {
		return value;
	}

	@Override
	public void value(DialectOverride.Check[] value) {
		this.value = value;
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return DialectOverride.Checks.class;
	}
}
