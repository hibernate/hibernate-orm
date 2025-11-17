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

import static org.hibernate.boot.models.DialectOverrideAnnotations.DIALECT_OVERRIDE_SQL_DELETE_ALLS;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("ClassExplicitlyAnnotation")
public class OverriddenSQLDeleteAllsAnnotation
		implements DialectOverride.SQLDeleteAlls, RepeatableContainer<DialectOverride.SQLDeleteAll> {
	private DialectOverride.SQLDeleteAll[] value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public OverriddenSQLDeleteAllsAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public OverriddenSQLDeleteAllsAnnotation(
			DialectOverride.SQLDeleteAlls annotation,
			ModelsContext modelContext) {
		this.value = extractJdkValue( annotation, DIALECT_OVERRIDE_SQL_DELETE_ALLS, "value", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public OverriddenSQLDeleteAllsAnnotation(
			Map<String, Object> attributeValues,
			ModelsContext modelContext) {
		this.value = (DialectOverride.SQLDeleteAll[]) attributeValues.get( "value" );
	}

	@Override
	public DialectOverride.SQLDeleteAll[] value() {
		return value;
	}

	@Override
	public void value(DialectOverride.SQLDeleteAll[] value) {
		this.value = value;
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return DialectOverride.SQLDeleteAlls.class;
	}
}
