/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.DialectOverride;
import org.hibernate.annotations.DialectOverride.SQLRestrictions;
import org.hibernate.boot.models.DialectOverrideAnnotations;
import org.hibernate.boot.models.annotations.spi.RepeatableContainer;
import org.hibernate.models.spi.ModelsContext;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class OverriddenSQLRestrictionsAnnotation
		implements DialectOverride.SQLRestrictions, RepeatableContainer<DialectOverride.SQLRestriction> {
	private DialectOverride.SQLRestriction[] value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public OverriddenSQLRestrictionsAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public OverriddenSQLRestrictionsAnnotation(SQLRestrictions annotation, ModelsContext modelContext) {
		this.value = extractJdkValue(
				annotation,
				DialectOverrideAnnotations.DIALECT_OVERRIDE_SQL_RESTRICTIONS,
				"value",
				modelContext
		);
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public OverriddenSQLRestrictionsAnnotation(
			Map<String, Object> attributeValues,
			ModelsContext modelContext) {
		this.value = (DialectOverride.SQLRestriction[]) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return SQLRestrictions.class;
	}

	@Override
	public DialectOverride.SQLRestriction[] value() {
		return value;
	}

	public void value(DialectOverride.SQLRestriction[] value) {
		this.value = value;
	}


}
