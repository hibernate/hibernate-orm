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


import static org.hibernate.boot.models.DialectOverrideAnnotations.DIALECT_OVERRIDE_SQL_INSERTS;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("ClassExplicitlyAnnotation")
public class OverriddenSQLInsertsAnnotation
		implements DialectOverride.SQLInserts, RepeatableContainer<DialectOverride.SQLInsert> {
	private DialectOverride.SQLInsert[] value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public OverriddenSQLInsertsAnnotation(ModelsContext ModelsContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public OverriddenSQLInsertsAnnotation(
			DialectOverride.SQLInserts annotation,
			ModelsContext modelContext) {
		this.value = extractJdkValue( annotation, DIALECT_OVERRIDE_SQL_INSERTS, "value", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public OverriddenSQLInsertsAnnotation(
			Map<String, Object> attributeValues,
			ModelsContext modelContext) {
		this.value = (DialectOverride.SQLInsert[]) attributeValues.get( "value" );
	}

	@Override
	public DialectOverride.SQLInsert[] value() {
		return value;
	}

	@Override
	public void value(DialectOverride.SQLInsert[] value) {
		this.value = value;
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return DialectOverride.SQLInserts.class;
	}
}
