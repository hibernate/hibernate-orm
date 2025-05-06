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

import static org.hibernate.boot.models.DialectOverrideAnnotations.DIALECT_OVERRIDE_COLUMN_DEFAULTS;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("ClassExplicitlyAnnotation")
public class OverriddenColumnDefaultsAnnotation
		implements DialectOverride.ColumnDefaults,
		RepeatableContainer<DialectOverride.ColumnDefault> {
	private DialectOverride.ColumnDefault[] value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public OverriddenColumnDefaultsAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public OverriddenColumnDefaultsAnnotation(
			DialectOverride.ColumnDefaults annotation,
			ModelsContext modelContext) {
		this.value = extractJdkValue( annotation, DIALECT_OVERRIDE_COLUMN_DEFAULTS, "value", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public OverriddenColumnDefaultsAnnotation(
			Map<String, Object> attributeValues,
			ModelsContext modelContext) {
		this.value = (DialectOverride.ColumnDefault[]) attributeValues.get( "value" );
	}

	@Override
	public DialectOverride.ColumnDefault[] value() {
		return value;
	}

	@Override
	public void value(DialectOverride.ColumnDefault[] value) {
		this.value = value;
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return DialectOverride.ColumnDefaults.class;
	}
}
